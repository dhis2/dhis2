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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.analytics.event.HeaderName.ENROLLMENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.EVENT_DATE;
import static org.hisp.dhis.analytics.event.HeaderName.INCIDENT_DATE;
import static org.hisp.dhis.analytics.event.data.GridHeaderMapper.getHeaderName;

import org.hisp.dhis.analytics.event.HeaderName;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.Test;

/**
 * Unit tests for GridHeaderMapper.
 *
 * @author maikel arabori
 */
public class GridHeaderMapperTest
{
    @Test
    public void testGetHeaderNameFor_NAME_EVENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();

        // When
        final String actualName = getHeaderName( aMockedProgramStageWithLabels, EVENT_DATE );

        // Then
        assertThat( actualName, is( aMockedProgramStageWithLabels.getExecutionDateLabel() ) );
    }

    @Test
    public void testGetHeaderNameFor_NAME_ENROLLMENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();

        // When
        final String actualName = getHeaderName( aMockedProgramStageWithLabels, ENROLLMENT_DATE );

        // Then
        assertThat( actualName, is( aMockedProgramStageWithLabels.getProgram().getEnrollmentDateLabel() ) );
    }

    @Test
    public void testGetHeaderNameFor_NAME_INCIDENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();

        // When
        final String actualName = getHeaderName( aMockedProgramStageWithLabels, INCIDENT_DATE );

        // Then
        assertThat( actualName, is( aMockedProgramStageWithLabels.getProgram().getIncidentDateLabel() ) );
    }

    @Test
    public void testGetHeaderNameWhenNoLabelIsSetFor_NAME_EVENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithNoLabels = mockProgramStageWithoutLabels();

        // When
        final String actualName = getHeaderName( aMockedProgramStageWithNoLabels, EVENT_DATE );

        // Then
        assertThat( actualName, is( EVENT_DATE.value() ) );
    }

    @Test
    public void testGetHeaderNameWhenNoLabelIsSetFor_NAME_ENROLLMENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithNoLabels = mockProgramStageWithoutLabels();

        // When
        final String actualName = getHeaderName( aMockedProgramStageWithNoLabels, ENROLLMENT_DATE );

        // Then
        assertThat( actualName, is( ENROLLMENT_DATE.value() ) );
    }

    @Test
    public void testGetHeaderNameWhenNoLabelIsSetFor_NAME_INCIDENT_DATE()
    {
        // Given
        final ProgramStage aMockedProgramStageWithNoLabels = mockProgramStageWithoutLabels();

        // When
        final String actualName = getHeaderName( aMockedProgramStageWithNoLabels, INCIDENT_DATE );

        // Then
        assertThat( actualName, is( INCIDENT_DATE.value() ) );
    }

    @Test
    public void testGetHeaderNameWhenProgramStageIsNull()
    {
        // Given
        final ProgramStage nullProgramStage = null;

        // When
        final String actualName = getHeaderName( nullProgramStage, INCIDENT_DATE );

        // Then
        assertThat( actualName, is( INCIDENT_DATE.value() ) );
    }

    @Test
    public void testGetHeaderNameWhenProgramIsNull()
    {
        // Given
        final ProgramStage programStageWithNullProgram = mockProgramStageWithNullProgram();

        // When
        final String actualName = getHeaderName( programStageWithNullProgram, ENROLLMENT_DATE );

        // Then
        assertThat( actualName, is( ENROLLMENT_DATE.value() ) );
    }

    @Test
    public void testGetHeaderNameWhenHeaderNameIsNull()
    {
        // Given
        final ProgramStage aMockedProgramStageWithLabels = mockProgramStageWithLabels();
        final HeaderName aNullHeaderName = null;

        // When
        final String actualName = getHeaderName( aMockedProgramStageWithLabels, aNullHeaderName );

        // Then
        assertThat( actualName, is( EMPTY ) );
    }

    private ProgramStage mockProgramStageWithLabels()
    {
        final ProgramStage programStage = new ProgramStage();
        programStage.setExecutionDateLabel( "execution date label" );

        final Program program = new Program();
        program.setEnrollmentDateLabel( "enrollment date label" );
        program.setIncidentDateLabel( "incident date label" );

        programStage.setProgram( program );

        return programStage;
    }

    private ProgramStage mockProgramStageWithoutLabels()
    {
        final ProgramStage programStage = new ProgramStage();
        final Program program = new Program();

        programStage.setProgram( program );

        return programStage;
    }

    private ProgramStage mockProgramStageWithNullProgram()
    {
        final ProgramStage programStage = new ProgramStage();

        programStage.setProgram( null );

        return programStage;
    }
}
