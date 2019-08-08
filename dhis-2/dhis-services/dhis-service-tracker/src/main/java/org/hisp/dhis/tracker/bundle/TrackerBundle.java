package org.hisp.dhis.tracker.bundle;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.FlushMode;
import org.hisp.dhis.tracker.TrackerBundleReportMode;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.ValidationMode;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.Relationship;
import org.hisp.dhis.tracker.domain.TrackedEntity;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.user.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class TrackerBundle
{
    /**
     * User to use for import job.
     */
    private User user;

    /**
     * Should import be imported or just validated.
     */
    private TrackerBundleMode importMode = TrackerBundleMode.COMMIT;

    /**
     * What identifiers to match on.
     */
    private TrackerIdentifier identifier = TrackerIdentifier.UID;

    /**
     * Sets import strategy (create, update, etc).
     */
    private TrackerImportStrategy importStrategy = TrackerImportStrategy.CREATE;

    /**
     * Should import be treated as a atomic import (all or nothing).
     */
    private AtomicMode atomicMode = AtomicMode.ALL;

    /**
     * Flush for every object or per type.
     */
    private FlushMode flushMode = FlushMode.AUTO;

    /**
     * Validation mode to use, defaults to fully validated objects.
     */
    private ValidationMode validationMode = ValidationMode.FULL;

    /**
     * Give full report, or only include errors.
     */
    private TrackerBundleReportMode reportMode = TrackerBundleReportMode.ERRORS;

    /**
     * Preheat bundle for all attached objects (or null if preheater not run yet).
     */
    private TrackerPreheat preheat;

    /**
     * Tracked entities to import.
     */
    private List<TrackedEntity> trackedEntities = new ArrayList<>();

    /**
     * Enrollments to import.
     */
    private List<Enrollment> enrollments = new ArrayList<>();

    /**
     * Events to import.
     */
    private List<Event> events = new ArrayList<>();

    private List<Relationship> relationships = new ArrayList<>();

    public TrackerBundle()
    {
    }

    public User getUser()
    {
        return user;
    }

    public TrackerBundle setUser( User user )
    {
        this.user = user;
        return this;
    }

    public String getUsername()
    {
        return user != null ? user.getUsername() : "system-process";
    }

    public TrackerBundleMode getImportMode()
    {
        return importMode;
    }

    public TrackerBundle setImportMode( TrackerBundleMode importMode )
    {
        this.importMode = importMode;
        return this;
    }

    public TrackerIdentifier getIdentifier()
    {
        return identifier;
    }

    public TrackerBundle setIdentifier( TrackerIdentifier identifier )
    {
        this.identifier = identifier;
        return this;
    }

    public TrackerImportStrategy getImportStrategy()
    {
        return importStrategy;
    }

    public TrackerBundle setImportStrategy( TrackerImportStrategy importStrategy )
    {
        this.importStrategy = importStrategy;
        return this;
    }

    public AtomicMode getAtomicMode()
    {
        return atomicMode;
    }

    public TrackerBundle setAtomicMode( AtomicMode atomicMode )
    {
        this.atomicMode = atomicMode;
        return this;
    }

    public FlushMode getFlushMode()
    {
        return flushMode;
    }

    public TrackerBundle setFlushMode( FlushMode flushMode )
    {
        this.flushMode = flushMode;
        return this;
    }

    public ValidationMode getValidationMode()
    {
        return validationMode;
    }

    public TrackerBundle setValidationMode( ValidationMode validationMode )
    {
        this.validationMode = validationMode;
        return this;
    }

    public TrackerBundleReportMode getReportMode()
    {
        return reportMode;
    }

    public TrackerBundle setReportMode( TrackerBundleReportMode reportMode )
    {
        this.reportMode = reportMode;
        return this;
    }

    public TrackerPreheat getPreheat()
    {
        return preheat;
    }

    public TrackerBundle setPreheat( TrackerPreheat preheat )
    {
        this.preheat = preheat;
        return this;
    }

    public List<TrackedEntity> getTrackedEntities()
    {
        return trackedEntities;
    }

    public TrackerBundle setTrackedEntities( List<TrackedEntity> trackedEntities )
    {
        this.trackedEntities = trackedEntities;
        return this;
    }

    public TrackerBundle addTrackedEntity( TrackedEntity... trackedEntity )
    {
        trackedEntities.addAll( Arrays.asList( trackedEntity ) );
        return this;
    }

    public List<Enrollment> getEnrollments()
    {
        return enrollments;
    }

    public TrackerBundle setEnrollments( List<Enrollment> enrollments )
    {
        this.enrollments = enrollments;
        return this;
    }

    public TrackerBundle addEnrollment( Enrollment... enrollment )
    {
        enrollments.addAll( Arrays.asList( enrollment ) );
        return this;
    }

    public List<Event> getEvents()
    {
        return events;
    }

    public TrackerBundle setEvents( List<Event> events )
    {
        this.events = events;
        return this;
    }

    public TrackerBundle addEvent( Event... event )
    {
        events.addAll( Arrays.asList( event ) );
        return this;
    }

    @Override
    public String toString()
    {
        return "TrackerBundle{" +
            "user=" + getUsername() +
            ", importMode=" + importMode +
            ", identifier=" + identifier +
            ", importStrategy=" + importStrategy +
            ", atomicMode=" + atomicMode +
            ", flushMode=" + flushMode +
            ", validationMode=" + validationMode +
            ", reportMode=" + reportMode +
            ", preheat=" + preheat +
            ", trackedEntities=" + trackedEntities +
            ", enrollments=" + enrollments +
            ", events=" + events +
            '}';
    }
}
