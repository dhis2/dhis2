package org.hisp.dhis.deletedobject.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
 *
 */

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.deletedobject.DeletedObject;
import org.hisp.dhis.deletedobject.DeletedObjectId;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectStore;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class HibernateDeletedObjectStore
    implements DeletedObjectStore
{
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public DeletedObjectId save( DeletedObject deletedObject )
    {
        return (DeletedObjectId) getCurrentSession().save( deletedObject );
    }

    @Override
    public void delete( DeletedObject deletedObject )
    {
        getCurrentSession().delete( deletedObject );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DeletedObject> getByKlass( String klass )
    {
        Query query = getCurrentSession().createQuery( "SELECT c FROM DeletedObject c WHERE c.deletedObjectId.klass=:klass" );
        query.setString( "klass", klass );

        return query.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<DeletedObject> getAll( DeletedObjectQuery query )
    {
        Criteria criteria = getCurrentSession().createCriteria( DeletedObject.class );

        if ( query.getKlass() != null )
        {
            criteria.add( Restrictions.eq( "deletedObjectId.klass", query.getKlass() ) );
        }

        if ( query.getFirst() != null )
        {
            criteria.setFirstResult( query.getFirst() );
            criteria.setMaxResults( query.getMax() );
        }

        return criteria.list();
    }

    private Session getCurrentSession()
    {
        return sessionFactory.getCurrentSession();
    }
}
