package org.hisp.dhis.program.item;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.parser.expression.ParserExceptionWithoutContext;
import org.hisp.dhis.program.ProgramIndicatorExprVisitor;
import org.hisp.dhis.system.util.ValidationUtils;

import static org.hisp.dhis.parser.expression.ParserUtils.assumeStageElementSyntax;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ItemContext;

/**
 * Program indicator expression item ProgramItemStageElement
 *
 * @author Jim Grace
 */
public class ProgramItemStageElement
    extends ProgramItem
{
    @Override
    public Object getDescription( ItemContext ctx, ProgramIndicatorExprVisitor visitor )
    {
        assumeStageElementSyntax( ctx );

        String programStageId = ctx.uid0.getText();
        String dataElementId = ctx.uid1.getText();

        ValueType valueType = visitor.validateStageDataElement( ctx.getText(), programStageId, dataElementId );

        return ValidationUtils.getSubstitutionValue( valueType );
    }

    @Override
    public Object getSql( ItemContext ctx, ProgramIndicatorExprVisitor visitor )
    {
        assumeStageElementSyntax( ctx );

        String programStageId = ctx.uid0.getText();
        String dataElementId = ctx.uid1.getText();

        String column = visitor.getStatementBuilder().getProgramIndicatorDataValueSelectSql(
            programStageId, dataElementId, visitor.getReportingStartDate(), visitor.getReportingEndDate(), visitor.getProgramIndicator() );

        if ( visitor.getReplaceNulls() )
        {
            DataElement dataElement = visitor.getDataElementService().getDataElement( dataElementId );

            if ( dataElement == null )
            {
                throw new ParserExceptionWithoutContext( "Data element " + dataElementId + " not found during SQL generation." );
            }

            column = replaceNullValues( column, dataElement.getValueType() );
        }

        return column;
    }
}
