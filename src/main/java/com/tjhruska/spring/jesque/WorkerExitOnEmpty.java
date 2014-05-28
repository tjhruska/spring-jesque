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

import static net.greghaines.jesque.worker.JobExecutor.State.RUNNING;
import static net.greghaines.jesque.worker.WorkerEvent.WORKER_POLL;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import net.greghaines.jesque.Config;
import net.greghaines.jesque.Job;
import net.greghaines.jesque.json.ObjectMapperFactory;
import net.greghaines.jesque.utils.JesqueUtils;
import net.greghaines.jesque.worker.JobFactory;
import net.greghaines.jesque.worker.WorkerImpl;

/**
 * This worker will only run as long as there are jobs to run.  
 * If there are no jobs in any queue 'maxLoopsOnEmptyQueue' times, then the worker will exit. 
 * Pair this worker with a worker factory that will create new workers assigned to queues containing jobs.
 * This model can work well for jobs that need to work with some limited resource.  Where the worker factory
 * understands that limited resource, and can allocate workers to match design constraints.
 * 
 * @author Timothy Hruska <https://github.com/tjhruska>
 *
 */
public class WorkerExitOnEmpty extends WorkerImpl {
  private int maxLoopsOnEmptyQueues;

  /**
   * Basic worker that will exit if all queues are empty after 3 polling attempts.
   * @param config jesque configuration for how to connect to redis queues
   * @param queues source of jobs to process
   * @param jobFactory factory that takes raw jobs from redis queue, and converts them to executable jobs
   */
  public WorkerExitOnEmpty(final Config config, final Collection<String> queues, final JobFactory jobFactory) {
    this(config, queues, jobFactory, 3);
  }

  /**
   * Basic worker that will exit if all queues are empty after maxLoopsOnEmptyQueues polling attempts.
   * @param config jesque configuration for how to connect to redis queues
   * @param queues source of jobs to process
   * @param jobFactory factory that takes raw jobs from redis queue, and converts them to executable jobs
   * @param maxLoopsOnEmptyQueues number of loops to poll empty queues before exiting
   */
  public WorkerExitOnEmpty(final Config config, final Collection<String> queues, final JobFactory jobFactory,
      int maxLoopsOnEmptyQueues) {
    super(config, queues, jobFactory);
    this.maxLoopsOnEmptyQueues = maxLoopsOnEmptyQueues;
  }

  /**
   * Worker will be set to exit if all queues are empty maxLoopOnEmptyQueues times
   * @see net.greghaines.jesque.worker.WorkerImpl#poll()
   */
  @Override
  protected void poll() {
    int missCount = 0;
    String curQueue = null;
    int allQueuesEmptyCount = 0;

    while (RUNNING.equals(this.state.get())) {
      try {
        if (isThreadNameChangingEnabled()) {
          renameThread("Waiting for " + JesqueUtils.join(",", this.queueNames));
        }
        curQueue = this.queueNames.poll(EMPTY_QUEUE_SLEEP_TIME,
            TimeUnit.MILLISECONDS);
        if (curQueue != null) {
          this.queueNames.add(curQueue); // Rotate the queues
          checkPaused();
          // Might have been waiting in poll()/checkPaused() for a while
          if (RUNNING.equals(this.state.get())) {
            this.listenerDelegate.fireEvent(WORKER_POLL, this, curQueue, null,
                null, null, null);
            final String payload = pop(curQueue);
            if (payload != null) {
              final Job job = ObjectMapperFactory.get().readValue(payload,
                  Job.class);
              process(job, curQueue);
              missCount = 0;
              allQueuesEmptyCount = 0;
            } else if (++missCount >= this.queueNames.size()
                && RUNNING.equals(this.state.get())) {
              // Keeps worker from busy-spinning on empty queues
              missCount = 0;
              Thread.sleep(EMPTY_QUEUE_SLEEP_TIME);

              allQueuesEmptyCount++;
              if (allQueuesEmptyCount >= maxLoopsOnEmptyQueues) {
                end(false); // sets state to SHUTDOWN which will break the loop
              }
            }
          }
        }
      } catch (InterruptedException ie) {
        if (!isShutdown()) {
          recoverFromException(curQueue, ie);
        }
      } catch (Exception e) {
        recoverFromException(curQueue, e);
      }
    }
  }
} 