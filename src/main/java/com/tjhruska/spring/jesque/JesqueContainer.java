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

import net.greghaines.jesque.worker.Worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JesqueContainer maintains a group of workers that are all created using the same injected workerFactory.
 * 
 * @author Timothy Hruska <https://github.com/tjhruska>
 *
 */
public class JesqueContainer {
  private static final Logger log = LoggerFactory.getLogger(JesqueContainer.class);
  
  private Callable<Worker> workerFactory;

  private Integer maxWorkerCount;
  private Worker[] workers;
  private Thread[] workerThreads;
  private boolean paused;

  public Worker[] getWorkers() {
    return workers;
  }
  
  public Thread[] getWorkerThreads() {
    return workerThreads;
  }
  
  public Integer getMaxWorkerCount() {
    return maxWorkerCount;
  }

  /**
   * This JesqueContainer that will hold exactly one worker.
   * @param workerFactory that will create/replenish the workers for this container
   */
  public JesqueContainer(Callable<Worker> workerFactory) {
    this.workerFactory = workerFactory;
    this.maxWorkerCount = 1;
    this.workers = new Worker[maxWorkerCount];
    this.workerThreads = new Thread[maxWorkerCount];
    this.paused = false;
  }
  
  /**
   * This JesqueContainer that will hold up to maxWorkerCount.
   * @param workerFactory that will create/replenish the workers for this container
   * @param maxWorkerCount up to this number of workers will be maintained in this container
   */
  public JesqueContainer(Callable<Worker> workerFactory, Integer maxWorkerCount) {
    this.workerFactory = workerFactory;
    this.maxWorkerCount = maxWorkerCount;
    this.workers = new Worker[maxWorkerCount];
    this.workerThreads = new Thread[maxWorkerCount];
    this.paused = false;
  }
  
  /**
   * Spin through to attempt to replace any dead/missing workers.  
   * WorkerFactory is not required to return a worker.  
   *    (This is mostly useful when the workers are tied to limited resources.)
   */
  public void checkWorkers() {
    for (int i = 0; i < maxWorkerCount; i++) {
      if (!paused && (workerThreads[i] == null || !workerThreads[i].isAlive())) {
        Worker worker;
        try {
          worker = workerFactory.call();
        } catch (Exception e) {
          throw new RuntimeException("Failed to get a worker from the workerFactory", e);
        }
        if (worker != null) {
          log.info("Started worker(s) of type '{}' with queues: '{}'", worker.getName(), worker.getQueues());
    
			    Thread workerThread = new Thread(worker);
			    workerThread.setDaemon(false);
			    workerThread.start();
        
			    workers[i] = worker;
			    workerThreads[i] = workerThread;
        }
      }
    }
  }
  
  /**
   * This does not check the state of the contained workers.
   * @return current pause state of the container.
   */
  public boolean isPaused() {
    return paused;
  }

  /**
   * Container will remember the last pause state, and only send changes to workers.
   * Workers will pause after they complete any currently running jobs.
   * @param paused state to send to the workers if changing
   */
  public void togglePause(boolean paused) {
    if (this.paused == paused) {
       return;
    }
    this.paused = paused;
    for (int i = 0; i < maxWorkerCount; i++) {
      if (workerThreads[i] != null && workerThreads[i].isAlive()) {
        if (paused) {
		      log.info("pausing worker(s) '{}' with queues '{}'", workers[i].getName(), workers[i].getQueues());
		    } else {
		      log.info("unpausing worker(s) '{}' with queues '{}'", workers[i].getName(), workers[i].getQueues());
		    }
		    workers[i].togglePause(paused);
      }
    }
  }

  /**
   * Sends end() message to all currently live workers.  
   * If now is false workers won't die until they complete current jobs.
   * @param now if true will abort currently running jobs.
   */
  public void stop(boolean now) {
    for (int i = 0; i < maxWorkerCount; i++) {
      if (workerThreads[i] != null && workerThreads[i].isAlive()) {
        log.info("stopping worker(s) '{}' with queues '{}'", workers[i].getName(), workers[i].getQueues());
        workers[i].end(now);
      }
    }
  }
  
  /**
   * Wait on any currently live worker threads until they exit.
   * @param millis how long to wait for each worker before moving on to join the next worker
   * @throws InterruptedException
   */
  public void join(long millis) throws InterruptedException {
    for (int i = 0; i < maxWorkerCount; i++) {
      if (workerThreads[i] != null && workerThreads[i].isAlive()) {
        log.info("joinging against worker(s) '{}' with queues '{}'", workers[i].getName(), workers[i].getQueues());
        workers[i].join(millis);
      }
    }
  }
} 