/*
 * Copyright 2014 Timothy Hruska <https://github.com/tjhruska>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tjhruska.spring.jesque;

import java.util.concurrent.Callable;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.worker.JobFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * BeanJobFactory converts a Job object back to the runnable job, if the job was
 * enqueued as a BeanJob it will load the bean from the application context. If
 * it was not enqueued as a BeanJob then it will delegate it to the
 * fallbackJobFactory. If the job is a bean job that implements RunnableWithInit
 * it will initialize the beans with the arguments used when the job was
 * enqueued.
 *
 * @author Timothy Hruska <https://github.com/tjhruska>
 *
 */
public class BeanJobFactory implements JobFactory, ApplicationContextAware {

  ApplicationContext context;
  JobFactory fallbackJobFactory;
  boolean addLoggingProxy;

  /**
   * Construct a BeanJobFactory that can only materialize BeanJobs. Bean jobs
   * must implement either Runnable, and RunnableWithInit.
   */
  public BeanJobFactory() {
    super();
    this.addLoggingProxy = false;
  }

  /**
   * Construct a BeanJobFactory that can materialize BeanJobs, or fallback to
   * the fallbackJobFactory for other jobs. Bean jobs must implement either
   * Runnable, and RunnableWithInit.
   *
   * @param fallbackJobFactory
   *          used to materialize jobs that were not enqueued as BeanJobs.
   */
  public BeanJobFactory(JobFactory fallbackJobFactory) {
    super();
    this.fallbackJobFactory = fallbackJobFactory;
    this.addLoggingProxy = false;
  }

  /**
   * Construct a BeanJobFactory that can materialize BeanJobs, or fallback to
   * the fallbackJobFactory for other jobs. Bean jobs must implement either
   * Runnable, and RunnableWithInit.
   *
   * @param fallbackJobFactory
   *          used to materialize jobs that were not enqueued as BeanJobs.
   * @param addLoggingProxy
   *          used to cause returned jobs to be a logging proxy that will log
   *          before and after job run
   */
  public BeanJobFactory(JobFactory fallbackJobFactory, boolean addLoggingProxy) {
    super();
    this.fallbackJobFactory = fallbackJobFactory;
    this.addLoggingProxy = addLoggingProxy;
  }

  public void setFallbackJobFactory(JobFactory fallbackJobFactory) {
    this.fallbackJobFactory = fallbackJobFactory;
  }

  public void setAddLoggingProxy(boolean addLoggingProxy) {
    this.addLoggingProxy = addLoggingProxy;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public Object materializeJob(Job job) throws Exception {
    Object returnJob = null;
    if (!job.getClassName().equals("com.tjhruska.spring.jesque.BeanJob") && fallbackJobFactory != null) {
      returnJob = fallbackJobFactory.materializeJob(job);
    } else if (!job.getClassName().equals("com.tjhruska.spring.jesque.BeanJob")) {
      throw new RuntimeException(
          "BeanJobFactory only knows how to materialize BeanJob bean jobs.  If you also want to load non bean based jobs then inject a fallbackJobFactory to handle those.");
    } else {
      if (job.getArgs().length < 1) {
        throw new RuntimeException(
            "BeanJobFactory expects at least 1 argument with the first being the bean name, args were empty.");
      }

      String jobName = (String) job.getArgs()[0];
      returnJob = context.getBean(jobName);

      Object[] remaining = new Object[job.getArgs().length - 1];
      for (int i = 0; i < job.getArgs().length - 1; i++) {
        remaining[i] = job.getArgs()[i + 1];
      }

      if (returnJob instanceof RunnableWithInit) {
        ((RunnableWithInit) returnJob).init(remaining);
      } else if (remaining.length > 0) {
        throw new RuntimeException(
            "Variable arguments passed into BeanJob required bean implement RunnableWithInit interface, bean "
                + jobName + " doesn't.");
      }
    }

    if (addLoggingProxy && returnJob instanceof Runnable) {
      returnJob = new LoggingProxyRunnable((Runnable) returnJob);
    }

    if (addLoggingProxy && returnJob instanceof Callable) {
      returnJob = new LoggingProxyCallable((Callable) returnJob);
    }

    return returnJob;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.context = applicationContext;
  }
}