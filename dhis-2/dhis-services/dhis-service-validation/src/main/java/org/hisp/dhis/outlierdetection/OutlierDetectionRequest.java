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

package org.hisp.dhis.outlierdetection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import com.google.common.base.Preconditions;

import lombok.Getter;

/**
 * Encapsulation of an outlier detection request.
 *
 * @author Lars Helge Overland
 */
@Getter
public class OutlierDetectionRequest
{
    private List<DataElement> dataElements = new ArrayList<>();

    private Date startDate;

    private Date endDate;

    private List<OrganisationUnit> orgUnits = new ArrayList<>();

    private OrgUnitSelection orgUnitSelection;

    private OutlierDetectionAlgorithm outlierAlgorithm = OutlierDetectionAlgorithm.Z_SCORE;

    private double threshold;

    private Order orderBy;

    private int maxResults;

    public List<Long> getDataElementIds()
    {
        return dataElements.stream()
            .map( DataElement::getId )
            .collect( Collectors.toList() );
    }

    private OutlierDetectionRequest()
    {
    }

    public static class Builder
    {
        private OutlierDetectionRequest query;

        /**
         * Initializes the {@link OutlierDetectionRequest} with default values.
         */
        public Builder()
        {
            this.query = new OutlierDetectionRequest();

            this.query.orgUnitSelection = OrgUnitSelection.DESCENDANTS;
            this.query.outlierAlgorithm = OutlierDetectionAlgorithm.Z_SCORE;
            this.query.threshold = 3.0d;
            this.query.orderBy = Order.MEAN_ABS_DEV;
            this.query.maxResults = 1000;
        }

        public Builder withDataElements( List<DataElement> dataElements )
        {
            this.query.dataElements = dataElements;
            return this;
        }

        public Builder withStartEndDate( Date startDate, Date endDate )
        {
            this.query.startDate = startDate;
            this.query.endDate = endDate;
            return this;
        }

        public Builder withOrgUnits( List<OrganisationUnit> orgUnits )
        {
            this.query.orgUnits = orgUnits;
            return this;
        }

        public Builder withThreshold( double threshold )
        {
            this.query.threshold = threshold;
            return this;
        }

        public Builder withOrderBy( Order orderBy )
        {
            this.query.orderBy = orderBy;
            return this;
        }

        public Builder withMaxResults( int maxResults )
        {
            this.query.maxResults = maxResults;
            return this;
        }

        public OutlierDetectionRequest build()
        {
            Preconditions.checkNotNull( this.query.orgUnitSelection );
            Preconditions.checkNotNull( this.query.outlierAlgorithm );
            Preconditions.checkNotNull( this.query.orderBy );
            return this.query;
        }
    }
}

