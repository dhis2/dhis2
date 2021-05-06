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
package org.hisp.dhis.programrule.action.validation;

import java.util.function.Supplier;

import javax.annotation.Nonnull;

import lombok.NonNull;

import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.option.OptionService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramStageSectionService;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */

@Component( "programRuleActionValidationServiceSupplier" )
public class ProgramRuleActionValidationSupplier implements Supplier<ProgramRuleActionValidationService>
{
    @NonNull
    private final ProgramStageService programStageService;

    @NonNull
    private final ProgramService programService;

    @NonNull
    private final ProgramStageSectionService sectionService;

    @NonNull
    private final ProgramStageDataElementService stageDataElementService;

    @NonNull
    private final TrackedEntityAttributeService trackedEntityAttributeService;

    @NonNull
    private final DataElementService dataElementService;

    @NonNull
    private final ProgramNotificationTemplateService programNotificationTemplateService;

    @Nonnull
    private final OptionService optionService;

    public ProgramRuleActionValidationSupplier( @NonNull ProgramStageService programStageService,
        @NonNull ProgramService programService, @NonNull ProgramStageSectionService sectionService,
        @NonNull ProgramStageDataElementService stageDataElementService,
        @NonNull TrackedEntityAttributeService trackedEntityAttributeService,
        @NonNull DataElementService dataElementService,
        @NonNull ProgramNotificationTemplateService programNotificationTemplateService,
        @Nonnull OptionService optionService )
    {
        this.programStageService = programStageService;
        this.programService = programService;
        this.sectionService = sectionService;
        this.stageDataElementService = stageDataElementService;
        this.trackedEntityAttributeService = trackedEntityAttributeService;
        this.dataElementService = dataElementService;
        this.programNotificationTemplateService = programNotificationTemplateService;
        this.optionService = optionService;
    }

    @Override
    public ProgramRuleActionValidationService get()
    {
        return ProgramRuleActionValidationService.builder()
            .programStageService( programStageService )
            .programService( programService )
            .sectionService( sectionService )
            .stageDataElementService( stageDataElementService )
            .trackedEntityAttributeService( trackedEntityAttributeService )
            .dataElementService( dataElementService )
            .notificationTemplateService( programNotificationTemplateService )
            .optionService( optionService )
            .build();
    }
}
