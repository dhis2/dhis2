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
package org.hisp.dhis.commons.jackson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatch;
import org.hisp.dhis.commons.jackson.jsonpatch.JsonPatchException;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * @author Morten Olav Hansen
 */
public class RemoveOperationTest
{
    private final ObjectMapper jsonMapper = JacksonObjectMapperConfig.staticJsonMapper();

    @Test( expected = JsonPatchException.class )
    public void testRemoveInvalidKeyShouldThrowException()
        throws JsonProcessingException,
        JsonPatchException
    {
        JsonPatch patch = jsonMapper.readValue( "[" +
            "{\"op\": \"remove\", \"path\": \"/aaa\"}" +
            "]", JsonPatch.class );

        assertNotNull( patch );
        JsonNode root = jsonMapper.createObjectNode();

        assertFalse( root.has( "aaa" ) );
        root = patch.apply( root );
    }

    @Test
    public void testRemoveProperty()
        throws JsonProcessingException,
        JsonPatchException
    {
        JsonPatch patch = jsonMapper.readValue( "[" +
            "{\"op\": \"remove\", \"path\": \"/aaa\"}" +
            "]", JsonPatch.class );

        assertNotNull( patch );

        ObjectNode root = jsonMapper.createObjectNode();
        root.set( "aaa", TextNode.valueOf( "bbb" ) );

        assertTrue( root.has( "aaa" ) );
        root = (ObjectNode) patch.apply( root );
        assertFalse( root.has( "aaa" ) );
    }

    @Test
    public void testRemovePropertyArrayIndex()
        throws JsonProcessingException,
        JsonPatchException
    {
        JsonPatch patch = jsonMapper.readValue( "[" +
            "{\"op\": \"remove\", \"path\": \"/aaa/1\"}" +
            "]", JsonPatch.class );

        assertNotNull( patch );

        ObjectNode root = jsonMapper.createObjectNode();

        ArrayNode arrayNode = jsonMapper.createArrayNode();
        arrayNode.add( 10 );
        arrayNode.add( 20 );
        arrayNode.add( 30 );

        root.set( "aaa", arrayNode );

        assertTrue( root.has( "aaa" ) );
        assertEquals( 3, arrayNode.size() );

        root = (ObjectNode) patch.apply( root );

        arrayNode = (ArrayNode) root.get( "aaa" );
        assertNotNull( arrayNode );
        assertEquals( 2, arrayNode.size() );
    }
}
