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
package org.hisp.dhis.dataitem.query;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.hisp.dhis.common.DimensionItemType.INDICATOR;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.dataitem.DataItem.builder;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.always;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.displayFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.identifiableTokenFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifAny;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.ifSet;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.nameFiltering;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.rootJunction;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.skipValueType;
import static org.hisp.dhis.dataitem.query.shared.FilteringStatement.uidFiltering;
import static org.hisp.dhis.dataitem.query.shared.LimitStatement.maxLimit;
import static org.hisp.dhis.dataitem.query.shared.OrderingStatement.ordering;
import static org.hisp.dhis.dataitem.query.shared.ParamPresenceChecker.hasStringNonBlankPresence;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.LOCALE;
import static org.hisp.dhis.dataitem.query.shared.QueryParam.PROGRAM_ID;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_SELECT;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_UNION;
import static org.hisp.dhis.dataitem.query.shared.StatementUtil.SPACED_WHERE;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.READ_ACCESS;
import static org.hisp.dhis.dataitem.query.shared.UserAccessStatement.sharingConditions;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dataitem.query.shared.OptionalFilterBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

/**
 * This component is responsible for providing query capabilities on top of
 * Indicator objects.
 *
 * @author maikel arabori
 */
@Slf4j
@Component
public class IndicatorQuery implements DataItemQuery
{
    private static final String COMMON_COLUMNS = "indicator.uid, indicator.name,"
        + " indicator.code, indicator.sharing as indicator_sharing";

    private static final String ITEM_UID = "indicator.uid";

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public IndicatorQuery( @Qualifier( "readOnlyJdbcTemplate" )
    final JdbcTemplate jdbcTemplate )
    {
        checkNotNull( jdbcTemplate );

        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate( jdbcTemplate );
    }

    @Override
    public List<DataItem> find( final MapSqlParameterSource paramsMap )
    {
        final List<DataItem> dataItems = new ArrayList<>();

        // It returns an empty for the cases below:
        //
        // 1) Very specific case, for Indicator objects, needed to handle filter
        // by value type NUMBER.
        // When the value type filter does not have a NUMBER type, we should not
        // execute this query.
        //
        // 2) If we have a program id filter, we should not return any indicator
        // because indicators don't have programs directly associated with.
        if ( skipValueType( NUMBER, paramsMap ) || hasStringNonBlankPresence( paramsMap, PROGRAM_ID ) )
        {
            return dataItems;
        }

        final SqlRowSet rowSet = namedParameterJdbcTemplate.queryForRowSet( getIndicatorQuery( paramsMap ), paramsMap );

        while ( rowSet.next() )
        {
            final String name = trimToNull( rowSet.getString( "name" ) );
            final String displayName = defaultIfBlank( trimToNull( rowSet.getString( "i18n_name" ) ), name );

            // Specific case where we have to force a vale type. Indicators
            // don't have a value type but they always evaluate to numbers.
            dataItems.add( builder().name( name ).displayName( displayName ).id( rowSet.getString( "uid" ) )
                .code( rowSet.getString( "code" ) ).dimensionItemType( INDICATOR ).valueType( NUMBER ).build() );
        }

        return dataItems;
    }

    @Override
    public int count( final MapSqlParameterSource paramsMap )
    {
        // It returns an empty for the cases below:
        //
        // 1) Very specific case, for Indicator objects, needed to handle filter
        // by value type NUMBER.
        // When the value type filter does not have a NUMBER type, we should not
        // execute this query.
        //
        // 2) If we have a program id filter, we should not return any indicator
        // because indicators don't have programs directly associated with.
        if ( skipValueType( NUMBER, paramsMap ) || hasStringNonBlankPresence( paramsMap, PROGRAM_ID ) )
        {
            return 0;
        }

        final StringBuilder sql = new StringBuilder();

        sql.append( SPACED_SELECT + "count(*) from (" )
            .append( getIndicatorQuery( paramsMap ).replace( maxLimit( paramsMap ), EMPTY ) )
            .append( ") t" );

        return namedParameterJdbcTemplate.queryForObject( sql.toString(), paramsMap, Integer.class );
    }

