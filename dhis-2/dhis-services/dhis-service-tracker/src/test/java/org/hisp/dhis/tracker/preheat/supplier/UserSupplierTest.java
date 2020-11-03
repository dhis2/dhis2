package org.hisp.dhis.tracker.preheat.supplier;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Luciano Fiandesio
 */
public class UserSupplierTest
{
    @InjectMocks
    private UserSupplier supplier;

    @Mock
    private IdentifiableObjectManager manager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private BeanRandomizer rnd = new BeanRandomizer();

    @Test
    public void verifySupplier()
    {
        final List<Event> events = rnd.randomObjects( Event.class, 5 );
        final List<User> users = rnd.randomObjects( User.class, 5 );
        final List<String> userIds = events.stream().map( e -> e.getAssignedUser().getUid() )
            .collect( Collectors.toList() );

        IntStream.range( 0, 5 )
            .forEach( i -> users.get( i ).setUid( events.get( i ).getAssignedUser().getUid() ) );

        when( manager.getByUid( eq( User.class ),
            argThat( t -> t.containsAll( userIds ) ) ) ).thenReturn( users );

        final TrackerPreheatParams preheatParams = TrackerPreheatParams.builder()
            .events( events )
            .build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( preheatParams, preheat );

        for ( String userUid : userIds )
        {
            assertThat( preheat.get( TrackerIdScheme.UID, User.class, userUid ), is( notNullValue() ) );
        }
        // Make sure also User Credentials object are cached in the pre-heat
        assertThat( preheat.getAll( TrackerIdScheme.UID, UserCredentials.class ), hasSize( 5 ) );
    }
}