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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.DataValue;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.ValidationErrorReporter;
import org.hisp.dhis.tracker.validation.TrackerImportValidationContext;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1009;
import static org.hisp.dhis.tracker.report.TrackerErrorCode.E1084;

/**
 * @author Enrico Colasante
 */
@Component
public class EventDataValuesValidationHook
    extends AbstractTrackerDtoValidationHook
{
    public EventDataValuesValidationHook( TrackedEntityAttributeService teAttrService )
    {
        super( Event.class, TrackerImportStrategy.CREATE_AND_UPDATE, teAttrService );
    }

    @Override
    public void validateEvent( ValidationErrorReporter reporter, Event event )
    {
        TrackerImportValidationContext context = reporter.getValidationContext();

        for ( DataValue dataValue : event.getDataValues() )
        {
            addErrorIf( () -> !isValidDateStringAndNotNull( dataValue.getCreatedAt() ),
                reporter, TrackerErrorCode.E1300, dataValue.getCreatedAt() );
            addErrorIf( () -> !isValidDateStringAndNotNull( dataValue.getUpdatedAt() ),
                reporter, TrackerErrorCode.E1301, dataValue.getUpdatedAt() );

            validateDataElement( reporter, context, dataValue );
        }
        validateMandatoryDataValue( reporter, context, event );
    }

    private void validateDataElement( ValidationErrorReporter reporter, TrackerImportValidationContext ctx,
        DataValue dataValue )
    {
        DataElement dataElement = ctx.getDataElement( dataValue.getDataElement() );

        if ( dataElement == null )
        {
            addError( reporter, TrackerErrorCode.E1304, dataValue.getDataElement() );
        }
        else
        {
            final String status = ValidationUtils.dataValueIsValid( dataValue.getValue(), dataElement );

            if ( status != null )
            {
                addError( reporter, TrackerErrorCode.E1302, dataElement.getUid(), status );
            }
            else
            {
                validateFileNotAlreadyAssigned( reporter, dataValue, dataElement );
            }
        }
    }

    public void validateMandatoryDataValue( ValidationErrorReporter reporter, TrackerImportValidationContext ctx,
        Event event )
    {
        if ( StringUtils.isEmpty( event.getProgramStage() ) )
            return;

        ProgramStage programStage = ctx.getProgramStage( event.getProgramStage() );

        final Set<ProgramStageDataElement> mandatoryDataElements = programStage.getProgramStageDataElements();

        Set<String> eventDataElements = event.getDataValues().stream()
            .map( DataValue::getDataElement )
            .collect( Collectors.toSet() );

        for ( ProgramStageDataElement mandatoryDataElement : mandatoryDataElements )
        {
            if ( !eventDataElements.contains( mandatoryDataElement.getDataElement().getUid() ) )
            {
                addError( reporter, TrackerErrorCode.E1303, mandatoryDataElement.getDataElement() );
            }
        }
    }

    private void validateFileNotAlreadyAssigned( ValidationErrorReporter reporter, DataValue dataValue,
        DataElement dataElement )
    {
        boolean isFile = dataElement.getValueType() != null && dataElement.getValueType().isFile();
        if ( !isFile )
        {
            return;
        }

        FileResource fileResource = reporter.getValidationContext().getFileResource( dataValue.getValue() );

        addErrorIfNull( fileResource, reporter, E1084, dataValue.getValue() );
        addErrorIf( () -> fileResource != null && fileResource.isAssigned(), reporter, E1009, dataValue.getValue() );
    }
}