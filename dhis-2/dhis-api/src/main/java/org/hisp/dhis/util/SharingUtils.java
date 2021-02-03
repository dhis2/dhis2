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
package org.hisp.dhis.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.user.UserGroupAccess;
import org.hisp.dhis.user.sharing.Sharing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SharingUtils
{
    public static final Set<UserGroupAccess> getDtoUserGroupAccesses( Sharing sharing )
    {
        return sharing.hasUserGroupAccesses() ? sharing.getUserGroups().values()
            .stream().map( uag -> uag.toDtoObject() ).collect( Collectors.toSet() ) : new HashSet<>();
    }

    public static final Set<org.hisp.dhis.user.UserAccess> getDtoUserAccess( Sharing sharing )
    {
        return sharing.hasUserAccesses() ? sharing.getUsers().values()
            .stream().map( ua -> ua.toDtoObject() ).collect( Collectors.toSet() ) : new HashSet<>();
    }

    public static final Sharing generateSharingFromIdentifiableObject( IdentifiableObject object )
    {
        Sharing sharing = new Sharing();
        sharing.setOwner( object.getCreatedBy() );
        sharing.setExternal( object.getExternalAccess() );
        sharing.setPublicAccess( object.getPublicAccess() );
        sharing.setDtoUserGroupAccesses( object.getUserGroupAccesses() );
        sharing.setDtoUserAccesses( object.getUserAccesses() );
        return sharing;
    }

    public static String withAccess( String jsonb, UnaryOperator<String> accessTransformation )
        throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure( MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true );
        mapper.configure( SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true );
        Sharing value = mapper.readValue( jsonb, Sharing.class );
        return mapper.writeValueAsString( value.withAccess( accessTransformation ) );
    }
}