    @Override
    public Class<? extends BaseIdentifiableObject> getRootEntity()
    {
        return QueryableDataItem.INDICATOR.getEntity();
    }

    private String getIndicatorQuery( final MapSqlParameterSource paramsMap )
    {
        final StringBuilder sql = new StringBuilder();

        // Creating a temp translated table to be queried.
        sql.append( SPACED_SELECT + "* from (" );

        if ( hasStringNonBlankPresence( paramsMap, LOCALE ) )
        {
            // Selecting only rows that contains translated names.
            sql.append( selectRowsContainingTranslatedName( false ) )

                .append( SPACED_UNION )

                // Selecting ALL rows, not taking into consideration
                // translations.
                .append( selectAllRowsIgnoringAnyTranslation() )

                /// AND excluding ALL translated rows previously selected
                /// (translated names).
                .append( SPACED_WHERE + "(" + ITEM_UID + ") not in (" )

                .append( selectRowsContainingTranslatedName( true ) )

                // Closing NOT IN exclusions.
                .append( ")" );
        }
        else
        {
            // Retrieving all rows ignoring translation as no locale is defined.
            sql.append( selectAllRowsIgnoringAnyTranslation() );
        }

        sql.append(
            " group by indicator.name, " + ITEM_UID + ", indicator.code, i18n_name,"
                + " indicator_sharing" );

        // Closing the temp table.
        sql.append( " ) t" );

        sql.append( SPACED_WHERE );

        // Applying filters, ordering and limits.

        // Mandatory filters. They do not respect the root junction filtering.
        sql.append( always( sharingConditions( "t.indicator_sharing", READ_ACCESS, paramsMap ) ) );

        // Optional filters, based on the current root junction.
        final OptionalFilterBuilder optionalFilters = new OptionalFilterBuilder( paramsMap );
        optionalFilters.append( ifSet( displayFiltering( "t.i18n_name", paramsMap ) ) );
        optionalFilters.append( ifSet( nameFiltering( "t.name", paramsMap ) ) );
        optionalFilters.append( ifSet( uidFiltering( "t.uid", paramsMap ) ) );
        sql.append( ifAny( optionalFilters.toString() ) );

        final String identifiableStatement = identifiableTokenFiltering( "t.uid", "t.code", "t.i18n_name",
            null, paramsMap );

        if ( isNotBlank( identifiableStatement ) )
        {
            sql.append( rootJunction( paramsMap ) );
            sql.append( identifiableStatement );
        }

        sql.append( ifSet( ordering( "t.i18n_name, t.uid", "t.name, t.uid", paramsMap ) ) );
        sql.append( ifSet( maxLimit( paramsMap ) ) );

        final String fullStatement = sql.toString();

        log.trace( "Full SQL: " + fullStatement );

        return fullStatement;
    }

    private String selectRowsContainingTranslatedName( final boolean onlyUidColumn )
    {
        final StringBuilder sql = new StringBuilder();

        if ( onlyUidColumn )
        {
            sql.append( SPACED_SELECT + ITEM_UID );
        }
        else
        {
            sql.append( SPACED_SELECT + COMMON_COLUMNS )
                .append( ", i_displayname.value as i18n_name" );
        }

        sql.append( " from indicator " )
            .append(
                " join jsonb_to_recordset(indicator.translations) as i_displayname(value TEXT, locale TEXT, property TEXT) on i_displayname.locale = :"
                    + LOCALE + " and i_displayname.property = 'NAME'" );

        return sql.toString();
    }

    private String selectAllRowsIgnoringAnyTranslation()
    {
        return new StringBuilder()
            .append( SPACED_SELECT + COMMON_COLUMNS )
            .append( ", indicator.name as i18n_name" )
            .append( " from indicator " ).toString();
    }
}
