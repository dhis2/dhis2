package org.hisp.dhis.program;
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

import static org.hisp.dhis.program.ProgramIndicator.KEY_ATTRIBUTE;
import static org.hisp.dhis.program.ProgramIndicator.KEY_DATAELEMENT;
import static org.hisp.dhis.program.ProgramIndicator.KEY_PROGRAM_VARIABLE;
import static org.junit.Assert.*;
import static org.junit.Assert.assertFalse;

import java.util.*;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementDomain;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.util.DateUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ProgramIndicatorServiceTest
    extends DhisSpringTest
{

    @Autowired
    private ProgramIndicatorService programIndicatorService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ConstantService constantService;

    private Date incidentDate;

    private Date enrollmentDate;

    private ProgramStage psA;

    private ProgramStage psB;

    private Program programA;

    private Program programB;

    private ProgramInstance programInstance;

    private DataElement deAInteger;

    private DataElement deB;

    private DataElement deC;

    private DataElement deD;

    private DataElement deEText;

    private DataElement deFNumber;

    private DataElement deGBoolean;

    private DataElement deHDate;

    private TrackedEntityAttribute atA;

    private TrackedEntityAttribute atB;

    private ProgramIndicator indicatorA;

    private ProgramIndicator indicatorB;

    private ProgramIndicator indicatorC;

    private ProgramIndicator indicatorD;

    private ProgramIndicator indicatorE;

    private ProgramIndicator indicatorF;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        // ---------------------------------------------------------------------
        // Program
        // ---------------------------------------------------------------------

        programA = createProgram( 'A', new HashSet<>(), organisationUnit );
        programA.setUid( "Program000A" );
        programService.addProgram( programA );

        psA = new ProgramStage( "StageA", programA );
        psA.setSortOrder( 1 );
        psA.setUid( "ProgrmStagA" );
        programStageService.saveProgramStage( psA );

        psB = new ProgramStage( "StageB", programA );
        psB.setSortOrder( 2 );
        psB.setUid( "ProgrmStagB" );
        programStageService.saveProgramStage( psB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( psA );
        programStages.add( psB );
        programA.setProgramStages( programStages );
        programService.updateProgram( programA );

        programB = createProgram( 'B', new HashSet<>(), organisationUnit );
        programB.setUid( "Program000B" );
        programService.addProgram( programB );

        // ---------------------------------------------------------------------
        // Program Stage DE
        // ---------------------------------------------------------------------

        deAInteger = createDataElement( 'A' );
        deAInteger.setDomainType( DataElementDomain.TRACKER );
        deAInteger.setUid( "DataElmentA" );

        deB = createDataElement( 'B' );
        deB.setDomainType( DataElementDomain.TRACKER );
        deB.setUid( "DataElmentB" );

        deC = createDataElement( 'C' );
        deC.setDomainType( DataElementDomain.TRACKER );
        deC.setUid( "DataElmentC" );

        deD = createDataElement( 'D' );
        deD.setDomainType( DataElementDomain.TRACKER );
        deD.setUid( "DataElmentD" );

        deEText = createDataElement( 'E' );
        deEText.setValueType( ValueType.TEXT );
        deEText.setDomainType( DataElementDomain.TRACKER );
        deEText.setUid( "DataElmentE" );

        deFNumber = createDataElement( 'F' );
        deFNumber.setValueType( ValueType.NUMBER );
        deFNumber.setDomainType( DataElementDomain.TRACKER );
        deFNumber.setUid( "DataElmentF" );

        deGBoolean = createDataElement( 'G' );
        deGBoolean.setValueType( ValueType.BOOLEAN );
        deGBoolean.setDomainType( DataElementDomain.TRACKER );
        deGBoolean.setUid( "DataElmentG" );

        deHDate = createDataElement( 'H' );
        deHDate.setValueType( ValueType.DATE );
        deHDate.setDomainType( DataElementDomain.TRACKER );
        deHDate.setUid( "DataElmentH" );

        dataElementService.addDataElement(deAInteger);
        dataElementService.addDataElement( deB );
        dataElementService.addDataElement( deC );
        dataElementService.addDataElement( deD );
        dataElementService.addDataElement( deEText );
        dataElementService.addDataElement( deFNumber );
        dataElementService.addDataElement( deGBoolean );
        dataElementService.addDataElement( deHDate );

        ProgramStageDataElement stageDataElementA = new ProgramStageDataElement( psA, deAInteger, false, 1 );
        ProgramStageDataElement stageDataElementB = new ProgramStageDataElement( psA, deB, false, 2 );
        ProgramStageDataElement stageDataElementC = new ProgramStageDataElement( psB, deAInteger, false, 1 );
        ProgramStageDataElement stageDataElementD = new ProgramStageDataElement( psB, deB, false, 2 );
        ProgramStageDataElement stageDataElementE = new ProgramStageDataElement( psA, deC, false, 3 );
        ProgramStageDataElement stageDataElementF = new ProgramStageDataElement( psA, deD, false, 4 );
        ProgramStageDataElement stageDataElementG = new ProgramStageDataElement( psA, deEText, false, 5 );
        ProgramStageDataElement stageDataElementH = new ProgramStageDataElement( psA, deFNumber, false, 6 );
        ProgramStageDataElement stageDataElementI = new ProgramStageDataElement( psA, deGBoolean, false, 6 );
        ProgramStageDataElement stageDataElementJ = new ProgramStageDataElement( psA, deHDate, false, 6 );

        programStageDataElementService.addProgramStageDataElement( stageDataElementA );
        programStageDataElementService.addProgramStageDataElement( stageDataElementB );
        programStageDataElementService.addProgramStageDataElement( stageDataElementC );
        programStageDataElementService.addProgramStageDataElement( stageDataElementD );
        programStageDataElementService.addProgramStageDataElement( stageDataElementE );
        programStageDataElementService.addProgramStageDataElement( stageDataElementF );
        programStageDataElementService.addProgramStageDataElement( stageDataElementG );
        programStageDataElementService.addProgramStageDataElement( stageDataElementH );
        programStageDataElementService.addProgramStageDataElement( stageDataElementI );
        programStageDataElementService.addProgramStageDataElement( stageDataElementJ );

        // ---------------------------------------------------------------------
        // TrackedEntityInstance & Enrollment
        // ---------------------------------------------------------------------

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        incidentDate = DateUtils.getMediumDate( "2014-10-22" );
        enrollmentDate = DateUtils.getMediumDate( "2014-12-31" );

        programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, programA, enrollmentDate,
                incidentDate, organisationUnit );

        incidentDate = DateUtils.getMediumDate( "2014-10-22" );
        enrollmentDate = DateUtils.getMediumDate( "2014-12-31" );

        programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, programA, enrollmentDate,
                incidentDate, organisationUnit );

        // TODO enroll twice?

        // ---------------------------------------------------------------------
        // TrackedEntityAttribute
        // ---------------------------------------------------------------------

        atA = createTrackedEntityAttribute( 'A', ValueType.NUMBER );
        atB = createTrackedEntityAttribute( 'B', ValueType.NUMBER );

        atA.setUid( "Attribute0A" );
        atB.setUid( "Attribute0B" );

        attributeService.addTrackedEntityAttribute( atA );
        attributeService.addTrackedEntityAttribute( atB );

        TrackedEntityAttributeValue attributeValueA = new TrackedEntityAttributeValue( atA, entityInstance, "1" );
        TrackedEntityAttributeValue attributeValueB = new TrackedEntityAttributeValue( atB, entityInstance, "2" );

        attributeValueService.addTrackedEntityAttributeValue( attributeValueA );
        attributeValueService.addTrackedEntityAttributeValue( attributeValueB );

        // ---------------------------------------------------------------------
        // TrackedEntityDataValue
        // ---------------------------------------------------------------------

        ProgramStageInstance stageInstanceA = programStageInstanceService.createProgramStageInstance( programInstance,
                psA, enrollmentDate, incidentDate, organisationUnit );
        ProgramStageInstance stageInstanceB = programStageInstanceService.createProgramStageInstance( programInstance,
                psB, enrollmentDate, incidentDate, organisationUnit );

        Set<ProgramStageInstance> programStageInstances = new HashSet<>();
        programStageInstances.add( stageInstanceA );
        programStageInstances.add( stageInstanceB );
        programInstance.setProgramStageInstances( programStageInstances );
        programInstance.setProgram( programA );

        // ---------------------------------------------------------------------
        // Constant
        // ---------------------------------------------------------------------

        Constant constantA = createConstant( 'A', 7.0 );
        constantService.saveConstant( constantA );

        // ---------------------------------------------------------------------
        // ProgramIndicator
        // ---------------------------------------------------------------------

        String expressionA = "( d2:daysBetween(" + KEY_PROGRAM_VARIABLE + "{" + ProgramIndicator.VAR_ENROLLMENT_DATE + "}, " + KEY_PROGRAM_VARIABLE + "{"
                + ProgramIndicator.VAR_INCIDENT_DATE + "}) )  / " + ProgramIndicator.KEY_CONSTANT + "{" + constantA.getUid() + "}";
        indicatorA = createProgramIndicator( 'A', programA, expressionA, null );
        programA.getProgramIndicators().add( indicatorA );

        indicatorB = createProgramIndicator( 'B', programA, "70", null );
        programA.getProgramIndicators().add( indicatorB );

        indicatorC = createProgramIndicator( 'C', programA, "0", null );
        programA.getProgramIndicators().add( indicatorC );

        String expressionD = "0 + A + 4 + " + ProgramIndicator.KEY_PROGRAM_VARIABLE + "{" + ProgramIndicator.VAR_INCIDENT_DATE + "}";
        indicatorD = createProgramIndicator( 'D', programB, expressionD, null );

        String expressionE = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deAInteger.getUid() + "} + " + KEY_DATAELEMENT + "{"
                + psB.getUid() + "." + deAInteger.getUid() + "} - " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} + " + KEY_ATTRIBUTE
                + "{" + atB.getUid() + "}";
        String filterE = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deAInteger.getUid() + "} + " + KEY_ATTRIBUTE + "{" + atA.getUid() + "} > 10";
        indicatorE = createProgramIndicator( 'E', programB, expressionE, filterE );

        String expressionF = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deAInteger.getUid() + "}";
        String filterF = KEY_DATAELEMENT + "{" + psA.getUid() + "." + deAInteger.getUid() + "} > " +
            KEY_ATTRIBUTE + "{" + atA.getUid() + "}";
        indicatorF = createProgramIndicator( 'F', AnalyticsType.ENROLLMENT, programB, expressionF, filterF );
        indicatorF.getAnalyticsPeriodBoundaries().add( new AnalyticsPeriodBoundary(AnalyticsPeriodBoundary.EVENT_DATE,
            AnalyticsPeriodBoundaryType.BEFORE_END_OF_REPORTING_PERIOD, PeriodType.getByNameIgnoreCase( "daily" ), 10) );
    }

    // -------------------------------------------------------------------------
    // CRUD tests
    // -------------------------------------------------------------------------

    @Test
    public void testAddProgramIndicator()
    {
        long idA = programIndicatorService.addProgramIndicator( indicatorA );
        long idB = programIndicatorService.addProgramIndicator( indicatorB );
        long idC = programIndicatorService.addProgramIndicator( indicatorC );

        assertNotNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idB ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idC ) );
    }

    @Test
    public void testDeleteProgramIndicator()
    {
        long idA = programIndicatorService.addProgramIndicator( indicatorB );
        long idB = programIndicatorService.addProgramIndicator( indicatorA );

        assertNotNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idB ) );

        programIndicatorService.deleteProgramIndicator( indicatorB );

        assertNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNotNull( programIndicatorService.getProgramIndicator( idB ) );

        programIndicatorService.deleteProgramIndicator( indicatorA );

        assertNull( programIndicatorService.getProgramIndicator( idA ) );
        assertNull( programIndicatorService.getProgramIndicator( idB ) );
    }

    @Test
    public void testUpdateProgramIndicator()
    {
        long idA = programIndicatorService.addProgramIndicator( indicatorB );

        assertNotNull( programIndicatorService.getProgramIndicator( idA ) );

        indicatorB.setName( "B" );
        programIndicatorService.updateProgramIndicator( indicatorB );

        assertEquals( "B", programIndicatorService.getProgramIndicator( idA ).getName() );
    }

    @Test
    public void testGetProgramIndicatorById()
    {
        long idA = programIndicatorService.addProgramIndicator( indicatorB );
        long idB = programIndicatorService.addProgramIndicator( indicatorA );

        assertEquals( indicatorB, programIndicatorService.getProgramIndicator( idA ) );
        assertEquals( indicatorA, programIndicatorService.getProgramIndicator( idB ) );
    }

    @Test
    public void testGetProgramIndicatorByName()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );

        assertEquals( "IndicatorA", programIndicatorService.getProgramIndicator( "IndicatorA" ).getName() );
        assertEquals( "IndicatorB", programIndicatorService.getProgramIndicator( "IndicatorB" ).getName() );
    }

    @Test
    public void testGetAllProgramIndicators()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );

        assertTrue( equals( programIndicatorService.getAllProgramIndicators(), indicatorB, indicatorA ) );
    }

    @Test
    public void testProgramIndicatorsWithNoExpression()
    {
        ProgramIndicator piWithNoExpression = createProgramIndicator( 'A', programA, null, null );
        ProgramIndicator piWithExpression = createProgramIndicator( 'B', programA, " 1 + 1", null );

        programIndicatorService.addProgramIndicator( piWithNoExpression );
        programIndicatorService.addProgramIndicator( piWithExpression );

        List<ProgramIndicator> programIndicators = programIndicatorService.getProgramIndicatorsWithNoExpression();

        assertFalse( programIndicators.isEmpty() );
        assertEquals( 1, programIndicators.size() );
        assertTrue( programIndicators.contains( piWithNoExpression ) );
        assertFalse( programIndicators.contains( piWithExpression ) );
    }

    // -------------------------------------------------------------------------
    // Logic tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetExpressionDescription()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );

        String description = programIndicatorService.getExpressionDescription( indicatorB.getExpression() );
        assertEquals( "70", description );

        description = programIndicatorService.getExpressionDescription( indicatorA.getExpression() );
        assertEquals( "( d2:daysBetween(Enrollment date, Incident date) )  / ConstantA", description );
    }

    @Test
    public void testGetAnyValueExistsFilterEventAnalyticsSQl()
    {
        String expected = "\"DataElmentA\" is not null or \"Attribute0A\" is not null";
        String expression = "#{ProgrmStagA.DataElmentA} - A{Attribute0A}";

        assertEquals( expected, programIndicatorService.getAnyValueExistsClauseAnalyticsSql( expression, AnalyticsType.EVENT ) );
    }

    @Test
    public void testGetAnyValueExistsFilterEnrollmentAnalyticsSQl()
    {
        String expected = "\"Attribute0A\" is not null or \"ProgrmStagA_DataElmentA\" is not null";
        String expression = "#{ProgrmStagA.DataElmentA} - A{Attribute0A}";

        assertEquals( expected, programIndicatorService.getAnyValueExistsClauseAnalyticsSql( expression, AnalyticsType.ENROLLMENT ) );
    }

    @Test
    public void testGetAnalyticsSQl()
    {
        String expected = "coalesce(\"" + deAInteger.getUid() + "\"::numeric,0) + coalesce(\"" + atA.getUid() + "\"::numeric,0) > 10";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( indicatorE.getFilter(), indicatorE, new Date(), new Date() ) );
    }

    @Test
    public void testGetAnalyticsSQl2()
    {
        String expected = "((cast(incidentdate as date) - cast(enrollmentdate as date))) / 7.0";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( indicatorA.getExpression(), indicatorA, new Date(), new Date() ) );
    }

    @Test
    public void testExpressionIsValid()
    {
        programIndicatorService.addProgramIndicator( indicatorB );
        programIndicatorService.addProgramIndicator( indicatorA );
        programIndicatorService.addProgramIndicator( indicatorD );

        assertTrue( programIndicatorService.expressionIsValid( indicatorB.getExpression() ) );
        assertTrue( programIndicatorService.expressionIsValid( indicatorA.getExpression() ) );
        assertFalse( programIndicatorService.expressionIsValid( indicatorD.getExpression() ) );
    }

    @Test
    public void testExpressionWithFunctionIsValid()
    {
        String exprA = "#{" + psA.getUid() + "." + deAInteger.getUid() + "}";
        String exprB = "d2:zing(#{" + psA.getUid() + "." + deAInteger.getUid() + "})";
        String exprC = "d2:condition('#{" + psA.getUid() + "." + deAInteger.getUid() + "} > 10',2,1)";

        assertTrue( programIndicatorService.expressionIsValid( exprA ) );
        assertTrue( programIndicatorService.expressionIsValid( exprB ) );
        assertTrue( programIndicatorService.expressionIsValid( exprC ) );
    }

    @Test
    public void testFilterExpressionValidityWithD2FunctionsAndDifferentValueTypes()
    {
        assertTrue( programIndicatorService.filterIsValid( "d2:hasValue(#{" + psA.getUid() + "." + deAInteger.getUid() + "})" ) );
        assertTrue( programIndicatorService.filterIsValid( "d2:hasValue(#{" + psA.getUid() + "." + deEText.getUid() + "})" ) );
        assertTrue( programIndicatorService.filterIsValid( "d2:hasValue(#{" + psA.getUid() + "." + deFNumber.getUid() + "})" ) );
        assertTrue( programIndicatorService.filterIsValid( "d2:hasValue(#{" + psA.getUid() + "." + deGBoolean.getUid() + "})" ) );
        assertTrue( programIndicatorService.filterIsValid( "d2:hasValue(#{" + psA.getUid() + "." + deHDate.getUid() + "})" ) );
    }


    @Test
    public void testValueCount()
    {
        String expected = "coalesce(\"DataElmentA\"::numeric,0)" +
            " + coalesce(\"Attribute0A\"::numeric,0)" +
            " + nullif(cast((case when \"DataElmentA\" is not null then 1 else 0 end + case when \"Attribute0A\" is not null then 1 else 0 end) as double),0)";

        String expression = "#{ProgrmStagA.DataElmentA} + A{Attribute0A} + V{value_count}";

        assertEquals( expected, programIndicatorService.getAnalyticsSql( expression, indicatorA, new Date(), new Date() ) );
    }

    @Test
    public void testComparisonOperator()
    {
        String expected = "coalesce(\"DataElmentA\"::numeric,0) = 'Ongoing'";
        String expression = "#{ProgrmStagA.DataElmentA} == 'Ongoing'";
        assertEquals( expected,
            programIndicatorService.getAnalyticsSql( expression, indicatorA, new Date(), new Date() ) );
    }

    @Test
    public void testNestedSubqueryWithTableAlias()
    {
        Date dateFrom = getDate( 2019, 1, 1 );
        Date dateTo = getDate( 2019, 12, 31 );

        // Generated subquery, since indicatorF is type Enrollment

        String expected = "coalesce((select \"DataElmentA\" from analytics_event_Program000B where analytics_event_Program000B.pi = axx1.pi and \"DataElmentA\" is not null and executiondate < cast( '"
                + "2020-01-11" + "' as date ) and ps = 'ProgrmStagA' order by executiondate desc limit 1 )::numeric,0) - " +
                "coalesce((select \"DataElmentC\" from analytics_event_Program000B where analytics_event_Program000B.pi = axx1.pi and \"DataElmentC\" is not null and executiondate < cast( '"
                + "2020-01-11" + "' as date ) and ps = 'ProgrmStagB' order by executiondate desc limit 1 )::numeric,0)";

        String expression = "#{ProgrmStagA.DataElmentA} - #{ProgrmStagB.DataElmentC}";

        assertEquals( expected,
            programIndicatorService.getAnalyticsSql( expression, indicatorF, dateFrom, dateTo, "axx1" ) );
    }
}
