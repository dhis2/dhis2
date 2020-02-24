package org.hisp.dhis.sms.incoming;

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

import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.sms.MessageQueue;
import org.hisp.dhis.user.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import static com.google.common.base.Preconditions.checkNotNull;

@Service( "org.hisp.dhis.sms.incoming.IncomingSmsService" )
public class DefaultIncomingSmsService
    implements IncomingSmsService
{
    private static final String DEFAULT_GATEWAY = "default";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final IncomingSmsStore incomingSmsStore;

    private final MessageQueue incomingSmsQueue;

    public DefaultIncomingSmsService(IncomingSmsStore incomingSmsStore, @Lazy MessageQueue incomingSmsQueue) {

        checkNotNull( incomingSmsQueue );
        checkNotNull( incomingSmsStore );

        this.incomingSmsStore = incomingSmsStore;
        this.incomingSmsQueue = incomingSmsQueue;
    }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    public List<IncomingSms> listAllMessage()
    {
        return incomingSmsStore.getAllSmses();
    }

    @Override
    @Transactional
    public int save( IncomingSms sms )
    {
        if ( sms.getReceivedDate() != null )
        {
            sms.setSentDate( sms.getReceivedDate() );
        }
        else
        {
            sms.setSentDate( new Date() );
        }

        sms.setReceivedDate( new Date() );
        sms.setGatewayId( StringUtils.defaultIfBlank( sms.getGatewayId(), DEFAULT_GATEWAY ) );

        incomingSmsStore.save( sms );
        incomingSmsQueue.put( sms );
        return sms.getId();
    }

    @Override
    @Transactional
    public int save( String message, String originator, String gateway, Date receivedTime, User user )
    {
        IncomingSms sms = new IncomingSms();
        sms.setText( message );
        sms.setOriginator( originator );
        sms.setGatewayId( gateway );
        sms.setUser( user );

        if ( receivedTime != null )
        {
            sms.setSentDate( receivedTime );
        }
        else
        {
            sms.setSentDate( new Date() );
        }
        
        sms.setReceivedDate( new Date() );
        
        return save( sms );
    }

    @Override
    @Transactional
    public void deleteById( Integer id )
    {
        IncomingSms incomingSms = incomingSmsStore.get( id );

        incomingSmsStore.delete( incomingSms );
    }

    @Override
    @Transactional(readOnly = true)
    public IncomingSms findBy( Integer id )
    {
        return incomingSmsStore.get( id );
    }

    @Override
    @Transactional(readOnly = true)
    public IncomingSms getNextUnprocessed()
    {
        return null;
    }

    @Override
    @Transactional
    public void update( IncomingSms incomingSms )
    {
        incomingSmsStore.update( incomingSms );
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomingSms> getSmsByStatus( SmsMessageStatus status, String keyword )
    {
        return incomingSmsStore.getSmsByStatus( status, keyword );
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomingSms> getSmsByStatus( SmsMessageStatus status, String keyword, Integer min, Integer max )
    {
        return incomingSmsStore.getSmsByStatus( status, keyword, min, max );
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomingSms> getAllUnparsedMessages()
    {
        return incomingSmsStore.getAllUnparsedMessages();
    }
}
