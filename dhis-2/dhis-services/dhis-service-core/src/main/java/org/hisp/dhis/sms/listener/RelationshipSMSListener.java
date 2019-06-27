package org.hisp.dhis.sms.listener;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipItem;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.sms.incoming.IncomingSms;
import org.hisp.dhis.smscompression.SMSConsts.SubmissionType;
import org.hisp.dhis.smscompression.SMSResponse;
import org.hisp.dhis.smscompression.models.RelationshipSMSSubmission;
import org.hisp.dhis.smscompression.models.SMSSubmission;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipSMSListener
    extends
    NewSMSListener
{

    private enum RelationshipDir
    {
        FROM, TO;
    }

    @Autowired
    private RelationshipService relationshipService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Override
    protected SMSResponse postProcess( IncomingSms sms, SMSSubmission submission )
        throws SMSProcessingException
    {
        RelationshipSMSSubmission subm = (RelationshipSMSSubmission) submission;

        String fromid = subm.getFrom();
        String toid = subm.getTo();
        String typeid = subm.getRelationshipType();

        RelationshipType relType = relationshipTypeService.getRelationshipType( typeid );
        if ( relType == null )
        {
            throw new SMSProcessingException( SMSResponse.INVALID_RELTYPE.set( typeid ) );
        }

        RelationshipItem fromItem = createRelationshipItem( relType, RelationshipDir.FROM, fromid );
        RelationshipItem toItem = createRelationshipItem( relType, RelationshipDir.TO, toid );

        Relationship rel = new Relationship();
        // If we aren't given a UID for the relationship, it will be
        // auto-generated
        if ( subm.getRelationship() != null )
            rel.setUid( subm.getRelationship() );
        rel.setRelationshipType( relType );
        rel.setFrom( fromItem );
        rel.setTo( toItem );
        rel.setCreated( new Date() );
        rel.setLastUpdated( new Date() );
        // TODO: Are there values we need to account for in relationships?
//		rel.setAttributeValues(attributeValues);

        relationshipService.addRelationship( rel );

        return SMSResponse.SUCCESS;
    }

    private RelationshipItem createRelationshipItem( RelationshipType relType, RelationshipDir dir, String objId )
    {
        RelationshipItem relItem = new RelationshipItem();
        RelationshipEntity fromEnt = relType.getFromConstraint().getRelationshipEntity();
        RelationshipEntity toEnt = relType.getFromConstraint().getRelationshipEntity();
        RelationshipEntity relEnt = dir == RelationshipDir.FROM ? fromEnt : toEnt;

        switch ( relEnt )
        {
        case TRACKED_ENTITY_INSTANCE:
            TrackedEntityInstance tei = trackedEntityInstanceService.getTrackedEntityInstance( objId );
            if ( tei == null )
            {
                throw new SMSProcessingException( SMSResponse.INVALID_TEI.set( objId ) );
            }
            relItem.setTrackedEntityInstance( tei );
            break;

        case PROGRAM_INSTANCE:
            ProgramInstance progInst = programInstanceService.getProgramInstance( objId );
            if ( progInst == null )
            {
                throw new SMSProcessingException( SMSResponse.INVALID_ENROLL.set( objId ) );
            }
            relItem.setProgramInstance( progInst );
            break;

        case PROGRAM_STAGE_INSTANCE:
            ProgramStageInstance stageInst = programStageInstanceService.getProgramStageInstance( objId );
            if ( stageInst == null )
            {
                throw new SMSProcessingException( SMSResponse.INVALID_EVENT.set( objId ) );
            }
            relItem.setProgramStageInstance( stageInst );
            break;

        }

        return relItem;
    }

    @Override
    protected boolean handlesType( SubmissionType type )
    {
        return (type == SubmissionType.RELATIONSHIP);
    }

}
