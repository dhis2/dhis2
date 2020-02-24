package org.hisp.dhis.dxf2.metadata.objectbundle.validation;

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundle;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.feedback.ObjectReport;
import org.hisp.dhis.feedback.TypeReport;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.preheat.Preheat;
import org.hisp.dhis.preheat.PreheatIdentifier;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.User;

/**
 * @author Luciano Fiandesio
 */
public class UniquenessCheck
    implements
    ValidationCheck
{

    // private TypeReport checkUniqueness( ObjectBundle bundle, Class<? extends
    // IdentifiableObject> klass, List<IdentifiableObject> objects,
    // Preheat preheat, PreheatIdentifier identifier )
    //

    @Override
    public TypeReport check( ObjectBundle bundle, Class<? extends IdentifiableObject> klass,
        List<IdentifiableObject> persistedObjects, List<IdentifiableObject> nonPersistedObjects,
        ImportStrategy importStrategy, ValidationContext ctx )
    {

        TypeReport typeReport = new TypeReport( klass );

        List<IdentifiableObject> objects = selectObjects( persistedObjects, nonPersistedObjects, importStrategy );

        if ( objects.isEmpty() )
        {
            return typeReport;
        }

        Iterator<IdentifiableObject> iterator = objects.iterator();

        while ( iterator.hasNext() )
        {
            IdentifiableObject object = iterator.next();
            List<ErrorReport> errorReports = new ArrayList<>();

            if ( User.class.isInstance( object ) )
            {
                User user = (User) object;
                errorReports.addAll( checkUniqueness( user, bundle.getPreheat(), bundle.getPreheatIdentifier(), ctx ) );
                errorReports.addAll( checkUniqueness( user.getUserCredentials(), bundle.getPreheat(),
                    bundle.getPreheatIdentifier(), ctx ) );
            }
            else
            {
                errorReports = checkUniqueness( object, bundle.getPreheat(), bundle.getPreheatIdentifier(), ctx );
            }

            if ( !errorReports.isEmpty() )
            {
                ObjectReport objectReport = new ObjectReport( object, bundle );
                objectReport.setDisplayName( IdentifiableObjectUtils.getDisplayName( object ) );
                objectReport.addErrorReports( errorReports );
                typeReport.addObjectReport( objectReport );
                typeReport.getStats().incIgnored();

                iterator.remove();
            }
        }

        return typeReport;

    }

    private List<ErrorReport> checkUniqueness( IdentifiableObject object, Preheat preheat, PreheatIdentifier identifier,
        ValidationContext ctx )
    {
        List<ErrorReport> errorReports = new ArrayList<>();

        if ( object == null || preheat.isDefault( object ) )
            return errorReports;

        if ( !preheat.getUniquenessMap().containsKey( object.getClass() ) )
        {
            preheat.getUniquenessMap().put( object.getClass(), new HashMap<>() );
        }

        Map<String, Map<Object, String>> uniquenessMap = preheat.getUniquenessMap().get( object.getClass() );

        Schema schema = ctx.getSchemaService().getDynamicSchema( object.getClass() );
        List<Property> uniqueProperties = schema.getProperties().stream()
            .filter( p -> p.isPersisted() && p.isOwner() && p.isUnique() && p.isSimple() )
            .collect( Collectors.toList() );

        uniqueProperties.forEach( property -> {
            if ( !uniquenessMap.containsKey( property.getName() ) )
            {
                uniquenessMap.put( property.getName(), new HashMap<>() );
            }

            Object value = ReflectionUtils.invokeMethod( object, property.getGetterMethod() );

            if ( value != null )
            {
                String persistedUid = uniquenessMap.get( property.getName() ).get( value );

                if ( persistedUid != null )
                {
                    if ( !object.getUid().equals( persistedUid ) )
                    {
                        errorReports.add( new ErrorReport( object.getClass(), ErrorCode.E5003, property.getName(),
                            value, identifier.getIdentifiersWithName( object ), persistedUid ).setMainId( persistedUid )
                                .setErrorProperty( property.getName() ) );
                    }
                }
                else
                {
                    uniquenessMap.get( property.getName() ).put( value, object.getUid() );
                }
            }
        } );

        return errorReports;
    }

}
