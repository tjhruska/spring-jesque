package com.tjhruska.spring.jesque;

import javax.annotation.Resource;

import org.junit.Assert;

import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tjhruska.spring.jesque.JesqueJobTransporter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class JesqueJobTransporterFunctionalTest {

  JesqueJobTransporter transporter;
  
  @Resource (name="jedisPool1")
  public JedisPool jedisPool1;
  		
  @Resource (name="jedisPool2")
  public JedisPool jedisPool2;
  
  @Resource (name="jesqueClient1")
  public Client jesqueClient1;
  
  @Resource (name="jesqueClient2")
  public Client jesqueClient2;
  
  Jedis jedis1;
  Jedis jedis2;
  
  @Before
  public void setup() {
    transporter = new JesqueJobTransporter(jedisPool1, "foo", jedisPool2, "bar", "resque:queue:", 10);
    transporter.setBeanName("testTransporter");
    jedis1 = jedisPool1.getResource();
    jedis2 = jedisPool2.getResource();
    
    jedis1.flushDB();
    jedis2.flushDB();
  }
  
  @After
  public void teardown() {
    jedisPool1.returnBrokenResource(jedis1);
    jedisPool2.returnBrokenResource(jedis2);
    
    jedis1.flushDB();
    jedis2.flushDB();
  }
  
  public void loadSomeJobs(Client jesqueClient, String queueName, Integer numberOfJobs) {
    for (int i = 0; i < numberOfJobs; i++) {
      jesqueClient.enqueue(queueName, new Job("testJob"));
    }
  }
  
  public void assertSrcDestQueueSizes(Long expectedSourceQueueSize, Long expectedDestQueueSize) {
    assertQueueSize(jedis1, transporter.getSourceQueueName(), expectedSourceQueueSize);
    assertQueueSize(jedis2, transporter.getDestQueueName(), expectedDestQueueSize);
  }
  
  public void assertQueueSize(Jedis jedis, String queueName, Long expectedQueueSize) {
    Long actualQueueSize = jedis.llen(transporter.getQualifiedQueueName(queueName));
    Assert.assertEquals("Queue " + queueName + " was the wrong size:", expectedQueueSize, actualQueueSize);
  }
  
  @Test
  public void noJobsToTransfer() {
    assertSrcDestQueueSizes(0L, 0L);
    transporter.run();
    assertSrcDestQueueSizes(0L, 0L);
  }  
  
  @Test
  public void noJobsToTransferSomeInQueue() {
    loadSomeJobs(jesqueClient2, transporter.getDestQueueName(), 127);
    assertSrcDestQueueSizes(0L, 127L);
    transporter.run();
    assertSrcDestQueueSizes(0L, 127L);
  }
  
  @Test
  public void someJobsWithNoLimit() {
    transporter.setDestinationLimit(null);
    loadSomeJobs(jesqueClient1, transporter.getSourceQueueName(), 127);
    assertSrcDestQueueSizes(127L, 0L);
    transporter.run();
    assertSrcDestQueueSizes(0L, 127L);
  }
  
  @Test
  public void someJobsWithNoLimit2() {
    transporter.setDestinationLimit(null);
    loadSomeJobs(jesqueClient1, transporter.getSourceQueueName(), 127);
    loadSomeJobs(jesqueClient2, transporter.getDestQueueName(), 127);
    assertSrcDestQueueSizes(127L, 127L);
    transporter.run();
    assertSrcDestQueueSizes(0L, 254L);
  }
  
  @Test
  public void someJobsWithSmallLimit() {
    transporter.setDestinationLimit(10);
    loadSomeJobs(jesqueClient1, transporter.getSourceQueueName(), 127);
    assertSrcDestQueueSizes(127L, 0L);
    transporter.run();
    assertSrcDestQueueSizes(117L, 10L);
  }
  
  @Test
  public void someJobsWithSmallLimit2() {
    transporter.setDestinationLimit(10);
    loadSomeJobs(jesqueClient1, transporter.getSourceQueueName(), 127);
    loadSomeJobs(jesqueClient2, transporter.getDestQueueName(), 127);
    assertSrcDestQueueSizes(127L, 127L);
    transporter.run();
    assertSrcDestQueueSizes(127L, 127L);
  }
  
  @Test
  public void someJobsWithLargeLimit() {
    transporter.setDestinationLimit(1000);
    loadSomeJobs(jesqueClient1, transporter.getSourceQueueName(), 127);
    assertSrcDestQueueSizes(127L, 0L);
    transporter.run();
    assertSrcDestQueueSizes(0L, 127L);
  }
  
  @Test
  public void someJobsWithLargeLimit2() {
    transporter.setDestinationLimit(1000);
    loadSomeJobs(jesqueClient1, transporter.getSourceQueueName(), 127);
    loadSomeJobs(jesqueClient2, transporter.getDestQueueName(), 127);
    assertSrcDestQueueSizes(127L, 127L);
    transporter.run();
    assertSrcDestQueueSizes(0L, 254L);
  }
} 