package org.hisp.dhis.system.util;

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

import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DigitsValueTypeOptions;
import org.hisp.dhis.common.FileTypeValueOptions;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static org.hisp.dhis.system.util.ValidationUtils.bboxIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.coordinateIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;
import static org.hisp.dhis.system.util.ValidationUtils.emailIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.expressionIsValidSQl;
import static org.hisp.dhis.system.util.ValidationUtils.getLatitude;
import static org.hisp.dhis.system.util.ValidationUtils.getLongitude;
import static org.hisp.dhis.system.util.ValidationUtils.isValidHexColor;
import static org.hisp.dhis.system.util.ValidationUtils.normalizeBoolean;
import static org.hisp.dhis.system.util.ValidationUtils.passwordIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.usernameIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.uuidIsValid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Lars Helge Overland
 */
public class ValidationUtilsTest
{
    @Test
    public void testCoordinateIsValid()
    {
        assertTrue( coordinateIsValid( "[+37.99034,-28.94221]" ) );
        assertTrue( coordinateIsValid( "[37.99034,-28.94221]" ) );
        assertTrue( coordinateIsValid( "[+37.99034,28.94221]" ) );
        assertTrue( coordinateIsValid( "[170.99034,78.94221]" ) );
        assertTrue( coordinateIsValid( "[-167,-28.94221]" ) );
        assertTrue( coordinateIsValid( "[37.99034,28]" ) );

        assertFalse( coordinateIsValid( "23.34343,56.3232" ) );
        assertFalse( coordinateIsValid( "23.34343 56.3232" ) );
        assertFalse( coordinateIsValid( "[23.34f43,56.3232]" ) );
        assertFalse( coordinateIsValid( "23.34343,56.323.2" ) );
        assertFalse( coordinateIsValid( "[23.34343,56..3232]" ) );
        assertFalse( coordinateIsValid( "[++37,-28.94221]" ) );
        assertFalse( coordinateIsValid( "S-0.27726 E37.08472" ) );
        assertFalse( coordinateIsValid( null ) );

        assertFalse( coordinateIsValid( "-185.12345,45.45423" ) );
        assertFalse( coordinateIsValid( "192.56789,-45.34332" ) );
        assertFalse( coordinateIsValid( "140.34,92.23323" ) );
        assertFalse( coordinateIsValid( "123.34,-94.23323" ) );
        assertFalse( coordinateIsValid( "000.34,-94.23323" ) );
        assertFalse( coordinateIsValid( "123.34,-00.23323" ) );
    }

    @Test
    public void testBboxIsValid()
    {
        assertTrue( bboxIsValid( "-13.2682125,7.3721619,-10.4261178,9.904012" ) );
        assertTrue( bboxIsValid( "12.26821,-23.3721,13.4261,-21.904" ) );
        assertTrue( bboxIsValid( "4,-23.37,5,-24.904" ) );
        assertTrue( bboxIsValid( "2.23, -23.37, 5.22, -24.90" ) );
        assertTrue( bboxIsValid( "-179.234,-89.342,178.323,88.135" ) );

        assertFalse( bboxIsValid( "[12.23,14.41,34.12,12.45]" ) );
        assertFalse( bboxIsValid( "22,23,14,41,34,11,11,41" ) );
        assertFalse( bboxIsValid( "22,23.14,41.34,11.11,41" ) );
        assertFalse( bboxIsValid( "-181.234,-89.342,178.323,88.135" ) );
        assertFalse( bboxIsValid( "-179.234,-92.342,178.323,88.135" ) );
        assertFalse( bboxIsValid( "-179.234,-89.342,185.323,88.135" ) );
        assertFalse( bboxIsValid( "-179.234,-89.342,178.323,94.135" ) );
    }

    @Test
    public void testGetLongitude()
    {
        assertEquals( "+37.99034", getLongitude( "[+37.99034,-28.94221]" ) );
        assertEquals( "37.99034", getLongitude( "[37.99034,28.94221]" ) );
        assertNull( getLongitude( "23.34343,56.3232" ) );
        assertNull( getLongitude( null ) );
    }

    @Test
    public void testGetLatitude()
    {
        assertEquals( "-28.94221", getLatitude( "[+37.99034,-28.94221]" ) );
        assertEquals( "28.94221", getLatitude( "[37.99034,28.94221]" ) );
        assertNull( getLatitude( "23.34343,56.3232" ) );
        assertNull( getLatitude( null ) );
    }

    @Test
    public void testPasswordIsValid()
    {
        assertFalse( passwordIsValid( "Johnd1" ) );
        assertFalse( passwordIsValid( "johndoe1" ) );
        assertFalse( passwordIsValid( "Johndoedoe" ) );
        assertTrue( passwordIsValid( "Johndoe1" ) );
    }

    @Test
    public void testEmailIsValid()
    {
        assertFalse( emailIsValid( "john@doe" ) );
        assertTrue( emailIsValid( "john@doe.com" ) );
    }

