package org.hisp.dhis.sms.config;

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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hisp.dhis.common.CodeGenerator;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Zubair <rajazubair.asghar@gmail.com>
 */
@Slf4j
@Service( "org.hisp.dhis.sms.config.GatewayAdministrationService" )
public class DefaultGatewayAdministrationService
    implements GatewayAdministrationService
{
    private Map<String, SmsGatewayConfig> gatewayConfigurationMap = new HashMap<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final SmsConfigurationManager smsConfigurationManager;

    private final PBEStringEncryptor pbeStringEncryptor;

    public DefaultGatewayAdministrationService( SmsConfigurationManager smsConfigurationManager,
        @Qualifier( "tripleDesStringEncryptor" ) PBEStringEncryptor pbeStringEncryptor )
    {
        checkNotNull( smsConfigurationManager );
        checkNotNull( pbeStringEncryptor );

        this.smsConfigurationManager = smsConfigurationManager;
        this.pbeStringEncryptor = pbeStringEncryptor;
    }

    // -------------------------------------------------------------------------
    // GatewayAdministrationService implementation
    // -------------------------------------------------------------------------

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        initializeSmsConfig();
    }

    @Override
    public void setDefaultGateway( String uid )
    {
        setDefaultGateway( getByUid( uid ) );
    }

    @Override
    public void setDefaultGateway( SmsGatewayConfig config )
    {
        SmsConfiguration configuration = getSmsConfiguration();

        List<SmsGatewayConfig> persistedConfigs = configuration.getGateways();

        List<SmsGatewayConfig> updatedConfigs = new ArrayList<>();

        for ( SmsGatewayConfig persisted : persistedConfigs )
        {
            if ( persisted.equals( config ) )
            {
                persisted.setDefault( true );
            }
            else
            {
                persisted.setDefault( false );
            }

            updatedConfigs.add( persisted );
        }

        configuration.setGateways( updatedConfigs );

        smsConfigurationManager.updateSmsConfiguration( configuration );
        initializeSmsConfig();
    }

    @Override
    public boolean addOrUpdateGateway( SmsGatewayConfig updatedConfig, Class<?> klass )
    {
        SmsConfiguration smsConfiguration = getSmsConfiguration();

        if ( smsConfiguration != null )
        {
            SmsGatewayConfig persistedConfig = smsConfigurationManager.checkInstanceOfGateway( klass );

            int index = -1;

            if ( persistedConfig != null )
            {
                index = smsConfiguration.getGateways().indexOf( persistedConfig );
            }

            updatedConfig.setUid( CodeGenerator.generateCode( 10 ) );

            if ( getDefaultGateway() == null )
            {
                updatedConfig.setDefault( true );
            }

            if ( index >= 0 )
            {
                updatedConfig.setUid( persistedConfig.getUid() );
                updatedConfig.setDefault( persistedConfig.isDefault() );

                if ( persistedConfig.getPassword() != null )
                {
                    if ( persistedConfig.getPassword().equals( updatedConfig.getPassword() ) )
                    {
                        updatedConfig.setPassword( pbeStringEncryptor.encrypt( updatedConfig.getPassword() ) );
                    }
                }

                if ( persistedConfig instanceof GenericHttpGatewayConfig )
                {
                    List<GenericGatewayParameter> newList = new ArrayList<>();

                    GenericHttpGatewayConfig persistedGenericConfig = (GenericHttpGatewayConfig) persistedConfig;
                    GenericHttpGatewayConfig updatedGenericConfig = (GenericHttpGatewayConfig) updatedConfig;

                    List<GenericGatewayParameter> persistedList = persistedGenericConfig.getParameters()
                            .stream().filter( GenericGatewayParameter::isConfidential )
                            .collect( Collectors.toList() );

                    List<GenericGatewayParameter> updatedList = updatedGenericConfig.getParameters()
                            .stream().filter( GenericGatewayParameter::isConfidential )
                            .collect( Collectors.toList() );

                    for ( GenericGatewayParameter p: updatedList )
                    {
                        if ( !isPresent( persistedList, p ) )
                        {
                            p.setValue( pbeStringEncryptor.encrypt( p.getValue() ) );
                        }

                        newList.add( p );
                    }

                    updatedGenericConfig.setParameters( Stream
                            .concat( updatedGenericConfig.getParameters().stream(), newList.stream() )
                            .distinct().collect( Collectors.toList() ) );

                    updatedConfig = updatedGenericConfig;
                    persistedConfig = updatedConfig;
                }

                smsConfiguration.getGateways().set( index, persistedConfig );
            }
            else
            {
                if ( updatedConfig instanceof GenericHttpGatewayConfig )
                {
                    ((GenericHttpGatewayConfig) updatedConfig).getParameters().stream()
                            .filter( GenericGatewayParameter::isConfidential )
                            .forEach( p -> p.setValue( pbeStringEncryptor.encrypt( p.getValue() ) ) );
                }

                updatedConfig.setPassword( pbeStringEncryptor.encrypt( updatedConfig.getPassword() ) );

                persistedConfig = updatedConfig;
                smsConfiguration.getGateways().add( persistedConfig );
            }

            smsConfigurationManager.updateSmsConfiguration( smsConfiguration );
            initializeSmsConfig();

            return true;
        }

        return false;
    }

    @Override
    public boolean addGateway( SmsGatewayConfig config )
    {
        if ( config != null )
        {
            config.setUid( CodeGenerator.generateCode( 10 )  );

            SmsConfiguration smsConfiguration = getSmsConfiguration();

            if ( smsConfiguration.getGateways().isEmpty() )
            {
                config.setDefault( true );
            }
            else
            {
                config.setDefault( false );
            }

            smsConfiguration.getGateways().add( config );

            smsConfigurationManager.updateSmsConfiguration( smsConfiguration );
            initializeSmsConfig();

            return true;
        }

        return false;
    }

    @Override
    public void updateGateway( SmsGatewayConfig persisted, SmsGatewayConfig updatedConfig )
    {
        if ( persisted == null || updatedConfig == null )
        {
            log.warn( "Gateway configurations cannot be null" );
            return;
        }

        updatedConfig.setUid( persisted.getUid() );
        updatedConfig.setDefault( persisted.isDefault() );

        SmsConfiguration configuration = getSmsConfiguration();

        configuration.getGateways().remove( persisted );

        configuration.getGateways().add( updatedConfig );

        smsConfigurationManager.updateSmsConfiguration( configuration );
    }

    @Override
    public boolean removeGatewayByUid( String uid )
    {
        SmsConfiguration smsConfiguration = getSmsConfiguration();

        for ( SmsGatewayConfig gateway : smsConfiguration.getGateways() )
        {
            if ( gateway.getUid().equals( uid ) )
            {
                smsConfiguration.getGateways().remove( gateway );

                if( gateway.isDefault() )
                {
                    if (  !smsConfiguration.getGateways().isEmpty() )
                    {
                        smsConfiguration.getGateways().get( 0 ).setDefault( true );
                    }
                }

                smsConfigurationManager.updateSmsConfiguration( smsConfiguration );
                initializeSmsConfig();

                return true;
            }
        }

        return false;
    }

    @Override
    public SmsGatewayConfig getByUid( String uid )
    {
        List<SmsGatewayConfig> list = getSmsConfiguration().getGateways();

        if ( !list.isEmpty() )
        {
            for ( SmsGatewayConfig gw : list )
            {
                if ( gw.getUid().equals( uid ) )
                {
                    return gw;
                }
            }
        }

        return null;
    }

    @Override
    public SmsGatewayConfig getDefaultGateway()
    {
        List<SmsGatewayConfig> list = getSmsConfiguration().getGateways();

        if (  !list.isEmpty() )
        {
            for ( SmsGatewayConfig gw : list )
            {
                if ( gw.isDefault() )
                {
                    return gw;
                }
            }
        }

        return null;
    }

    @Override
    public boolean hasDefaultGateway()
    {
        return getDefaultGateway() != null;
    }

    @Override
    public boolean loadGatewayConfigurationMap( SmsConfiguration smsConfiguration )
    {
        gatewayConfigurationMap.clear();

        List<SmsGatewayConfig> gatewayList = smsConfiguration.getGateways();

        if ( !gatewayList.isEmpty() )
        {
            for ( SmsGatewayConfig smsGatewayConfig : gatewayList )
            {
                gatewayConfigurationMap.put( smsGatewayConfig.getName(), smsGatewayConfig );
            }

            return true;
        }

        return false;
    }

    @Override
    public Class<? extends SmsGatewayConfig> getGatewayType( SmsGatewayConfig config )
    {
        if ( config == null )
        {
            return null;
        }

        SmsConfiguration configuration = getSmsConfiguration();

        for ( SmsGatewayConfig gatewayConfig : configuration.getGateways() )
        {
            if ( gatewayConfig.getUid().equals( config.getUid() ) )
            {
                return gatewayConfig.getClass();
            }
        }

        return null;
    }

    @Override
    public Map<String, SmsGatewayConfig> getGatewayConfigurationMap()
    {
        return gatewayConfigurationMap;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private SmsConfiguration getSmsConfiguration()
    {
        SmsConfiguration smsConfiguration = smsConfigurationManager.getSmsConfiguration();

        if ( smsConfiguration != null )
        {
            return smsConfiguration;
        }

        return new SmsConfiguration();
    }

    private void initializeSmsConfig()
    {
        SmsConfiguration smsConfiguration = getSmsConfiguration();

        if ( smsConfiguration == null )
        {
            log.info( "SMS configuration not found" );
            return;
        }

        List<SmsGatewayConfig> gatewayList = smsConfiguration.getGateways();

        if ( gatewayList == null || gatewayList.isEmpty() )
        {
            log.info( "No Gateway configuration not found" );

            loadGatewayConfigurationMap( smsConfiguration );
            return;
        }

        log.info( "Gateway configuration found: " + gatewayList );

        loadGatewayConfigurationMap( smsConfiguration );
    }

    private boolean isPresent( List<GenericGatewayParameter> parameters, GenericGatewayParameter parameter )
    {
        for ( GenericGatewayParameter p : parameters )
        {
            if ( p.equals( parameter ) )
            {
                return true;
            }
        }

        return false;
    }
}
