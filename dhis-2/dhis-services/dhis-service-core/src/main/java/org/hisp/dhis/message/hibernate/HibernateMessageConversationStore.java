package org.hisp.dhis.message.hibernate;

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

import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageConversationStatus;
import org.hisp.dhis.message.MessageConversationStore;
import org.hisp.dhis.message.UserMessage;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Lars Helge Overland
 */
public class HibernateMessageConversationStore
    extends HibernateIdentifiableObjectStore<MessageConversation>
    implements MessageConversationStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private StatementBuilder statementBuilder;

    public void setStatementBuilder( StatementBuilder statementBuilder )
    {
        this.statementBuilder = statementBuilder;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    private List<Integer> getUserMessagesForUser( User user )
    {
        String userMessagesSql = "select distinct mc.messageconversationid " +
            "from messageconversation mc INNER JOIN messageconversation_usermessages as mcu " +
            "on mc.messageconversationid = mcu.messageconversationid " +
            "INNER JOIN usermessage as um on mcu.usermessageid = um.usermessageid  where um.userid = " + user.getId();

        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet( userMessagesSql );

        List<Integer> userMessages = new ArrayList<>( );
        while (sqlRowSet.next()) {
            userMessages.add( Integer.parseInt( sqlRowSet.getString( 1 ) ) );
        }

        return userMessages;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<MessageConversation> getMessageConversations( User user, MessageConversationStatus status,
        boolean followUpOnly, boolean unreadOnly,
        Integer first, Integer max )
    {
        Assert.notNull( user, "User must be specified" );

        List<Integer> userMessages = getUserMessagesForUser( user );
        if ( userMessages.size() == 0 )
        {
            return new ArrayList<>( );
        }

        String hql = "select distinct mc " +
            "from MessageConversation mc " +
            "inner join mc.userMessages as um " +
            "left join mc.user as ui " +
            "left join mc.lastSender as ls " +
            "where mc.id in (" + userMessages.toString().replace("[", "").replace("]", "") + ") ";

        if ( status != null )
        {
            hql += "and where status = :status ";
        }

        if ( followUpOnly )
        {
            hql += (status != null ? "and" : "where") + " um.followUp = true ";
        }

        if ( unreadOnly )
        {
            hql += (status != null || followUpOnly ? "and" : "where") + " um.read = false ";
        }

        hql += "order by mc.lastMessage desc ";

        Query query = getQuery( hql );

        if ( status != null )
        {
            query.setParameter( "status", status.name() );
        }

        if ( first != null && max != null )
        {
            query.setFirstResult( first );
            query.setMaxResults( max );
        }

        return (List<MessageConversation>) query.list()
            .stream()
            .map( o -> mapRowToMessageConversations( (MessageConversation) o, user ) )
            .collect( Collectors.toList() );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<MessageConversation> getMessageConversations( Collection<String> uids )
    {
        return getSharingCriteria()
            .add( Restrictions.in( "uid", uids ) )
            .list();
    }

    @Override
    public int getMessageConversationCount( User user, boolean followUpOnly, boolean unreadOnly )
    {
        Assert.notNull( user, "User must be specified" );

        String sql = "select count(*) from messageconversation mc " +
            "left join messageconversation_usermessages mu on mc.messageconversationid=mu.messageconversationid " +
            "left join usermessage um on mu.usermessageid=um.usermessageid " + "where um.userid=" + user.getId() + " ";

        if ( followUpOnly )
        {
            sql += "and um.isfollowup=true ";
        }

        if ( unreadOnly )
        {
            sql += "and um.isread=false ";
        }

        return jdbcTemplate.queryForObject( sql, Integer.class );
    }

    @Override
    public long getUnreadUserMessageConversationCount( User user )
    {
        Assert.notNull( user, "User must be specified" );

        String sql = "select count(*) from usermessage where userid = " + user.getId() + " and isread=false;";

        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet( sql );

        sqlRowSet.next();

        return Long.parseLong( sqlRowSet.getString( 1 ));
    }

    @Override
    public int deleteMessages( User sender )
    {
        Assert.notNull( sender, "User must be specified" );

        String sql = "delete from messageconversation_messages where messageid in (" +
            "select messageid from message where userid = " + sender.getId() + ")";

        getSqlQuery( sql ).executeUpdate();

        String hql = "delete Message m where m.sender = :sender";

        Query query = getQuery( hql );
        query.setEntity( "sender", sender );
        return query.executeUpdate();
    }

    @Override
    public int deleteUserMessages( User user )
    {
        Assert.notNull( user, "User must be specified" );

        String sql = "delete from messageconversation_usermessages where usermessageid in (" +
            "select usermessageid from usermessage where userid = " + user.getId() + ")";

        getSqlQuery( sql ).executeUpdate();

        String hql = "delete UserMessage u where u.user = :user";

        Query query = getQuery( hql );
        query.setEntity( "user", user );
        return query.executeUpdate();
    }

    @Override
    public int removeUserFromMessageConversations( User lastSender )
    {
        Assert.notNull( lastSender, "User must be specified" );

        String hql = "update MessageConversation m set m.lastSender = null where m.lastSender = :lastSender";

        Query query = getQuery( hql );
        query.setEntity( "lastSender", lastSender );
        return query.executeUpdate();
    }

    @Override
    public List<UserMessage> getLastRecipients( User user, Integer first, Integer max )
    {
        Assert.notNull( user, "User must be specified" );

        String sql = " select distinct userinfoid, surname, firstname from userinfo uf " +
            "join usermessage um on (uf.userinfoid = um.userid) " +
            "join messageconversation_usermessages mu on (um.usermessageid = mu.usermessageid) " +
            "join messageconversation mc on (mu.messageconversationid = mc.messageconversationid) " +
            "where mc.lastsenderid = " + user.getId();

        sql += " order by userinfoid desc";

        if ( first != null && max != null )
        {
            sql += " " + statementBuilder.limitRecord( first, max );
        }

        return jdbcTemplate.query( sql, ( resultSet, count ) -> {
            UserMessage recipient = new UserMessage();

            recipient.setId( resultSet.getInt( 1 ) );
            recipient.setLastRecipientSurname( resultSet.getString( 2 ) );
            recipient.setLastRecipientFirstname( resultSet.getString( 3 ) );

            return recipient;
        } );
    }

    private MessageConversation mapRowToMessageConversations( MessageConversation mc, User user )
    {
        Boolean isRead = false;
        Boolean isFollowUp = false;

        for ( UserMessage userMessage : mc.getUserMessages() )
        {
            if ( userMessage.getUser().getUid().equals( user.getUid() ) )
            {
                if ( userMessage.isRead() )
                {
                    isRead = true;
                }

                if ( userMessage.isFollowUp() )
                {
                    isFollowUp = true;
                }
            }
        }

        mc.setRead( isRead );
        mc.setFollowUp( isFollowUp );

        if ( mc.getUser() != null )
        {
            mc.setUserFirstname( mc.getUser().getFirstName() );
            mc.setUserSurname( mc.getUser().getSurname() );
        }

        if ( mc.getLastSender() != null )
        {
            mc.setLastSenderFirstname( mc.getLastSender().getFirstName() );
            mc.setLastSenderSurname( mc.getLastSender().getSurname() );
        }

        return mc;
    }
}