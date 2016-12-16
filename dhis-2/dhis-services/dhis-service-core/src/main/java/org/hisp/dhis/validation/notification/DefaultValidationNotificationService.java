package org.hisp.dhis.validation.notification;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.email.Email;
import org.hisp.dhis.email.EmailService;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.sms.SmsMessageBatchCreatorService;
import org.hisp.dhis.sms.config.SmsMessageSender;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroupService;
import org.hisp.dhis.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Halvdan Hoem Grelland
 */
public class DefaultValidationNotificationService
    implements ValidationNotificationService
{
    private static final Predicate<ValidationResult> APPLICABLE_VALIDATION_RESULT_PREDICATE =
        v -> Objects.nonNull( v ) && Objects.nonNull( v.getValidationRule() ) && !v.getValidationRule().getNotificationTemplates().isEmpty();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private NotificationMessageRenderer<ValidationResult> notificationRenderer;

    @Autowired
    private MessageService messageService;

    @Autowired @Qualifier( "org.hisp.dhis.sms.config.SmsMessageSender" )
    private MessageSender smsMessageSender;

    @Autowired @Qualifier( "org.hisp.dhis.message.EmailMessageSender" )
    private MessageSender emailMessageSender;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DefaultValidationNotificationService()
    {
    }

    // -------------------------------------------------------------------------
    // ValidationNotificationService implementation
    // -------------------------------------------------------------------------

    @Override
    public void sendNotifications( Set<ValidationResult> results )
    {
        Set<Message> message =
            results.stream()
                .filter( APPLICABLE_VALIDATION_RESULT_PREDICATE )
                .map( vr -> ImmutablePair.of( vr, renderNotificationMessages( vr ) ) )
                .flatMap( pair -> toMessageStream( pair.getLeft(), pair.getRight() ) )
                .collect( Collectors.toSet() );

        dispatchMessages( message );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Stream<Message> toMessageStream(
        final ValidationResult validationResult, Map<ValidationNotificationTemplate, NotificationMessage> templateToNotificationMap )
    {
        return templateToNotificationMap.entrySet().stream()
            .map( entry -> new Message( entry.getValue(), resolveRecipients( validationResult, entry.getKey() ) ) );
    }

    private Recipients resolveRecipients( ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        ValidationNotificationRecipient recipient = template.getNotificationRecipient();

        if ( recipient.isExternalRecipient() )
        {
            return new Recipients( recipientsFromOrgUnit( validationResult, template ) );
        }
        else
        {
            return new Recipients( recipientsFromUserGroups( validationResult, template ) );
        }
    }

    private static Map<DeliveryChannel, String> recipientsFromOrgUnit( final ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        Map<DeliveryChannel, String> channelPrincipalMap = new EnumMap<>( DeliveryChannel.class );
        Set<DeliveryChannel> channels = template.getDeliveryChannels();

        OrganisationUnit organisationUnit = validationResult.getOrgUnit();

        String email = organisationUnit.getEmail();

        if ( StringUtils.isNotBlank( email ) && channels.contains( DeliveryChannel.EMAIL ) )
        {
            channelPrincipalMap.put( DeliveryChannel.EMAIL, email );
        }

        String phone = organisationUnit.getPhoneNumber();

        if ( StringUtils.isNotBlank( phone ) && channels.contains( DeliveryChannel.SMS ) )
        {
            channelPrincipalMap.put( DeliveryChannel.SMS, null );
        }

        return channelPrincipalMap;
    }

    private static Set<User> recipientsFromUserGroups( final ValidationResult validationResult, ValidationNotificationTemplate template )
    {
        final boolean limitToHierarchy = template.getNotifyUsersInHierarchyOnly();

        final List<OrganisationUnit> ancestors =
            limitToHierarchy ? validationResult.getOrgUnit().getAncestors() : Collections.emptyList();

        return template.getRecipientUserGroups().stream()
                .flatMap( ug -> ug.getMembers().stream() )
                .distinct()
                .filter( user -> !limitToHierarchy || ancestors.contains( user.getOrganisationUnit() ) )
                .collect( Collectors.toSet() );
    }

    private Map<ValidationNotificationTemplate, NotificationMessage> renderNotificationMessages( ValidationResult validationResult )
    {
        return validationResult.getValidationRule().getNotificationTemplates().stream()
                .collect( Collectors.toMap( t -> t, t -> notificationRenderer.render( validationResult, t ) ) );
    }

    private void dispatchMessages( Set<Message> messages )
    {
        messages.forEach( this::sendMessage );
    }

    private void sendMessage( Message message )
    {
        if ( message.recipients.isExternal() )
        {
            sendExternalMessage( message );
        }
        else
        {
            sendDhisMessage( message );
        }
    }

    private void sendDhisMessage( Message message )
    {
        messageService.sendMessage( message.message.getSubject(), message.message.getMessage(), null, message.recipients.userRecipients );
    }

    private void sendExternalMessage( Message message )
    {
        Map<DeliveryChannel, String> recipients = message.recipients.externalRecipients;

        recipients.entrySet().forEach( entry -> {
            if ( entry.getKey() == DeliveryChannel.EMAIL )
            {
                sendEmail( message );
            }
            else if ( entry.getKey() == DeliveryChannel.SMS )
            {
                sendSms( message );
            }
        });
    }

    // TODO Generify the two below methods into one.

    // TODO Make sure email is valid etc.
    private void sendEmail( Message notification )
    {
        emailMessageSender.sendMessage(
            notification.message.getSubject(),
            notification.message.getMessage(),
            notification.recipients.externalRecipients.get( DeliveryChannel.EMAIL )
        );
    }

    // TODO Make sure number is valid etc.
    private void sendSms( Message notfication )
    {
        smsMessageSender.sendMessage(
            "",
            notfication.message.getMessage(),
            notfication.recipients.externalRecipients.get( DeliveryChannel.SMS )
        );
    }

    // -------------------------------------------------------------------------
    // Internal classes
    // -------------------------------------------------------------------------

    private static class Recipients
    {
        final Set<User> userRecipients;
        final Map<DeliveryChannel, String> externalRecipients;

        Recipients( Set<User> userRecipients )
        {
            this.userRecipients = userRecipients;
            this.externalRecipients = null;
        }

        Recipients( Map<DeliveryChannel, String> externalRecipients )
        {
            this.userRecipients = null;
            this.externalRecipients = externalRecipients;
        }

        boolean isExternal()
        {
            return externalRecipients != null;
        }
    }

    private static class Message
    {
        final NotificationMessage message;
        final Recipients recipients;

        public Message( NotificationMessage message, Recipients recipients )
        {
            this.message = message;
            this.recipients = recipients;
        }
    }
}
