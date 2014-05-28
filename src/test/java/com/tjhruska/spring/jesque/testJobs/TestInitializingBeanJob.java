package com.tjhruska.spring.jesque.testJobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tjhruska.spring.jesque.RunnableWithInit;

public class TestInitializingBeanJob implements RunnableWithInit {
  private static final Logger log = LoggerFactory.getLogger(TestInitializingBeanJob.class);

  public static Integer staticRunCount = 0;
  
  public Integer runCount = 0;
  public String arg1;
  public String arg2;

  public void run() {
    log.info("TestInitializingBeanJob run called with arg1:  {}, arg2:  {}.", arg1, arg2);
    synchronized (runCount) {
      runCount++;
    }
    synchronized (staticRunCount) {
      staticRunCount++;
    }
  }

  @Override
  public void init(Object... args) {
    if (args.length < 2) {
      throw new RuntimeException ("Expected at least 2 args");
    }
    arg1 = (String)args[0];
    arg2 = (String)args[1];
  }

  public String getArg1() {
    return arg1;
  }

  public String getArg2() {
    return arg2;
  }
}
