package org.hisp.dhis.jdbc.batchhandler;
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

import org.hisp.dhis.validation.ValidationResult;
import org.hisp.quick.JdbcConfiguration;
import org.hisp.quick.batchhandler.AbstractBatchHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Stian Sandvold
 */
public class ValidationResultBatchHandler
    extends AbstractBatchHandler<ValidationResult>
{
    public ValidationResultBatchHandler( JdbcConfiguration configuration )
    {
        super( configuration );
    }

    @Override
    public String getTableName()
    {
        return "validationresult";
    }

    @Override
    public String getAutoIncrementColumn()
    {
        return "validationresultid";
    }

    @Override
    public boolean isInclusiveUniqueColumns()
    {
        return true;
    }

    @Override
    public List<String> getIdentifierColumns()
    {
        return getStringList( "validationresultid" );
    }

    @Override
    public List<Object> getIdentifierValues( ValidationResult validationResult )
    {
        return getObjectList( validationResult.getId() );
    }

    @Override
    public List<String> getUniqueColumns()
    {
        return getStringList(
            "validationruleid",
            "periodid",
            "organisationunitid",
            "attributeoptioncomboid",
            "dayinperiod"
        );
    }

    @Override
    public List<Object> getUniqueValues( ValidationResult validationResult )
    {
        return getObjectList(
            validationResult.getValidationRule().getId(),
            validationResult.getPeriod().getId(),
            validationResult.getOrganisationUnit().getId(),
            validationResult.getAttributeOptionCombo().getId(),
            validationResult.getDayInPeriod()
        );
    }

    @Override
    public List<String> getColumns()
    {
        return getStringList(
            "leftsidevalue",
            "rightsidevalue",
            "validationruleid",
            "periodid",
            "organisationunitid",
            "attributeoptioncomboid",
            "dayinperiod"
        );
    }

    @Override
    public List<Object> getValues( ValidationResult validationResult )
    {
        return getObjectList(
            validationResult.getLeftsideValue(),
            validationResult.getRightsideValue(),
            validationResult.getValidationRule().getId(),
            validationResult.getPeriod().getId(),
            validationResult.getOrganisationUnit().getId(),
            validationResult.getAttributeOptionCombo().getId(),
            validationResult.getDayInPeriod()
        );
    }

    @Override
    public ValidationResult mapRow( ResultSet resultSet )
        throws SQLException
    {
        return null;
    }
}
