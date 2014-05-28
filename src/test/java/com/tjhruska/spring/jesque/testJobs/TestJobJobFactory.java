package com.tjhruska.spring.jesque.testJobs;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.JobFactory;

public class TestJobJobFactory implements JobFactory {

  @Override
  public Object materializeJob(Job job) throws Exception {
    return new TestJob();
  }

}
