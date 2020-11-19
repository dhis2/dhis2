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

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1118;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1120;

import java.util.Optional;

import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.TrackerWarningReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

@Component
public class AssignedUserValidationHook
    extends AbstractTrackerDtoValidationHook
{
    public AssignedUserValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( teAttrService );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        if ( event.getAssignedUser() != null )
        {
            if ( isNotValidAssignedUserUid( event ) || userNotPresentInPreheat( reporter, event ) )
            {
                TrackerErrorReport.TrackerErrorReportBuilder report = TrackerErrorReport.builder()
                    .errorCode( E1118 )
                    .trackerType( TrackerType.EVENT )
                    .addArg( event.getAssignedUser() );

                reporter.addError( report );
            }
            if ( isNotEnabledUserAssignment( reporter, event ) )
            {
                reporter.addWarning( TrackerWarningReport.builder()
                    .warningCode( E1120 )
                    .trackerType( TrackerType.EVENT )
                    .addArg( event.getProgramStage() ) );
            }
        }
    }

    private Boolean isNotEnabledUserAssignment( ValidationErrorReporter reporter, Event event )
    {
        /*
         * TODO: should we have an helper method in base class (or a dedicated service)
         * to easily access preheat maps ? this is hard to read
         */

        Boolean userAssignmentEnabled = ((ProgramStage) reporter.getValidationContext().getBundle().getPreheat().get(
            TrackerIdScheme.UID, ProgramStage.class, event.getProgramStage() )).isEnableUserAssignment();

        return !Optional.ofNullable( userAssignmentEnabled )
                .orElse( false );
    }

    private boolean userNotPresentInPreheat( ValidationErrorReporter reporter, Event event )
    {
        /*
         * TODO: should we have an helper method in base class (or a dedicated service)
         * to easily access preheat maps ? this is hard to read
         */
        return reporter.getValidationContext().getBundle().getPreheat().get( TrackerIdScheme.UID, User.class,
            event.getAssignedUser() ) == null;
    }

    private boolean isNotValidAssignedUserUid( Event event )
    {
        return !CodeGenerator.isValidUid( event.getAssignedUser() );
    }

    @Override
    public void validateEnrollment( ValidationErrorReporter reporter, Enrollment enrollment )
    {
        /*
         * No implementation.
         */
    }

    @Override
    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        /*
         * No implementation.
         */
    }

    @Override
    public void validateTrackedEntity( ValidationErrorReporter reporter, TrackedEntity tei )
    {
        /*
         * No implementation.
         */
    }
}
