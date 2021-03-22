/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.program.notification;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.notification.ProgramNotificationMessageRenderer;
import org.hisp.dhis.notification.ProgramStageNotificationMessageRenderer;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.sms.config.SmsGateway;
import org.hisp.dhis.system.util.ValidationUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.common.collect.Lists;

/**
 * @author Zubair Asghar
 */
@Slf4j
@Service( "org.hisp.dhis.program.notification.TrackerNotificationWebHookService" )
public class DefaultTrackerNotificationWebHookService implements TrackerNotificationWebHookService
{
    private final ProgramInstanceStore programInstanceStore;

    private final ProgramStageInstanceStore programStageInstanceStore;

    private final RestTemplate restTemplate;

    private final RenderService renderService;

    public DefaultTrackerNotificationWebHookService( @NonNull ProgramInstanceStore programInstanceStore,
        @NonNull ProgramStageInstanceStore programStageInstanceStore,
        @Nonnull RestTemplate restTemplate, @Nonnull RenderService renderService )
    {
        this.programInstanceStore = programInstanceStore;
        this.programStageInstanceStore = programStageInstanceStore;
        this.restTemplate = restTemplate;
        this.renderService = renderService;
    }

    @Override
    @Transactional
    public void handleEnrollment( String pi )
    {
        ProgramInstance instance = programInstanceStore.getByUid( pi );

        if ( instance == null || programInstanceStore.isLinkedToWebHookNotification( instance.getProgram() ) )
        {
            return;
        }

        List<ProgramNotificationTemplate> templates = instance.getProgram().getNotificationTemplates()
            .stream()
            .filter( t -> ProgramNotificationRecipient.WEB_HOOK == t.getNotificationRecipient() )
            .collect( Collectors.toList() );

        Map<String, String> payload = new HashMap<>();
        ProgramNotificationMessageRenderer.VARIABLE_RESOLVERS
            .forEach( ( key, value ) -> payload.put( key.name(), value.apply( instance ) ) );
        sendPost( templates, renderService.toJsonAsString( payload ) );
    }

    @Override
    @Transactional
    public void handleEvent( String psi )
    {
        ProgramStageInstance instance = programStageInstanceStore.getByUid( psi );

        if ( instance == null || programStageInstanceStore.isLinkedToWebHookNotification( instance.getProgramStage() ) )
        {
            return;
        }

        List<ProgramNotificationTemplate> templates = instance.getProgramStage().getNotificationTemplates()
            .stream()
            .filter( t -> t.getNotificationRecipient() == ProgramNotificationRecipient.WEB_HOOK )
            .collect( Collectors.toList() );

        Map<String, String> payload = new HashMap<>();
        ProgramStageNotificationMessageRenderer.VARIABLE_RESOLVERS
            .forEach( ( key, value ) -> payload.put( key.name(), value.apply( instance ) ) );
        sendPost( templates, renderService.toJsonAsString( payload ) );
    }

    private void sendPost( List<ProgramNotificationTemplate> templates, String payload )
    {
        ResponseEntity<String> responseEntity = null;

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put( "Content-type", Lists.newArrayList( "application/json" ) );

        HttpEntity<String> httpEntity = new HttpEntity<>( payload, httpHeaders );

        for ( ProgramNotificationTemplate t : templates )
        {
            if ( !ValidationUtils.urlIsValid( t.getMessageTemplate() ) )
            {
                log.error( String.format( "Webhook url: %s is invalid for template: %s", t.getMessageTemplate(),
                    t.getUid() ) );
                continue;
            }

            URI uri = UriComponentsBuilder.fromHttpUrl( t.getMessageTemplate() ).build().encode().toUri();

            try
            {
                responseEntity = restTemplate.exchange( uri, HttpMethod.POST, httpEntity, String.class );
            }
            catch ( HttpClientErrorException ex )
            {
                log.error( "Client error " + ex.getMessage() );
            }
            catch ( HttpServerErrorException ex )
            {
                log.error( "Server error " + ex.getMessage() );
            }
            catch ( Exception ex )
            {
                log.error( "Error " + ex.getMessage() );
            }

            if ( responseEntity != null && SmsGateway.OK_CODES.contains( responseEntity.getStatusCode() ) )
            {
                log.info( String.format( "Post request successful for url: %s and template: %s", uri, t.getUid() ) );
            }
            else
            {
                log.info( String.format( "Post request failed for url: %s and template: %s", uri, t.getUid() ) );
            }
        }
    }
}
