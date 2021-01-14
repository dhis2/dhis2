package org.hisp.dhis.analytics.event.data;

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

import static org.hisp.dhis.analytics.event.data.JdbcEventAnalyticsManager.ExceptionHandler.handle;
import static org.hisp.dhis.feedback.ErrorCode.E7132;
import static org.hisp.dhis.feedback.ErrorCode.E7133;
import static org.junit.Assert.assertThrows;
import static org.postgresql.util.PSQLState.BAD_DATETIME_FORMAT;
import static org.postgresql.util.PSQLState.DIVISION_BY_ZERO;

import org.hisp.dhis.common.QueryRuntimeException;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;

public class JdbcEventAnalyticsManagerTest
{
    @Test
    public void testHandlingDataIntegrityExceptionWhenDivisionByZero()
    {
        // Given
        final DataIntegrityViolationException aDivisionByZeroException = mockDataIntegrityExceptionDivisionByZero();

        // When
        assertThrows( E7132.getMessage(), QueryRuntimeException.class, () -> handle( aDivisionByZeroException ) );
    }

    @Test
    public void testHandlingAnyOtherDataIntegrityException()
    {
        // Given
        final DataIntegrityViolationException anyDataIntegrityException = mockAnyOtherDataIntegrityException();

        // When
        assertThrows( E7133.getMessage(), QueryRuntimeException.class, () -> handle( anyDataIntegrityException ) );
    }

    @Test
    public void testHandlingWhenExceptionIsNull()
    {
        // Given
        final DataIntegrityViolationException aNullException = null;

        // When
        assertThrows( E7133.getMessage(), QueryRuntimeException.class, () -> handle( aNullException ) );
    }

    @Test
    public void testHandlingWhenExceptionCauseNull()
    {
        // Given
        final DataIntegrityViolationException aNullExceptionCause = new DataIntegrityViolationException( "null",
            null );

        assertThrows( E7133.getMessage(), QueryRuntimeException.class, () -> handle( aNullExceptionCause ) );
    }

    @Test
    public void testHandlingWhenExceptionCauseIsNotPSQLException()
    {
        // Given
        final ArrayIndexOutOfBoundsException aRandomCause = new ArrayIndexOutOfBoundsException();
        final DataIntegrityViolationException aNonPSQLExceptionCause = new DataIntegrityViolationException(
            "not caused by PSQLException", aRandomCause );

        // When
        assertThrows( E7133.getMessage(), QueryRuntimeException.class, () -> handle( aNonPSQLExceptionCause ) );
    }

    private DataIntegrityViolationException mockDataIntegrityExceptionDivisionByZero()
    {
        final PSQLException psqlException = new PSQLException( "ERROR: division by zero", DIVISION_BY_ZERO );

        return new DataIntegrityViolationException(
            "ERROR: division by zero; nested exception is org.postgresql.util.PSQLException: ERROR: division by zero",
            psqlException );
    }

    private DataIntegrityViolationException mockAnyOtherDataIntegrityException()
    {
        final PSQLException psqlException = new PSQLException( "ERROR: bad time format", BAD_DATETIME_FORMAT );

        return new DataIntegrityViolationException(
            "ERROR: bad time format; nested exception is org.postgresql.util.PSQLException: ERROR: bad time format",
            psqlException );
    }
}
