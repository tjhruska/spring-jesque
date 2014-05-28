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

import java.util.ArrayList;
import java.util.List;

import net.greghaines.jesque.Job;

/**
 * BeanJob can be used to enqueue jobs into a JesqueClient.
 * This class is a convenience class for setting the class of the BeanJob.
 * You can just enqueue a Job with the class being BeanJobRunner to get the same effect.
 * 
 * @author Timothy Hruska <https://github.com/tjhruska>
 *
 */
public class BeanJob extends Job {

  private static final long serialVersionUID = 1L;

  /**
   * @param beanId of the bean job to run
   */
  public BeanJob(String beanId) {
    super("com.tjhruska.spring.jesque.BeanJob");
    this.setArgs(beanId);
  }

  /**
   * @param beanId of the bean job to run
   * @param args to insert into the RunnableWithInit job
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public BeanJob(String beanId, List<?> args) {
    super("com.tjhruska.spring.jesque.BeanJob");
    List fullArgs = new ArrayList();
    fullArgs.add(beanId);
    fullArgs.addAll(args);
    this.setArgs(fullArgs.toArray());
  }

  /**
   * @param beanId of the bean job to run
   * @param args to insert into the RunnableWithInit job
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public BeanJob(String beanId, Object... args) {
    super("com.tjhruska.spring.jesque.BeanJob");
    List fullArgs = new ArrayList();
    fullArgs.add(beanId);
    for (int i = 0; i < args.length; i++) {
      fullArgs.add(args[i]);
    }
    this.setArgs(fullArgs.toArray());
  }
} 