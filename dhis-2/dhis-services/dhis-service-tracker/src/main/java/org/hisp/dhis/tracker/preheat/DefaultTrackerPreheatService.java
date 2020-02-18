package org.hisp.dhis.tracker.preheat;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.timer.SystemTimer;
import org.hisp.dhis.commons.timer.Timer;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.program.*;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierCollector;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service
public class DefaultTrackerPreheatService implements TrackerPreheatService
{
    private final SchemaService schemaService;
    private final QueryService queryService;
    private final IdentifiableObjectManager manager;
    private final CurrentUserService currentUserService;
    private final PeriodStore periodStore;
    private final TrackedEntityInstanceStore trackedEntityInstanceStore;
    private final ProgramInstanceStore programInstanceStore;
    private final ProgramStageInstanceStore programStageInstanceStore;

    public DefaultTrackerPreheatService(
        SchemaService schemaService,
        QueryService queryService,
        IdentifiableObjectManager manager,
        CurrentUserService currentUserService,
        PeriodStore periodStore,
        TrackedEntityInstanceStore trackedEntityInstanceStore,
        ProgramInstanceStore programInstanceStore,
        ProgramStageInstanceStore programStageInstanceStore )
    {
        this.schemaService = schemaService;
        this.queryService = queryService;
        this.manager = manager;
        this.currentUserService = currentUserService;
        this.periodStore = periodStore;
        this.trackedEntityInstanceStore = trackedEntityInstanceStore;
        this.programInstanceStore = programInstanceStore;
        this.programStageInstanceStore = programStageInstanceStore;
    }

    @Override
    public TrackerPreheat preheat( TrackerPreheatParams params )
    {
        Timer timer = new SystemTimer().start();

        TrackerPreheat preheat = new TrackerPreheat();
        preheat.setUser( params.getUser() );
        preheat.setDefaults( manager.getDefaults() );

        if ( preheat.getUser() == null )
        {
            preheat.setUser( currentUserService.getCurrentUser() );
        }

        generateUid( params );

        Map<Class<?>, Set<String>> identifierMap = TrackerIdentifierCollector.collect( params );

        for ( Class<?> klass : identifierMap.keySet() )
        {
            Set<String> identifiers = identifierMap.get( klass ); // assume UID for now, will be done according to IdSchemes
            List<List<String>> splitList = Lists.partition( new ArrayList<>( identifiers ), 20000 );

            if ( klass.isAssignableFrom( TrackedEntity.class ) )
            {
                for ( List<String> ids : splitList )
                {
                    List<org.hisp.dhis.trackedentity.TrackedEntityInstance> trackedEntityInstances =
                        trackedEntityInstanceStore.getByUid( ids, preheat.getUser() );
                    preheat.putTrackedEntities( TrackerIdentifier.UID, trackedEntityInstances );
                }
            }
            else if ( klass.isAssignableFrom( Enrollment.class ) )
            {
                for ( List<String> ids : splitList )
                {
                    List<ProgramInstance> programInstances = programInstanceStore.getByUid( ids, preheat.getUser() );
                    preheat.putEnrollments( TrackerIdentifier.UID, programInstances );
                }
            }
            else if ( klass.isAssignableFrom( Event.class ) )
            {
                for ( List<String> ids : splitList )
                {
                    List<ProgramStageInstance> programStageInstances = programStageInstanceStore.getByUid( ids, preheat.getUser() );
                    preheat.putEvents( TrackerIdentifier.UID, programStageInstances );
                }
            }
            else
            {
                for ( List<String> ids : splitList )
                {
                    Query query = Query.from( schemaService.getDynamicSchema( klass ) );
                    query.setUser( preheat.getUser() );
                    query.add( Restrictions.in( "id", ids ) );
                    List<? extends IdentifiableObject> objects = queryService.query( query );
                    preheat.put( TrackerIdentifier.UID, objects );
                }
            }
        }

        // since TrackedEntityTypes are not really required by incoming payload, and they are small in size/count, we preload them all here
        preheat.put( TrackerIdentifier.UID, manager.getAll( TrackedEntityType.class ) );

        periodStore.getAll().forEach( period -> preheat.getPeriodMap().put( period.getName(), period ) );
        periodStore.getAllPeriodTypes().forEach( periodType -> preheat.getPeriodTypeMap().put( periodType.getName(), periodType ) );

        List<ProgramInstance> programInstances = programInstanceStore.getByType( ProgramType.WITHOUT_REGISTRATION );
        programInstances.forEach( pi -> preheat.putEnrollment( TrackerIdentifier.UID, pi.getProgram().getUid(), pi ) );

        log.info( "(" + preheat.getUsername() + ") Import:TrackerPreheat took " + timer.toString() );

        return preheat;
    }

    @Override
    public void validate( TrackerPreheatParams params )
    {

    }

    private void generateUid( TrackerPreheatParams params )
    {
        params.getTrackedEntities().stream()
            .filter( o -> StringUtils.isEmpty( o.getTrackedEntity() ) )
            .forEach( o -> o.setTrackedEntity( CodeGenerator.generateUid() ) );

        params.getEnrollments().stream()
            .filter( o -> StringUtils.isEmpty( o.getEnrollment() ) )
            .forEach( o -> o.setEnrollment( CodeGenerator.generateUid() ) );

        params.getEvents().stream()
            .filter( o -> StringUtils.isEmpty( o.getEvent() ) )
            .forEach( o -> o.setEvent( CodeGenerator.generateUid() ) );
    }
}
