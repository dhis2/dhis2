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

package org.hisp.dhis.period;

import com.google.common.collect.Lists;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;

import java.util.Date;
import java.util.List;

/**
 * YearlyPeriodType for the decade period.
 *
 * @author Dusan Bernat
 */

public class DecadePeriodType extends YearlyPeriodType{

    /**
     * Generates the last 10 years where the last one is the year which the given
     * date is inside.
     */
    @Override
    public List<Period> generateLastYears(Date date )
    {
        Calendar calendar = getCalendar();

        DateTimeUnit dateTimeUnit = createLocalDateUnitInstance( date );
        dateTimeUnit = calendar.minusYears( dateTimeUnit, 9 );
        dateTimeUnit.setDay( 1 );
        dateTimeUnit.setMonth( 1 );

        List<Period> periods = Lists.newArrayList();

        for ( int i = 0; i < 10; ++i )
        {
            periods.add( createPeriod( dateTimeUnit, calendar ) );
            dateTimeUnit = calendar.plusYears( dateTimeUnit, 1 );
        }

        return periods;
    }
}
