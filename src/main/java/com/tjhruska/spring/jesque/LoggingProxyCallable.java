package com.tjhruska.spring.jesque;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingProxyCallable implements Callable<Object> {
  public static final Logger log = LoggerFactory.getLogger(LoggingProxyCallable.class);

  private Callable<Object> delegate;

  public LoggingProxyCallable(Callable<Object> delegate) {
    this.delegate = delegate;
  }

  public Callable<Object> getDelegate() {
    return delegate;
  }

  @Override
  public Object call() throws Exception {
    long timeMillis = 0L;
    try {
      log.info("Beginning call to delegate:  " + delegate.getClass().getName());
      timeMillis = System.currentTimeMillis();
      Object returnObject = delegate.call();
      timeMillis = System.currentTimeMillis() - timeMillis;
      log.info("Finished call to delegate:  " + delegate.getClass().getName() + " in " + timeMillis + " millis");
      return returnObject;
    } catch (Exception e) {
      timeMillis = System.currentTimeMillis() - timeMillis;
      log.info(
          "Callable Job " + delegate.getClass().getName() + " failed in " + timeMillis + " millis with exception:", e);
      throw e;
    }
  }
}
