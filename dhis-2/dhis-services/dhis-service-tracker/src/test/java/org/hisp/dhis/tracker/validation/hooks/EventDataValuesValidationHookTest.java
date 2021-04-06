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
package org.hisp.dhis.tracker.validation.hooks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.util.DateUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
public class EventDataValuesValidationHookTest
{
    private final static String VALID_DATA_ELEMENT = "validDataElement";

    private EventDataValuesValidationHook hookToTest;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerImportValidationContext validationContext;

    @Mock
    private Event event;

    @Before
    public void setUp()
    {
        hookToTest = new EventDataValuesValidationHook();

        DataElement validDataElement = new DataElement();
        validDataElement.setValueType( ValueType.TEXT );

        TrackerBundle bundle = TrackerBundle.builder().build();

        when( validationContext.getBundle() ).thenReturn( bundle );
        when( validationContext.getDataElement( VALID_DATA_ELEMENT ) ).thenReturn( validDataElement );
    }

    @Test
    public void successValidationWhenDataElementIsValid()
    {
        // Given
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue() ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), empty() );
    }

    @Test
    public void successValidationWhenCreatedAtIsNull()
    {
        // Given
        DataValue validDataValue = validDataValue();
        validDataValue.setCreatedAt( null );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void failValidationWhenUpdatedAtIsNull()
    {
        // Given
        DataValue validDataValue = validDataValue();
        validDataValue.setUpdatedAt( null );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void failValidationWhenDataElementIsInvalid()
    {
        // Given
        DataValue validDataValue = validDataValue();
        validDataValue.setDataElement( "INVALID_DE" );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1304, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenMandatoryDataElementIsNotPresent()
    {
        // Given

        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageDataElement = new ProgramStageDataElement();
        DataElement dataElement = new DataElement();
        dataElement.setUid( "MANDATORY_DE" );
        mandatoryStageDataElement.setDataElement( dataElement );
        mandatoryStageDataElement.setCompulsory( true );

        ProgramStageDataElement stageDataElement = new ProgramStageDataElement();
        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue().getDataElement() );
        stageDataElement.setDataElement( validDataElement );
        stageDataElement.setCompulsory( true );

        programStage.setProgramStageDataElements( Sets.newHashSet( mandatoryStageDataElement, stageDataElement ) );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue() ) );
        when( event.getProgramStage() ).thenReturn( "PROGRAM_STAGE" );
        when( event.getStatus() ).thenReturn( EventStatus.COMPLETED );
        when( validationContext.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1303, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failSuccessWhenMandatoryDataElementIsNotPresentButMandatoryValidationIsNotNeeded()
    {
        // Given
        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageDataElement = new ProgramStageDataElement();
        DataElement dataElement = new DataElement();
        dataElement.setUid( "MANDATORY_DE" );
        mandatoryStageDataElement.setDataElement( dataElement );
        mandatoryStageDataElement.setCompulsory( true );

        ProgramStageDataElement stageDataElement = new ProgramStageDataElement();
        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue().getDataElement() );
        stageDataElement.setDataElement( validDataElement );
        stageDataElement.setCompulsory( true );

        programStage.setProgramStageDataElements( Sets.newHashSet( mandatoryStageDataElement, stageDataElement ) );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue() ) );
        when( event.getProgramStage() ).thenReturn( "PROGRAM_STAGE" );
        when( event.getStatus() ).thenReturn( EventStatus.ACTIVE );
        when( validationContext.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), empty() );
    }

    @Test
    public void failValidationWhenDataElementIsNotPresentInProgramStage()
    {
        // Given
        ProgramStage programStage = new ProgramStage();
        ProgramStageDataElement mandatoryStageDataElement = new ProgramStageDataElement();
        DataElement dataElement = new DataElement();
        dataElement.setUid( validDataValue().getDataElement() );
        mandatoryStageDataElement.setDataElement( dataElement );
        mandatoryStageDataElement.setCompulsory( true );

        DataValue notPresentDataValue = validDataValue();
        notPresentDataValue.setDataElement( "de_not_present_in_progam_stage" );

        DataElement notPresentDataElement = new DataElement();
        notPresentDataElement.setValueType( ValueType.TEXT );
        notPresentDataElement.setUid( "de_not_present_in_progam_stage" );

        programStage.setProgramStageDataElements( Sets.newHashSet( mandatoryStageDataElement ) );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue(), notPresentDataValue ) );
        when( event.getProgramStage() ).thenReturn( "PROGRAM_STAGE" );
        when( event.getStatus() ).thenReturn( EventStatus.ACTIVE );
        when( validationContext.getProgramStage( "PROGRAM_STAGE" ) ).thenReturn( programStage );
        when( validationContext.getDataElement( "de_not_present_in_progam_stage" ) )
            .thenReturn( notPresentDataElement );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1305, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenDataElementValueTypeIsNull()
    {
        // Given
        DataValue validDataValue = validDataValue();
        validDataValue.setDataElement( "INVALID_DE" );

        DataElement invalidDataElement = new DataElement();
        invalidDataElement.setUid( "INVALID_DE" );
        invalidDataElement.setValueType( null );

        when( validationContext.getDataElement( VALID_DATA_ELEMENT ) ).thenReturn( invalidDataElement );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue() ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1302, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenFileResourceIsNull()
    {
        // Given
        DataValue validDataValue = validDataValue();
        validDataValue.setValue( "QX4LpiTZmUH" );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.FILE_RESOURCE );

        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getFileResource( validDataValue.getDataElement() ) ).thenReturn( null );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1084, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenFileResourceIsAlreadyAssigned()
    {
        // Given
        DataValue validDataValue = validDataValue();
        validDataValue.setValue( "QX4LpiTZmUH" );

        DataElement validDataElement = new DataElement();
        validDataElement.setUid( validDataValue.getDataElement() );
        validDataElement.setValueType( ValueType.FILE_RESOURCE );

        FileResource fileResource = new FileResource();
        fileResource.setAssigned( true );

        when( validationContext.getDataElement( validDataValue.getDataElement() ) ).thenReturn( validDataElement );
        when( validationContext.getFileResource( validDataValue.getValue() ) ).thenReturn( fileResource );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1009, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    @Test
    public void failValidationWhenDataElementValueTypeIsInvalid()
    {
        runAndAssertValidationForDataValue( ValueType.NUMBER, "not_a_number" );
        runAndAssertValidationForDataValue( ValueType.UNIT_INTERVAL, "3" );
        runAndAssertValidationForDataValue( ValueType.PERCENTAGE, "1234" );
        runAndAssertValidationForDataValue( ValueType.INTEGER, "10.5" );
        runAndAssertValidationForDataValue( ValueType.INTEGER_POSITIVE, "-10" );
        runAndAssertValidationForDataValue( ValueType.INTEGER_NEGATIVE, "+10" );
        runAndAssertValidationForDataValue( ValueType.INTEGER_ZERO_OR_POSITIVE, "-10" );
        runAndAssertValidationForDataValue( ValueType.BOOLEAN, "not_a_bool" );
        runAndAssertValidationForDataValue( ValueType.TRUE_ONLY, "false" );
        runAndAssertValidationForDataValue( ValueType.DATE, "wrong_date" );
        runAndAssertValidationForDataValue( ValueType.DATETIME, "wrong_date_time" );
        runAndAssertValidationForDataValue( ValueType.COORDINATE, "10" );
        runAndAssertValidationForDataValue( ValueType.URL, "not_valid_url" );
        runAndAssertValidationForDataValue( ValueType.FILE_RESOURCE, "not_valid_uid" );
    }

    @Test
    public void successValidationDataElementOptionValueIsValid()
    {
        DataValue validDataValue = validDataValue();
        validDataValue.setDataElement( "de" );
        validDataValue.setValue( "code" );

        DataElement dataElement = new DataElement();
        dataElement.setUid( "de" );
        dataElement.setValueType( ValueType.TEXT );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );

        Option option1 = new Option();
        option1.setCode( "CODE1" );

        optionSet.setOptions( Arrays.asList( option, option1 ) );

        dataElement.setOptionSet( optionSet );

        when( validationContext.getDataElement( "de" ) ).thenReturn( dataElement );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        assertFalse( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 0 ) );
    }

    @Test
    public void failValidationDataElementOptionValueIsInValid()
    {
        DataValue validDataValue = validDataValue();
        validDataValue.setDataElement( "de" );
        validDataValue.setValue( "value" );

        DataElement dataElement = new DataElement();
        dataElement.setUid( "de" );
        dataElement.setValueType( ValueType.TEXT );

        OptionSet optionSet = new OptionSet();
        Option option = new Option();
        option.setCode( "CODE" );

        Option option1 = new Option();
        option1.setCode( "CODE1" );

        optionSet.setOptions( Arrays.asList( option, option1 ) );

        dataElement.setOptionSet( optionSet );

        when( validationContext.getDataElement( "de" ) ).thenReturn( dataElement );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        assertTrue( reporter.hasErrors() );
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( 1, reporter.getReportList().stream()
            .filter( e -> e.getErrorCode() == TrackerErrorCode.E1125 ).count() );
    }

    private void runAndAssertValidationForDataValue( ValueType valueType, String value )
    {
        // Given
        DataValue validDataValue = validDataValue();
        validDataValue.setDataElement( "INVALID_DE" );
        validDataValue.setValue( value );

        DataElement invalidDataElement = new DataElement();
        invalidDataElement.setUid( "INVALID_DE" );
        invalidDataElement.setValueType( valueType );

        when( validationContext.getDataElement( "INVALID_DE" ) ).thenReturn( invalidDataElement );
        when( event.getDataValues() ).thenReturn( Sets.newHashSet( validDataValue ) );

        // When
        ValidationErrorReporter reporter = new ValidationErrorReporter( validationContext, event );
        hookToTest.validateEvent( reporter, event );

        // Then
        assertThat( reporter.getReportList(), hasSize( 1 ) );
        assertEquals( TrackerErrorCode.E1302, reporter.getReportList().get( 0 ).getErrorCode() );
    }

    private DataValue validDataValue()
    {
        DataValue dataValue = new DataValue();
        dataValue.setCreatedAt( DateUtils.instantFromDateAsString( "2020-10-10" ) );
        dataValue.setUpdatedAt( DateUtils.instantFromDateAsString( "2020-10-10" ) );
        dataValue.setValue( "text" );
        dataValue.setDataElement( "validDataElement" );
        return dataValue;
    }
}
