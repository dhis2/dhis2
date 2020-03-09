package org.hisp.dhis.artemis.audit.listener;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This component configures the Hibernate Auditing listeners. The listeners are
 * responsible for "intercepting" Hibernate-managed objects after a save/update
 * operation and pass them to the Auditing sub-system.
 * <p>
 * This bean is not active during tests.
 *
 * @author Luciano Fiandesio
 */
@Component
@Conditional( value = AuditEnabledCondition.class )
public class HibernateListenerConfigurer
{
    @PersistenceUnit
    private EntityManagerFactory emf;

    private final PostInsertAuditListener postInsertAuditListener;
    private final PostUpdateAuditListener postUpdateEventListener;
    private final PostDeleteAuditListener postDeleteEventListener;
    private final PostLoadAuditListener postLoadEventListener;

    public HibernateListenerConfigurer(
        PostInsertAuditListener postInsertAuditListener,
        PostUpdateAuditListener postUpdateEventListener,
        PostDeleteAuditListener postDeleteEventListener,
        PostLoadAuditListener postLoadEventListener )
    {
        checkNotNull( postDeleteEventListener );
        checkNotNull( postUpdateEventListener );
        checkNotNull( postInsertAuditListener );
        checkNotNull( postLoadEventListener );

        this.postInsertAuditListener = postInsertAuditListener;
        this.postUpdateEventListener = postUpdateEventListener;
        this.postDeleteEventListener = postDeleteEventListener;
        this.postLoadEventListener = postLoadEventListener;
    }

    @PostConstruct
    protected void init()
    {
        SessionFactoryImpl sessionFactory = emf.unwrap( SessionFactoryImpl.class );

        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService( EventListenerRegistry.class );

        registry.getEventListenerGroup( EventType.POST_COMMIT_INSERT ).appendListener( postInsertAuditListener );

        registry.getEventListenerGroup( EventType.POST_COMMIT_UPDATE ).appendListener( postUpdateEventListener );

        registry.getEventListenerGroup( EventType.POST_COMMIT_DELETE ).appendListener( postDeleteEventListener );

        registry.getEventListenerGroup( EventType.POST_LOAD ).appendListener( postLoadEventListener );
    }
}
