package org.hisp.dhis.security.action;

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

import com.google.common.collect.ImmutableMap;
import com.opensymphony.xwork2.Action;
import org.apache.struts2.ServletActionContext;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.ui.resourcebundle.ResourceBundleManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static org.hisp.dhis.security.oidc.provider.AzureAdProvider.AZURE_DISPLAY_ALIAS;
import static org.hisp.dhis.security.oidc.provider.AzureAdProvider.AZURE_TENANT;
import static org.hisp.dhis.security.oidc.provider.AzureAdProvider.PROVIDER_PREFIX;

/**
 * @author mortenoh
 */
public class LoginAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DeviceResolver deviceResolver;

    public void setDeviceResolver( DeviceResolver deviceResolver )
    {
        this.deviceResolver = deviceResolver;
    }

    @Autowired
    private ResourceBundleManager resourceBundleManager;

    @Autowired
    private DhisConfigurationProvider configurationProvider;

    @Autowired
    private I18nManager i18nManager;

    // -------------------------------------------------------------------------
    // Input & Output
    // -------------------------------------------------------------------------

    private Boolean failed = false;

    public void setFailed( Boolean failed )
    {
        this.failed = failed;
    }

    public Boolean getFailed()
    {
        return failed;
    }

    private List<Locale> availableLocales;

    public List<Locale> getAvailableLocales()
    {
        return availableLocales;
    }

    private final Map<String, Object> oidcConfig = new HashMap<>();

    public Map<String, Object> getOidcConfig()
    {
        return oidcConfig;
    }

    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------

    @Override
    public String execute()
        throws Exception
    {
        setOidcConfig();

        Device device = deviceResolver.resolveDevice( ServletActionContext.getRequest() );

        ServletActionContext.getResponse().addHeader( "Login-Page", "true" );

        if ( device.isMobile() || device.isTablet() )
        {
            return "mobile";
        }

        availableLocales = new ArrayList<>( resourceBundleManager.getAvailableLocales() );

        return "standard";
    }

    private void setOidcConfig()
    {
        boolean isOidcEnabled = configurationProvider.
            getProperty( ConfigurationKey.OIDC_OAUTH2_LOGIN_ENABLED ).equalsIgnoreCase( "on" );

        if ( !isOidcEnabled )
        {
            return;
        }

        parseGoogle();
        parseAzure();
    }

    private void parseAzure()
    {
        Properties properties = configurationProvider.getProperties();
        String defaultAlias = i18nManager.getI18n().getString( "login_with_azure" );

        List<Map<String, String>> tenants = new ArrayList<>();

        for ( int i = 0; i < 10; i++ )
        {
            String id = properties.getProperty( PROVIDER_PREFIX + i + AZURE_TENANT, "" );

            if ( id.isEmpty() )
            {
                continue;
            }

            String alias = properties.getProperty( PROVIDER_PREFIX + i + AZURE_DISPLAY_ALIAS, defaultAlias );

            tenants.add( ImmutableMap.of( "id", id, "alias", alias ) );
        }

        oidcConfig.put( "azureAdTenants", tenants );
    }

    private void parseGoogle()
    {
        oidcConfig.put( "isGoogleEnabled",
            !configurationProvider.getProperty( ConfigurationKey.OIDC_PROVIDER_GOOGLE_CLIENT_ID ).isEmpty() );
    }
}
