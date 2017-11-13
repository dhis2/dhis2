package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author Henning Håkonsen
 */
public class PushAnalysisJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = -1848833906375595488L;

    private int pushAnalysisId;

    public PushAnalysisJobParameters()
    {
    }

    public PushAnalysisJobParameters( int pushAnalysisId )
    {
        this.pushAnalysisId = pushAnalysisId;
    }

    public int getPushAnalysisId()
    {
        return pushAnalysisId;
    }
}
