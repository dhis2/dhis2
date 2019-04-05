package org.hisp.dhis.webapi.controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.keyjsonvalue.KeyJsonValue;
import org.hisp.dhis.programstagefilter.ProgramStageInstanceFilter;
import org.hisp.dhis.programstagefilter.ProgramStageInstanceFilterService;

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

import org.hisp.dhis.schema.descriptors.ProgramStageInstanceFilterSchemaDescriptor;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 *
 */
@Controller
@RequestMapping( value = ProgramStageInstanceFilterSchemaDescriptor.API_ENDPOINT )
public class EventFilterController
{
    
    @Autowired
    private ProgramStageInstanceFilterService psiFilterService;
    
    @Autowired
    private WebMessageService messageService;
    
    /**
     * Returns all eventFilter definitions filtered by program if provided.
     */
    @RequestMapping( value = "", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody List<ProgramStageInstanceFilter> getEventFilters( @RequestParam( required = false ) String program, HttpServletResponse response )
    {
        return psiFilterService.getAll( program );
    }

    /**
     * Returns the specified eventFilter if exists.
     */
    @RequestMapping( value = "/{uid}", method = RequestMethod.GET, produces = "application/json" )
    public @ResponseBody ProgramStageInstanceFilter getEventFilter( @PathVariable String uid, HttpServletResponse response )
        throws WebMessageException
    {
        ProgramStageInstanceFilter psiFilter = psiFilterService.get( uid );
        if ( psiFilter == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "EventFilter '" + uid + "' was not found." ) );
        }
        return psiFilter;
    }

    /**
     * Deletes all keys with the given namespace.
     */
    @RequestMapping( value = "/{uid}", method = RequestMethod.DELETE )
    public void deleteEventFilter( @PathVariable String uid, HttpServletResponse response )
        throws WebMessageException
    {
        ProgramStageInstanceFilter psiFilter = psiFilterService.get( uid );
        if ( psiFilter == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "EventFilter '" + uid + "' was not found." ) );
        }
        psiFilterService.delete( psiFilter );
        messageService.sendJson( WebMessageUtils.ok( "EventFilter '" + uid + "' deleted." ), response );
    }

   

    /**
     * Add a new eventFilter
     */
    @RequestMapping( value = "", method = RequestMethod.POST, produces = "application/json", consumes = "application/json" )
    public void addEventFilter(@RequestBody ProgramStageInstanceFilter psiFilter, HttpServletResponse response )
        throws WebMessageException
    {
        psiFilterService.add( psiFilter );
        response.setStatus( HttpServletResponse.SC_CREATED );
        messageService.sendJson( WebMessageUtils.created( "EventFilter created." ), response );
    }

    /**
     * Update an eventFilter definition
     */
    @RequestMapping( value = "/{uid}", method = RequestMethod.PUT, produces = "application/json", consumes = "application/json" )
    public void updateEventFilter( @PathVariable String uid, @RequestBody ProgramStageInstanceFilter psiFilter,
        HttpServletRequest request, HttpServletResponse response )
        throws WebMessageException, IOException
    {
        ProgramStageInstanceFilter existingPsiFilter = psiFilterService.get( uid );
        if ( existingPsiFilter == null )
        {
            throw new WebMessageException( WebMessageUtils.notFound( "EventFilter '" + uid + "' was not found." ) );
        }
        
        psiFilter.setUid( uid );
        psiFilterService.update( psiFilter );
        response.setStatus( HttpServletResponse.SC_OK );
        messageService.sendJson( WebMessageUtils.ok( "EventFilter updated." ), response );
    }

}
