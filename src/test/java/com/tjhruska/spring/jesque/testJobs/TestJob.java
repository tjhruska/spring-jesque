package com.tjhruska.spring.jesque.testJobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJob implements Runnable{
  private static final Logger log = LoggerFactory.getLogger(TestJob.class);
  private boolean ran = false;

  @Override
  public void run() {
    log.info("Test job run method called.");
    ran = true;
  }
  
  public boolean hasRun() {
    return ran;
  }
} 