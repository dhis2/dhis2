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
package org.hisp.dhis.tracker.programrule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.programrule.engine.RuleEffectByObject;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerProgramRuleService;
import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.hisp.dhis.tracker.converter.EnrollmentTrackerConverterService;
import org.hisp.dhis.tracker.converter.EventTrackerConverterService;
import org.hisp.dhis.tracker.converter.TrackerConverterService;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

/**
 * @author Enrico Colasante
 */
@Service
@RequiredArgsConstructor
public class DefaultTrackerProgramRuleService
    implements TrackerProgramRuleService
{
    @NonNull
    @Qualifier( "serviceTrackerRuleEngine" )
    private final ProgramRuleEngine programRuleEngine;

    @NonNull
    private final EnrollmentTrackerConverterService enrollmentTrackerConverterService;

    @NonNull
    private final EventTrackerConverterService eventTrackerConverterService;

    @NonNull
    private final TrackerConverterService<Attribute, TrackedEntityAttributeValue> attributeValueTrackerConverterService;

    @Override
    @Transactional( readOnly = true )
    public List<RuleEffectByObject> calculateRuleEffects( TrackerBundle bundle )
    {
        Stream<RuleEffectByObject> enrollmentStream = bundle.getEnrollments()
            .stream()
            .flatMap( e -> {
                ProgramInstance enrollment = enrollmentTrackerConverterService.fromForRuleEngine( bundle.getPreheat(),
                    e );

                return programRuleEngine
                    .evaluateNTI( enrollment,
                        getEventsFromEnrollment( enrollment.getUid(), bundle, bundle.getEvents() ),
                        getAttributes( e, bundle ) )
                    .stream();
            } );

        return Stream.concat(
            enrollmentStream,
            calculateEventRuleEffects( bundle ).stream() )
            .collect( Collectors.toList() );
    }

    private List<TrackedEntityAttributeValue> getAttributes( Enrollment enrollment, TrackerBundle bundle )
    {
        List<TrackedEntityAttributeValue> attributeValues = attributeValueTrackerConverterService
            .from( bundle.getPreheat(), enrollment.getAttributes() );

        TrackedEntityInstance trackedEntity = bundle.getPreheat()
            .getTrackedEntity( bundle.getIdentifier(), enrollment.getTrackedEntity() );

        if ( trackedEntity != null )
        {
            attributeValues.addAll( trackedEntity.getTrackedEntityAttributeValues() );
        }
        else
        {
            bundle.getTrackedEntity( enrollment.getTrackedEntity() )
                .ifPresent( tei -> {
                    List<TrackedEntityAttributeValue> teiAttributes = attributeValueTrackerConverterService
                        .from( bundle.getPreheat(), tei.getAttributes() );
                    attributeValues.addAll( teiAttributes );
                } );
        }

        return attributeValues;
    }

    @Transactional( readOnly = true )
    public List<RuleEffectByObject> calculateEventRuleEffects( TrackerBundle bundle )
    {
        List<String> enrollmentUids = bundle.getEnrollments().stream().map( Enrollment::getEnrollment )
            .collect( Collectors.toList() );

        Map<String, List<Event>> eventsByEnrollment = bundle.getEvents()
            .stream()
            .filter( event -> !enrollmentUids.contains( event.getEnrollment() ) )
            .collect( Collectors.groupingBy( Event::getEnrollment ) );

        return eventsByEnrollment
            .entrySet()
            .stream()
            .flatMap( entry -> {
                ProgramInstance enrollment = getEnrollment( bundle, entry.getKey() );
                List<ProgramStageInstance> programStageInstances = eventTrackerConverterService
                    .fromForRuleEngine( bundle.getPreheat(), entry.getValue() );
                if ( enrollment == null )
                {
                    return programRuleEngine.evaluateProgramEventNTI( Sets.newHashSet( programStageInstances ),
                        bundle.getPreheat().get( Program.class, entry.getValue().get( 0 ).getProgram() ) )
                        .stream();
                }
                else
                {
                    List<TrackedEntityAttributeValue> attributeValues = bundle.getEnrollments()
                        .stream()
                        .filter( e -> entry.getValue().equals( e.getEnrollment() ) )
                        .findAny()
                        .map( e -> getAttributes( e, bundle ) )
                        .orElse( Collections.EMPTY_LIST );
                    return programRuleEngine.evaluateNTI( enrollment,
                        getEventsFromEnrollment( enrollment.getUid(), bundle, bundle.getEvents() ), attributeValues )
                        .stream();
                }
            } )
            .collect( Collectors.toList() );
    }

    private ProgramInstance getEnrollment( TrackerBundle bundle, String enrollmentUid )
    {
        return bundle.getPreheat().getEnrollment( TrackerIdScheme.UID, enrollmentUid );
    }

    private Set<ProgramStageInstance> getEventsFromEnrollment( String enrollment, TrackerBundle bundle,
        List<Event> events )
    {
        List<ProgramStageInstance> preheatEvents = bundle.getPreheat().getEvents().values()
            .stream()
            .flatMap( psi -> psi.values().stream() )
            .collect( Collectors.toList() );
        Stream<ProgramStageInstance> programStageInstances = preheatEvents
            .stream()
            .filter( e -> e.getProgramInstance().getUid().equals( enrollment ) );
        Stream<ProgramStageInstance> bundleEvents = events
            .stream()
            .filter( e -> e.getEnrollment().equals( enrollment ) )
            .map( event -> eventTrackerConverterService.from( bundle.getPreheat(), event ) );

        return Stream.concat( programStageInstances, bundleEvents ).collect( Collectors.toSet() );

    }
}
