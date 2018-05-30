package org.hisp.dhis.dxf2.sync;

/*
 * Copyright (c) 2004-2018, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstances;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.CodecUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David Katuscak
 */
public class TrackerSynchronization
{
    private static final Log log = LogFactory.getLog( TrackerSynchronization.class );

    private final TrackedEntityInstanceService teiService;

    private final SystemSettingManager systemSettingManager;

    private final RestTemplate restTemplate;

    private final RenderService renderService;

    @Autowired
    public TrackerSynchronization( TrackedEntityInstanceService teiService, SystemSettingManager systemSettingManager, RestTemplate restTemplate, RenderService renderService )
    {
        this.teiService = teiService;
        this.systemSettingManager = systemSettingManager;
        this.restTemplate = restTemplate;
        this.renderService = renderService;
    }

    public void syncTrackerProgramData()
    {
        if ( !SyncUtils.testServerAvailability( systemSettingManager, restTemplate ).isAvailable() )
        {
            return;
        }

        log.info( "Starting tracker data synchronization job." );

        final Date startTime = new Date();

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();

        queryParams.setIncludeDeleted( true );
        queryParams.setSynchronizationQuery( true );

        int objectsToSync = teiService.getTrackedEntityInstanceCount( queryParams, true, true );

        if ( objectsToSync == 0 )
        {
            log.info( "Nothing to sync." );
            return;
        }

        log.info( objectsToSync + " TEIs to sync were found." );

        final String username = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_USERNAME );
        final String password = (String) systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_PASSWORD );
        final int trackerSyncPageSize = (int) systemSettingManager.getSystemSetting( SettingKey.TRACKER_SYNC_PAGE_SIZE );
        final int pages = (objectsToSync / trackerSyncPageSize) + ((objectsToSync % trackerSyncPageSize == 0) ? 0 : 1);  //Have to use this as (int) Match.ceil doesn't work until I am casting int to double
        final String syncUrl = systemSettingManager.getSystemSetting( SettingKey.REMOTE_INSTANCE_URL ) + SyncEndpoint.TEIS_ENDPOINT.getPath() + SyncUtils.IMPORT_STRATEGY_SYNC_SUFFIX;

        log.info( "Remote server URL for Tracker POST sync: " + syncUrl );
        log.info( "Tracker sync job has " + pages + " pages to sync. With page size: " + trackerSyncPageSize );

        //TODO: Add functionality (to the query/queryParams) to order by timestamp? (Then I can always start by the oldest one and move to the newest ones.)

        queryParams.setPageSize( trackerSyncPageSize );
        TrackedEntityInstanceParams params = TrackedEntityInstanceParams.TRUE;
        boolean syncResult = true;

        for ( int i = 1; i <= pages; i++ )
        {
            queryParams.setPage( i );

            List<TrackedEntityInstance> dtoTeis = teiService.getTrackedEntityInstances( queryParams, params, true );
            filterOutNonSynchronizableAttributes( dtoTeis );
            log.info( String.format( "Syncing page %d, page size is: %d", i, trackerSyncPageSize ) );

            if ( log.isDebugEnabled() )
            {
                log.debug( "TEIs that are going to be synced are: " + dtoTeis );
            }

            if ( sendTrackerSyncRequest( dtoTeis, username, password ) )
            {
                List<String> teiUIDs = dtoTeis.stream().map( TrackedEntityInstance::getTrackedEntityInstance ).collect( Collectors.toList() );
                log.info( "The lastSynced flag of these TEIs should be updated to: " + teiUIDs );
                teiService.updateTrackedEntityInstancesSyncTimestamp( teiUIDs, startTime );
            }
            else
            {
                syncResult = false;
            }
        }

        if ( syncResult )
        {
            long syncDuration = System.currentTimeMillis() - startTime.getTime();
            log.info( "SUCCESS! Tracker sync was successfully done! It took " + syncDuration + " ms." );
        }
    }

    private void filterOutNonSynchronizableAttributes( List<TrackedEntityInstance> dtoTeis )
    {
        for ( TrackedEntityInstance tei : dtoTeis )
        {
            tei.setAttributes( tei.getAttributes().stream()
                .filter( attr -> !attr.getSkipSynchronization() )
                .collect( Collectors.toList() ) );
        }
    }

    private boolean sendTrackerSyncRequest( List<TrackedEntityInstance> dtoTeis, String username, String password )
    {
        TrackedEntityInstances teis = new TrackedEntityInstances();
        teis.setTrackedEntityInstances( dtoTeis );

        final RequestCallback requestCallback = request ->
        {
            request.getHeaders().setContentType( MediaType.APPLICATION_JSON );
            request.getHeaders().add( SyncUtils.HEADER_AUTHORIZATION, CodecUtils.getBasicAuthString( username, password ) );
            renderService.toJson( request.getBody(), teis );
        };

        return SyncUtils.sendSyncRequest( systemSettingManager, restTemplate, requestCallback, SyncEndpoint.TEIS_ENDPOINT );
    }
}
