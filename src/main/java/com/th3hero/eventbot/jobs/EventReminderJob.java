package com.th3hero.eventbot.jobs;


import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

public class EventReminderJob implements Job {
    public static final JobKey JOB_KEY = JobKey.jobKey("event_reminder");

    @Override
    public void execute(JobExecutionContext executionContext) {

    }
}
