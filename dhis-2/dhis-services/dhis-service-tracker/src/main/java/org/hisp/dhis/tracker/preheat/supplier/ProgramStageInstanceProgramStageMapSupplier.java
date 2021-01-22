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
package org.hisp.dhis.tracker.preheat.supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

/**
 * This supplier adds to the pre-heat object a List of all Program Stages UIDs
 * that have at least ONE Program Stage Instance that is not logically deleted
 * ('deleted = true') and the status is not 'SKIPPED'
 *
 * @author Luciano Fiandesio
 */
@Component
public class ProgramStageInstanceProgramStageMapSupplier
    extends JdbcAbstractPreheatSupplier
{
    private static final String PS_UID = "programStageUid";

    private static final String PI_UID = "programInstanceUid";

    private static final String SQL = "select ps.uid as " + PS_UID + ", pi.uid as " + PI_UID + " " +
        " from programstage AS ps " +
        " JOIN programinstance AS pi ON pi.programid = ps.programid " +
        " where exists( select programstageinstanceid from programstageinstance psi where  psi.deleted = false " +
        " and psi.status != 'SKIPPED' and ps.programstageid = psi.programstageid " +
        " and pi.programinstanceid = psi.programinstanceid ) " +
        " and ps.uid in (:ids)";

    protected ProgramStageInstanceProgramStageMapSupplier( JdbcTemplate jdbcTemplate )
    {
        super( jdbcTemplate );
    }

    @Override
    public void preheatAdd( TrackerImportParams params, TrackerPreheat preheat )
    {
        if ( params.getEvents().size() == 0 )
        {
            return;
        }

        List<String> programStageUids = params.getEvents().stream().map( Event::getProgramStage )
            .collect( Collectors.toList() );

        if ( !programStageUids.isEmpty() )
        {
            List<Pair<String, String>> programStageWithEvents = new ArrayList<>();

            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue( "ids", programStageUids );
            jdbcTemplate.query( SQL, parameters, rs -> {

                programStageWithEvents.add( Pair.of( rs.getString( PS_UID ), rs.getString( PI_UID ) ) );

            } );

            preheat.setProgramStageWithEvents( programStageWithEvents );
        }
    }
}
