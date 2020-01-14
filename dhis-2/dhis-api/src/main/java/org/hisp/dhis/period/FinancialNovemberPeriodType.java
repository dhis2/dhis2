package org.hisp.dhis.period;

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

import java.util.Calendar;

import org.hisp.dhis.calendar.DateTimeUnit;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
public class FinancialNovemberPeriodType 
    extends FinancialPeriodType
{

    private static final long serialVersionUID = -8443905531396977357L;

    private static final String ISO_FORMAT = "yyyyNov";

    private static final String ISO8601_DURATION = "P1Y";

    public static final String NAME = "FinancialNov";

    @Override
    public int getBaseMonth()
    {
        return Calendar.NOVEMBER;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public String getIsoDate( DateTimeUnit dateTimeUnit, org.hisp.dhis.calendar.Calendar calendar )
    {
        return String.format( "%dNov", dateTimeUnit.getYear() + 1 );
    }

    @Override
    public String getIsoFormat()
    {
        return ISO_FORMAT;
    }

    @Override
    public String getIso8601Duration()
    {
        return ISO8601_DURATION;
    }
}
