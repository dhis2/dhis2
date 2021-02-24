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
package org.hisp.dhis.schema;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.node.annotation.NodeCollection;
import org.hisp.dhis.node.annotation.NodeRoot;
import org.hisp.dhis.node.annotation.NodeSimple;
import org.junit.Test;

@NodeRoot( value = "collectionItem" )
class Item
{
    @NodeSimple
    private String value;
}

class CollectionFields
{
    @NodeCollection
    private List<String> property = new ArrayList<>();

    @NodeCollection( value = "renamedProperty" )
    private final List<String> propertyToBeRenamed = new ArrayList<>();

    @NodeCollection( isReadable = true, isWritable = false )
    private final List<String> readOnly = new ArrayList<>();

    @NodeCollection( isReadable = false, isWritable = true )
    private final List<String> writeOnly = new ArrayList<>();

    @NodeCollection( namespace = "http://ns.example.org" )
    private final List<String> propertyWithNamespace = new ArrayList<>();

    public List<String> getProperty()
    {
        return property;
    }

    public void setProperty( List<String> property )
    {
        this.property = property;
    }

    @NodeCollection
    private final List<Item> items1 = new ArrayList<>();

    @NodeCollection( value = "items", itemName = "item" )
    private final List<Item> items2 = new ArrayList<>();
}

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class FieldCollectionNodePropertyIntrospectorTest extends AbstractNodePropertyIntrospectorTest
{

    public FieldCollectionNodePropertyIntrospectorTest()
    {
        super( CollectionFields.class );
    }

    @Test
    @Override
    public void testContainsKey()
    {
        assertFalse( propertyMap.containsKey( "propertyToBeRenamed" ) );
        assertContainsOnly( propertyMap.keySet(),
            "property", "renamedProperty", "readOnly", "writeOnly", "propertyWithNamespace", "items1", "items" );
    }

    @Test
    public void testItemFieldName()
    {
        assertEquals( "items2", propertyMap.get( "items" ).getFieldName() );
    }

    @Test
    public void testItemName()
    {
        assertEquals( "collectionItem", propertyMap.get( "items1" ).getName() );
        assertEquals( "items1", propertyMap.get( "items1" ).getCollectionName() );

        assertEquals( "item", propertyMap.get( "items" ).getName() );
        assertEquals( "items", propertyMap.get( "items" ).getCollectionName() );
    }

    @Test
    public void testItemKlass()
    {
        assertEquals( Item.class, propertyMap.get( "items1" ).getItemKlass() );
    }
}