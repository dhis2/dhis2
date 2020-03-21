package org.hisp.dhis.sms.config;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StrSubstitutor;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.outboundmessage.OutboundMessageBatch;
import org.hisp.dhis.outboundmessage.OutboundMessageResponse;
import org.hisp.dhis.sms.outbound.GatewayResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

@Component( "org.hisp.dhis.sms.config.SimplisticHttpGateWay" )
public class SimplisticHttpGateWay
    extends SmsGateway
{
    private static final Log log = LogFactory.getLog( SimplisticHttpGateWay.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final RestTemplate restTemplate;

    public SimplisticHttpGateWay( RestTemplate restTemplate )
    {
        checkNotNull( restTemplate );
        this.restTemplate = restTemplate;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    public boolean accept( SmsGatewayConfig gatewayConfig )
    {
        return gatewayConfig instanceof GenericHttpGatewayConfig;
    }

    @Override
    public List<OutboundMessageResponse> sendBatch( OutboundMessageBatch batch, SmsGatewayConfig gatewayConfig )
    {
        return batch.getMessages()
            .parallelStream()
            .map( m -> send( m.getSubject(), m.getText(), m.getRecipients(), gatewayConfig ) )
            .collect( Collectors.toList() );
    }

    @Override
    public OutboundMessageResponse send( String subject, String text, Set<String> recipients, SmsGatewayConfig config )
    {
        GenericHttpGatewayConfig genericConfig = (GenericHttpGatewayConfig) config;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl( config.getUrlTemplate() );

        ResponseEntity<String> responseEntity = null;

        try
        {
           
            URI url = uriBuilder.build().encode().toUri();
            
            HttpEntity<String> requestEntity = getRequestEntity( genericConfig, text, recipients );

            responseEntity = restTemplate.exchange( url, genericConfig.isUseGet() ? HttpMethod.GET : HttpMethod.POST, requestEntity, String.class );
            
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
        
        return getResponse( responseEntity );
    }
    

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private HttpEntity<String> getRequestEntity( GenericHttpGatewayConfig config, String text, Set<String> recipients )
    {
        List<GenericGatewayParameter> parameters = config.getParameters();

        Map<String, String> valueStore = new HashMap<>();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.put( "Content-type", Collections.singletonList( config.getContentType().getValue() ) );

        for ( GenericGatewayParameter parameter : parameters )
        {
            if ( parameter.isHeader() )
            {
                httpHeaders.put( parameter.getKey(), Collections.singletonList( parameter.getValueForKey() ) );
                continue;
            }

            if ( parameter.isEncode() )
            {
                valueStore.put( parameter.getKey(), encodeUrl( parameter.getValueForKey() ) );
                continue;
            }

            valueStore.put( parameter.getKey(), parameter.getValueForKey() );
        }

        valueStore.put( KEY_TEXT, text );
        valueStore.put( KEY_RECIPIENT, StringUtils.join( recipients, "," ) );

        final StrSubstitutor substitutor = new StrSubstitutor( valueStore ); // Matches on ${...}

        String data = substitutor.replace( config.getConfigurationTemplate() );

        return new HttpEntity<>( data, httpHeaders );
    }

    private String encodeUrl( String value )
    {
        String v = "";
        try
        {
            v = URLEncoder.encode( value, StandardCharsets.UTF_8.toString() );
        }
        catch( UnsupportedEncodingException e )
        {
            DebugUtils.getStackTrace( e );
        }

        return v;
    }

    private OutboundMessageResponse getResponse( ResponseEntity<String> responseEntity )
    {
        OutboundMessageResponse status = new OutboundMessageResponse();

        if ( responseEntity == null || !OK_CODES.contains( responseEntity.getStatusCode() ) )
        {
            log.warn( "Send sms api failed" );
            status.setResponseObject( GatewayResponse.FAILED );
            status.setOk( false );

            return status;
        }

        log.info( "Send sms api succeeded with response: " + responseEntity.getBody() );
        return wrapHttpStatus( responseEntity.getStatusCode() );
    }
}
