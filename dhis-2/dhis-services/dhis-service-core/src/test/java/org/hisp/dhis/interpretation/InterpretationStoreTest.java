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
package org.hisp.dhis.interpretation;

import static org.junit.Assert.assertEquals;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.visualization.Visualization;
import org.hisp.dhis.visualization.VisualizationStore;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

public class InterpretationStoreTest
    extends DhisSpringTest
{
    @Autowired
    private VisualizationStore visualizationStore;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private InterpretationStore interpretationStore;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private Visualization vzA;

    private Interpretation ipA;

    private Interpretation ipB;

    private Interpretation ipC;

    @Before
    public void beforeTest()
    {
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );

        manager.save( ouA );
        manager.save( ouB );
        manager.save( ouC );

        vzA = createVisualization( 'A' );
        visualizationStore.save( vzA );

        ipA = new Interpretation( vzA, ouA, "Interpration of visualization A" );
        ipB = new Interpretation( vzA, ouB, "Interpration of visualization B" );
        ipC = new Interpretation( vzA, ouC, "Interpration of visualization C" );
    }

    @Test
    public void testSaveGet()
    {
        interpretationStore.save( ipA );
        interpretationStore.save( ipB );
        interpretationStore.save( ipC );

        assertEquals( ipA, interpretationStore.get( ipA.getId() ) );
        assertEquals( ipB, interpretationStore.get( ipB.getId() ) );
        assertEquals( ipC, interpretationStore.get( ipC.getId() ) );
    }

    @Test
    public void testMigrateMigrations()
    {
        interpretationStore.save( ipA );
        interpretationStore.save( ipB );
        interpretationStore.save( ipC );

        ipA = interpretationStore.getByUid( ipA.getUid() );
        ipB = interpretationStore.getByUid( ipB.getUid() );
        ipC = interpretationStore.getByUid( ipC.getUid() );

        assertEquals( ouA, ipA.getOrganisationUnit() );
        assertEquals( ouB, ipB.getOrganisationUnit() );
        assertEquals( ouC, ipC.getOrganisationUnit() );

        interpretationStore.migrate( Sets.newHashSet( ouA, ouB ), ouC );

        ipA = interpretationStore.getByUid( ipA.getUid() );
        ipB = interpretationStore.getByUid( ipB.getUid() );
        ipC = interpretationStore.getByUid( ipC.getUid() );

        assertEquals( ouC, ipA.getOrganisationUnit() );
        assertEquals( ouC, ipB.getOrganisationUnit() );
        assertEquals( ouC, ipC.getOrganisationUnit() );
    }
}