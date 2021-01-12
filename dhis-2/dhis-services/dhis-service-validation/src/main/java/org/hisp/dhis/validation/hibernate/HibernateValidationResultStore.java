package org.hisp.dhis.validation.hibernate;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultStore;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Stian Sandvold
 */
@Slf4j
@Repository( "org.hisp.dhis.validation.ValidationResultStore" )
public class HibernateValidationResultStore
    extends HibernateGenericStore<ValidationResult>
    implements ValidationResultStore
{
    protected CurrentUserService currentUserService;

    public HibernateValidationResultStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService )
    {
        super( sessionFactory, jdbcTemplate, publisher, ValidationResult.class, true );
        checkNotNull( currentUserService );
        this.currentUserService = currentUserService;
    }

    /**
     * Allows injection (e.g. by a unit test)
     */
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    @Override
    public List<ValidationResult> getAllUnreportedValidationResults()
    {
        return getQuery( "from ValidationResult vr where vr.notificationSent = false"
            + getUserRestrictions( "and") ).list();
    }

    @Override
    public ValidationResult getById( long id )
    {
        return getSingleResult( getQuery( "from ValidationResult vr where vr.id = :id"
            + getUserRestrictions( "and") ).setParameter( "id", id ) );
    }

    @Override
    public List<ValidationResult> query( ValidationResultQuery query )
    {
        Query<ValidationResult> hibernateQuery = getQuery(
            "from ValidationResult vr" + getQueryRestrictions( query ) );
        addQueryParameters( query, hibernateQuery );

        if ( !query.isSkipPaging() )
        {
            Pager pager = query.getPager();
            hibernateQuery.setFirstResult( pager.getOffset() );
            hibernateQuery.setMaxResults( pager.getPageSize() );
        }

        return hibernateQuery.getResultList();
    }

    @Override
    public int count( ValidationResultQuery query )
    {
        Query<Long> hibernateQuery = getTypedQuery(
            "select count(*) from ValidationResult vr" + getQueryRestrictions( query ) );
        addQueryParameters( query, hibernateQuery );

        return hibernateQuery.getSingleResult().intValue();
    }

    @Override
    public List<ValidationResult> getValidationResults( OrganisationUnit orgUnit,
        boolean includeOrgUnitDescendants, Collection<ValidationRule> validationRules, Collection<Period> periods )
    {
        if ( isEmpty( validationRules ) || isEmpty( periods ) )
        {
            return new ArrayList<>();
        }

        String orgUnitFilter = orgUnit == null ? "" : "vr.organisationUnit.path like :orgUnitPath and ";

        Query<ValidationResult> query = getQuery( "from ValidationResult vr where " + orgUnitFilter + "vr.validationRule in :validationRules and vr.period in :periods " );

        if ( orgUnit != null )
        {
            query.setParameter( "orgUnitPath", orgUnit.getPath()
                + ( includeOrgUnitDescendants ? "%" : "" ) );
        }

        query.setParameter( "validationRules", validationRules );
        query.setParameter( "periods", periods );

        return query.list();
    }

    @Override
    public void save( ValidationResult validationResult )
    {
        validationResult.setCreated( new Date() );
        super.save( validationResult );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void addQueryParameters(ValidationResultQuery query, Query<?> hibernateQuery) {
        if ( !isEmpty( query.getOu() ) )
        {
            hibernateQuery.setParameter( "orgUnitsUids", query.getOu() );
        }
        if ( !isEmpty( query.getVr() ) )
        {
            hibernateQuery.setParameter( "validationRulesUids", query.getVr() );
        }
        if ( !isEmpty( query.getPe() ) )
        {
            int i = 1;
            for ( String period : query.getPe() )
            {
                String parameterName = "periodId" + (i++);
                Period p = PeriodType.getPeriodFromIsoString( period );
                if ( p == null )
                { // assume ID
                    hibernateQuery.setParameter( parameterName, Long.parseLong( period ) );
                }
                else
                { // assume ISO period: match overlap with ISO period
                    hibernateQuery.setParameter( parameterName + "Start", p.getStartDate() );
                    hibernateQuery.setParameter( parameterName + "End", p.getEndDate() );
                }
            }
        }
    }

    private String getQueryRestrictions( ValidationResultQuery query ) {
        StringBuilder restrictions = new StringBuilder();
        restrictions.append(getUserRestrictions("where" ));
        String whereAnd = restrictions.length() == 0 ? "where" : "and";
        if ( !isEmpty( query.getOu() ) )
        {
            restrictions.append(" " + whereAnd + " vr.organisationUnit.uid in :orgUnitsUids ");
            whereAnd = "and";
        }
        if ( !isEmpty( query.getVr() )) {
            restrictions.append(" " + whereAnd + " vr.validationRule.uid in :validationRulesUids ");
            whereAnd = "and";
        }
        if ( !isEmpty( query.getPe() ) )
        {
            restrictions.append( " " + whereAnd + "(" );
            int i = 1;
            for ( String period : query.getPe() )
            {
                if ( i > 1 )
                    restrictions.append( " or " );
                String parameterName = ":periodId" + (i++);
                Period p = PeriodType.getPeriodFromIsoString( period );
                if ( p == null )
                { // assume ID
                    restrictions.append( " vr.period.id = " + parameterName );
                }
                else
                { // assume ISO period: match overlap with ISO period
                    restrictions.append( " ((vr.period.startDate <= " + parameterName
                        + "End ) and (vr.period.endDate >= " + parameterName + "Start ))" );
                }
            }
            restrictions.append( ")" );
        }
        return restrictions.toString();
    }

    /**
     * If we should, restrict which validation results the user is entitled
     * to see, based on the user's organisation units and on the user's
     * dimension constraints if the user has them.
     * <p>
     * If the current user is null (e.g. running a system process or
     * a JUnit test) or superuser, there is no restriction.
     *
     * @param whereAnd "where" or "and", to add restrictions to where clause.
     * @return String to add restrictions to the HQL query.
     */
    private String getUserRestrictions( String whereAnd )
    {
        String restrictions = "";

        final User user = currentUserService.getCurrentUser();

        if ( user == null || currentUserService.currentUserIsSuper() )
        {
            return restrictions;
        }

        // ---------------------------------------------------------------------
        // Restrict by the user's organisation unit sub-trees, if any
        // ---------------------------------------------------------------------

        Set<OrganisationUnit> userOrgUnits = user.getDataViewOrganisationUnitsWithFallback();

        if ( !userOrgUnits.isEmpty() )
        {
            for ( OrganisationUnit ou : userOrgUnits )
            {
                restrictions += ( restrictions.length() == 0 ? " " + whereAnd + " (" : " or " )
                    + "locate('" + ou.getUid() + "',vr.organisationUnit.path) <> 0";
            }
            restrictions += ")";
            whereAnd = "and";
        }

        // ---------------------------------------------------------------------
        // Restrict by the user's category dimension constraints, if any
        // ---------------------------------------------------------------------

        Set<Category> categories = user.getUserCredentials().getCatDimensionConstraints();

        if ( !isEmpty( categories ) )
        {
            String validCategoryOptionByCategory =
                isReadable( "co", user ) +
                " and exists (select 'x' from Category c where co in elements(c.categoryOptions)" +
                " and c.id in (" + StringUtils.join( IdentifiableObjectUtils.getIdentifiers( categories ), "," ) + ") )";

            restrictions += " " + whereAnd + " 1 = (select min(case when " +  validCategoryOptionByCategory + " then 1 else 0 end)" +
                " from CategoryOption co" +
                " where co in elements(vr.attributeOptionCombo.categoryOptions) )";

            whereAnd = "and";
        }

        // ---------------------------------------------------------------------
        // Restrict by the user's cat option group dimension constraints, if any
        // ---------------------------------------------------------------------

        Set<CategoryOptionGroupSet> cogsets = user.getUserCredentials().getCogsDimensionConstraints();

        if ( !isEmpty( cogsets ) )
        {
            String validCategoryOptionByCategoryOptionGroup =
                "exists (select 'x' from CategoryOptionGroup g" +
                    " join g.groupSets s" +
                    " where g.id in elements(co.groups)" +
                    " and s.id in (" + StringUtils.join( IdentifiableObjectUtils.getIdentifiers( cogsets ), "," ) + ")" +
                    " and " + isReadable( "g", user ) + " )";

            restrictions += " " + whereAnd +
                " 1 = (select min(case when " +  validCategoryOptionByCategoryOptionGroup + " then 1 else 0 end)" +
                " from CategoryOption co" +
                " where co in elements(vr.attributeOptionCombo.categoryOptions) )";
        }

        log.debug( "Restrictions = " + restrictions );

        return restrictions;
    }

    /**
     * Returns a HQL string that determines whether an object is readable
     * by a user.
     *
     * @param x the object to test for readability.
     * @param u the user who might be able to read the object.
     * @return HQL that evaluates to true or false depending on readability.
     */
    private String isReadable( String x, User u )
    {
        String groupUids = null;

        if ( !u.getGroups().isEmpty() )
        {
            Set<String> groups = u.getGroups().stream().map(BaseIdentifiableObject::getUid).collect( Collectors.toSet() );
            groupUids = "{" + String.join( ",", groups ) + "}";
        }

        StringBuilder builder = new StringBuilder();
        builder.append( "( function('jsonb_extract_path_text'," + x + ".sharing, 'public') is null" +
            " or function('jsonb_extract_path_text'," + x + ".sharing, 'public') like 'r%'" +
            " or   function('jsonb_extract_path_text'," + x + ".sharing, 'owner')= '" + u.getUid() +"'" );

        if ( groupUids != null )
        {
            builder.append( " or function('jsonb_has_user_group_ids'," + x + ".sharing, '" + groupUids + "') = true and function('jsonb_check_user_groups_access'," + x + ".sharing, 'r%', '" + groupUids + "') = true" );
        }

        builder.append( " )  " );

        return builder.toString();
    }
}
