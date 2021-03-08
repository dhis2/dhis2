/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.program.notification;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.MetadataObject;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.notification.NotificationTemplate;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.user.UserGroup;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

/**
 * @author Halvdan Hoem Grelland
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement( namespace = DxfNamespaces.DXF_2_0 )
public class ProgramNotificationTemplate
    extends BaseIdentifiableObject implements NotificationTemplate, MetadataObject
{
    @JsonProperty
    private String subjectTemplate;

    @JsonProperty
    private String messageTemplate;

    @JsonProperty
    private NotificationTrigger notificationTrigger = NotificationTrigger.COMPLETION;

    @JsonProperty
    private ProgramNotificationRecipient notificationRecipient = ProgramNotificationRecipient.USER_GROUP;

    @JsonProperty
    private Set<DeliveryChannel> deliveryChannels = Sets.newHashSet();

    @JsonProperty
    private Boolean notifyUsersInHierarchyOnly;

    @JsonProperty
    private Boolean notifyParentOrganisationUnitOnly;

    // -------------------------------------------------------------------------
    // Conditionally relevant properties
    // -------------------------------------------------------------------------

    @JsonProperty
    private Integer relativeScheduledDays = null;

    @JsonProperty
    private UserGroup recipientUserGroup = null;

    @JsonProperty
    private TrackedEntityAttribute recipientProgramAttribute = null;

    @JsonProperty
    private DataElement recipientDataElement = null;

    @JsonProperty
    private boolean sendRepeatable;

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "uid", uid )
            .add( "name", name )
            .add( "notificationTrigger", notificationTrigger )
            .add( "notificationRecipient", notificationRecipient )
            .add( "deliveryChannels", deliveryChannels )
            .add( "messageTemplate", messageTemplate )
            .add( "subjectTemplate", subjectTemplate )
            .toString();
    }
}
