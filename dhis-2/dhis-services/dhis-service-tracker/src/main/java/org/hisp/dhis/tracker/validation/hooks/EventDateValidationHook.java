package org.hisp.dhis.tracker.validation.hooks;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.api.client.util.Preconditions.checkNotNull;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1042;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1043;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1046;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1047;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1051;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1052;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

import java.util.Date;

import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class EventDateValidationHook
    extends AbstractTrackerDtoValidationHook
{
    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        Program program = context.getProgram( event.getProgram() );

        if ( event.getOccurredAt() == null && !allowBlankOccuredAtDate( event, program ) )
        {
            reporter.addError( newReport( TrackerErrorCode.E1031 )
                .addArg( event ) );
            return;
        }

        validateDateFormat( reporter, event );
        validateExpiryDays( reporter, event, program );
        validatePeriodType( reporter, event, program );
    }

    private void validateExpiryDays( ValidationErrorReporter reporter, Event event, Program program )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();
        User actingUser = context.getBundle().getUser();

        checkNotNull( actingUser, TrackerImporterAssertErrors.USER_CANT_BE_NULL );
        checkNotNull( event, TrackerImporterAssertErrors.EVENT_CANT_BE_NULL );
        checkNotNull( program, TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL );

        if ( (program.getCompleteEventsExpiryDays() > 0 && EventStatus.COMPLETED == event.getStatus()) )
        {
            if ( actingUser.isAuthorized( Authorities.F_EDIT_EXPIRED.getAuthority() ) )
            {
                return;
            }

            Date completedDate = null;

            if ( event.getCompletedAt() != null )
            {
                completedDate = DateUtils.parseDate( event.getCompletedAt() );
            }

            addErrorIfNull( completedDate, reporter, E1042, event );

            if ( completedDate != null && (new Date())
                .after( DateUtils.getDateAfterAddition( completedDate, program.getCompleteEventsExpiryDays() ) ) )
            {
                addError( reporter, E1043, event );
            }
        }
    }

    private void validatePeriodType( ValidationErrorReporter reporter, Event event, Program program )
    {
        checkNotNull( event, TrackerImporterAssertErrors.EVENT_CANT_BE_NULL );
        checkNotNull( program, TrackerImporterAssertErrors.PROGRAM_CANT_BE_NULL );

        PeriodType periodType = program.getExpiryPeriodType();

        if ( periodType == null || program.getExpiryDays() == 0 )
        {
            // Nothing more to check here, return out
            return;
        }

        String referenceDate = event.getOccurredAt() != null ? event.getOccurredAt() : event.getScheduledAt();

        addErrorIfNull( referenceDate, reporter, E1046, event );

        Period period = periodType.createPeriod( new Date() );

        if ( DateUtils.parseDate( referenceDate ).before( period.getStartDate() ) )
        {
            addError( reporter, E1047, event );
        }
    }

    private void validateDateFormat( ValidationErrorReporter reporter, Event event )
    {
        checkNotNull( event, TrackerImporterAssertErrors.EVENT_CANT_BE_NULL );

        if ( event.getScheduledAt() != null && isNotValidDateString( event.getScheduledAt() ) )
        {
            addError( reporter, E1051, event.getScheduledAt() );
        }

        if ( event.getOccurredAt() != null && isNotValidDateString( event.getOccurredAt() ) )
        {
            addError( reporter, E1052, event.getScheduledAt() );
        }
    }

    private boolean allowBlankOccuredAtDate( Event event, Program program )
    {
        if ( program.isWithoutRegistration() )
        {
            return false;
        }

        EventStatus eventStatus = event.getStatus();

        return eventStatus == EventStatus.SCHEDULE || eventStatus == EventStatus.OVERDUE || eventStatus == EventStatus.SKIPPED ? true : false;
    }
}