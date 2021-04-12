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
package org.hisp.dhis.gist;

import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.schema.GistPreferences.Flag;
import org.hisp.dhis.schema.GistTransform;
import org.hisp.dhis.schema.Property;

/**
 * Contains the "business logic" aspects of building and running a
 * {@link GistQuery}.
 *
 * @author Jan Bernitt
 */
final class GistLogic
{

    static boolean isIncludedField( Property p, GistAll all )
    {
        Flag included = p.getGistPreferences().getIncluded();
        if ( included == Flag.TRUE )
        {
            return true;
        }
        if ( included == Flag.FALSE )
        {
            return false;
        }
        // AUTO:
        return all.isIncluded( p ) && isAutoIncludedField( p );
    }

    private static boolean isAutoIncludedField( Property p )
    {
        return p.isPersisted()
            && p.isReadable()
            && p.getFieldName() != null;
    }

    static boolean isPersistentCollectionField( Property p )
    {
        return p.isPersisted() && p.isCollection() && (p.isOneToMany() || p.isManyToMany());
    }

    static boolean isPersistentReferenceField( Property p )
    {
        return p.isPersisted() && (p.isOneToOne() || p.isManyToOne());
    }

    static Class<?> getBaseType( Property p )
    {
        return p.isCollection() ? p.getItemKlass() : p.getKlass();
    }

    static boolean isNonNestedPath( String path )
    {
        return path.indexOf( '.' ) < 0;
    }

    static String parentPath( String path )
    {
        return isNonNestedPath( path ) ? "" : path.substring( 0, path.lastIndexOf( '.' ) );
    }

    static String pathOnSameParent( String path, String property )
    {
        return isNonNestedPath( path ) ? property : parentPath( path ) + '.' + property;
    }

    static boolean isHrefProperty( Property p )
    {
        return "href".equals( p.key() ) && p.getKlass() == String.class;
    }

    static boolean isCollectionSizeFilter( Filter filter, Property property )
    {
        return isNonNestedPath( filter.getPropertyPath() )
            && (filter.getOperator().isSizeCompare() ||
                (filter.getOperator().isOrderCompare() && property.isCollection()));
    }

    static GistTransform effectiveTransform( Property property, GistTransform fallback, GistTransform target )
    {
        if ( target == GistTransform.AUTO || !property.getGistPreferences().isAvailableTransformation( target ) )
        {
            target = property.getGistPreferences().getDefaultTransformation();
        }
        if ( target == GistTransform.AUTO )
        {
            target = fallback;
        }
        return target == GistTransform.AUTO ? GistTransform.NONE : target;
    }

    static Number rowCount( Object count )
    {
        return !(count instanceof Number) || ((Number) count).intValue() == -1 ? null : (Number) count;
    }

    private GistLogic()
    {
        throw new UnsupportedOperationException( "utility" );
    }
}