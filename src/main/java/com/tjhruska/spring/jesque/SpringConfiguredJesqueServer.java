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

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Manages the spring life-cycle for a collection of jesque containers.
 * @author Timothy Hruska <https://github.com/tjhruska>
 *
 */
public class SpringConfiguredJesqueServer extends Thread implements InitializingBean, DisposableBean {
  private static final Logger log = LoggerFactory.getLogger(SpringConfiguredJesqueServer.class);
  
  private Collection<JesqueContainer> jesqueContainers;
  private Integer sleepTime;
  private Boolean shutdown;
  
  /**
   * Allows for configuration of the sleepTime between calls to each jesqueContainer.checkWorkers()
   * The jesqueContainer only updates is collection of workers during this call.
   * Especially usefull if you want a responsive system, and utilize WorkerExitOnEmpty workers.
   * @param jesqueContainers containers of jesque workers
   * @param sleepTime time to sleep between calls to jesqueContainer.checkWorkers()
   */
  public SpringConfiguredJesqueServer( Collection<JesqueContainer> jesqueContainers, Integer sleepTime) {
    this.jesqueContainers = jesqueContainers;
    this.sleepTime = sleepTime;
  }

  /**
   * In the general case workers will not exit.  So there is little need to poll the jesqueContainer.
   * Worker trap exceptions, but in the off chance of an issue we still poll the jesqueContainer.checkWorkers().
   * Default sleepTime is 5 minutes.
   * @param jesqueContainers containers of jesque workers
   */
  public SpringConfiguredJesqueServer( Collection<JesqueContainer> jesqueContainers) {
    this.jesqueContainers = jesqueContainers;
    this.sleepTime = 30000;
  }

  public Collection<JesqueContainer> getJesqueContainers() {
    return jesqueContainers;
  }
  
  public Boolean isShutdown() {
    return shutdown;
  }

  /** 
   * Called by spring after all beans have been created.  Starts jesqueContainers.
   * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
   */
  public void afterPropertiesSet() throws Exception {
    log.info("Starting jesque server.");
    shutdown = false;
    this.setDaemon(false);
    start();
  }

  /**
   * Called by spring on shutdown.  Does not wait for jobs to stop.  
   * The non-daemon server thread should wait for workers to finish.
   * @see java.lang.Thread#destroy()
   */
  public void destroy() {
    log.info("Stopping jesque server.");
    
    for(JesqueContainer jesqueContainer : jesqueContainers) {
      jesqueContainer.stop(false);
    }
    
    shutdown = true;
  }
  
  /**
   * Send the pause state requested down to each jesqueContainer.
   * Any currently running jobs will finish, and the worker will then stop taking new jobs.
   * @param paused new state requested for jesqueContainers
   * @see com.tjhruska.spring.jesque.JesqueContainer#togglePause(boolean)
   */
  public void togglePause(boolean paused) {
    for(JesqueContainer jesqueContainer : jesqueContainers) {
      jesqueContainer.togglePause(paused);
    }
  }
  
  /**
   * This shouldn't be called in production, it will be handled by the spring lifecycle.
   * @see java.lang.Thread#run()
   */
  public void run() {
    log.info("Jesque server starting run loop.");
    while (!shutdown) {
      for(JesqueContainer jesqueContainer : jesqueContainers) {
	      jesqueContainer.checkWorkers();
	    }
      try {
        sleep(sleepTime);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    log.info("Jesque server exiting run loop.  Joining to any still running workers.");
    
    for(JesqueContainer jesqueContainer : jesqueContainers) {
      try {
        jesqueContainer.join(0);
      } catch (InterruptedException e) {
        log.info("Interrupted during join to jesqueContainer, moving on.");
      }
    }
  }
}
