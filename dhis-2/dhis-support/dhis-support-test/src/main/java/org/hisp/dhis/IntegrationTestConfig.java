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

package org.hisp.dhis;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgisContainerProvider;

import java.util.Properties;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
@Configuration
@ImportResource( locations = { "classpath*:/META-INF/dhis/beans.xml" } )
public class IntegrationTestConfig implements EnvironmentAware
{
    private static final Logger log = LoggerFactory.getLogger(IntegrationTestConfig.class);
    private static final String POSTGRES_DATABASE_NAME = "dhis";

    private static final String POSTGRES_CREDENTIALS = "dhis";

    private Environment environment;

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @Bean( name = "dhisConfigurationProvider" )
    public DhisConfigurationProvider dhisConfigurationProvider()
    {

        PostgresDhisConfigurationProvider dhisConfigurationProvider = new PostgresDhisConfigurationProvider();
        JdbcDatabaseContainer<?> postgreSQLContainer = initContainer();

        Properties properties = new Properties();

        properties.setProperty( "connection.url", postgreSQLContainer.getJdbcUrl() );
        properties.setProperty( "connection.dialect", "org.hisp.dhis.hibernate.dialect.DhisPostgresDialect" );
        properties.setProperty( "connection.driver_class", "org.postgresql.Driver" );
        properties.setProperty( "connection.username", postgreSQLContainer.getUsername() );
        properties.setProperty( "connection.password", postgreSQLContainer.getPassword() );

        dhisConfigurationProvider.addProperties( properties );

        return dhisConfigurationProvider;
    }

    private JdbcDatabaseContainer<?> initContainer()
    {
        JdbcDatabaseContainer<?> postgisContainer = new PostgisContainerProvider()
            .newInstance()
            .withDatabaseName( POSTGRES_DATABASE_NAME )
            .withUsername( POSTGRES_CREDENTIALS )
            .withPassword( POSTGRES_CREDENTIALS );

        postgisContainer.start();


        log.info("Postgis container initialized: " + postgisContainer.getJdbcUrl());

        return postgisContainer;
    }
}
