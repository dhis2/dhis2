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
package org.hisp.dhis.dataitem.query.shared;

import static org.apache.commons.lang3.StringUtils.replaceEach;

/**
 * This class keeps basic SQL keywords/constants so they can be reused by the
 * queries. It was created mainly to make SonarQube happy regarding what is
 * considered "code smell".
 */
public class StatementUtil
{
    private StatementUtil()
    {
    }

    public static final String SPACED_SELECT = " select ";

    public static final String SPACED_UNION = " union ";

    public static final String SPACED_WHERE = " where ";

    public static final String SPACED_OR = " or ";

    public static final String SPACED_AND = " and ";

    /**
     * This method is specific for strings used in "ilike" filters where some non
     * accepted characters will fail at querying time. It will only replace common
     * characters by the form accepted in SQL ilike queries.
     *
     * @param value the value where characters will ve replaced.
     * @return the input value with the characters replaced.
     */
    public static String addIlikeReplacingCharacters( final String value )
    {
        return replaceEach( value, new String[] { "%", "," }, new String[] { "\\%", "\\," } );
    }
}
