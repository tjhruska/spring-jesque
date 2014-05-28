package com.tjhruska.spring.jesque;

import static org.junit.Assert.*;

import javax.annotation.Resource;

import net.greghaines.jesque.client.Client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tjhruska.spring.jesque.JesqueJobTransporter;
import com.tjhruska.spring.jesque.SpringConfiguredJesqueServer;
import com.tjhruska.spring.jesque.testJobs.TestBeanJob;
import com.tjhruska.spring.jesque.testJobs.TestInitializingBeanJob;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class SpringConfiguredJesqueServerFunctionalTest {
  
  /*
   * THIS TEST WILL FLUSH THE REDIS DB!
   * 
   * This test requires two running instances of Redis.
   * 
   * Demonstration of >1 redis instance, with each having its own JesqueContainer configured.
   *   jesque1 -> port 6379 reading queues:  foo
   *   jesque2 -> port 6380 reading queues:  bar, baz
   * 
   * Write all jobs to queues on jesque1 for queues:  foo, bar, baz (only foo will run)
   * 
   * Use transporter to move jobs from jesque1.bar -> jesque2.bar
   * Use transporter to move jobs from jesque1.baz -> jesque2.baz
   * 
   * Using prototype beans for job results in the object being a new instance for full thread safety.
   * Using a singleton bean for a job results in the same cached object executing each time, 
   *   potential safety issues if there is any stored state.
   * 
   * Bean Jobs are required at this time to implement either Runnable, or RunnableWithInit
   * 
   */
  
  @Resource (name="jesqueServer")
  public SpringConfiguredJesqueServer jesqueServer;
  
  @Resource (name="jedisPool1")
  public JedisPool jedisPool1;
      
  @Resource (name="jedisPool2")
  public JedisPool jedisPool2;
  
  @Resource (name="jesqueClient1")
  public Client jesqueClient1;
  
  @Resource (name="jesqueClient2")
  public Client jesqueClient2;
  
  @Resource (name="barTransporter")
  public JesqueJobTransporter barTransporter;
  
  @Resource (name="bazTransporter")
  public JesqueJobTransporter bazTransporter;
 
  @Resource (name="testBeanJob")
  public TestBeanJob testBeanJob;
  
  @Resource (name="testInitializingBeanJob")
  public TestInitializingBeanJob testInitializingBeanJob;
  
  @Resource (name="testSingletonBeanJob")
  public TestInitializingBeanJob testSingletonBeanJob;
  
  Jedis jedis1;
  Jedis jedis2;
  
  @Before
  public void setup() {
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
  
  @After
  public void shutdownServer() {
    jesqueServer.destroy();
  }
  
  @Test
  public void happyPath() {
    
    TestBeanJob.staticRunCount = 0;
    TestInitializingBeanJob.staticRunCount = 0;
    
    jesqueServer.togglePause(true);
    
    for (int i = 0; i < 10; i++) {
      jesqueClient1.enqueue("foo", new BeanJob("testBeanJob"));
      jesqueClient1.enqueue("bar", new BeanJob("testInitializingBeanJob", "hello world", "bar"));
      jesqueClient1.enqueue("baz", new BeanJob("testSingletonBeanJob", "hello world", "baz"));
    }
    
    try {
      Thread.sleep(100);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    assertTrue("Expected foo queue jobs to not have started", TestBeanJob.staticRunCount.equals(0));
    jesqueServer.togglePause(false);
    
    for (int i = 0; i < 100; i++) {
      if (TestBeanJob.staticRunCount == 10) {
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    
    assertTrue("Expected foo queue jobs to be done", TestBeanJob.staticRunCount.equals(10));
    assertTrue("Expected bar and baz queue jobs to NOT be done", TestInitializingBeanJob.staticRunCount.equals(0));
    
    assertTrue("Prototype bean shouldn't have local state affected - testBeanJob", testBeanJob.runCount.equals(0));
    assertTrue("Not run yet - Prototype bean shouldn't have local state affected - testInitializingBeanJob", testInitializingBeanJob.runCount.equals(0));
    assertTrue("Not run yet - Singleton bean SHOULD have local state affected - testSingletonBeanJob", testSingletonBeanJob.runCount.equals(0));
 
    barTransporter.run();
    
    for (int i = 0; i < 100; i++) {
      if (TestInitializingBeanJob.staticRunCount == 10) {
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    
    assertTrue("Expected foo queue jobs to still be done", TestBeanJob.staticRunCount.equals(10));
    assertTrue("Expected bar jobs to be done", TestInitializingBeanJob.staticRunCount.equals(10));
    
    assertTrue("Prototype bean shouldn't have local state affected - testBeanJob", testBeanJob.runCount.equals(0));
    assertTrue("Prototype bean shouldn't have local state affected - testInitializingBeanJob", testInitializingBeanJob.runCount.equals(0));
    assertTrue("Not run yet - Singleton bean SHOULD have local state affected - testSingletonBeanJob", testSingletonBeanJob.runCount.equals(0));

    bazTransporter.run();
    
    for (int i = 0; i < 100; i++) {
      if (TestInitializingBeanJob.staticRunCount == 20) {
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    
    assertTrue("Expected foo queue jobs to really still be done", TestBeanJob.staticRunCount.equals(10));
    assertTrue("Expected bar and baz jobs to be done", TestInitializingBeanJob.staticRunCount.equals(20));

    assertTrue("Prototype bean shouldn't have local state affected - testBeanJob", testBeanJob.runCount.equals(0));
    assertTrue("Prototype bean shouldn't have local state affected - testInitializingBeanJob", testInitializingBeanJob.runCount.equals(0));
    assertTrue("Singleton bean SHOULD have local state affected - testSingletonBeanJob", testSingletonBeanJob.runCount.equals(10));
  }
}
