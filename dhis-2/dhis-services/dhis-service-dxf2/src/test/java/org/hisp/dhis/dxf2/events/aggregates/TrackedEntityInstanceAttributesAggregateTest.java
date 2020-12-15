package org.hisp.dhis.dxf2.events.aggregates;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dxf2.TrackerTest;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.dxf2.events.trackedentity.Attribute;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceQueryParams;
import org.hisp.dhis.trackedentity.TrackedEntityTypeAttribute;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAccess;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

import com.google.common.collect.Sets;

/**
 * @author Luciano Fiandesio
 */
public class TrackedEntityInstanceAttributesAggregateTest extends TrackerTest
{
    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private org.hisp.dhis.trackedentity.TrackedEntityInstanceService teiService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private TrackedEntityAttributeValueService attributeValueService;

    @Autowired
    private TrackedEntityInstanceAggregate trackedEntityInstanceAggregate;

    @Autowired
    private ProgramService programService;

    @Autowired
    private TrackerOwnershipManager trackerOwnershipManager;

    private Program programB;

    private final static int A = 65;
    private final static int C = 67;
    private final static int D = 68;
    private final static int F = 70;

    @Override
    protected void mockCurrentUserService()
    {
        User user = createUser( "testUser" );

        user.addOrganisationUnit( organisationUnitA );
        user.getTeiSearchOrganisationUnits().add( organisationUnitA );
        user.getTeiSearchOrganisationUnits().add( organisationUnitB );
       // makeUserSuper( user );
        manager.update( user );
        currentUserService = new MockCurrentUserService( user );


        ReflectionTestUtils.setField( trackedEntityInstanceAggregate, "currentUserService", currentUserService );
        ReflectionTestUtils.setField( trackedEntityInstanceService, "currentUserService", currentUserService );
        ReflectionTestUtils.setField( teiService, "currentUserService", currentUserService );

    }

    @Before
    public void setUp()
    {
        ReflectionTestUtils.setField( trackedEntityInstanceAggregate, "currentUserService", currentUserService );
    }

    @Test
    public void testTrackedEntityInstanceIncludeAllAttributes()
    {
        populatePrerequisites( false );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );


        assertAttributes( trackedEntityInstances.get( 0 ).getAttributes(), "A","B","C","E" );
    }

    @Test
    public void testTrackedEntityInstanceIncludeAllAttributesEnrollmentsEventsRelationshipsOwners()
    {
        populatePrerequisites( true );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );


        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, TrackedEntityInstanceParams.TRUE, false );


        assertThat( trackedEntityInstances.get( 0 ).getEnrollments(), hasSize( 1 ) );
        assertThat( trackedEntityInstances.get( 0 ).getProgramOwners(), hasSize( 2 ) );
    }



    @Test
    public void testTrackedEntityInstanceIncludeAllAttributesInProtectedProgramNoAccess()
    {
        populatePrerequisites( true );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setIncludeAllAttributes( true );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertAttributes( trackedEntityInstances.get( 0 ).getAttributes(), "A","B","C" );

    }

    @Test
    public void testTrackedEntityInstanceIncludeSpecificProtectedProgram()
    {
        populatePrerequisites( false );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setProgram( programB );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertAttributes( trackedEntityInstances.get( 0 ).getAttributes(), "A","B","E" );
    }

    @Test
    public void testTrackedEntityInstanceIncludeSpecificOpenProgram()
    {
        populatePrerequisites( false );

        TrackedEntityInstanceQueryParams queryParams = new TrackedEntityInstanceQueryParams();
        queryParams.setOrganisationUnits( Sets.newHashSet( organisationUnitA ) );
        queryParams.setProgram( programA );

        TrackedEntityInstanceParams params = new TrackedEntityInstanceParams();

        final List<TrackedEntityInstance> trackedEntityInstances = trackedEntityInstanceService
            .getTrackedEntityInstances2( queryParams, params, false );

        assertAttributes( trackedEntityInstances.get( 0 ).getAttributes(), "A","B","C" );
    }

    private Attribute findByValue(List<Attribute> attributes, String val) {

        return attributes.stream().filter( a -> a.getValue().equals( val ) ).findFirst()
            .orElseThrow( () -> new NullPointerException( "Attribute not found!" ) );

    }
    private void assertAttributes( final List<Attribute> attributes, String... atts )
    {
        assertThat( attributes, hasSize( atts.length ) );

        for(String att : atts)
        {
            assertThat( findByValue( attributes, att ).getAttribute(),
                is( attributeService.getTrackedEntityAttributeByName( "Attribute" + att ).getUid() ) );
        }

    }

    private void populatePrerequisites( boolean removeOwnership )
    {
        doInTransaction( () -> {

            ProgramStage programStageA = createProgramStage( programB, true );
            ProgramStage programStageB = createProgramStage( programB, true );

            ProgramStage programStageA1 = createProgramStage( programA, true );
            ProgramStage programStageA2 = createProgramStage( programA, true );

            // Create 5 Tracked Entity Attributes (named A .. E)
            IntStream.range( A, F ).mapToObj( i -> Character.toString( (char) i ) ).forEach( c -> attributeService
                .addTrackedEntityAttribute( createTrackedEntityAttribute( c.charAt( 0 ), ValueType.TEXT ) ) );

            // Transform the Tracked Entity Attributes into a List of
            // TrackedEntityTypeAttribute
            List<TrackedEntityTypeAttribute> teatList = IntStream.range( A, C )
                .mapToObj( i -> Character.toString( (char) i ) )
                .map( s -> new TrackedEntityTypeAttribute( trackedEntityTypeA,
                    attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
                .collect( Collectors.toList() );

            // Assign 2 (A, B) TrackedEntityTypeAttribute to Tracked Entity Type A
            trackedEntityTypeA.getTrackedEntityTypeAttributes().addAll( teatList );

            //Make TET public
            trackedEntityTypeA.setPublicAccess( AccessStringHelper.FULL );

            manager.update( trackedEntityTypeA );

            programB = createProgram( 'B', new HashSet<>(), organisationUnitA );
            programB.setProgramType( ProgramType.WITH_REGISTRATION );
            programB.setCategoryCombo( categoryComboA );
            programB.setAccessLevel( AccessLevel.PROTECTED );
            programB.setUid( CodeGenerator.generateUid() );
            programB.setCode( RandomStringUtils.randomAlphanumeric( 10 ) );

            Set<UserAccess> programBUserAccess = new HashSet<>();
            programBUserAccess.add(new UserAccess( currentUserService.getCurrentUser(), AccessStringHelper.FULL ));
            programB.setUserAccesses( programBUserAccess );
            programB.setProgramStages(
                Stream.of( programStageA, programStageB ).collect( Collectors.toCollection( HashSet::new ) ) );
            programService.addProgram( programB );

            programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
            programA.setProgramType( ProgramType.WITH_REGISTRATION );
            programA.setCategoryCombo( categoryComboA );
            programA.setUid( CodeGenerator.generateUid() );
            programA.setCode( RandomStringUtils.randomAlphanumeric( 10 ) );
            programA.setProgramStages(
                Stream.of( programStageA1, programStageA2 ).collect( Collectors.toCollection( HashSet::new ) ) );

            programService.addProgram( programA );

            //Because access strings isnt getting persisted with programService methods for some reason
            programB.setPublicAccess( AccessStringHelper.FULL );
            manager.update( programB );

            programA.setPublicAccess( AccessStringHelper.FULL );
            manager.update( programA );

            programStageA.setPublicAccess( AccessStringHelper.FULL );
            manager.update( programStageA );

            programStageB.setPublicAccess( AccessStringHelper.FULL );
            manager.update( programStageB );

            programStageA1.setPublicAccess( AccessStringHelper.FULL );
            manager.update( programStageA1 );

            programStageA2.setPublicAccess( AccessStringHelper.FULL );
            manager.update( programStageA2 );


            // Assign ProgramTrackedEntityAttribute C to program A
            List<ProgramTrackedEntityAttribute> pteaListA = IntStream.range( C,  D )
                .mapToObj( i -> Character.toString( (char) i ) ).map( s -> new ProgramTrackedEntityAttribute( programA,
                    attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
                .collect( Collectors.toList() );

            // Assign ProgramTrackedEntityAttribute D, E to program B
            List<ProgramTrackedEntityAttribute> pteaListB = IntStream.range( D,  F )
                .mapToObj( i -> Character.toString( (char) i ) ).map( s -> new ProgramTrackedEntityAttribute( programB,
                    attributeService.getTrackedEntityAttributeByName( "Attribute" + s ) ) )
                .collect( Collectors.toList() );

            programA.getProgramAttributes().addAll( pteaListA );
            programB.getProgramAttributes().addAll( pteaListB );
            manager.update( programA );
            manager.update( programB );


            // Create a TEI associated to program B
            final org.hisp.dhis.trackedentity.TrackedEntityInstance trackedEntityInstance = persistTrackedEntityInstance(
                ImmutableMap.of( "program", programB ) );

            ProgramInstance piB = new ProgramInstance( programB, trackedEntityInstance, organisationUnitA );
            piB.setEnrollmentDate( new Date() );
            manager.save( piB );

            ProgramInstance piA = new ProgramInstance( programA, trackedEntityInstance, organisationUnitA );
            piA.setEnrollmentDate( new Date() );
            manager.save( piA );

            if ( removeOwnership )
            {
                trackerOwnershipManager.assignOwnership( trackedEntityInstance, programB, organisationUnitB, true, true );
                trackerOwnershipManager.assignOwnership( trackedEntityInstance, programA, organisationUnitA, true, true );
            }
            else
            {
                trackerOwnershipManager.assignOwnership( trackedEntityInstance, programB, organisationUnitA, true, true );
                trackerOwnershipManager.assignOwnership( trackedEntityInstance, programA, organisationUnitA, true, true );
            }

            // Assign Attribute A,B,E to Tracked Entity Instance
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeService.getTrackedEntityAttributeByName("AttributeA"), trackedEntityInstance, "A" ) );
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeService.getTrackedEntityAttributeByName("AttributeB"), trackedEntityInstance, "B" ) );
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeService.getTrackedEntityAttributeByName("AttributeC"), trackedEntityInstance, "C" ) );
            attributeValueService.addTrackedEntityAttributeValue(
                new TrackedEntityAttributeValue( attributeService.getTrackedEntityAttributeByName("AttributeE"), trackedEntityInstance, "E" ) );
        } );
    }
}