    @Test
    public void testUuidIsValid()
    {
        assertTrue( uuidIsValid( "0b976c48-4577-437b-bba6-794d0e7ebde0" ) );
        assertTrue( uuidIsValid( "38052fd0-8c7a-4330-ac45-2c53b3a41a78" ) );
        assertTrue( uuidIsValid( "50be5898-2413-465f-91b9-aced950fc3ab" ) );

        assertFalse( uuidIsValid( "Jjg3j3-412-1435-342-jajg8234f" ) );
        assertFalse( uuidIsValid( "6cafdc73_2ca4_4c52-8a0a-d38adec33b24" ) );
        assertFalse( uuidIsValid( "e1809673dbf3482d8f84e493c65f74d9" ) );
    }

    @Test
    public void testUsernameIsValid()
    {
        assertTrue( usernameIsValid( "johnmichaeldoe" ) );
        assertTrue( usernameIsValid( "ted@johnson.com" ) );
        assertTrue( usernameIsValid( "harry@gmail.com" ) );

        assertFalse( usernameIsValid( null ) );
        assertFalse( usernameIsValid( CodeGenerator.generateCode( 400 ) ) );
    }

    @Test
    public void testDataValueIsZeroAndInsignificant()
    {
        DataElement de = new DataElement( "DEA" );
        de.setValueType( ValueType.INTEGER );
        de.setAggregationType( AggregationType.SUM );

        assertTrue( dataValueIsZeroAndInsignificant( "0", de ) );

        de.setAggregationType( AggregationType.AVERAGE_SUM_ORG_UNIT );
        assertFalse( dataValueIsZeroAndInsignificant( "0", de ) );
    }

    @Test
    public void testDataValueIsValid()
    {
        DataElement de = new DataElement( "DEA" );
        de.setValueType( ValueType.INTEGER );

        assertNull( dataValueIsValid( null, de ) );
        assertNull( dataValueIsValid( "", de ) );

        assertNull( dataValueIsValid( "34", de ) );
        assertNotNull( dataValueIsValid( "Yes", de ) );

        de.setValueType( ValueType.NUMBER );

        assertNull( dataValueIsValid( "3.7", de ) );
        assertNotNull( dataValueIsValid( "No", de ) );

        de.setValueType( ValueType.INTEGER_POSITIVE );

        assertNull( dataValueIsValid( "3", de ) );
        assertNotNull( dataValueIsValid( "-4", de ) );

        de.setValueType( ValueType.INTEGER_ZERO_OR_POSITIVE );

        assertNull( dataValueIsValid( "3", de ) );
        assertNotNull( dataValueIsValid( "-4", de ) );

        de.setValueType( ValueType.INTEGER_NEGATIVE );

        assertNull( dataValueIsValid( "-3", de ) );
        assertNotNull( dataValueIsValid( "4", de ) );

        de.setValueType( ValueType.TEXT );

        assertNull( dataValueIsValid( "0", de ) );

        de.setValueType( ValueType.BOOLEAN );

        assertNull( dataValueIsValid( "true", de ) );
        assertNull( dataValueIsValid( "false", de ) );
        assertNull( dataValueIsValid( "FALSE", de ) );
        assertNotNull( dataValueIsValid( "yes", de ) );

        de.setValueType( ValueType.TRUE_ONLY );

        assertNull( dataValueIsValid( "true", de ) );
        assertNull( dataValueIsValid( "TRUE", de ) );
        assertNotNull( dataValueIsValid( "false", de ) );

        de.setValueType( ValueType.DATE );
        assertNull( dataValueIsValid( "2013-04-01", de ) );
        assertNotNull( dataValueIsValid( "2012304-01", de ) );
        assertNotNull( dataValueIsValid( "Date", de ) );

        de.setValueType( ValueType.DATETIME );
        assertNull( dataValueIsValid( "2013-04-01T11:00:00.000Z", de ) );
        assertNotNull( dataValueIsValid( "2013-04-01", de ) );
        assertNotNull( dataValueIsValid( "abcd", de ) );
    }

    @Test
    public void testIsValidHexColor()
    {
        assertFalse( isValidHexColor( "abcpqr" ) );
        assertFalse( isValidHexColor( "#qwerty" ) );
        assertFalse( isValidHexColor( "FFAB#O" ) );

        assertTrue( isValidHexColor( "#FF0" ) );
        assertTrue( isValidHexColor( "#FF0000" ) );
        assertTrue( isValidHexColor( "FFFFFF" ) );
        assertTrue( isValidHexColor( "ffAAb4" ) );
        assertTrue( isValidHexColor( "#4a6" ) );
        assertTrue( isValidHexColor( "abc" ) );
    }

