package com.tjhruska.spring.jesque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingProxyRunnable implements Runnable {
  public static final Logger log = LoggerFactory.getLogger(LoggingProxyRunnable.class);

  private Runnable delegate;

  public LoggingProxyRunnable(Runnable delegate) {
    this.delegate = delegate;
  }

  public Runnable getDelegate() {
    return delegate;
  }

  @Override
  public void run() {
    long timeMillis = 0L;
    try {
      log.info("Beginning call to delegate:  " + delegate.getClass().getName());
      timeMillis = System.currentTimeMillis();
      delegate.run();
      timeMillis = System.currentTimeMillis() - timeMillis;
      log.info("Finished call to delegate:  " + delegate.getClass().getName() + " in " + timeMillis + " millis");
    } catch (Exception e) {
      timeMillis = System.currentTimeMillis() - timeMillis;
      log.info(
          "Callable Job " + delegate.getClass().getName() + " failed in " + timeMillis + " millis with exception:", e);
      throw e;
    }
  }

}
