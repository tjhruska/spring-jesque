package com.tjhruska.spring.jesque.testJobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBeanJob implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(TestBeanJob.class);
  
  public static Integer staticRunCount = 0;
  public Integer runCount = 0;

  public void run() {
    log.info("TestBeanJob run called.");
    synchronized (runCount) {
      runCount++;
    }
    synchronized (staticRunCount) {
      staticRunCount++;
    }
  }
}
