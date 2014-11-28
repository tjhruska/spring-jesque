package com.tjhruska.spring.jesque.testJobs;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBeanJobCallable implements Callable<Object> {
  private static final Logger log = LoggerFactory.getLogger(TestBeanJobCallable.class);

  public static Integer staticRunCount = 0;
  public Integer runCount = 0;

  @Override
  public Object call() {
    log.info("TestBeanJobCallable run called.");
    synchronized (runCount) {
      runCount++;
    }
    synchronized (staticRunCount) {
      staticRunCount++;
    }
    return new Object();
  }
}