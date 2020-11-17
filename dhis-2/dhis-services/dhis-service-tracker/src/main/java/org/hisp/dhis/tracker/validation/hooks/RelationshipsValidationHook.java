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

import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.PROGRAM_STAGE_INSTANCE;
import static org.hisp.dhis.relationship.RelationshipEntity.TRACKED_ENTITY_INSTANCE;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4000;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4001;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4004;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4007;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4008;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E4011;
import static org.hisp.dhis.tracker.report.ValidationErrorReporter.newReport;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.relationship.RelationshipConstraint;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.RelationshipItem;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerErrorReport;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

/**
 * @author Enrico Colasante
 */
@Component
public class RelationshipsValidationHook
    extends AbstractTrackerDtoValidationHook
{

    public RelationshipsValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( Relationship.class, TrackerImportStrategy.CREATE_AND_UPDATE, teAttrService );
    }

    public void validateRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        TrackerBundle bundle = context.getBundle();

        if ( bundle.getImportStrategy().isDelete() )
        {
            return;
        }

        boolean isValid = validateMandatoryData( reporter, relationship,
            bundle.getPreheat().getAll( TrackerIdScheme.UID, RelationshipType.class ) );

        // No need to check additional data if there are missing information on the
        // Relationship
        if ( isValid )
        {
            validateRelationshipConstraint( reporter, relationship, bundle );

            validateAutoRelationship( reporter, relationship );

            validateReferences( reporter, relationship.getFrom(), relationship.getRelationship() );
            validateReferences( reporter, relationship.getTo(), relationship.getRelationship() );
        }

    }

    private void validateRelationshipConstraint( ValidationErrorReporter reporter, Relationship relationship,
        TrackerBundle bundle )
    {
        getRelationshipType( bundle.getPreheat().getAll( TrackerIdScheme.UID, RelationshipType.class ),
            relationship.getRelationshipType() ).ifPresent( relationshipType -> {

                validateRelationshipConstraint( "from", relationship.getFrom(), relationshipType.getFromConstraint() )
                    .forEach( reporter::addError );
                validateRelationshipConstraint( "to", relationship.getTo(), relationshipType.getToConstraint() )
                    .forEach( reporter::addError );

            } );
    }

    private boolean validateMandatoryData( ValidationErrorReporter reporter, Relationship relationship,
        List<RelationshipType> relationshipsTypes )
    {
        addErrorIfNull( relationship.getFrom(), reporter, E4007 );
        addErrorIfNull( relationship.getTo(), reporter, E4008 );
        addErrorIf( () -> StringUtils.isEmpty( relationship.getRelationshipType() ), reporter, E4004 );

        addErrorIf( () -> !getRelationshipType( relationshipsTypes, relationship.getRelationshipType() ).isPresent(),
            reporter, E4009,
            relationship.getRelationshipType() );

        // make sure that both Relationship Item only contain *one* reference (tei, enrollment or event)
        addErrorIf(
            () -> relationship.getFrom() != null && countMatches( onlyValues( relationship.getFrom() ), "null" ) != 2,
            reporter, E4001, "from", relationship.getRelationship() );
        addErrorIf(
            () -> relationship.getTo() != null && countMatches( onlyValues( relationship.getTo() ), "null" ) != 2,
            reporter, E4001, "to", relationship.getRelationship() );

        final Optional<TrackerErrorReport> any = reporter.getReportList().stream()
            .filter( r -> relationship.getRelationship().equals( r.getUid() ) ).findAny();

        return !any.isPresent();
    }

    private Optional<RelationshipType> getRelationshipType( List<RelationshipType> relationshipsTypes,
        String relationshipTypeUid )
    {
        return relationshipsTypes.stream().filter( r -> r.getUid().equals( relationshipTypeUid ) ).findFirst();
    }

    private void validateAutoRelationship( ValidationErrorReporter reporter, Relationship relationship )
    {
        if ( Objects.equals( relationship.getFrom(), relationship.getTo() ) )
        {
            addError( reporter, E4000, relationship.getRelationship() );
        }
    }

    private TrackerType relationshipItemValueType( RelationshipItem item )
    {
        if ( StringUtils.isNotEmpty( item.getTrackedEntity() ) )
        {
            return TrackerType.TRACKED_ENTITY;
        }
        else if ( StringUtils.isNotEmpty( item.getEnrollment() ) )
        {
            return TrackerType.ENROLLMENT;
        }
        else if ( StringUtils.isNotEmpty( item.getEvent() ) )
        {
            return TrackerType.EVENT;
        }
        return null;
    }

    private List<TrackerErrorReport.TrackerErrorReportBuilder> validateRelationshipConstraint( String relSide,
        RelationshipItem item,
        RelationshipConstraint constraint )
    {
        ArrayList<TrackerErrorReport.TrackerErrorReportBuilder> result = new ArrayList<>();

        if ( constraint.getRelationshipEntity().equals( TRACKED_ENTITY_INSTANCE ) )
        {
            if ( item.getTrackedEntity() == null )
            {
                result.add(
                    newReport( TrackerErrorCode.E4010 ).addArg( relSide ).addArg( TrackerType.TRACKED_ENTITY.getName() )
                        .addArg( relationshipItemValueType( item ).getName() ) );
            }
        }
        else if ( constraint.getRelationshipEntity().equals( PROGRAM_INSTANCE ) )
        {
            if ( item.getEnrollment() == null )
            {
                result.add(
                    newReport( TrackerErrorCode.E4010 ).addArg( relSide ).addArg( TrackerType.ENROLLMENT.getName() )
                        .addArg( relationshipItemValueType( item ).getName() ) );
            }

        }
        else if ( constraint.getRelationshipEntity().equals( PROGRAM_STAGE_INSTANCE ) )
        {
            if ( item.getEvent() == null )
            {
                result.add(
                    newReport( TrackerErrorCode.E4010 ).addArg( relSide ).addArg( TrackerType.EVENT.getName() )
                        .addArg( relationshipItemValueType( item ).getName() ) );
            }
        }

        return result;
    }

    private String onlyValues( RelationshipItem item )
    {
        return item != null ? item.getTrackedEntity() + "-" + item.getEnrollment() + "-" + item.getEvent() : "null-null-null";
    }

    private void validateReferences( ValidationErrorReporter reporter, RelationshipItem item, String relationship )
    {
        TrackerType trackerType = relationshipItemValueType( item );
        String itemUid = getUidFromRelationshipItem( item );

        List<TrackerErrorReport> errors =
            reporter.getReportList()
                .stream()
                .filter( x -> x.getTrackerType().equals( trackerType ) )
                .collect( Collectors.toList() );
        for ( TrackerErrorReport error : errors )
        {
            if ( itemUid.equals( error.getUid() ) )
            {
                addError( reporter, E4011, relationship, trackerType.getName(), itemUid );
            }
        }
    }

    private String getUidFromRelationshipItem( RelationshipItem item )
    {
        if ( item.getTrackedEntity() != null )
        {
            return item.getTrackedEntity();
        }
        else if ( item.getEnrollment() != null )
        {
            return item.getEnrollment();
        }
        else if ( item.getEvent() != null )
        {
            return item.getEvent();
        }
        return null;
    }
}