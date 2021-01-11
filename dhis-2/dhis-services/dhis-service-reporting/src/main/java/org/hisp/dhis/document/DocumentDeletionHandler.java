package org.hisp.dhis.document;

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

import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.system.deletion.DeletionHandler;
import org.hisp.dhis.user.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
@Component( "org.hisp.dhis.document.DocumentDeletionHandler" )
public class DocumentDeletionHandler extends DeletionHandler
{
    private DocumentService documentService;

    private JdbcTemplate jdbcTemplate;

    public DocumentDeletionHandler( DocumentService documentService, JdbcTemplate jdbcTemplate )
    {
        checkNotNull( documentService );
        checkNotNull( jdbcTemplate );

        this.documentService = documentService;
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // DeletionHandler implementation
    // -------------------------------------------------------------------------

    @Override
    public String getClassName()
    {
        return Document.class.getSimpleName();
    }

    public String allowDeleteUser( User user )
    {
        return documentService.getCountDocumentByUser( user ) > 0 ? ERROR : null;
    }

    @Override
    public String allowDeleteFileResource( FileResource fileResource )
    {
        String sql = "SELECT COUNT(*) FROM document WHERE fileresource=" + fileResource.getId();

        int result = jdbcTemplate.queryForObject( sql, Integer.class );

        return result == 0 || fileResource.getStorageStatus() != FileResourceStorageStatus.STORED ? null : ERROR;
    }

    @Override
    public void deleteFileResource( FileResource fileResource )
    {
        String sql = "DELETE FROM document WHERE fileresource=" + fileResource.getId();

        jdbcTemplate.execute( sql );
    }
}