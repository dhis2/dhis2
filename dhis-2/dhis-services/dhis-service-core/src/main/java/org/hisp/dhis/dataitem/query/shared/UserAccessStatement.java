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
package org.hisp.dhis.dataitem.query.shared;

import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringNonBlankPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_GROUP_UIDS;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.USER_UID;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.CHECK_USER_GROUPS_ACCESS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.EXTRACT_PATH_TEXT;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_GROUP_IDS;
import static org.hisp.dhis.hibernate.jsonb.type.JsonbFunctions.HAS_USER_ID;
import static org.springframework.util.Assert.hasText;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

/**
 * This class held common user access SQL statements for data items.
 *
 * @author maikel arabori
 */
public class UserAccessStatement
{
    private UserAccessStatement()
    {
    }

    public static String sharingConditions( final String column, final MapSqlParameterSource paramsMap )
    {
        final StringBuilder conditions = new StringBuilder();

        conditions
            .append( " (" ) // Isolator

            .append( " ( " ) // Grouping clauses
            .append( publicAccessCondition( column ) )
            .append( " OR " )
            .append( ownerAccessCondition( column ) )
            .append( " OR " )
            .append( userAccessCondition( column ) )
            .append( " ) " ); // Grouping clauses closing

        if ( hasStringNonBlankPresence( paramsMap, USER_GROUP_UIDS ) )
        {
            conditions.append( " or (" + userGroupAccessCondition( column ) + ")" );
        }

        conditions.append( ")" ); // Isolator closing

        return conditions.toString();
    }

    public static String sharingConditions( final String columnOne, final String columnTwo,
        final MapSqlParameterSource paramsMap )
    {
        final StringBuilder conditions = new StringBuilder();

        conditions
            .append( " (" ) // Isolator

            .append( " ( " ) // Grouping clauses
            .append( "(" ) // Table 1 conditions
            .append( publicAccessCondition( columnOne ) )
            .append( " or " )
            .append( ownerAccessCondition( columnOne ) )
            .append( " or " )
            .append( userAccessCondition( columnOne ) )
            .append( ")" ) // Table 1 conditions end
            .append( " and (" ) // Table 2 conditions
            .append( publicAccessCondition( columnTwo ) )
            .append( " or " )
            .append( ownerAccessCondition( columnTwo ) )
            .append( " or " )
            .append( userAccessCondition( columnTwo ) )
            .append( ")" ) // Table 2 conditions end
            .append( " )" ); // Grouping clauses closing

        if ( hasStringNonBlankPresence( paramsMap, USER_GROUP_UIDS ) )
        {
            conditions.append( " or (" );

            // Program group access checks
            conditions.append( userGroupAccessCondition( columnOne ) );

            // DataElement access checks
            conditions.append( " and " + userGroupAccessCondition( columnTwo ) );

            // Closing OR condition
            conditions.append( ")" );
        }

        conditions.append( ")" ); // Isolator closing

        return conditions.toString();
    }

    static String ownerAccessCondition( final String column )
    {
        assertTableAlias( column );

        return "(" + EXTRACT_PATH_TEXT + "(" + column + ", 'owner') is null or "
            + EXTRACT_PATH_TEXT + "(" + column + ", 'owner') = 'null' or "
            + EXTRACT_PATH_TEXT + "(" + column + ", 'owner') = :userUid)";
    }

    static String publicAccessCondition( final String column )
    {
        assertTableAlias( column );

        return "(" + EXTRACT_PATH_TEXT + "(" + column + ", 'public') is null or "
            + EXTRACT_PATH_TEXT + "(" + column + ", 'public') = 'null' or "
            + EXTRACT_PATH_TEXT + "(" + column + ", 'public') like 'r%')";
    }

    static String userAccessCondition( final String tableName )
    {
        assertTableAlias( tableName );

        return "(" + HAS_USER_ID + "(" + tableName + ", :" + USER_UID + ") = true "
            + "and " + CHECK_USER_ACCESS + "(" + tableName + ", :" + USER_UID + ", 'r%') = true)";
    }

    static String userGroupAccessCondition( final String column )
    {
        assertTableAlias( column );

        return "(" + HAS_USER_GROUP_IDS + "(" + column + ", :" + USER_GROUP_UIDS + ") = true " +
            "and " + CHECK_USER_GROUPS_ACCESS + "(" + column + ", 'r%', :" + USER_GROUP_UIDS + ") = true)";
    }

    private static void assertTableAlias( String columnName )
    {
        hasText( columnName, "The argument columnName cannot be null/blank." );
    }
}
