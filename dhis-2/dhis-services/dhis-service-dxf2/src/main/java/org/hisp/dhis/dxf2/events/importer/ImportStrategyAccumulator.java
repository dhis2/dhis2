package org.hisp.dhis.dxf2.events.importer;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.program.ProgramStageInstance;

import lombok.Getter;

/**
 * This class aggregates Events by operation type (Insert, Update, Delete)
 * during an Event import process, based on the specified {@see ImportStrategy}
 *
 * @author Luciano Fiandesio
 */
@Getter
public class ImportStrategyAccumulator
{
    private final List<Event> create = new ArrayList<>();

    private final List<Event> update = new ArrayList<>();

    private final List<Event> delete = new ArrayList<>();

    public ImportStrategyAccumulator partitionEvents( List<Event> events, ImportStrategy importStrategy,
        Map<String, ProgramStageInstance> existingEvents )
    {
        if ( importStrategy.isCreate() )
        {
            create.addAll( events );
        }
        else if ( importStrategy.isCreateAndUpdate() )
        {
            for ( Event event : events )
            {
                sortCreatesAndUpdates( event, create, update, existingEvents );
            }
        }
        else if ( importStrategy.isUpdate() )
        {
            update.addAll( events );
        }
        else if ( importStrategy.isDelete() )
        {
            final Set<String> existingEventKeys = existingEvents.keySet();
            delete.addAll( events.stream()
                .filter( event -> existingEventKeys.contains( event.getUid() ) )
                .collect( Collectors.toSet() ) );
        }
        else if ( importStrategy.isSync() )
        {
            for ( Event event : events )
            {
                if ( event.isDeleted() )
                {
                    delete.add( event );
                }
                else
                {
                    sortCreatesAndUpdates( event, create, update, existingEvents );
                }
            }
        }
        return this;
    }

    private void sortCreatesAndUpdates( Event event, List<Event> create, List<Event> update,
        Map<String, ProgramStageInstance> existingEvents )
    {
        ProgramStageInstance programStageInstance = existingEvents.get( event.getEvent() );

        if ( programStageInstance == null )
        {
            create.add( event );
        }
        else
        {
            update.add( event );
        }
    }
}