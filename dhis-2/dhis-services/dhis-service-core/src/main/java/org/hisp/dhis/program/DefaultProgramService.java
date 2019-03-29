package org.hisp.dhis.program;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitQueryParams;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Abyot Asalefew
 */
public class DefaultProgramService
    implements ProgramService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramStore programStore;

    public void setProgramStore( ProgramStore programStore )
    {
        this.programStore = programStore;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public int addProgram( Program program )
    {
        programStore.save( program );
        return program.getId();
    }

    @Override
    @Transactional
    public void updateProgram( Program program )
    {
        programStore.update( program );
    }

    @Override
    @Transactional
    public void deleteProgram( Program program )
    {
        programStore.delete( program );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Program> getAllPrograms()
    {
        return programStore.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Program getProgram( int id )
    {
        return programStore.get( id );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Program> getPrograms( OrganisationUnit organisationUnit )
    {
        return programStore.get( organisationUnit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Program> getPrograms( ProgramType type )
    {
        return programStore.getByType( type );
    }

    @Override
    @Transactional(readOnly = true)
    public Program getProgram( String uid )
    {
        return programStore.getByUid( uid );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Program> getProgramsByTrackedEntityType( TrackedEntityType trackedEntityType )
    {
        return programStore.getByTrackedEntityType( trackedEntityType );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Program> getProgramsByDataEntryForm( DataEntryForm dataEntryForm )
    {
        return programStore.getByDataEntryForm( dataEntryForm );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Program> getUserPrograms()
    {
        return getUserPrograms( currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Program> getUserPrograms( User user )
    {
        if ( user == null || user.isSuper() )
        {
            return getAllPrograms();
        }

        return programStore.getDataReadAll( user );
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Program> getUserPrograms( ProgramType programType )
    {
        return getUserPrograms().stream().filter( p -> p.getProgramType() == programType ).collect( Collectors.toSet() );
    }

    @Override
    @Transactional
    public void mergeWithCurrentUserOrganisationUnits( Program program, Collection<OrganisationUnit> mergeOrganisationUnits )
    {
        Set<OrganisationUnit> selectedOrgUnits = Sets.newHashSet( program.getOrganisationUnits() );

        OrganisationUnitQueryParams params = new OrganisationUnitQueryParams();
        params.setParents( currentUserService.getCurrentUser().getOrganisationUnits() );

        Set<OrganisationUnit> userOrganisationUnits = Sets.newHashSet( organisationUnitService.getOrganisationUnitsByQuery( params ) );

        selectedOrgUnits.removeAll( userOrganisationUnits );
        selectedOrgUnits.addAll( mergeOrganisationUnits );

        program.updateOrganisationUnits( selectedOrgUnits );

        updateProgram( program );
    }

    // -------------------------------------------------------------------------
    // ProgramDataElement
    // -------------------------------------------------------------------------


    @Override
    @Transactional(readOnly = true)
    public List<ProgramDataElementDimensionItem> getGeneratedProgramDataElements( String programUid )
    {
        Program program = getProgram( programUid );

        List<ProgramDataElementDimensionItem> programDataElements = Lists.newArrayList();

        if ( program == null )
        {
            return programDataElements;
        }

        for ( DataElement element : program.getDataElements() )
        {
            programDataElements.add( new ProgramDataElementDimensionItem( program, element ) );
        }

        Collections.sort( programDataElements );

        return programDataElements;
    }
}
