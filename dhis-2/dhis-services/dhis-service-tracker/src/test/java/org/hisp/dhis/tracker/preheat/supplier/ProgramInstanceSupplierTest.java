package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class ProgramInstanceSupplierTest
{
    @InjectMocks
    private ProgramInstanceSupplier supplier;

    @Mock
    private ProgramInstanceStore store;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void verifySupplier()
    {
        final List<ProgramInstance> programInstances = rnd.randomObjects( ProgramInstance.class, 5 );
        when( store.getByType( WITHOUT_REGISTRATION ) ).thenReturn( programInstances );

        final TrackerPreheatParams preheatParams = TrackerPreheatParams.builder().build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( preheatParams, preheat );

        assertThat( preheat.getEnrollments().values(), hasSize( 5 ) );
        assertThat( preheat.getEnrollments().keySet(),
            containsInAnyOrder(
                programInstances.stream().map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() ) ) );
    }

}