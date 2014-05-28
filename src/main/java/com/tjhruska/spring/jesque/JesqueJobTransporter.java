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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * JesqueJobTransporter moves jobs from one queue to another.  
 * Those queues can be on the same redis instance, or different ones.
 * 
 * @author Timothy Hruska <https://github.com/tjhruska>
 *
 */
public class JesqueJobTransporter implements Runnable, BeanNameAware{
  private static final Logger log = LoggerFactory.getLogger(JesqueJobTransporter.class);

  private String beanName;
  
  private JedisPool sourceJedisPool;
  private String sourceQueueName;
   
  private JedisPool destJedisPool;
  private String destQueueName;
   
  private String queueNamePrefix;
  private Integer destinationLimit;
  
  /**
   * Greedy transporter.  This transporter will not stop moving any source jobs found to the destination queue.
   * @param sourceJedisPool redis soure database
   * @param sourceQueueName take jobs from this queue (greedy)
   * @param destJedisPool redis destination database
   * @param destQueueName write jobs to this queue
   * @param queueNamePrefix typically this will be the standard resque prefix "resque:queue:"
   */
  public JesqueJobTransporter(JedisPool sourceJedisPool, String sourceQueueName, JedisPool destJedisPool, 
      String destQueueName, String queueNamePrefix) {
    this.sourceJedisPool = sourceJedisPool;
    this.sourceQueueName = sourceQueueName;
    this.destJedisPool = destJedisPool;
    this.destQueueName = destQueueName;
    this.queueNamePrefix = queueNamePrefix;
  }
  
  /**
   * Limited transporter.  
   * This transporter only move source jobs to the destination queue if the size of the destination queue is below the destination limit.
   * @param sourceJedisPool redis soure database
   * @param sourceQueueName take jobs from this queue (greedy)
   * @param destJedisPool redis destination database
   * @param destQueueName write jobs to this queue
   * @param queueNamePrefix typically this will be the standard resque prefix "resque:queue:"
   * @param destinationLimit transporter will not load destination queue to have more jobs than this limit
   */
  public JesqueJobTransporter(JedisPool sourceJedisPool, String sourceQueueName, JedisPool destJedisPool, 
      String destQueueName, String queueNamePrefix, Integer destinationLimit) {
    this.sourceJedisPool = sourceJedisPool;
    this.sourceQueueName = sourceQueueName;
    this.destJedisPool = destJedisPool;
    this.destQueueName = destQueueName;
    this.queueNamePrefix = queueNamePrefix;
    this.destinationLimit = destinationLimit;
  }
  
  public void setBeanName(String name) {
    this.beanName = name;
  }

  public void setDestinationLimit(Integer destinationLimit) {
    this.destinationLimit = destinationLimit;
  }

  /**
   * Each execution of the run method will result in moving jobs from the source to the destination queues.
   * Jobs will be moved until the source queue is empty, or the destination queue limit has been reached (if provided).
   */
  @SuppressWarnings("resource")
  public void run() {
    Jedis sourceJedis = null;
    Jedis destJedis = null;
    
    boolean hadException = true;
    try {
      sourceJedis = sourceJedisPool.getResource();
      destJedis = destJedisPool.getResource();
      
      int jobCount = 0;
      while (needToPullJob(sourceJedis, destJedis)) {
        String jobString = sourceJedis.lpop(getQualifiedQueueName(sourceQueueName));
        if (jobString != null) {
          try {
            destJedis.rpush(getQualifiedQueueName(destQueueName), jobString);
          } catch (Exception e) {
            log.error("Failed to push job (will attempt to return to source) to '{}' queue: '{}'", getQualifiedQueueName(destQueueName), jobString);
            sourceJedis.lpush(getQualifiedQueueName(sourceQueueName), jobString);
            log.error("returned job to source queue");
            throw e;
          }
          jobCount++;
        }
      }
      
      log.info("{} job copied {} jobs from source queue '{}' to destination queue '{}'", new Object[] {beanName, jobCount, sourceQueueName, destQueueName});
      
      hadException = false;
      
    } finally {
      if (hadException) {
        sourceJedisPool.returnBrokenResource(sourceJedis);
        destJedisPool.returnBrokenResource(destJedis);
      } else {
        sourceJedisPool.returnResource(sourceJedis);
        destJedisPool.returnResource(destJedis);
      }
    }
  }
   
  public boolean needToPullJob(Jedis sourceJedis, Jedis destJedis) {
      Long sourceSize = sourceJedis.llen(getQualifiedQueueName(sourceQueueName));
      Long destSize = destJedis.llen(getQualifiedQueueName(destQueueName));
      
      boolean destinationNeedsARow = destinationLimit == null || (destinationLimit != null && destinationLimit > destSize);
    
      return (destinationNeedsARow && sourceSize > 0);
  }
  
  /**
   * @param queueName
   * @return queueNamePrefix + queueName
   */
  public String getQualifiedQueueName(String queueName) {
    return queueNamePrefix + queueName;
  }
  
  public String getQueueNamePrefix() {
    return queueNamePrefix;
  }

  public String getSourceQueueName() {
    return sourceQueueName;
  }

  public String getDestQueueName() {
    return destQueueName;
  }
} 