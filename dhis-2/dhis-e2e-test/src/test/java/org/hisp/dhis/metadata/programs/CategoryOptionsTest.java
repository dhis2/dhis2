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
package org.hisp.dhis.metadata.programs;

import com.google.gson.JsonElement;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.metadata.CategoryOptionActions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
public class CategoryOptionsTest
    extends ApiTest
{
    private LoginActions loginActions;

    private CategoryOptionActions categoryOptionActions;

    @BeforeAll
    public void beforeAll()
    {
        loginActions = new LoginActions();
        categoryOptionActions = new CategoryOptionActions();
    }

    @BeforeEach
    public void before()
    {
        loginActions.loginAsSuperUser();
    }

    @Test
    public void testCategoryOptionOrgUnitsConnections()
    {
        loginActions.loginAsSuperUser();

        Set<String> associatedOrgUnitsAsSuperUser = extractAssociatedOrgUnits( "fjvZIRlTBrp" );

        loginActions.loginAsDefaultUser();

        Set<String> associatedOrgUnitsAsTracker = extractAssociatedOrgUnits( "fjvZIRlTBrp" );

        assertTrue( associatedOrgUnitsAsSuperUser.containsAll( associatedOrgUnitsAsTracker ) );
        assertTrue( associatedOrgUnitsAsSuperUser.size() >= associatedOrgUnitsAsTracker.size() );

    }

    private Set<String> extractAssociatedOrgUnits( String categoryOptionUids )
    {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                categoryOptionActions.getOrgUnitsAssociations( categoryOptionUids )
                    .getBody()
                    .getAsJsonArray( categoryOptionUids )
                    .iterator(),
                Spliterator.ORDERED ),
            false )
            .map( JsonElement::getAsString )
            .collect( Collectors.toSet() );
    }
}