    @Test
    public void testExpressionIsValidSQl()
    {
        assertFalse( expressionIsValidSQl( "10 == 10; delete from table" ) );
        assertFalse( expressionIsValidSQl( "select from table" ) );

        assertTrue( expressionIsValidSQl( "\"abcdef12345\" < 30" ) );
        assertTrue( expressionIsValidSQl( "\"abcdef12345\" >= \"bcdefg23456\"" ) );
        assertTrue( expressionIsValidSQl( "\"DO0v7fkhUNd\" > -30000 and \"DO0v7fkhUNd\" < 30000" ) );
        assertTrue( expressionIsValidSQl( "\"oZg33kd9taw\" == 'Female'" ) );
        assertTrue( expressionIsValidSQl( "\"oZg33kd9taw\" == 'Female' and \"qrur9Dvnyt5\" <= 5" ) );
    }

    @Test
    public void testNormalizeBoolean()
    {
        assertEquals( "true", normalizeBoolean( "1", ValueType.BOOLEAN ) );
        assertEquals( "true", normalizeBoolean( "T", ValueType.BOOLEAN ) );
        assertEquals( "true", normalizeBoolean( "true", ValueType.BOOLEAN ) );
        assertEquals( "true", normalizeBoolean( "TRUE", ValueType.BOOLEAN ) );
        assertEquals( "true", normalizeBoolean( "t", ValueType.BOOLEAN ) );

        assertEquals( "test", normalizeBoolean( "test", ValueType.TEXT ) );

        assertEquals( "false", normalizeBoolean( "0", ValueType.BOOLEAN ) );
        assertEquals( "false", normalizeBoolean( "f", ValueType.BOOLEAN ) );
        assertEquals( "false", normalizeBoolean( "False", ValueType.BOOLEAN ) );
        assertEquals( "false", normalizeBoolean( "FALSE", ValueType.BOOLEAN ) );
        assertEquals( "false", normalizeBoolean( "F", ValueType.BOOLEAN ) );
    }

    @Test
    public void testFileValueTypeOptionValidation()
        throws IOException
    {
        long oneHundredMegaBytes = 1024 * (1024 * 100L);

        ValueType valueType = ValueType.FILE_RESOURCE;

        FileTypeValueOptions options = new FileTypeValueOptions();
        options.setMaxFileSize( oneHundredMegaBytes );
        options.setAllowedContentTypes( ImmutableSet.of( "jpg", "pdf" ) );

        File file = makeTempFile( oneHundredMegaBytes, "jpg" );
        assertNull( dataValueIsValid( file.getAbsolutePath(), valueType, options ) );

        assertEquals( "not_valid_file_do_not_exist",
            dataValueIsValid( new File( "/this_is_not_a_valid_path" ).getAbsolutePath(), valueType, options ) );

        file = makeTempFile( 1024 * (1024 * 101L), "jpg" );
        assertEquals( "not_valid_file_size_too_big",
            dataValueIsValid( file.getAbsolutePath(), valueType, options ) );

        file = makeTempFile( oneHundredMegaBytes, "com" );
        assertEquals( "not_valid_file_extension",
            dataValueIsValid( file.getAbsolutePath(), valueType, options ) );

        assertEquals( "not_valid_value_type_option_class",
            dataValueIsValid( file.getAbsolutePath(), valueType, new DigitsValueTypeOptions() ) );
    }

    @Test
    public void testDigitsValueTypeOptionsValidationWithNumber()
    {
        ValueType valueType = ValueType.NUMBER;

        DigitsValueTypeOptions options = new DigitsValueTypeOptions();
        options.setInteger( 1 );
        options.setFraction( 1 );

        assertNull( dataValueIsValid( 1D, valueType, options ) );
        assertNull( dataValueIsValid( 1.0D, valueType, options ) );
        assertNull( dataValueIsValid( 1.000D, valueType, options ) );

        assertEquals( "not_valid_number", dataValueIsValid( 11D, valueType, options ) );
        assertEquals( "not_valid_number", dataValueIsValid( 1.01D, valueType, options ) );
    }

    @Test
    public void testDigitsValueTypeOptionsValidationWithString()
    {
        ValueType valueType = ValueType.DIGITS;

        DigitsValueTypeOptions options = new DigitsValueTypeOptions();
        options.setInteger( 1 );
        options.setFraction( 1 );

        assertNull( dataValueIsValid( "1", valueType, options ) );
        assertNull( dataValueIsValid( "1.0", valueType, options ) );

        assertEquals( "not_valid_digits", dataValueIsValid( "11", valueType, options ) );
        assertEquals( "not_valid_digits", dataValueIsValid( "1.00", valueType, options ) );
        assertEquals( "not_valid_digits", dataValueIsValid( "1.01", valueType, options ) );
    }

    private static File makeTempFile( long length, String extension )
        throws IOException
    {
        File file = File.createTempFile( "test", "." + extension );
        file.deleteOnExit();

        RandomAccessFile f = new RandomAccessFile( file, "rw" );
        f.setLength( length );

        return file;
    }
}
