package org.hisp.dhis.deletedobject;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "deletedObject", namespace = DxfNamespaces.DXF_2_0 )
public class DeletedObject
    implements Serializable
{
    /**
     * The Unique Identifier for this Object.
     */
    private DeletedObjectId deletedObjectId;

    /**
     * The unique code for this Object.
     */
    private String code;

    /**
     * Date this object was deleted.
     */
    private Date deletedAt = new Date();

    private DeletedObject()
    {
    }

    public DeletedObject( IdentifiableObject identifiableObject )
    {
        Assert.notNull( identifiableObject, "IdentifiableObject is required and can not be null." );
        Assert.notNull( identifiableObject.getUid(), "IdentifiableObject.uid is required and can not be null." );

        this.deletedObjectId = new DeletedObjectId(
            ClassUtils.getShortName( identifiableObject.getClass() ), identifiableObject.getUid() );
        this.code = identifiableObject.getCode();
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public DeletedObjectId getDeletedObjectId()
    {
        return deletedObjectId;
    }

    public void setDeletedObjectId( DeletedObjectId deletedObjectId )
    {
        this.deletedObjectId = deletedObjectId;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Date getDeletedAt()
    {
        return deletedAt;
    }

    public void setDeletedAt( Date deletedAt )
    {
        this.deletedAt = deletedAt;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass() != o.getClass() ) return false;

        DeletedObject that = (DeletedObject) o;

        return Objects.equal( deletedObjectId, that.deletedObjectId ) &&
            Objects.equal( code, that.code ) &&
            Objects.equal( deletedAt, that.deletedAt );
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode( deletedObjectId, code, deletedAt );
    }

    @Override
    public String toString()
    {
        return MoreObjects.toStringHelper( this )
            .add( "deletedObjectId", deletedObjectId )
            .add( "code", code )
            .add( "deletedAt", deletedAt )
            .toString();
    }
}
