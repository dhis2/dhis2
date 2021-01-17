package org.hisp.dhis.preheat;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.TransactionalIntegrationTest;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.attribute.AttributeValue;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PreheatServiceTest
    extends TransactionalIntegrationTest
{
    @Autowired
    private PreheatService preheatService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Autowired
    private AttributeService attributeService;

    @Autowired
    private UserService _userService;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    protected void setUpTest() throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Ignore
    @Test( expected = PreheatException.class )
    public void testValidateAllFail()
    {
        PreheatParams params = new PreheatParams().setPreheatMode( PreheatMode.ALL );
        preheatService.validate( params );
    }

    @Test
    public void testValidateAll()
    {
        PreheatParams params = new PreheatParams().setPreheatMode( PreheatMode.ALL );
        params.getClasses().add( DataElement.class );

        preheatService.validate( params );
    }

    @Test
    public void testCollectNoObjectsDE()
    {
        DataElement dataElement = createDataElement( 'A' );
        Map<Class<? extends IdentifiableObject>, Set<String>> references = preheatService.collectReferences(
            dataElement ).get( PreheatIdentifier.UID );

        assertFalse( references.containsKey( OptionSet.class ) );
        assertFalse( references.containsKey( LegendSet.class ) );
        assertTrue( references.containsKey( CategoryCombo.class ) );
        assertFalse( references.containsKey( User.class ) );
    }

    @Test
    public void testCollectNoObjectsDEG()
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );
        Map<Class<? extends IdentifiableObject>, Set<String>> references = preheatService.collectReferences(
            dataElementGroup ).get( PreheatIdentifier.UID );

        assertFalse( references.containsKey( DataElement.class ) );
        assertFalse( references.containsKey( User.class ) );
    }

    @Test
    public void testCollectReferenceUidDEG1()
    {
        DataElementGroup deg1 = createDataElementGroup( 'A' );

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        User user = createUser( 'A' );

        deg1.addDataElement( de1 );
        deg1.addDataElement( de2 );
        deg1.addDataElement( de3 );

        deg1.setUser( user );

        Map<Class<? extends IdentifiableObject>, Set<String>> references = preheatService.collectReferences( deg1 )
            .get( PreheatIdentifier.UID );

        assertTrue( references.containsKey( DataElement.class ) );
        assertTrue( references.containsKey( User.class ) );

        assertEquals( 3, references.get( DataElement.class ).size() );
        assertEquals( 1, references.get( User.class ).size() );

        assertTrue( references.get( DataElement.class ).contains( de1.getUid() ) );
        assertTrue( references.get( DataElement.class ).contains( de2.getUid() ) );
        assertTrue( references.get( DataElement.class ).contains( de3.getUid() ) );

        assertTrue( references.get( User.class ).contains( user.getUid() ) );
    }

    @Test
    public void testCollectReferenceUidDEG2()
    {
        DataElementGroup deg1 = createDataElementGroup( 'A' );
        DataElementGroup deg2 = createDataElementGroup( 'B' );

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        deg1.addDataElement( de1 );
        deg1.addDataElement( de2 );
        deg2.addDataElement( de3 );

        Map<Class<? extends IdentifiableObject>, Set<String>> references = preheatService.collectReferences(
            Lists.newArrayList( deg1, deg2 ) ).get( PreheatIdentifier.UID );

        assertTrue( references.containsKey( DataElement.class ) );
        assertEquals( 3, references.get( DataElement.class ).size() );

        assertTrue( references.get( DataElement.class ).contains( de1.getUid() ) );
        assertTrue( references.get( DataElement.class ).contains( de2.getUid() ) );
        assertTrue( references.get( DataElement.class ).contains( de3.getUid() ) );
    }

    @Test
    public void testCollectReferenceCodeDEG1()
    {
        DataElementGroup dataElementGroup = createDataElementGroup( 'A' );

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        User user = createUser( 'A' );

        dataElementGroup.addDataElement( de1 );
        dataElementGroup.addDataElement( de2 );
        dataElementGroup.addDataElement( de3 );

        dataElementGroup.setUser( user );

        Map<Class<? extends IdentifiableObject>, Set<String>> references = preheatService.collectReferences( dataElementGroup )
            .get( PreheatIdentifier.CODE );

        assertTrue( references.containsKey( DataElement.class ) );
        assertTrue( references.containsKey( User.class ) );

        assertEquals( 3, references.get( DataElement.class ).size() );
        assertEquals( 1, references.get( User.class ).size() );

        assertTrue( references.get( DataElement.class ).contains( de1.getCode() ) );
        assertTrue( references.get( DataElement.class ).contains( de2.getCode() ) );
        assertTrue( references.get( DataElement.class ).contains( de3.getCode() ) );

        assertTrue( references.get( User.class ).contains( user.getCode() ) );
    }

    @Test
    public void testCollectReferenceCodeDEG2()
    {
        DataElementGroup deg1 = createDataElementGroup( 'A' );
        DataElementGroup deg2 = createDataElementGroup( 'B' );

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        deg1.addDataElement( de1 );
        deg1.addDataElement( de2 );
        deg2.addDataElement( de3 );

        Map<Class<? extends IdentifiableObject>, Set<String>> references = preheatService.collectReferences(
            Lists.newArrayList( deg1, deg2 ) ).get( PreheatIdentifier.CODE );

        assertTrue( references.containsKey( DataElement.class ) );
        assertEquals( 3, references.get( DataElement.class ).size() );

        assertTrue( references.get( DataElement.class ).contains( de1.getCode() ) );
        assertTrue( references.get( DataElement.class ).contains( de2.getCode() ) );
        assertTrue( references.get( DataElement.class ).contains( de3.getCode() ) );
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void testPreheatAllUID()
    {
        DataElementGroup dataElementGroup = new DataElementGroup( "DataElementGroupA" );
        dataElementGroup.setAutoFields();

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );

        dataElementGroup.addDataElement( de1 );
        dataElementGroup.addDataElement( de2 );
        dataElementGroup.addDataElement( de3 );

        dataElementGroup.setUser( user );
        manager.save( dataElementGroup );

        PreheatParams params = new PreheatParams();
        params.setPreheatMode( PreheatMode.ALL );
        params.setClasses( Sets.newHashSet( DataElement.class, DataElementGroup.class, User.class ) );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, DataElement.class ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, DataElementGroup.class ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, User.class ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElementGroup.class, dataElementGroup.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, User.class, user.getUid() ) );
    }

    @Test
    public void testPreheatAllMetadataUID()
    {
        DataElementGroup dataElementGroup = new DataElementGroup( "DataElementGroupA" );
        dataElementGroup.setAutoFields();

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );

        dataElementGroup.addDataElement( de1 );
        dataElementGroup.addDataElement( de2 );
        dataElementGroup.addDataElement( de3 );

        dataElementGroup.setUser( user );
        manager.save( dataElementGroup );

        PreheatParams params = new PreheatParams();
        params.setPreheatMode( PreheatMode.ALL );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, DataElement.class ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, DataElementGroup.class ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, User.class ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElementGroup.class, dataElementGroup.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, User.class, user.getUid() ) );
    }

    @Test
    public void testPreheatReferenceUID()
    {
        DataElementGroup dataElementGroup = new DataElementGroup( "DataElementGroupA" );
        dataElementGroup.setAutoFields();

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );

        dataElementGroup.addDataElement( de1 );
        dataElementGroup.addDataElement( de2 );
        dataElementGroup.addDataElement( de3 );

        dataElementGroup.setUser( user );
        manager.save( dataElementGroup );

        PreheatParams params = new PreheatParams();
        params.setPreheatMode( PreheatMode.REFERENCE );

        params.getObjects().put( DataElement.class, Lists.newArrayList( de1, de2 ) );
        params.getObjects().put( User.class, Lists.newArrayList( user ) );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, DataElement.class ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.UID, DataElementGroup.class ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, User.class ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElementGroup.class, dataElementGroup.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, User.class, user.getUid() ) );
    }

    @Test
    public void testPreheatReferenceCODE()
    {
        DataElementGroup dataElementGroup = new DataElementGroup( "DataElementGroupA" );
        dataElementGroup.setAutoFields();

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );

        dataElementGroup.addDataElement( de1 );
        dataElementGroup.addDataElement( de2 );
        dataElementGroup.addDataElement( de3 );

        dataElementGroup.setUser( user );
        manager.save( dataElementGroup );

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setPreheatMode( PreheatMode.REFERENCE );

        params.getObjects().put( DataElement.class, Lists.newArrayList( de1, de2 ) );
        params.getObjects().put( User.class, Lists.newArrayList( user ) );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.CODE ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.CODE, DataElement.class ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE, DataElementGroup.class ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, de1.getCode() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, de2.getCode() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, de3.getCode() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.CODE, DataElementGroup.class, dataElementGroup.getCode() ) );
    }

    @Test
    public void testPreheatReferenceWithScanUID()
    {
        DataElementGroup dataElementGroup = fromJson( "preheat/degAUidRef.json", DataElementGroup.class );
        defaultSetup();

        PreheatParams params = new PreheatParams();
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.getObjects().put( DataElementGroup.class, Lists.newArrayList( dataElementGroup ) );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, DataElement.class ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, DataElementGroup.class ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID, User.class ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghA" ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghB" ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, "deabcdefghC" ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, User.class, "userabcdefA" ) );
    }

    @Test
    public void testPreheatReferenceWithScanCODE()
    {
        DataElementGroup dataElementGroup = fromJson( "preheat/degACodeRef.json", DataElementGroup.class );
        defaultSetup();

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.getObjects().put( DataElementGroup.class, Lists.newArrayList( dataElementGroup ) );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.CODE ) );
        assertFalse( preheat.isEmpty( PreheatIdentifier.CODE, DataElement.class ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE, DataElementGroup.class ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, "DataElementCodeA" ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, "DataElementCodeB" ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, "DataElementCodeC" ) );
    }

    @Test
    public void testPreheatReferenceConnectUID()
    {
        DataElementGroup dataElementGroup = fromJson( "preheat/degAUidRef.json", DataElementGroup.class );
        defaultSetup();

        PreheatParams params = new PreheatParams();
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.getObjects().put( DataElementGroup.class, Lists.newArrayList( dataElementGroup ) );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );
        preheatService.connectReferences( dataElementGroup, preheat, PreheatIdentifier.UID );

        List<DataElement> members = new ArrayList<>( dataElementGroup.getMembers() );

        assertContains( members, "DataElementA", "DataElementCodeA" );
        assertContains( members, "DataElementB", "DataElementCodeB" );
        assertContains( members, "DataElementC", "DataElementCodeC" );

        assertEquals( "FirstNameA", dataElementGroup.getUser().getFirstName() );
        assertEquals( "SurnameA", dataElementGroup.getUser().getSurname() );
        assertEquals( "UserCodeA", dataElementGroup.getUser().getCode() );
    }

    @Test
    public void testPreheatReferenceConnectCODE()
    {
        DataElementGroup dataElementGroup = fromJson( "preheat/degACodeRef.json", DataElementGroup.class );
        defaultSetup();

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.getObjects().put( DataElementGroup.class, Lists.newArrayList( dataElementGroup ) );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );
        preheatService.connectReferences( dataElementGroup, preheat, PreheatIdentifier.CODE );

        List<DataElement> members = new ArrayList<>( dataElementGroup.getMembers() );

        assertContains( members, "DataElementA", "DataElementCodeA" );
        assertContains( members, "DataElementB", "DataElementCodeB" );
        assertContains( members, "DataElementC", "DataElementCodeC" );

        assertEquals( "FirstNameA", dataElementGroup.getUser().getFirstName() );
        assertEquals( "SurnameA", dataElementGroup.getUser().getSurname() );
        assertEquals( "UserCodeA", dataElementGroup.getUser().getCode() );
    }

    @Test
    public void testPreheatReferenceConnectAUTO()
    {
        DataElementGroup dataElementGroup = fromJson( "preheat/degAAutoRef.json", DataElementGroup.class );
        defaultSetup();

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.AUTO );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.getObjects().put( DataElementGroup.class, Lists.newArrayList( dataElementGroup ) );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );
        preheatService.connectReferences( dataElementGroup, preheat, PreheatIdentifier.AUTO );

        List<DataElement> members = new ArrayList<>( dataElementGroup.getMembers() );

        assertContains( members, "DataElementA", "DataElementCodeA" );
        assertContains( members, "DataElementB", "DataElementCodeB" );
        assertContains( members, "DataElementC", "DataElementCodeC" );

        // TODO Fix this
