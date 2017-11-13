package org.hisp.dhis.predictor;

import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.scheduling.parameters.PredictorJobParameters;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author Henning Håkonsen
 */
public class PredictorJob
    implements Job
{
    @Autowired
    private PredictorService predictorService;

    @Override
    public JobType getJobType()
    {
        return JobType.PREDICTOR;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
        throws Exception
    {
        PredictorJobParameters predictorJobParameters = ( PredictorJobParameters ) jobConfiguration.getJobParameters();

        if ( predictorJobParameters == null )
        {
            throw new Exception( "No job parameters present in predictor job" );
        }

        List<String> predictors = predictorJobParameters.getPredictors();
        Date startDate = predictorJobParameters.getStartDate();
        Date endDate = predictorJobParameters.getEndDate();

        predictorService.predictPredictors( predictors, startDate, endDate );
    }
}
