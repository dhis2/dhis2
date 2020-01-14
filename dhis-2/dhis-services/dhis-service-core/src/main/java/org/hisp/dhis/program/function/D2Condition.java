package org.hisp.dhis.program.function;

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

import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.function.SimpleScalarFunction;

import static org.hisp.dhis.parser.expression.ParserUtils.castClass;
import static org.hisp.dhis.parser.expression.ParserUtils.trimQuotes;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;

/**
 * Program indicator function: d2 condition
 *
 * @author Jim Grace
 */
public class D2Condition
    extends SimpleScalarFunction
{
    @Override
    public final Object evaluate( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        String testExpression = trimQuotes( ctx.stringLiteral().getText() );

        visitor.getProgramIndicatorService().validate( testExpression, Boolean.class, visitor.getItemDescriptions() );

        Object valueIfTrue = visitor.visit( ctx.expr( 0 ) );
        Object valueIfFalse = visitor.visit( ctx.expr( 1 ) );

        castClass( valueIfTrue.getClass(), valueIfFalse );

        return valueIfTrue;
    }

    @Override
    public Object getSql( ExprContext ctx, CommonExpressionVisitor visitor )
    {
        String testExpression = trimQuotes( ctx.stringLiteral().getText() );

        String testSql = visitor.getProgramIndicatorService().getAnalyticsSql( testExpression,
            visitor.getProgramIndicator(), visitor.getReportingStartDate(), visitor.getReportingEndDate() );

        String valueIfTrue = visitor.castStringVisit( ctx.expr( 0 ) );
        String valueIfFalse = visitor.castStringVisit( ctx.expr( 1 ) );

        return "case when (" + testSql + ") then " + valueIfTrue + " else " + valueIfFalse + " end";
    }
}
