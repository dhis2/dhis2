package org.hisp.dhis.scheduling;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.concurrent.ListenableFuture;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

import static org.hisp.dhis.scheduling.JobStatus.DISABLED;

/**
 * Cron refers to the cron expression used for scheduling. Key refers to the key
 * identifying the scheduled jobs.
 *
 * @author Henning Håkonsen
 */
public class DefaultSchedulingManager
    implements SchedulingManager
{
    private static final Log log = LogFactory.getLog( DefaultSchedulingManager.class );

    public static final String CONTINOUS_CRON = "* * * * * ?";
    public static final String HOUR_CRON = "0 0 * ? * *";

    private Map<JobType, Job> jobMap = new HashMap<>();

    private Map<String, ScheduledFuture<?>> futures = new HashMap<>();

    private Map<String, ListenableFuture<?>> currentTasks = new HashMap<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private JobConfigurationService jobConfigurationService;

    @Autowired
    private MessageService messageService;

    private TaskScheduler jobScheduler;

    public void setTaskScheduler( TaskScheduler JobScheduler )
    {
        this.jobScheduler = JobScheduler;
    }

    private AsyncListenableTaskExecutor jobExecutor;

    public void setTaskExecutor( AsyncListenableTaskExecutor jobExecutor )
    {
        this.jobExecutor = jobExecutor;
    }

    @Autowired
    private List<Job> jobs;

    @PostConstruct
    public void init()
    {
        jobs.forEach( job -> {
            if ( job == null )
            {
                log.fatal( "Scheduling manager tried to add job, but it was null" );
            }
            else
            {
                jobMap.put( job.getJobType(), job );
            }
        } );
    }

    // -------------------------------------------------------------------------
    // Queue
    // -------------------------------------------------------------------------

    private List<JobConfiguration> runningJobConfigurations = new ArrayList<>();

    public boolean isJobConfigurationRunning( JobConfiguration jobConfiguration )
    {
        return !jobConfiguration.isContinuousExecution() && runningJobConfigurations.stream().anyMatch(
            jobConfig -> jobConfig.getJobType().equals( jobConfiguration.getJobType() ) &&
                !jobConfig.isContinuousExecution() );
    }

    public void jobConfigurationStarted( JobConfiguration jobConfiguration )
    {
        runningJobConfigurations.add( jobConfiguration );
        jobConfigurationService.updateJobConfiguration( jobConfiguration );
    }

    public void jobConfigurationFinished( JobConfiguration jobConfiguration )
    {
        runningJobConfigurations.remove( jobConfiguration );

        JobConfiguration tempJobConfiguration = jobConfigurationService
            .getJobConfigurationByUid( jobConfiguration.getUid() );

        if ( tempJobConfiguration != null )
        {
            if ( tempJobConfiguration.getJobStatus() == DISABLED )
            {
                jobConfiguration.setJobStatus( DISABLED );
                jobConfiguration.setEnabled( false );
            }

            jobConfigurationService.updateJobConfiguration( jobConfiguration );
        }
    }

    // -------------------------------------------------------------------------
    // SchedulingManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void scheduleJob( JobConfiguration jobConfiguration )
    {
        if ( ifJobInSystemStop( jobConfiguration.getUid() ) )
        {
            JobInstance jobInstance = new DefaultJobInstance();

            if ( jobConfiguration.getUid() != null && !futures.containsKey( jobConfiguration.getUid() ) )
            {
                ScheduledFuture<?> future = jobScheduler
                    .schedule( () -> {
                        try
                        {
                            jobInstance.execute( jobConfiguration, this, messageService );
                        }
                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }, new CronTrigger( jobConfiguration.getCronExpression() ) );

                futures.put( jobConfiguration.getUid(), future );

                log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and cron: " +
                    jobConfiguration.getCronExpression() );
            }
        }
    }

    @Override
    public void scheduleJob( Date date, JobConfiguration jobConfiguration )
    {
        if ( ifJobInSystemStop( jobConfiguration.getUid() ) )
        {
            JobInstance jobInstance = new DefaultJobInstance();

            if ( jobConfiguration.getUid() != null && !futures.containsKey( jobConfiguration.getUid() ) && date != null &&
                date.getTime() > new Date().getTime() )
            {
                ScheduledFuture<?> future = jobScheduler
                    .schedule( () -> {
                        try
                        {
                            jobInstance.execute( jobConfiguration, this, messageService );
                        }
                        catch ( Exception e )
                        {
                            e.printStackTrace();
                        }
                    }, date );

                futures.put( jobConfiguration.getUid(), future );

                log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and execution time: " + date );

            }
        }
    }

    @Override
    public void scheduleJobs( List<JobConfiguration> jobConfigurations )
    {
        jobConfigurations.forEach( this::scheduleJob );
    }

    @Override
    public void scheduleJobWithFixedDelay( JobConfiguration jobConfiguration, Date delay, int interval )
    {
        if ( ifJobInSystemStop( jobConfiguration.getUid() ) )
        {
            JobInstance jobInstance = new DefaultJobInstance();

            ScheduledFuture<?> future = jobScheduler.scheduleWithFixedDelay( () -> {
                try
                {
                    jobInstance.execute( jobConfiguration, this, messageService );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }, delay,interval );

            futures.put( jobConfiguration.getUid(), future );

            log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and first execution time: " + delay );
        }
    }

    @Override
    public void scheduleJobAtFixedRate( JobConfiguration jobConfiguration, int interval )
    {
        if ( ifJobInSystemStop( jobConfiguration.getUid() ) )
        {
            JobInstance jobInstance = new DefaultJobInstance();

            ScheduledFuture<?> future = jobScheduler.scheduleAtFixedRate( () -> {
                try
                {
                    jobInstance.execute( jobConfiguration, this, messageService );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }, interval );

            futures.put( jobConfiguration.getUid(), future );

            log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and fixed rate: " + interval );
        }
    }

    @Override
    public void stopJob( JobConfiguration jobConfiguration )
    {
        if ( isJobInSystem( jobConfiguration.getUid() ) )
        {
            jobConfiguration.setLastExecutedStatus( JobStatus.STOPPED );
            jobConfigurationService.updateJobConfiguration( jobConfiguration );

            internalStopJob( jobConfiguration.getUid() );
        }
    }

    @Override
    public void stopJob( String jobKey )
    {
        JobConfiguration jobConfiguration = jobConfigurationService.getJobConfigurationByUid( jobKey );
        stopJob( jobConfiguration );
    }

    @Override
    public void stopAllJobs()
    {
        Iterator<String> keys = futures.keySet().iterator();

        while ( keys.hasNext() )
        {
            String key = keys.next();

            ScheduledFuture<?> future = futures.get( key );

            boolean result = future != null && future.cancel( true );

            keys.remove();

            log.info( "Stopped job with key: " + key + " successfully: " + result );
        }
    }

    @Override
    public void executeJob( JobConfiguration jobConfiguration )
    {
        if ( jobConfiguration != null && !isJobInProgress( jobConfiguration.getUid() ) )
        {
            internalExecuteJobConfiguration( jobConfiguration );
        }
    }

    @Override
    public void executeJob( Runnable job )
    {
        jobExecutor.execute( job );
    }

    @Override
    public String getCronForJob( String jobKey )
    {
        return null;
    }

    @Override
    public Map<String, ScheduledFuture<?>> getAllFutureJobs()
    {
        return futures;
    }

    @Override
    public JobStatus getJobStatus( String jobKey )
    {
        ScheduledFuture<?> future = futures.get( jobKey );

        return getStatus( future );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    @Override
    public boolean isJobInProgress( String jobKey )
    {
        return JobStatus.RUNNING == getCurrentJobStatus( jobKey );
    }

    public Job getJob( JobType jobType )
    {
        return jobMap.get( jobType );
    }

    // -------------------------------------------------------------------------
    // Spring execution/scheduling
    // -------------------------------------------------------------------------

    @Override
    public <T> ListenableFuture<T> executeJob( Callable<T> callable )
    {
        return jobExecutor.submitListenable( callable );
    }

    private void internalExecuteJobConfiguration( JobConfiguration jobConfiguration )
    {
        JobInstance jobInstance = new DefaultJobInstance();

        ListenableFuture<?> future = jobExecutor.submitListenable( () -> {
            try
            {
                jobInstance.execute( jobConfiguration, this, messageService );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        } );
        currentTasks.put( jobConfiguration.getUid(), future );
    }

    private boolean internalStopJob( String uid )
    {
        if ( uid != null )
        {
            ScheduledFuture<?> future = futures.get( uid );

            if ( future == null )
            {
                log.info( "Tried to stop job with key '" + uid + "', but was not found" );
                return true;
            }
            else
            {
                boolean result = future.cancel( true );

                futures.remove( uid );

                log.info( "Stopped job with key: " + uid + " successfully: " + result );

                return result;
            }
        }

        return false;
    }

    private JobStatus getStatus( Future<?> future )
    {
        if ( future == null )
        {
            return JobStatus.SCHEDULED;
        }
        else if ( future.isCancelled() )
        {
            return JobStatus.STOPPED;
        }
        else if ( future.isDone() )
        {
            return JobStatus.COMPLETED;
        }
        else
        {
            return JobStatus.RUNNING;
        }
    }

   private boolean ifJobInSystemStop( String jobKey )
    {
        return !isJobInSystem( jobKey ) || internalStopJob( jobKey );
    }

    private boolean isJobInSystem( String jobKey )
    {
        return futures.get( jobKey ) != null || currentTasks.get( jobKey ) != null;
    }

    private JobStatus getCurrentJobStatus( String jobKey )
    {
        ListenableFuture<?> future = currentTasks.get( jobKey );

        return getStatus( future );
    }
}
