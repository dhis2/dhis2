package org.hisp.dhis.validation;
/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.jdbc.batchhandler.ValidationResultBatchHandler;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * @author Stian Sandvold
 */
@Transactional
public class DefaultValidationResultService
    implements ValidationResultService
{
    @Autowired
    private ValidationResultStore validationResultStore;

    @Autowired
    private BatchHandlerFactory batchHandlerFactory;

    @Override
    public void saveValidationResults( Collection<ValidationResult> validationResults )
    {
        BatchHandler<ValidationResult> validationResultBatchHandler = batchHandlerFactory
            .createBatchHandler( ValidationResultBatchHandler.class ).init();

        validationResults.forEach( validationResult ->
        {
            if ( !validationResultBatchHandler.objectExists( validationResult ) )
            {
                validationResultBatchHandler.addObject( validationResult );
            }
        } );

        validationResultBatchHandler.flush();
    }

    public List<ValidationResult> getAllValidationResults()
    {
        return validationResultStore.getAll();
    }

    @Override
    public List<ValidationResult> getAllUnReportedValidationResults()
    {
        return validationResultStore.getAllUnreportedValidationResults();
    }

    @Override
    public void deleteValidationResult( ValidationResult validationResult )
    {
        validationResultStore.delete( validationResult );
    }

    @Override
    public void updateValidationResults( List<ValidationResult> validationResults )
    {
        BatchHandler<ValidationResult> validationResultBatchHandler = batchHandlerFactory
            .createBatchHandler( ValidationResultBatchHandler.class ).init();

        validationResults.forEach( validationResult ->
        {
            validationResultBatchHandler.updateObject( validationResult );
        } );

        validationResultBatchHandler.flush();
    }
}
