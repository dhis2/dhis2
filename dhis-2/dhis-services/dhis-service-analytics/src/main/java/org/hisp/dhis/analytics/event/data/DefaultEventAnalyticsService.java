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
package org.hisp.dhis.analytics.event.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.DataQueryParams.DENOMINATOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.DENOMINATOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.DIVISOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.DIVISOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.FACTOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.FACTOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.MULTIPLIER_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.MULTIPLIER_ID;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.event.HeaderName.CENTER;
import static org.hisp.dhis.analytics.event.HeaderName.COUNT;
import static org.hisp.dhis.analytics.event.HeaderName.ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.EVENT;
import static org.hisp.dhis.analytics.event.HeaderName.EVENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.EXTENT;
import static org.hisp.dhis.analytics.event.HeaderName.GEOMETRY;
import static org.hisp.dhis.analytics.event.HeaderName.INCIDENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.LATITUDE;
import static org.hisp.dhis.analytics.event.HeaderName.LONGITUDE;
import static org.hisp.dhis.analytics.event.HeaderName.ORG_UNIT_CODE;
import static org.hisp.dhis.analytics.event.HeaderName.ORG_UNIT_NAME;
import static org.hisp.dhis.analytics.event.HeaderName.POINTS;
import static org.hisp.dhis.analytics.event.HeaderName.PROGRAM_INSTANCE;
import static org.hisp.dhis.analytics.event.HeaderName.PROGRAM_STAGE;
import static org.hisp.dhis.analytics.event.HeaderName.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.analytics.event.data.GridHeaderMapper.getHeaderName;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.DATE;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.reporttable.ReportTable.COLUMN_NAMES;
import static org.hisp.dhis.reporttable.ReportTable.DASH_PRETTY_SEPARATOR;
import static org.hisp.dhis.reporttable.ReportTable.SPACE;
import static org.hisp.dhis.reporttable.ReportTable.TOTAL_COLUMN_PRETTY_NAME;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.EventAnalyticsDimensionalItem;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventAnalyticsUtils;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.analytics.event.EventAnalyticsService" )
public class DefaultEventAnalyticsService
    extends AbstractAnalyticsService
    implements EventAnalyticsService
{
    private static final Option OPT_TRUE = new Option( "Yes", "1" );

    private static final Option OPT_FALSE = new Option( "No", "0" );

    private final DataElementService dataElementService;

    private final TrackedEntityAttributeService trackedEntityAttributeService;

    private final EventAnalyticsManager eventAnalyticsManager;

    private final EnrollmentAnalyticsManager enrollmentAnalyticsManager;

    private final EventDataQueryService eventDataQueryService;

    private final EventQueryPlanner queryPlanner;

    private final DatabaseInfo databaseInfo;

    private final AnalyticsCache analyticsCache;

    public DefaultEventAnalyticsService( DataElementService dataElementService,
        TrackedEntityAttributeService trackedEntityAttributeService, EventAnalyticsManager eventAnalyticsManager,
        EventDataQueryService eventDataQueryService, AnalyticsSecurityManager securityManager,
        EventQueryPlanner queryPlanner, EventQueryValidator queryValidator, DatabaseInfo databaseInfo,
        AnalyticsCache analyticsCache, EnrollmentAnalyticsManager enrollmentAnalyticsManager )
    {
        super( securityManager, queryValidator );

        checkNotNull( dataElementService );
        checkNotNull( trackedEntityAttributeService );
        checkNotNull( eventAnalyticsManager );
        checkNotNull( eventDataQueryService );
        checkNotNull( queryPlanner );
        checkNotNull( databaseInfo );
        checkNotNull( analyticsCache );

        this.dataElementService = dataElementService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this.eventAnalyticsManager = eventAnalyticsManager;
        this.eventDataQueryService = eventDataQueryService;
        this.queryPlanner = queryPlanner;
        this.databaseInfo = databaseInfo;
        this.analyticsCache = analyticsCache;
        this.enrollmentAnalyticsManager = enrollmentAnalyticsManager;
    }

    // -------------------------------------------------------------------------
    // EventAnalyticsService implementation
    // -------------------------------------------------------------------------

    // TODO Use [longitude/latitude] format for event points
    // TODO Order event analytics tables on execution date to avoid default sort
    // TODO Sorting in queries

    @Override
    public Grid getAggregatedEventData( EventQueryParams params, List<String> columns, List<String> rows )
    {
        return AnalyticsUtils.isTableLayout( columns, rows )
            ? getAggregatedEventDataTableLayout( params, columns, rows )
            : getAggregatedEventData( params );
    }

    @Override
    public Grid getAggregatedEventData( AnalyticalObject object )
    {
        EventQueryParams params = eventDataQueryService.getFromAnalyticalObject( (EventAnalyticalObject) object );

        return getAggregatedEventData( params );
    }

    @Override
    public Grid getAggregatedEventData( EventQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Decide access, add constraints and validate
        // ---------------------------------------------------------------------

        securityManager.decideAccessEventQuery( params );

        queryValidator.validate( params );

        if ( analyticsCache.isEnabled() )
        {
            final EventQueryParams immutableParams = new EventQueryParams.Builder( params ).build();
            return analyticsCache.getOrFetch( params, p -> getAggregatedEventDataGrid( immutableParams ) );
        }

        return getAggregatedEventDataGrid( params );
    }

    /**
     * Creates a grid with table layout for downloading event reports. The grid
     * is dynamically made from rows and columns input, which refers to the
     * dimensions requested.
     *
     * For event reports each option for a dimension will be an
     * {@link EventAnalyticsDimensionalItem} and all permutations will be added
     * to the grid.
     *
     * @param params the {@link EventQueryParams}.
     * @param columns the identifiers of the dimensions to use as columns.
     * @param rows the identifiers of the dimensions to use as rows.
     * @return aggregated data as a Grid object.
     */
    private Grid getAggregatedEventDataTableLayout( EventQueryParams params, List<String> columns, List<String> rows )
    {
        params.removeProgramIndicatorItems();

        Grid grid = getAggregatedEventData( params );

        ListUtils.removeEmptys( columns );
        ListUtils.removeEmptys( rows );

        Map<String, List<EventAnalyticsDimensionalItem>> tableColumns = new LinkedHashMap<>();

        if ( columns != null )
        {
            for ( String dimension : columns )
            {
                addEventDataObjects( grid, params, tableColumns, dimension );
            }
        }

        Map<String, List<EventAnalyticsDimensionalItem>> tableRows = new LinkedHashMap<>();
        List<String> rowDimensions = new ArrayList<>();

        if ( rows != null )
        {
            for ( String dimension : rows )
            {
                rowDimensions.add( dimension );
                addEventDataObjects( grid, params, tableRows, dimension );
            }
        }

        List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations = EventAnalyticsUtils
            .generateEventDataPermutations( tableRows );
        List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations = EventAnalyticsUtils
            .generateEventDataPermutations( tableColumns );

        return generateOutputGrid( grid, params, rowPermutations, columnPermutations, rowDimensions );
    }

    /**
     * Generates an output grid for event analytics download based on input
     * parameters.
     *
     * @param grid the result grid.
     * @param params the {@link EventQueryParams}.
     * @param rowPermutations the row permutations
     * @param columnPermutations the column permutations.
     * @param rowDimensions the row dimensions.
     * @return grid with table layout.
     */
    @SuppressWarnings( "unchecked" )
    private Grid generateOutputGrid( Grid grid, EventQueryParams params,
        List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations,
        List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations, List<String> rowDimensions )
    {
        Grid outputGrid = new ListGrid();
        outputGrid.setTitle( IdentifiableObjectUtils.join( params.getFilterItems() ) );

        for ( String row : rowDimensions )
        {
            MetadataItem metadataItem = (MetadataItem) ((Map<String, Object>) grid.getMetaData()
                .get( ITEMS.getKey() )).get( row );

            String name = StringUtils.defaultIfEmpty( metadataItem.getName(), row );
            String col = StringUtils.defaultIfEmpty( COLUMN_NAMES.get( row ), row );

            outputGrid.addHeader( new GridHeader( name, col, ValueType.TEXT, false, true ) );
        }

        columnPermutations.forEach( permutation -> {
            StringBuilder builder = new StringBuilder();

            permutation.forEach( ( key, value ) -> {
                if ( !key.equals( ORGUNIT_DIM_ID ) && !key.equals( PERIOD_DIM_ID ) )
                {
                    builder.append( key ).append( SPACE );
                }
                builder.append( value.getDisplayProperty( params.getDisplayProperty() ) )
                    .append( DASH_PRETTY_SEPARATOR );
            } );

            String display = builder.length() > 0 ? builder.substring( 0, builder.lastIndexOf( DASH_PRETTY_SEPARATOR ) )
                : TOTAL_COLUMN_PRETTY_NAME;

            outputGrid.addHeader( new GridHeader( display, display,
                ValueType.NUMBER, false, false ) );
        } );

        for ( Map<String, EventAnalyticsDimensionalItem> rowCombination : rowPermutations )
        {
            outputGrid.addRow();
            List<List<String>> ids = new ArrayList<>();
            Map<String, EventAnalyticsDimensionalItem> displayObjects = new HashMap<>();

            boolean fillDisplayList = true;

            for ( Map<String, EventAnalyticsDimensionalItem> columnCombination : columnPermutations )
            {
                List<String> idList = new ArrayList<>();

                boolean finalFillDisplayList = fillDisplayList;
                rowCombination.forEach( ( key, value ) -> {
                    idList.add( value.toString() );

                    if ( finalFillDisplayList )
                    {
                        displayObjects.put( value.getParentUid(), value );
                    }
                } );

                columnCombination.forEach( ( key, value ) -> idList.add( value.toString() ) );

                ids.add( idList );
                fillDisplayList = false;
            }

            rowDimensions.forEach( dimension -> outputGrid
                .addValue( displayObjects.get( dimension ).getDisplayProperty( params.getDisplayProperty() ) ) );

            EventAnalyticsUtils.addValues( ids, grid, outputGrid );
        }

        return outputGrid;
    }

    /**
     * Puts elements into the mapping table. The elements are fetched from the
     * query parameters.
     *
     * @param grid the {@link Grid}.
     * @param params the {@link EventQueryParams}.
     * @param table the map to add elements to.
     * @param dimension the dimension identifier.
     */
    private void addEventDataObjects( Grid grid, EventQueryParams params,
        Map<String, List<EventAnalyticsDimensionalItem>> table, String dimension )
    {
        List<EventAnalyticsDimensionalItem> objects = params.getEventReportDimensionalItemArrayExploded( dimension );

        if ( objects.isEmpty() )
        {
            ValueTypedDimensionalItemObject eventDimensionalItemObject = dataElementService.getDataElement( dimension );

            if ( eventDimensionalItemObject == null )
            {
                eventDimensionalItemObject = trackedEntityAttributeService
                    .getTrackedEntityAttribute( dimension );
            }

            addEventReportDimensionalItems( eventDimensionalItemObject, objects, grid, dimension );

            table.put( eventDimensionalItemObject.getDisplayProperty( params.getDisplayProperty() ), objects );
        }
        else
        {
            table.put( dimension, objects );
        }
    }

    /**
     * Adds dimensional items to the given list of objects. Send in a list of
     * {@link EventAnalyticsDimensionalItem} and add properties from
     * {@link ValueTypedDimensionalItemObject} parameter.
     *
     * @param eventDimensionalItemObject the
     *        {@link ValueTypedDimensionalItemObject} object to get properties
     *        from.
     * @param objects the list of {@link EventAnalyticsDimensionalItem} objects.
     * @param grid the {@link Grid} from the event analytics request.
     * @param dimension the dimension identifier.
     */
    @SuppressWarnings( "unchecked" )
    private void addEventReportDimensionalItems( ValueTypedDimensionalItemObject eventDimensionalItemObject,
        List<EventAnalyticsDimensionalItem> objects, Grid grid, String dimension )
    {
        Preconditions.checkNotNull( eventDimensionalItemObject,
            String.format( "Data dimension '%s' is invalid", dimension ) );

        String parentUid = eventDimensionalItemObject.getUid();

        if ( eventDimensionalItemObject.getValueType() == BOOLEAN )
        {
            objects.add( new EventAnalyticsDimensionalItem( OPT_TRUE, parentUid ) );
            objects.add( new EventAnalyticsDimensionalItem( OPT_FALSE, parentUid ) );
        }

        if ( eventDimensionalItemObject.hasOptionSet() )
        {
            for ( Option option : eventDimensionalItemObject.getOptionSet().getOptions() )
            {
                objects.add( new EventAnalyticsDimensionalItem( option, parentUid ) );
            }
        }
        else if ( eventDimensionalItemObject.hasLegendSet() )
        {
            List<String> legendOptions = (List<String>) ((Map<String, Object>) grid.getMetaData()
                .get( DIMENSIONS.getKey() ))
                    .get( dimension );

            if ( legendOptions.isEmpty() )
            {
                List<Legend> legends = eventDimensionalItemObject.getLegendSet().getSortedLegends();

                for ( Legend legend : legends )
                {
                    for ( int i = legend.getStartValue().intValue(); i < legend.getEndValue().intValue(); i++ )
                    {
                        objects.add( new EventAnalyticsDimensionalItem( new Option(
                            String.valueOf( i ), String.valueOf( i ) ), parentUid ) );
                    }
                }
            }
            else
            {
                for ( String legend : legendOptions )
                {
                    MetadataItem metadataItem = (MetadataItem) ((Map<String, Object>) grid.getMetaData()
                        .get( ITEMS.getKey() ))
                            .get( legend );

                    objects.add( new EventAnalyticsDimensionalItem(
                        new Option( metadataItem.getName(), legend ), parentUid ) );
                }
            }
        }
    }

    private Grid getAggregatedEventDataGrid( EventQueryParams params )
    {
        params.removeProgramIndicatorItems();

        Grid grid = new ListGrid();

        int maxLimit = queryValidator.getMaxLimit();

        // ---------------------------------------------------------------------
        // Headers and data
        // ---------------------------------------------------------------------

        if ( !params.isSkipData() )
        {
            // -----------------------------------------------------------------
            // Headers
            // -----------------------------------------------------------------

            if ( params.isCollapseDataDimensions() || params.isAggregateData() )
            {
                grid.addHeader( new GridHeader( DimensionalObject.DATA_COLLAPSED_DIM_ID,
                    DataQueryParams.DISPLAY_NAME_DATA_X, ValueType.TEXT, false, true ) );
            }
            else
            {
                for ( QueryItem item : params.getItems() )
                {
                    grid.addHeader( new GridHeader(
                        item.getItem().getUid(), item.getItem().getDisplayProperty( params.getDisplayProperty() ),
                        item.getValueType(), false, true, item.getOptionSet(), item.getLegendSet() ) );
                }
            }

            for ( DimensionalObject dimension : params.getDimensions() )
            {
                grid.addHeader( new GridHeader(
                    dimension.getDimension(), dimension.getDisplayProperty( params.getDisplayProperty() ),
                    TEXT, false, true ) );
            }

            grid.addHeader( new GridHeader( VALUE_ID, VALUE_HEADER_NAME, NUMBER, false, false ) );

            if ( params.isIncludeNumDen() )
            {
                grid
                    .addHeader( new GridHeader( NUMERATOR_ID, NUMERATOR_HEADER_NAME, NUMBER, false, false ) )
                    .addHeader( new GridHeader( DENOMINATOR_ID, DENOMINATOR_HEADER_NAME, NUMBER, false, false ) )
                    .addHeader( new GridHeader( FACTOR_ID, FACTOR_HEADER_NAME, NUMBER, false, false ) )
                    .addHeader( new GridHeader( MULTIPLIER_ID, MULTIPLIER_HEADER_NAME, NUMBER, false, false ) )
                    .addHeader( new GridHeader( DIVISOR_ID, DIVISOR_HEADER_NAME, NUMBER, false, false ) );
            }

            // -----------------------------------------------------------------
            // Data
            // -----------------------------------------------------------------

            Timer timer = new Timer().start().disablePrint();

            List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );

            timer.getSplitTime( "Planned event query, got partitions: " + params.getPartitions() );

            for ( EventQueryParams query : queries )
            {
                // Each query might be either an enrollment or event indicator

                if ( query.hasEnrollmentProgramIndicatorDimension() )
                {
                    enrollmentAnalyticsManager.getAggregatedEventData( query, grid, maxLimit );
                }
                else
                {
                    eventAnalyticsManager.getAggregatedEventData( query, grid, maxLimit );
                }
            }

            timer.getTime( "Got aggregated events" );

            if ( maxLimit > 0 && grid.getHeight() > maxLimit )
            {
                throwIllegalQueryEx( ErrorCode.E7128, maxLimit );
            }

            // -----------------------------------------------------------------
            // Limit and sort, done again due to potential multiple partitions
            // -----------------------------------------------------------------

            if ( params.hasSortOrder() && grid.getHeight() > 0 )
            {
                grid.sortGrid( 1, params.getSortOrderAsInt() );
            }

            if ( params.hasLimit() && grid.getHeight() > params.getLimit() )
            {
                grid.limitGrid( params.getLimit() );
            }
        }

        // ---------------------------------------------------------------------
        // Meta-data
        // ---------------------------------------------------------------------

        addMetadata( params, grid );

        return grid;
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    @Override
    public Grid getEvents( EventQueryParams params )
    {
        return getGrid( params );
    }

    @Override
    public Grid getEventClusters( EventQueryParams params )
    {
        if ( !databaseInfo.isSpatialSupport() )
        {
            throwIllegalQueryEx( ErrorCode.E7218 );
        }

        params = new EventQueryParams.Builder( params )
            .withGeometryOnly( true )
            .withStartEndDatesForPeriods()
            .build();

        securityManager.decideAccessEventQuery( params );

        queryValidator.validate( params );

        Grid grid = new ListGrid();

        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        grid
            .addHeader( new GridHeader( ITEM_COUNT, COUNT.value(), NUMBER, false, false ) )
            .addHeader( new GridHeader( ITEM_CENTER, CENTER.value(), TEXT, false, false ) )
            .addHeader( new GridHeader( ITEM_EXTENT, EXTENT.value(), TEXT, false, false ) )
            .addHeader( new GridHeader( ITEM_POINTS, POINTS.value(), TEXT, false, false ) );

        // ---------------------------------------------------------------------
        // Data
        // ---------------------------------------------------------------------

        params = queryPlanner.planEventQuery( params );

        eventAnalyticsManager.getEventClusters( params, grid, queryValidator.getMaxLimit() );

        return grid;
    }

    @Override
    public Rectangle getRectangle( EventQueryParams params )
    {
        if ( !databaseInfo.isSpatialSupport() )
        {
            throwIllegalQueryEx( ErrorCode.E7218 );
        }

        params = new EventQueryParams.Builder( params )
            .withGeometryOnly( true )
            .withStartEndDatesForPeriods()
            .build();

        securityManager.decideAccessEventQuery( params );

        queryValidator.validate( params );

        params = queryPlanner.planEventQuery( params );

        return eventAnalyticsManager.getRectangle( params );
    }

    /**
     * Creates a grid with headers.
     *
     * @param params the {@link EventQueryParams}.
     */
    @Override
    protected Grid createGridWithHeaders( EventQueryParams params )
    {
        Grid grid = new ListGrid();

        grid
            .addHeader( new GridHeader( ITEM_EVENT, EVENT.value(), TEXT, false, true ) )
            .addHeader( new GridHeader( ITEM_PROGRAM_STAGE, PROGRAM_STAGE.value(), TEXT, false, true ) )
            .addHeader( new GridHeader( ITEM_EVENT_DATE, getHeaderName( params.getProgramStage(), EVENT_DATE ),
                DATE, false, true ) );

        if ( params.getProgram().isRegistration() )
        {
            grid
                .addHeader( new GridHeader( ITEM_ENROLLMENT_DATE,
                    getHeaderName( params.getProgramStage(), ENROLLMENT_DATE ), DATE, false, true ) )
                .addHeader( new GridHeader( ITEM_INCIDENT_DATE,
                    getHeaderName( params.getProgramStage(), INCIDENT_DATE ), DATE, false, true ) )
                .addHeader(
                    new GridHeader( ITEM_TRACKED_ENTITY_INSTANCE, TRACKED_ENTITY_INSTANCE.value(), TEXT, false, true ) )
                .addHeader( new GridHeader( ITEM_PROGRAM_INSTANCE, PROGRAM_INSTANCE.value(), TEXT, false, true ) );
        }

        grid
            .addHeader( new GridHeader( ITEM_GEOMETRY, GEOMETRY.value(), TEXT, false, true ) )
            .addHeader( new GridHeader( ITEM_LONGITUDE, LONGITUDE.value(), NUMBER, false, true ) )
            .addHeader( new GridHeader( ITEM_LATITUDE, LATITUDE.value(), NUMBER, false, true ) )
            .addHeader( new GridHeader( ITEM_ORG_UNIT_NAME, ORG_UNIT_NAME.value(), TEXT, false, true ) )
            .addHeader( new GridHeader( ITEM_ORG_UNIT_CODE, ORG_UNIT_CODE.value(), TEXT, false, true ) );

        return grid;
    }

    /**
     * Adds event data to the given grid. Returns the number of events matching
     * the given event query.
     *
     * @param grid the {@link Grid}.
     * @param params the {@link EventQueryParams}.
     * @return the count of events.
     */
    @Override
    protected long addEventData( Grid grid, EventQueryParams params )
    {
        Timer timer = new Timer().start().disablePrint();

        params = queryPlanner.planEventQuery( params );

        timer.getSplitTime( "Planned event query, got partitions: " + params.getPartitions() );

        long count = 0;

        if ( params.getPartitions().hasAny() )
        {
            if ( params.isPaging() )
            {
                count += eventAnalyticsManager.getEventCount( params );
            }

            eventAnalyticsManager.getEvents( params, grid, queryValidator.getMaxLimit() );

            timer.getTime( "Got events " + grid.getHeight() );
        }

        return count;
    }
}
