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
package org.hisp.dhis.schema.introspection;

import static java.util.Arrays.asList;
import static org.hisp.dhis.system.util.AnnotationUtils.getAnnotation;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.Map;

import org.hisp.dhis.schema.GistPreferences;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.annotation.Gist;

/**
 * A {@link PropertyIntrospector} that adds information to existing
 * {@link Property} values if they are annotated with
 * {@link org.hisp.dhis.schema.annotation.Gist}.
 *
 * @author Jan Bernitt
 */
public class GistPropertyIntrospector implements PropertyIntrospector
{
    @Override
    public void introspect( Class<?> klass, Map<String, Property> properties )
    {
        for ( Property property : properties.values() )
        {
            if ( property.getKlass() != null )
            {
                initFromGistAnnotation( klass, property );
            }
        }
    }

    private void initFromGistAnnotation( Class<?> klass, Property property )
    {
        Method getter = property.getGetterMethod();
        Gist gist = getAnnotation( getter, Gist.class );
        if ( gist == null )
        {
            gist = getAnnotation( klass, Gist.class );
        }
        Gist valueTypeGist = getAnnotation( getter.getReturnType(), Gist.class );
        if ( gist != null )
        {
            String[] fields = gist.fields();
            if ( fields.length == 0 && valueTypeGist != null )
            {
                fields = valueTypeGist.fields();
            }
            property.setGistPreferences( new GistPreferences(
                gist.included(),
                asList( fields ),
                gist.transformation(),
                EnumSet.copyOf( asList( gist.availableTransformations() ) ) ) );
        }
        else if ( valueTypeGist != null )
        {
            property.setGistPreferences( property.getGistPreferences().withFields( valueTypeGist.fields() ) );
        }
    }
}
