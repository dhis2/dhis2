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
package org.hisp.dhis.query.member;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.NamedParams;
import org.hisp.dhis.schema.RelationViewType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder( toBuilder = true )
@RequiredArgsConstructor( access = AccessLevel.PRIVATE )
public final class MemberQuery<T extends IdentifiableObject>
{

    /**
     * Query properties about the owner of the collection property.
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static final class Owner
    {

        /**
         * The object type that has the collection
         */
        private final Class<? extends IdentifiableObject> type;

        /**
         * Id of the collection owner object.
         */
        private final String id;

        /**
         * Name of the collection property in the {@link #type}.
         */
        private final String collectionProperty;

        @Override
        public String toString()
        {
            return type.getSimpleName() + "[" + id + "]." + collectionProperty;
        }
    }

    private final Owner owner;

    private final Class<T> elementType;

    private final int pageOffset;

    private final int pageSize;

    private final String contextRoot;

    /**
     * When true the result set is not the elements contained in the collection
     * but those not contained (yet).
     */
    private final boolean inverse;

    /**
     * Names of those properties that should be included in the response.
     */
    @Builder.Default
    private final List<Field> fields = emptyList();

    /**
     * List of filter property expressions. An expression has the format
     * {@code property:operator:value} or {@code property:operator}.
     */
    @Builder.Default
    private final List<Filter> filters = emptyList();

    @Builder.Default
    private final List<Order> orders = emptyList();

    public List<String> getFieldNames()
    {
        return fields.stream().map( Field::getPropertyPath ).collect( toList() );
    }

    public MemberQuery<T> with( NamedParams params )
    {
        int page = params.getInt( "page", 0 );
        int size = params.getInt( "pageSize", 25 );
        RelationViewType relations = params.getEnum( "relations", RelationViewType.AUTO );
        return toBuilder().pageSize( size ).pageOffset( page * size )
            .inverse( params.getBoolean( "inverse" ) )
            .fields(
                params.getStrings( "fields" ).stream().map( s -> Field.parse( s, relations ) ).collect( toList() ) )
            .filters( params.getStrings( "filter" ).stream().map( Filter::parse ).collect( toList() ) )
            .orders( params.getStrings( "sort" ).stream().map( Order::parse ).collect( toList() ) )
            .build();
    }

    public MemberQuery<T> withFilter( Filter filter )
    {
        return withAddedItem( filter, getFilters(), MemberQueryBuilder::filters );
    }

    public MemberQuery<T> withOrder( Order order )
    {
        return withAddedItem( order, getOrders(), MemberQueryBuilder::orders );
    }

    public MemberQuery<T> withField( Field field )
    {
        return withAddedItem( field, getFields(), MemberQueryBuilder::fields );
    }

    public MemberQuery<T> withFields( List<Field> fields )
    {
        return toBuilder().fields( fields ).build();
    }

    private <E> MemberQuery<T> withAddedItem( E e, List<E> collection,
        BiFunction<MemberQueryBuilder<T>, List<E>, MemberQueryBuilder<T>> setter )
    {
        List<E> plus1 = new ArrayList<>( collection );
        plus1.add( e );
        return setter.apply( toBuilder(), plus1 ).build();
    }

    public enum Direction
    {
        ASC,
        DESC
    }

    public enum Comparison
    {
        // identity comparison
        NULL( "null" ),
        NOT_NULL( "!null" ),
        EQ( "eq" ),
        NE( "!eq", "ne", "neq" ),
        // numeric comparison
        LT( "lt" ),
        LE( "le", "lte" ),
        GT( "gt" ),
        GE( "ge", "gte" ),
        // set operations
        IN( "in" ),
        NOT_IN( "!in" ),
        // string comparison
        EMPTY( "empty" ),
        NOT_EMPTY( "!empty" ),
        LIKE( "like" ),
        NOT_LIKE( "!like" ),
        STARTS_WITH( "$like", "startsWith" ),
        NOT_STARTS_WITH( "!$like" ),
        ENDS_WITH( "like$", "endsWith" ),
        NOT_ENDS_WITH( "!like$" );

        private final String[] symbols;

        Comparison( String... symbols )
        {
            this.symbols = symbols;
        }

        public static Comparison parse( String symbol )
        {
            String s = symbol.toLowerCase();
            for ( Comparison op : values() )
            {
                if ( asList( op.symbols ).contains( s ) )
                {
                    return op;
                }
            }
            throw new IllegalArgumentException( "Not an comparison operator symbol: " + symbol );
        }

        public boolean isIdentityCompare()
        {
            return this == NULL || this == NOT_NULL || this == EQ || this == NE;
        }

        public boolean isOrderCompare()
        {
            return this == EQ || this == NE || isNumericCompare();
        }

        public boolean isNumericCompare()
        {
            return this == LT || this == LE || this == GE || this == GT;
        }

        public boolean isSetCompare()
        {
            return this == IN || this == NOT_IN;
        }

        public boolean isStringCompare()
        {
            return ordinal() >= EMPTY.ordinal();
        }
    }

    @Getter
    @AllArgsConstructor
    public static final class Field
    {
        private final String propertyPath;

        private final RelationViewType relations;

        public static Field parse( String field, RelationViewType global )
        {
            int endOfPropertyName = field.indexOf( ':' );
            if ( endOfPropertyName < 0 )
            {
                return new Field( field, global );
            }
            return new Field( field.substring( 0, endOfPropertyName ),
                RelationViewType
                    .valueOf( field.substring( endOfPropertyName + 1 ).toUpperCase().replace( '-', '_' ) ) );
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static final class Order
    {
        private final String propertyPath;

        @Builder.Default
        private final Direction direction = Direction.ASC;

        public static Order parse( String order )
        {
            String[] parts = order.split( ":" );
            if ( parts.length == 1 )
            {
                return new Order( order, Direction.ASC );
            }
            if ( parts.length == 2 )
            {
                return new Order( parts[0], Direction.valueOf( parts[1].toUpperCase() ) );
            }
            throw new IllegalArgumentException( "Not a valid order expression: " + order );
        }

        @Override
        public String toString()
        {
            return propertyPath + " " + direction.name();
        }
    }

    @Getter
    @AllArgsConstructor
    public static final class Filter
    {
        private final String propertyPath;

        private final Comparison operator;

        private final String[] value;

        public static Filter parse( String filter )
        {
            String[] parts = filter.split( ":" );
            if ( parts.length == 2 )
            {
                return new Filter( parts[0], Comparison.parse( parts[1] ), new String[0] );
            }
            if ( parts.length == 3 )
            {
                return new Filter( parts[0], Comparison.parse( parts[1] ), parts[2].split( "," ) );
            }
            throw new IllegalArgumentException( "Not a valid filter expression: " + filter );
        }

        @Override
        public String toString()
        {
            return propertyPath + " " + operator + " " + Arrays.toString( value );
        }
    }
}
