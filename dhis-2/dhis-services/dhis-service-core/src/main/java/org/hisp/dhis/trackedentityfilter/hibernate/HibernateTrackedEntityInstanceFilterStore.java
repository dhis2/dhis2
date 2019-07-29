package org.hisp.dhis.trackedentityfilter.hibernate;

import org.hibernate.SessionFactory;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentityfilter.TrackedEntityInstanceFilter;
import org.hisp.dhis.trackedentityfilter.TrackedEntityInstanceFilterStore;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

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

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
@Repository( "org.hisp.dhis.trackedentityfilter.TrackedEntityInstanceFilterStore" )
public class HibernateTrackedEntityInstanceFilterStore
    extends HibernateIdentifiableObjectStore<TrackedEntityInstanceFilter> implements TrackedEntityInstanceFilterStore
{

    public HibernateTrackedEntityInstanceFilterStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        CurrentUserService currentUserService, DeletedObjectService deletedObjectService, AclService aclService )
    {
        super( sessionFactory, jdbcTemplate, TrackedEntityInstanceFilter.class, currentUserService,
            deletedObjectService, aclService, true );
    }

    @Override
    public List<TrackedEntityInstanceFilter> get( Program program )
    {
        CriteriaBuilder builder = getCriteriaBuilder();

        return getList( builder, newJpaParameters()
            .addPredicate( root -> builder.equal( root.get( "program" ), program ) ) );
    }
}
