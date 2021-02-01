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
package org.hisp.dhis.preheat;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.hibernate.HibernateProxyUtils;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class Preheat
{
    /**
     * User to use for import job (important for threaded imports).
     */
    private User user;

    /**
     * Internal map of all objects mapped by identifier => class type => uid.
     */
    private Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> map = new HashMap<>();

    /**
     * Internal map of all default object (like category option combo, etc).
     */
    private Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults = new HashMap<>();

    /**
     * Map of unique columns, mapped by class type => uid => value.
     */
    private Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> uniquenessMap = new HashMap<>();

    /**
     * All periods available.
     */
    private Map<String, Period> periodMap = new HashMap<>();

    /**
     * All periodTypes available.
     */
    private Map<String, PeriodType> periodTypeMap = new HashMap<>();

    /**
     * Map of all required attributes, mapped by class type.
     */
    private Map<Class<?>, Set<String>> mandatoryAttributes = new HashMap<>();

    /**
     * Map of all unique attributes, mapped by class type.
     */
    private Map<Class<?>, Set<String>> uniqueAttributes = new HashMap<>();

    /**
     * Map of all unique attributes values, mapped by class type => attribute
     * uid => object uid.
     */
    private Map<Class<?>, Map<String, Map<String, String>>> uniqueAttributeValues = new HashMap<>();

    public Preheat()
    {
    }

    public User getUser()
    {
        return user;
    }

    public String getUsername()
    {
        return user != null ? user.getUsername() : "system-process";
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public <T extends IdentifiableObject> T get( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass, IdentifiableObject object )
    {
        return get( identifier, klass, identifier.getIdentifier( object ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( PreheatIdentifier identifier,
        Class<? extends IdentifiableObject> klass, String key )
    {
        identifier = getIdentifier( klass, identifier );

        if ( !containsKey( identifier, klass, key ) )
        {
            return null;
        }

        return (T) map.get( identifier ).get( klass ).get( key );
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> List<T> getAll( PreheatIdentifier identifier, List<T> keys )
    {
        List<T> objects = new ArrayList<>();

        for ( T key : keys )
        {
            IdentifiableObject identifiableObject = get( identifier, key );

            if ( identifiableObject != null )
            {
                objects.add( (T) identifiableObject );
            }
        }

        return objects;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> T get( PreheatIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return null;
        }

        T reference = null;

        Class<? extends IdentifiableObject> realClass = HibernateProxyUtils.getRealClass( object );
        identifier = getIdentifier( realClass, identifier );

        if ( PreheatIdentifier.UID == identifier )
        {
            reference = get( PreheatIdentifier.UID, realClass, object.getUid() );
        }

        if ( PreheatIdentifier.CODE == identifier )
        {
            reference = get( PreheatIdentifier.CODE, realClass, object.getCode() );
        }

        return reference;
    }

    public boolean containsKey( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        identifier = getIdentifier( klass, identifier );

        return !(isEmpty() || isEmpty( identifier ) || isEmpty( identifier, klass ))
            && map.get( identifier ).get( klass ).containsKey( key );
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean isEmpty( PreheatIdentifier identifier )
    {
        return !map.containsKey( identifier ) || map.get( identifier ).isEmpty();
    }

    public boolean isEmpty( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass )
    {
        return isEmpty( identifier ) || !map.get( identifier ).containsKey( klass )
            || map.get( identifier ).get( klass ).isEmpty();
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> Preheat put( PreheatIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return this;
        }

        Class<? extends IdentifiableObject> realClass = HibernateProxyUtils.getRealClass( object );
        identifier = getIdentifier( realClass, identifier );

        if ( PreheatIdentifier.UID == identifier )
        {
            if ( !map.containsKey( PreheatIdentifier.UID ) )
            {
                map.put( PreheatIdentifier.UID, new HashMap<>() );
            }

            if ( !map.get( PreheatIdentifier.UID ).containsKey( realClass ) )
            {
                map.get( PreheatIdentifier.UID ).put( realClass, new HashMap<>() );
            }

            if ( User.class.isAssignableFrom( realClass ) )
            {
                if ( !map.get( PreheatIdentifier.UID ).containsKey( UserCredentials.class ) )
                {
                    map.get( PreheatIdentifier.UID ).put( UserCredentials.class, new HashMap<>() );
                }

                User user = (User) object;

                Map<String, IdentifiableObject> identifierMap = map.get( PreheatIdentifier.UID )
                    .get( UserCredentials.class );

                if ( !StringUtils.isEmpty( user.getUid() ) && !identifierMap.containsKey( user.getUid() ) )
                {
                    identifierMap.put( user.getUid(), user.getUserCredentials() );
                }
            }

            Map<String, IdentifiableObject> identifierMap = map.get( PreheatIdentifier.UID ).get( realClass );
            String key = PreheatIdentifier.UID.getIdentifier( object );

            if ( !StringUtils.isEmpty( key ) && !identifierMap.containsKey( key ) )
            {
                identifierMap.put( key, object );
            }
        }

        if ( PreheatIdentifier.CODE == identifier )
        {
            if ( !map.containsKey( PreheatIdentifier.CODE ) )
            {
                map.put( PreheatIdentifier.CODE, new HashMap<>() );
            }

            if ( !map.get( PreheatIdentifier.CODE ).containsKey( realClass ) )
            {
                map.get( PreheatIdentifier.CODE ).put( realClass, new HashMap<>() );
            }

            if ( User.class.isAssignableFrom( realClass ) )
            {
                if ( !map.get( PreheatIdentifier.CODE ).containsKey( UserCredentials.class ) )
                {
                    map.get( PreheatIdentifier.CODE ).put( UserCredentials.class, new HashMap<>() );
                }

                User user = (User) object;

                Map<String, IdentifiableObject> identifierMap = map.get( PreheatIdentifier.CODE )
                    .get( UserCredentials.class );
                identifierMap.put( user.getCode(), user.getUserCredentials() );
            }

            Map<String, IdentifiableObject> identifierMap = map.get( PreheatIdentifier.CODE ).get( realClass );
            String key = PreheatIdentifier.CODE.getIdentifier( object );

            if ( !StringUtils.isEmpty( key ) && !identifierMap.containsKey( key ) )
            {
                identifierMap.put( key, object );
            }
        }

        return this;
    }

    @SuppressWarnings( "unchecked" )
    public <T extends IdentifiableObject> Preheat replace( PreheatIdentifier identifier, T object )
    {
        if ( object == null )
        {
            return this;
        }

        Class<? extends IdentifiableObject> realClass = HibernateProxyUtils.getRealClass( object );
        identifier = getIdentifier( realClass, identifier );

        if ( PreheatIdentifier.UID == identifier )
        {
            if ( !map.containsKey( PreheatIdentifier.UID ) )
            {
                map.put( PreheatIdentifier.UID, new HashMap<>() );
            }

            if ( !map.get( PreheatIdentifier.UID ).containsKey( realClass ) )
            {
                map.get( PreheatIdentifier.UID ).put( realClass, new HashMap<>() );
            }

            if ( User.class.isAssignableFrom( realClass ) )
            {
                if ( !map.get( PreheatIdentifier.UID ).containsKey( UserCredentials.class ) )
                {
                    map.get( PreheatIdentifier.UID ).put( UserCredentials.class, new HashMap<>() );
                }

                User user = (User) object;

                Map<String, IdentifiableObject> identifierMap = map.get( PreheatIdentifier.UID )
                    .get( UserCredentials.class );

                if ( !StringUtils.isEmpty( user.getUid() ) && !identifierMap.containsKey( user.getUid() ) )
                {
                    identifierMap.put( user.getUid(), user.getUserCredentials() );
                }
            }

            Map<String, IdentifiableObject> identifierMap = map.get( PreheatIdentifier.UID ).get( realClass );
            String key = PreheatIdentifier.UID.getIdentifier( object );

            if ( !StringUtils.isEmpty( key ) )
            {
                identifierMap.put( key, object );
            }
        }

        if ( PreheatIdentifier.CODE == identifier )
        {
            if ( !map.containsKey( PreheatIdentifier.CODE ) )
                map.put( PreheatIdentifier.CODE, new HashMap<>() );

            if ( !map.get( PreheatIdentifier.CODE ).containsKey( realClass ) )
                map.get( PreheatIdentifier.CODE ).put( realClass, new HashMap<>() );

            if ( User.class.isAssignableFrom( realClass ) )
            {
                if ( !map.get( PreheatIdentifier.CODE ).containsKey( UserCredentials.class ) )
                {
                    map.get( PreheatIdentifier.CODE ).put( UserCredentials.class, new HashMap<>() );
                }

                User user = (User) object;

                Map<String, IdentifiableObject> identifierMap = map.get( PreheatIdentifier.CODE )
                    .get( UserCredentials.class );
                identifierMap.put( user.getCode(), user.getUserCredentials() );
            }

            Map<String, IdentifiableObject> identifierMap = map.get( PreheatIdentifier.CODE ).get( realClass );
            String key = PreheatIdentifier.CODE.getIdentifier( object );

            if ( !StringUtils.isEmpty( key ) )
            {
                identifierMap.put( key, object );
            }
        }

        return this;
    }

    public <T extends IdentifiableObject> Preheat put( PreheatIdentifier identifier, Collection<T> objects )
    {
        for ( T object : objects )
        {
            if ( isDefault( object ) )
                continue;
            put( identifier, object );
        }

        return this;
    }

    public Preheat remove( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass, String key )
    {
        if ( containsKey( identifier, klass, key ) )
        {
            map.get( identifier ).get( klass ).remove( key );
        }

        return this;
    }

    @SuppressWarnings( "unchecked" )
    public Preheat remove( PreheatIdentifier identifier, IdentifiableObject object )
    {
        Class<? extends IdentifiableObject> klass = HibernateProxyUtils.getRealClass( object );

        if ( PreheatIdentifier.UID == identifier )
        {
            String key = PreheatIdentifier.UID.getIdentifier( object );

            if ( containsKey( PreheatIdentifier.UID, klass, key ) )
            {
                map.get( PreheatIdentifier.UID ).get( klass ).remove( key );
            }
        }

        if ( PreheatIdentifier.CODE == identifier )
        {
            String key = PreheatIdentifier.CODE.getIdentifier( object );

            if ( containsKey( PreheatIdentifier.CODE, klass, key ) )
            {
                map.get( PreheatIdentifier.CODE ).get( klass ).remove( key );
            }
        }

        return this;
    }

    public Preheat remove( PreheatIdentifier identifier, Class<? extends IdentifiableObject> klass,
        Collection<String> keys )
    {
        for ( String key : keys )
        {
            remove( identifier, klass, key );
        }

        return this;
    }

    public Map<PreheatIdentifier, Map<Class<? extends IdentifiableObject>, Map<String, IdentifiableObject>>> getMap()
    {
        return map;
    }

    public Map<Class<? extends IdentifiableObject>, IdentifiableObject> getDefaults()
    {
        return defaults;
    }

    public void setDefaults( Map<Class<? extends IdentifiableObject>, IdentifiableObject> defaults )
    {
        this.defaults = defaults;
    }

    public void setUniquenessMap(
        Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> uniquenessMap )
    {
        this.uniquenessMap = uniquenessMap;
    }

    public Map<Class<? extends IdentifiableObject>, Map<String, Map<Object, String>>> getUniquenessMap()
    {
        return uniquenessMap;
    }

    public Map<String, Period> getPeriodMap()
    {
        return periodMap;
    }

    public void setPeriodMap( Map<String, Period> periodMap )
    {
        this.periodMap = periodMap;
    }

    public Map<String, PeriodType> getPeriodTypeMap()
    {
        return periodTypeMap;
    }

    public void setPeriodTypeMap( Map<String, PeriodType> periodTypeMap )
    {
        this.periodTypeMap = periodTypeMap;
    }

    public Map<Class<?>, Set<String>> getMandatoryAttributes()
    {
        return mandatoryAttributes;
    }

    public void setMandatoryAttributes( Map<Class<?>, Set<String>> mandatoryAttributes )
    {
        this.mandatoryAttributes = mandatoryAttributes;
    }

    public Map<Class<?>, Set<String>> getUniqueAttributes()
    {
        return uniqueAttributes;
    }

    public void setUniqueAttributes( Map<Class<?>, Set<String>> uniqueAttributes )
    {
        this.uniqueAttributes = uniqueAttributes;
    }

    public Map<Class<?>, Map<String, Map<String, String>>> getUniqueAttributeValues()
    {
        return uniqueAttributeValues;
    }

    public void setUniqueAttributeValues( Map<Class<?>, Map<String, Map<String, String>>> uniqueAttributeValues )
    {
        this.uniqueAttributeValues = uniqueAttributeValues;
    }

    @SuppressWarnings( { "rawtypes" } )
    public static boolean isDefaultClass( Class klass )
    {
        return Category.class.isAssignableFrom( klass ) || CategoryOption.class.isAssignableFrom( klass )
            || CategoryCombo.class.isAssignableFrom( klass ) || CategoryOptionCombo.class.isAssignableFrom( klass );
    }

    public static boolean isDefaultObject( IdentifiableObject object )
    {
        return isDefaultClass( HibernateProxyUtils.getRealClass( object ) );
    }

    public boolean isDefault( IdentifiableObject object )
    {
        if ( !isDefaultObject( object ) )
        {
            return false;
        }

        IdentifiableObject defaultObject = getDefaults().get( HibernateProxyUtils.getRealClass( object ) );

        return defaultObject != null && defaultObject.getUid().equals( object.getUid() );
    }

    private PreheatIdentifier getIdentifier( Class<? extends IdentifiableObject> klass, PreheatIdentifier identifier )
    {
        return (klass == User.class || klass == UserCredentials.class
            || klass == UserAuthorityGroup.class)
                ? PreheatIdentifier.UID
                : identifier;
    }
}