//        assertEquals( "FirstNameA", dataElementGroup.getUser().getFirstName() );
//        assertEquals( "SurnameA", dataElementGroup.getUser().getSurname() );
//        assertEquals( "UserCodeA", dataElementGroup.getUser().getCode() );
    }

    /**
     * Fails with:
     * java.lang.ClassCastException: java.util.HashMap cannot be cast to java.util.Set
     * at org.hisp.dhis.preheat.PreheatServiceTest.testPreheatWithAttributeValues(PreheatServiceTest.java:597)
     *
     * @throws IOException
     */
    @Ignore
    @Test
    public void testPreheatWithAttributeValues() throws IOException
    {
        defaultSetupWithAttributes();

        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "preheat/dataset_with_sections.json" ).getInputStream(), RenderFormat.JSON );

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.AUTO );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.setObjects( metadata );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );

        assertEquals( 1, preheat.getUniqueAttributeValues().get( DataElement.class ).size() );
        List<String> keys = new ArrayList<>( preheat.getUniqueAttributeValues().get( DataElement.class ).keySet() );
        assertEquals( 3, preheat.getUniqueAttributeValues().get( DataElement.class ).get( keys.get( 0 ) ).size() );

        assertFalse( preheat.getMandatoryAttributes().isEmpty() );
        assertEquals( 1, preheat.getMandatoryAttributes().get( DataElement.class ).size() );
    }

    @Test
    public void testPreheatWithDataSetElements()
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = new HashMap<>();

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        DataSet dataSet = createDataSet( 'A' );
        dataSet.setAutoFields();

        dataSet.getDataSetElements().add( new DataSetElement( dataSet, de1 ) );
        dataSet.getDataSetElements().add( new DataSetElement( dataSet, de2 ) );
        dataSet.getDataSetElements().add( new DataSetElement( dataSet, de3 ) );

        metadata.put( DataSet.class, new ArrayList<>() );
        metadata.get( DataSet.class ).add( dataSet );

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.UID );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.setObjects( metadata );

        preheatService.validate( params );
        Preheat preheat = preheatService.preheat( params );

        Map<String, IdentifiableObject> map = preheat.getMap().get( PreheatIdentifier.UID ).get( DataElement.class );
        assertEquals( 3, map.size() );
    }

    @Test
    public void testUserPreheatCollection()
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = new HashMap<>();

        User user1 = createUser( "aaa" );
        User user2 = createUser( "bbb" );
        User user3 = createUser( "ccc" );

        metadata.put( User.class, new ArrayList<>() );
        metadata.get( User.class ).add( user1 );
        metadata.get( User.class ).add( user2 );
        metadata.get( User.class ).add( user3 );

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.setObjects( metadata );

        Preheat preheat = preheatService.preheat( params );
        assertNotNull( preheat.getMap().get( PreheatIdentifier.CODE ) );
        assertNotNull( preheat.getMap().get( PreheatIdentifier.UID ) );

        assertFalse( preheat.getMap().get( PreheatIdentifier.CODE ).isEmpty() );
        assertFalse( preheat.getMap().get( PreheatIdentifier.UID ).isEmpty() );

        assertEquals( 1, preheat.getMap().get( PreheatIdentifier.CODE ).size() );
        assertEquals( 3, preheat.getMap().get( PreheatIdentifier.UID ).size() );
    }

    @Test
    public void testDataElementUserByUidPreheat()
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = new HashMap<>();

        User user1 = createUser( "aaa" );
        DataElement dataElement1 = createDataElement( 'A' );
        dataElement1.setUser( user1 );

        DataElement dataElement2 = createDataElement( 'B' );
        dataElement2.setUser( user1 );

        DataElement dataElement3 = createDataElement( 'C' );
        dataElement3.setUser( user1 );

        metadata.put( DataElement.class, new ArrayList<>() );
        metadata.get( DataElement.class ).add( dataElement1 );
        metadata.get( DataElement.class ).add( dataElement2 );
        metadata.get( DataElement.class ).add( dataElement3 );

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.setObjects( metadata );

        Preheat preheat = preheatService.preheat( params );
        assertNotNull( preheat.getMap().get( PreheatIdentifier.CODE ) );
        assertNotNull( preheat.getMap().get( PreheatIdentifier.UID ) );

        assertFalse( preheat.getMap().get( PreheatIdentifier.CODE ).isEmpty() );
        assertFalse( preheat.getMap().get( PreheatIdentifier.UID ).isEmpty() );

        assertEquals( 1, preheat.getMap().get( PreheatIdentifier.CODE ).size() );
        assertEquals( 2, preheat.getMap().get( PreheatIdentifier.UID ).size() );
    }

    @Test
    public void testDataElementByCodeUserByUidGetUserByUidPreheat()
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = new HashMap<>();

        User user1 = createUser( "aaa" );
        DataElement dataElement1 = createDataElement( 'A' );
        dataElement1.setUser( user1 );

        DataElement dataElement2 = createDataElement( 'B' );
        dataElement2.setUser( user1 );

        DataElement dataElement3 = createDataElement( 'C' );
        dataElement3.setUser( user1 );

        metadata.put( DataElement.class, new ArrayList<>() );
        metadata.get( DataElement.class ).add( dataElement1 );
        metadata.get( DataElement.class ).add( dataElement2 );
        metadata.get( DataElement.class ).add( dataElement3 );

        PreheatParams params = new PreheatParams();
        params.setPreheatIdentifier( PreheatIdentifier.CODE );
        params.setPreheatMode( PreheatMode.REFERENCE );
        params.setObjects( metadata );

        Preheat preheat = preheatService.preheat( params );
        assertNotNull( preheat.getMap().get( PreheatIdentifier.CODE ) );
        assertNotNull( preheat.getMap().get( PreheatIdentifier.UID ) );

        assertFalse( preheat.getMap().get( PreheatIdentifier.CODE ).isEmpty() );
        assertFalse( preheat.getMap().get( PreheatIdentifier.UID ).isEmpty() );

        assertEquals( 1, preheat.getMap().get( PreheatIdentifier.CODE ).size() );
        assertEquals( 2, preheat.getMap().get( PreheatIdentifier.UID ).size() );

        assertNull( preheat.get( PreheatIdentifier.CODE, User.class, "some-user-uid" ) );
        assertNotNull( preheat.get( PreheatIdentifier.CODE, User.class, user1.getUid() ) );
    }

    private void defaultSetup()
    {
        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );
    }

    private void defaultSetupWithAttributes()
    {
        Attribute attribute = new Attribute( "AttributeA", ValueType.TEXT );
        attribute.setUnique( true );
        attribute.setMandatory( true );
        attribute.setDataElementAttribute( true );

        manager.save( attribute );

        AttributeValue attributeValue1 = new AttributeValue( "Value1", attribute );
        AttributeValue attributeValue2 = new AttributeValue( "Value2", attribute );
        AttributeValue attributeValue3 = new AttributeValue( "Value3", attribute );

        DataElement de1 = createDataElement( 'A' );
        DataElement de2 = createDataElement( 'B' );
        DataElement de3 = createDataElement( 'C' );

        attributeService.addAttributeValue( de1, attributeValue1 );
        attributeService.addAttributeValue( de2, attributeValue2 );
        attributeService.addAttributeValue( de3, attributeValue3 );

        manager.save( de1 );
        manager.save( de2 );
        manager.save( de3 );

        User user = createUser( 'A' );
        manager.save( user );
    }

    private void assertContains( List<DataElement> dataElements, String name, String code )
    {

        for ( DataElement dataElement : dataElements )
        {

            if ( dataElement.getCode().equals( code ) && dataElement.getName().equals( name ) )
            {

                return;
            }
        }
        fail( "The collection does not contain a DataElement with name: [" + name + "] and code: [" + code + "]" );
    }
}
