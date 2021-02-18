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
package org.hisp.dhis.webapi.controller.dataitem.helper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.webapi.controller.dataitem.helper.OrderingHelper.sort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataitem.DataItem;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.junit.Test;

/**
 * Unit tests for OrderingHelper.
 *
 * @author maikel arabori
 */
public class OrderingHelperTest
{
    @Test
    public void sortWhenDimensionalItemsIsEmpty()
    {
        // Given
        final List<DataItem> emptyDimensionalItems = emptyList();
        final OrderParams orderParams = new OrderParams();

        // When
        sort( emptyDimensionalItems, orderParams );

        // Then
        assertThat( emptyDimensionalItems, is( emptyList() ) );
    }

    @Test
    public void sortWhenOrderParamsIsNull()
    {
        // Given
        final List<DataItem> anyDimensionalItems = mockDimensionalItems( 2 );
        final List<DataItem> unchangedList = mockDimensionalItems( 2 );
        final OrderParams nullOrderParams = new OrderParams();

        // When
        sort( anyDimensionalItems, nullOrderParams );

        // Then
        assertEquals( anyDimensionalItems, unchangedList );
    }

    @Test
    public void sortWhenOrderParamsIsAsc()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "name:desc" ) );
        final OrderParams orderParams = new OrderParams( orderings );
        final List<DataItem> anyDimensionalItems = mockDimensionalItems( 2 );

        // When
        sort( anyDimensionalItems, orderParams );

        // Then
        assertEquals( anyDimensionalItems.get( 0 ).getName(), "d-1" );
        assertEquals( anyDimensionalItems.get( 1 ).getName(), "d-0" );
    }

    @Test
    public void sortWhenOrderParamsIsDesc()
    {
        // Given
        final Set<String> orderings = new HashSet<>( singletonList( "name:desc" ) );
        final OrderParams orderParams = new OrderParams( orderings );
        final List<DataItem> anyDimensionalItems = mockDimensionalItems( 2 );
        final List<DataItem> ascList = mockDimensionalItems( 2 );
        Collections.reverse( ascList );

        // When
        sort( anyDimensionalItems, orderParams );

        // Then
        assertEquals( anyDimensionalItems, ascList );
    }

    @Test
    public void sortWhenOrderParamsIsInvalid()
    {
        // Given
        final Set<String> orderingWithNoValue = new HashSet<>( singletonList( "name:" ) );
        final OrderParams orderParams = new OrderParams( orderingWithNoValue );
        final List<DataItem> anyDimensionalItems = mockDimensionalItems( 2 );

        // When
        assertThrows( "Unable to parse order param: `" + "name:" + "`", IllegalQueryException.class,
            () -> sort( anyDimensionalItems, orderParams ) );
    }

    private List<DataItem> mockDimensionalItems( final int totalOfItems )
    {
        final List<DataItem> dataItemEntities = new ArrayList<>( 0 );

        for ( int i = 0; i < totalOfItems; i++ )
        {
            final DataItem dataItem = DataItem.builder().name( "d-" + i ).id( "d-" + i ).build();
            dataItemEntities.add( dataItem );
        }

        return dataItemEntities;
    }
}
