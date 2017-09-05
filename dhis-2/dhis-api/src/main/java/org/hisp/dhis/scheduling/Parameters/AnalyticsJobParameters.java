package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.TaskId;

import java.util.Set;

/**
 * @author Henning Håkonsen
 */
public class AnalyticsJobParameters
    implements JobParameters
{
    private TaskId taskId;
    private Integer lastYears;
    private Set<String> skipTableTypes;
    private boolean skipResourceTables;

    public AnalyticsJobParameters()
    {}

    public AnalyticsJobParameters( Integer lastYears, TaskId taskId, Set<String> skipTableTypes, boolean skipResourceTables )
    {
        this.lastYears = lastYears;
        this.taskId = taskId;
        this.skipTableTypes = skipTableTypes;
        this.skipResourceTables = skipResourceTables;
    }

    public TaskId getTaskId()
    {
        return taskId;
    }

    public Integer getLastYears()
    {
        return lastYears;
    }

    public Set<String> getSkipTableTypes()
    {
        return skipTableTypes;
    }

    public boolean isSkipResourceTables()
    {
        return skipResourceTables;
    }
}
