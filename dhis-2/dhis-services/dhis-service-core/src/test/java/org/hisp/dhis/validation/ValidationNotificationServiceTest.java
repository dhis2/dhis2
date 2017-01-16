package org.hisp.dhis.validation;

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
 */

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.ValidationNotificationMessageRenderer;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.notification.DefaultValidationNotificationService;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Set;

import static org.junit.Assert.fail;

/**
 * @author Halvdan Hoem Grelland
 */
public class ValidationNotificationServiceTest
    extends DhisConvenienceTest
{
    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private ValidationNotificationMessageRenderer renderer = new ValidationNotificationMessageRenderer();

    private DefaultValidationNotificationService service;

    private MessageService messageService;

    @Before
    public void setUpTest()
    {
        messageService = Mockito.mock( MessageService.class );

        Mockito.when( messageService.sendMessage(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.anySetOf( User.class ),
            Mockito.any( User.class ),
            Mockito.anyBoolean(),
            Mockito.anyBoolean()
        ) ).then( invocation -> new Message( invocation.getArguments() ) );

        // Setup mocks
        service = new DefaultValidationNotificationService();
        service.setNotificationMessageRenderer( renderer );
        service.setMessageService( messageService );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    public void testValidationNotificationsAreGenerated()
    {
        // TODO
        fail();
    }

    // -------------------------------------------------------------------------
    // Mocking classes
    // -------------------------------------------------------------------------

    /**
     * Mocks the input to MessageService.sendMessage(..)
     */
    static class Message
    {
        final String subject, text, metaData;
        final Set<User> users;
        final User sender;
        final boolean includeFeedbackRecipients, forceNotifications;

        @SuppressWarnings( "unchecked" )
        Message( Object[] args )
        {
            this(
                (String) args[0],
                (String) args[1],
                (String) args[2],
                (Set<User>) args[3],
                (User) args[4],
                (boolean) args[5],
                (boolean) args[6]
            );
        }

        Message(
            String subject,
            String text,
            String metaData,
            Set<User> users,
            User sender,
            boolean includeFeedbackRecipients,
            boolean forceNotifications )
        {
            this.subject = subject;
            this.text = text;
            this.metaData = metaData;
            this.users = users;
            this.sender = sender;
            this.includeFeedbackRecipients = includeFeedbackRecipients;
            this.forceNotifications = forceNotifications;
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------
}
