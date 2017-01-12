package org.hisp.dhis.appmanager;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.ant.compress.taskdefs.Unzip;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.datavalue.DefaultDataValueService;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.hisp.dhis.keyjsonvalue.KeyJsonValueService;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.scriptlibrary.AppScriptLibrary;
import org.hisp.dhis.scriptlibrary.ScriptLibrary;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.annotation.PostConstruct;
import javax.json.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * @author Saptarshi Purkayastha
 */
public class DefaultAppManager
    implements AppManager
{
    private static final Log log = LogFactory.getLog ( DefaultDataValueService.class );

    private static final String MANIFEST_FILENAME = "manifest.webapp";

    /**
     * In-memory singleton list holding state for apps.
     */
    private List<App> apps = new ArrayList<>();

    /**
     * Mapping dataStore-namespaces and apps
     */
    private HashMap<String, App> appNamespaces = new HashMap<>();

    private HashMap<String,JsonNode> manifests = new HashMap<>();

    private Map<String, ScriptLibrary> scriptLibraries = new HashMap<String, ScriptLibrary>();

    @PostConstruct
    private void init()
    {
        verifyAppFolder();

        reloadApps();
    }

    @Autowired
    private SystemSettingManager appSettingManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private LocationManager locationManager;

    @Autowired
    private KeyJsonValueService keyJsonValueService;

    private JsonFactory jsonFactory = new JsonFactory();
    // -------------------------------------------------------------------------
    // AppManagerService implementation
    // -------------------------------------------------------------------------

    @Override
    public List<App> getApps ( String contextPath )
    {
        apps.forEach ( a -> a.init ( contextPath ) );

        return apps;
    }

    @Override
    public List<App> getAppsByType ( AppType appType, Collection<App> apps )
    {
        return apps.stream()
               .filter ( app -> app.getAppType() == appType )
               .collect ( Collectors.toList() );
    }

    @Override
    public List<App> getAppsByName ( final String name, Collection<App> apps, final String operator )
    {
        return apps.stream().filter ( app -> (
                                          ( "ilike".equalsIgnoreCase ( operator ) && app.getName().toLowerCase().contains ( name.toLowerCase() ) ) ||
                                          ( "eq".equalsIgnoreCase ( operator ) && app.getName().equals ( name ) ) ) ).
               collect ( Collectors.toList() );
    }

    @Override
    public List<App> filterApps ( List<String> filters, String contextPath )
    {
        List<App> apps = getApps ( contextPath );
        Set<App> returnList = new HashSet<> ( apps );

        for ( String filter : filters )
        {
            String[] split = filter.split ( ":" );

            if ( split.length != 3 )
            {
                throw new QueryParserException ( "Invalid filter: " + filter );
            }

            if ( "appType".equalsIgnoreCase ( split[0] ) )
            {
                String appType = split[2] != null ? split[2].toUpperCase() : null;
                returnList.retainAll ( getAppsByType ( AppType.valueOf ( appType ), returnList ) );
            }

            else if ( "name".equalsIgnoreCase ( split[0] ) )
            {
                returnList.retainAll ( getAppsByName ( split[2], returnList, split[1] ) );
            }
        }

        return new ArrayList<> ( returnList );
    }

    @Override
    public App getApp ( String key, String contextPath )
    {
        List<App> apps = getApps ( contextPath );

        for ( App app : apps )
        {
            if ( key.equals ( app.getKey() ) )
            {
                return app;
            }
        }

        return null;
    }


    public ScriptLibrary getScriptLibrary ( App app )
    {
        String key = app.getKey();
        log.info("Retreiving script libary for " + app);
        if ( scriptLibraries.containsKey ( key ) )
        {
            return scriptLibraries.get ( key );
        }

        else
        {
            ScriptLibrary sl =  new AppScriptLibrary( app, this );
            log.info("Creating script library for " + app.getKey());
            scriptLibraries.put ( key, sl );
            return sl;
        }
    }

    public long getLastModified(String appKey) {
        try {
            Resource resource = findResource(appKey, MANIFEST_FILENAME);
            return  resource.lastModified();
        } catch (Exception e) {
            return -1;
        }
    }


    public JsonNode retrieveManifestInfo (String appKey, String[] path )
    {
        log.info("Retrieve manifest info for " + appKey + "\nPath=" + String.join("/",path));
        try
        {
            JsonNode node;
            if (manifests.containsKey(appKey)) {
                log.info("Using exist manifest");
                node = manifests.get(appKey);
            } else {
                log.info("Loading " + MANIFEST_FILENAME + " for " + appKey);
                Resource manifest = findResource(appKey, MANIFEST_FILENAME);
                log.info("Found manifest: " + manifest.getURI());
                JsonParser jParser = jsonFactory.createParser(manifest.getInputStream());
                log.info("Parsed manifest");
                ObjectMapper mapper = new ObjectMapper();
                log.info("Created object mapper");
                node = mapper.readTree(jParser);
                log.info("Reading tree");
                manifests.put(appKey,node);
                log.info("Adding to manifests cache");
            }

            for ( int i = 0; i < path.length ; i++ ) //walk the binding path
            {
                log.info("Processing path component (" + path[i] + ")");
                if (node.isArray()) {
                    int num = Integer.parseInt ( path[i] );
                    if (! node.has(num)) {
                        throw new Exception("path component " + path[i] + " not found");
                    }
                    node = node.get( num );
                } else if (node.isObject()) {
                    if (! node.has(path[i])) {
                        throw new Exception("path component " + path[i] + " not found");
                    }
                    node = node.get(path[i]);
                } else if (i != path.length - 1) {
                    log.info("Path component not found (" + path[i] + ")");
                    return null; //we are not in the correct path
                }


            }
            log.info("walked full path");
            return node;
        }

        catch ( Exception e )
        {
            log.info ( "Could not access manifest for " + appKey + ":"  + e.toString() );
            return null; //return empty object
        }
    }



    public Resource findResource (  String appKey, String resourceName )
            throws IOException
    {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Iterable<Resource> locations = Lists.newArrayList();
        try
        {
            locations = Lists.newArrayList (
                    resourceLoader.getResource ( "file:" + getAppFolderPath() + "/" + appKey   + "/" ),
                    resourceLoader.getResource ( "classpath*:/apps/" + appKey + "/" )
            );

        }
        catch ( Exception e )
        {
            log.info ( "Could not init App resource locations" + e.toString() );

        }

        log.info ( "Looking for resource [" + resourceName + "] in " + appKey );

        for ( Resource location : locations )
        {
            Resource resource = location.createRelative ( resourceName );
            log.info ( "Checking " + resource.toString() + " under " + location.toString() );

            if ( resource.exists() && resource.isReadable() )
            {
                log.info ( "Found " + resource.toString() );
                return resource;
            }
        }

        return null;
    }


    @Override
    public List<App> getAccessibleApps ( String contextPath )
    {
        User user = currentUserService.getCurrentUser();

        return getApps ( contextPath ).stream().filter ( a -> this.isAccessible ( a, user ) ).collect ( Collectors.toList() );
    }

    @Override
    public AppStatus installApp ( File file, String fileName )
    {
        try
        {
            // -----------------------------------------------------------------
            // Parse ZIP file and it's manifest.webapp file.
            // -----------------------------------------------------------------

            ZipFile zip = new ZipFile ( file );

            ZipEntry entry = zip.getEntry ( MANIFEST_FILENAME );
            InputStream inputStream = zip.getInputStream ( entry );
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure ( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

            App app = mapper.readValue ( inputStream, App.class );

            // -----------------------------------------------------------------
            // Check for namespace and if it's already taken by another app
            // -----------------------------------------------------------------

            String namespace = app.getActivities().getDhis().getNamespace();

            if ( namespace != null && ( this.appNamespaces.containsKey ( namespace ) &&
                                        !app.equals ( appNamespaces.get ( namespace ) ) ) )
            {
                zip.close();
                return AppStatus.NAMESPACE_TAKEN;
            }

            // -----------------------------------------------------------------
            // Delete if app is already installed, assuming app update so no
            // data is deleted
            // -----------------------------------------------------------------

            deleteApp ( app.getName(), false );

            // -----------------------------------------------------------------
            // Unzip the app
            // -----------------------------------------------------------------

            log.info ( "Installing app, namespace: " + namespace );

            String dest = getAppFolderPath() + File.separator + fileName.substring ( 0, fileName.lastIndexOf ( '.' ) );
            Unzip unzip = new Unzip();
            unzip.setSrc ( file );
            unzip.setDest ( new File ( dest ) );
            unzip.execute();

            log.info ( "Installed app: " + app );

            // -----------------------------------------------------------------
            // Installation complete. Closing zip, reloading apps and return OK
            // -----------------------------------------------------------------

            zip.close();

            reloadApps();

            return AppStatus.OK;

        }

        catch ( ZipException e )
        {
            return AppStatus.INVALID_ZIP_FORMAT;
        }

        catch ( JsonParseException e )
        {
            return AppStatus.INVALID_MANIFEST_JSON;
        }

        catch ( JsonMappingException e )
        {
            return AppStatus.INVALID_MANIFEST_JSON;
        }

        catch ( IOException e )
        {
            return AppStatus.INSTALLATION_FAILED;
        }
    }

    @Override
    public boolean exists ( String appKey )
    {
        for ( App app : getApps ( null ) )
        {
            if ( app.getName().equals ( appKey ) || app.getFolderName().equals ( appKey ) )
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean deleteApp ( String name, boolean deleteAppData )
    {

        for ( App app : getApps ( null ) )
        {
            if ( app.getName().equals ( name ) || app.getFolderName().equals ( name ) )
            {
                try
                {
                    String key = app.getKey();
                    if (manifests.containsKey(key)) {
                        manifests.remove(key);
                    }
                    if (scriptLibraries.containsKey(key)) {
                        scriptLibraries.remove(key);
                    }
                    String folderPath = getAppFolderPath() + File.separator + app.getFolderName();
                    FileUtils.forceDelete ( new File ( folderPath ) );

                    // Delete if deleteAppData is true and a namespace associated with the app exists

                    if ( deleteAppData && appNamespaces.containsValue ( app ) )
                    {
                        appNamespaces.forEach ( ( namespace, app1 ) ->
                        {
                            if ( app1 == app )
                            {
                                keyJsonValueService.deleteNamespace ( namespace );
                            }
                        } );
                    }

                    return true;
                }

                catch ( IOException ex )
                {
                    log.error ( "Could not delete app: " + name, ex );
                    return false;
                }

                finally
                {
                    reloadApps(); // Reload app state
                }
            }
        }

        return false;
    }



    @Override
    public String getAppFolderPath()
    {
        try
        {
            return locationManager.getExternalDirectoryPath() + APPS_DIR;
        }

        catch ( LocationManagerException ex )
        {
            log.info ( "Could not get app folder path, external directory not set" );
            return null;
        }
    }

    @Override
    public String getAppStoreUrl()
    {
        return StringUtils.trimToNull ( ( String ) appSettingManager.getSystemSetting ( SettingKey.APP_STORE_URL ) );
    }

    @Override
    public void setAppStoreUrl ( String appStoreUrl )
    {
        appSettingManager.saveSystemSetting ( SettingKey.APP_STORE_URL, appStoreUrl );
    }

    /**
     * Sets the list of apps with detected apps from the file system.
     */
    @Override
    public void reloadApps()
    {
        List<App> appList = new ArrayList<>();
        HashMap<String, App> appNamespaces = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure ( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

        if ( null != getAppFolderPath() )
        {
            File appFolderPath = new File ( getAppFolderPath() );

            if ( appFolderPath.isDirectory() )
            {
                File[] listFiles = appFolderPath.listFiles();

                if ( listFiles != null )
                {
                    for ( File folder : listFiles )
                    {
                        if ( folder.isDirectory() )
                        {
                            File appManifest = new File ( folder, "manifest.webapp" );

                            if ( appManifest.exists() )
                            {
                                try
                                {
                                    App app = mapper.readValue ( appManifest, App.class );
                                    app.setFolderName ( folder.getName() );
                                    appList.add ( app );

                                    String appNamespace = app.getActivities().getDhis().getNamespace();

                                    if ( appNamespace != null )
                                    {
                                        appNamespaces.put ( appNamespace, app );
                                    }
                                }

                                catch ( IOException ex )
                                {
                                    log.error ( ex.getLocalizedMessage(), ex );
                                }
                            }
                        }
                    }
                }
            }
        }

        this.apps = appList;
        this.appNamespaces = appNamespaces;
        manifests = new HashMap<String,JsonNode>(); //clear out any manifests that were already parsed
        scriptLibraries = new HashMap<String,ScriptLibrary>();
        log.info ( "Detected apps: " + apps );
    }

    @Override
    public boolean isAccessible ( App app )
    {
        return isAccessible ( app, currentUserService.getCurrentUser() );
    }

    @Override
    public boolean isAccessible ( App app, User user )
    {
        if ( user == null || user.getUserCredentials() == null || app == null || app.getName() == null )
        {
            return false;
        }

        UserCredentials userCredentials = user.getUserCredentials();

        return userCredentials.getAllAuthorities().contains ( "ALL" ) ||
               userCredentials.getAllAuthorities().contains ( "M_dhis-web-maintenance-appmanager" ) ||
               userCredentials.getAllAuthorities().contains ( "See " + app.getName().trim() );
    }

    @Override
    public App getAppByNamespace ( String namespace )
    {
        return appNamespaces.get ( namespace );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Creates the app folder if it does not exist already.
     */
    private void verifyAppFolder()
    {
        String appFolderPath = getAppFolderPath();

        if ( appFolderPath != null && !appFolderPath.isEmpty() )
        {
            try
            {
                File folder = new File ( appFolderPath );

                if ( !folder.exists() )
                {
                    FileUtils.forceMkdir ( folder );
                }
            }

            catch ( IOException ex )
            {
                log.error ( ex.getMessage(), ex );
            }
        }
    }

}
