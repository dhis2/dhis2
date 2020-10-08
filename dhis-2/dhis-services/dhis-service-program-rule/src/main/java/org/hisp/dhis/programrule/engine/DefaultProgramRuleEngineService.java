package org.hisp.dhis.programrule.engine;

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

import java.util.List;

import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by zubair@dhis2.org on 23.10.17.
 */
@Slf4j
@Service( "org.hisp.dhis.programrule.engine.ProgramRuleEngineService" )
@AllArgsConstructor
public class DefaultProgramRuleEngineService implements ProgramRuleEngineService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @NonNull @Qualifier( "notificationRuleEngine" ) private final ProgramRuleEngine programRuleEngine;

    @NonNull@Qualifier( "serviceTrackerRuleEngine" ) private final ProgramRuleEngine programRuleEngineNew;

    @NonNull private final List<RuleActionImplementer> ruleActionImplementers;

    @NonNull private final ProgramInstanceService programInstanceService;

    @NonNull private final ProgramStageInstanceService programStageInstanceService;

    @NonNull private final ProgramService programService;
    

    @Override
    public List<RuleEffect> evaluateEnrollmentAndRunEffects( long programInstanceId )
    {
        ProgramInstance programInstance = programInstanceService.getProgramInstance( programInstanceId );

        if ( programInstance == null )
        {
            return Lists.newArrayList();
        }

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programInstance,
            Sets.newHashSet( programStageInstanceService
                .getProgramStageInstancesByProgramInstance( programInstance.getId() ) ) );

        RuleExecutor.builder().ruleActionImplementers( ruleActionImplementers ).programInstance( programInstance )
            .build().exec( ruleEffects );

        return ruleEffects;
    }

    @Override
    public List<RuleEffect> evaluateEventAndRunEffects( long programStageInstanceId )
    {
        ProgramStageInstance psi = programStageInstanceService.getProgramStageInstance( programStageInstanceId );

        if ( psi == null )
        {
            return Lists.newArrayList();
        }

        ProgramInstance programInstance = programInstanceService.getProgramInstance( psi.getProgramInstance().getId() );

        List<RuleEffect> ruleEffects = programRuleEngine.evaluate( programInstance, psi,
            Sets.newHashSet( programStageInstanceService
                .getProgramStageInstancesByProgramInstance( programInstance.getId() ) ) );

        RuleExecutor.builder().ruleActionImplementers( ruleActionImplementers ).programStageInstance( psi ).build()
            .exec( ruleEffects );

        return ruleEffects;
    }

    @Override
    public RuleValidationResult getDescription( String condition, String programId )
    {
        Program program = programService.getProgram( programId );

        return programRuleEngineNew.getDescription( condition, program );
    }

    @Builder()
    static class RuleExecutor
    {
        private ProgramInstance programInstance;

        private ProgramStageInstance programStageInstance;

        private List<RuleActionImplementer> ruleActionImplementers;

        void exec( List<RuleEffect> ruleEffects )
        {
            for ( RuleEffect effect : ruleEffects )
            {
                ruleActionImplementers.stream().filter( i -> i.accept( effect.ruleAction() ) ).forEach( i -> {
                    log.debug( String.format( "Invoking action implementer: %s", i.getClass().getSimpleName() ) );
                    if ( programInstance != null )
                    {
                        i.implement( effect, programInstance );
                    }
                    else
                    {
                        i.implement( effect, programStageInstance );
                    }
                } );
            }
        }
    }
}
