package com.tjhruska.spring.jesque;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.annotation.Resource;

import net.greghaines.jesque.Job;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tjhruska.spring.jesque.testJobs.TestBeanJob;
import com.tjhruska.spring.jesque.testJobs.TestBeanJobCallable;
import com.tjhruska.spring.jesque.testJobs.TestInitializingBeanJob;
import com.tjhruska.spring.jesque.testJobs.TestJob;
import com.tjhruska.spring.jesque.testJobs.TestJobJobFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class BeanJobFactoryUnitTest {

  @Resource(name = "beanJobFactory")
  BeanJobFactory beanJobFactory;

  @Test
  public void wrongClassName() {
    Job missingBeanName = new Job("com.tjhruska.spring.jesque.BeanJobFoo");
    try {
      beanJobFactory.materializeJob(missingBeanName);
    } catch (Exception e) {
      assertEquals("Wrong exception message", "BeanJobFactory only knows how to materialize BeanJob bean jobs.  "
          + "If you also want to load non bean based jobs then inject a fallbackJobFactory to handle those.",
          e.getMessage());
    }
  }

  @Test
  public void delegateFactoryIfNeeded() throws Exception {
    beanJobFactory.setFallbackJobFactory(new TestJobJobFactory());
    Job testJob = new Job("com.tjhruska.spring.jesque.testJobs.TestJob");
    Runnable runner = (Runnable) beanJobFactory.materializeJob(testJob);

    assertTrue("Job was not a TestJob", (runner instanceof TestJob));
  }

  @Test
  public void missingBeanJobName() {
    Job missingBeanName = new Job("com.tjhruska.spring.jesque.BeanJob");
    try {
      beanJobFactory.materializeJob(missingBeanName);
    } catch (Exception e) {
      assertEquals("Wrong exception message",
          "BeanJobFactory expects at least 1 argument with the first being the bean name, args were empty.",
          e.getMessage());
    }
  }

  @Test
  public void initMissingBean() {
    Job noSuchBeanJob = new BeanJob("missingBean");
    try {
      beanJobFactory.materializeJob(noSuchBeanJob);
    } catch (NoSuchBeanDefinitionException e) {
      return;
    } catch (Exception e) {
      fail("Didn't throw expected exception");
    }
    fail("Didn't throw expected exception");
  }

  @Test
  public void initFoundBean() throws Exception {
    Job testBeanJob = new BeanJob("testBeanJob");
    Runnable runner = (Runnable) beanJobFactory.materializeJob(testBeanJob);
    assertTrue("Wrong Bean", (runner instanceof TestBeanJob));
  }

  @Test
  public void initFoundBeanNonInitWithArgs() throws Exception {
    try {
      Job testBeanJob = new BeanJob("testBeanJob", "hello");
      beanJobFactory.materializeJob(testBeanJob);
      fail("Expected an exception, but didn't see it");
    } catch (RuntimeException e) {
      assertEquals(
          "Wrong exception message",
          "Variable arguments passed into BeanJob required bean implement RunnableWithInit interface, bean testBeanJob doesn't.",
          e.getMessage());
    }
  }

  @Test
  public void runFoundBeanPrototype() throws Exception {
    Job testBeanJob = new BeanJob("testBeanJob");
    Runnable runner = (Runnable) beanJobFactory.materializeJob(testBeanJob);

    runner.run();
    assertTrue("Wrong run count", ((TestBeanJob) runner).runCount.equals(1));

    runner.run();
    assertTrue("Wrong run count, same bean instance should have incremented to 2",
        ((TestBeanJob) runner).runCount.equals(2));

    runner = (Runnable) beanJobFactory.materializeJob(testBeanJob);
    runner.run();
    assertTrue("Wrong run count, prototype bean should have been 1", ((TestBeanJob) runner).runCount.equals(1));
  }

  @Test
  public void runFoundBeanInitializingPrototype() throws Exception {
    Job testBeanJob = new BeanJob("testInitializingBeanJob", "hello", "world");
    Runnable runner = (Runnable) beanJobFactory.materializeJob(testBeanJob);
    assertTrue("Wrong Bean", (runner instanceof TestInitializingBeanJob));
    assertEquals("Wrong Arg1", "hello", ((TestInitializingBeanJob) runner).getArg1());
    assertEquals("Wrong Arg2", "world", ((TestInitializingBeanJob) runner).getArg2());

    runner.run();
    assertTrue("Wrong run count", ((TestInitializingBeanJob) runner).runCount.equals(1));

    runner.run();
    assertTrue("Wrong run count, same bean should have been incremented to 2",
        ((TestInitializingBeanJob) runner).runCount.equals(2));

    runner = (Runnable) beanJobFactory.materializeJob(testBeanJob);
    runner.run();
    assertTrue("Wrong run count, prototype bean should have been same",
        ((TestInitializingBeanJob) runner).runCount.equals(1));
  }

  @Test
  public void runFoundBeanSingleton() throws Exception {
    Job testBeanJob = new BeanJob("testSingletonBeanJob");
    Runnable runner = (Runnable) beanJobFactory.materializeJob(testBeanJob);

    runner.run();
    assertTrue("Wrong run count", ((TestBeanJob) runner).runCount.equals(1));

    runner.run();
    assertTrue("Wrong run count, same bean should have been incremented to 2",
        ((TestBeanJob) runner).runCount.equals(2));

    runner = (Runnable) beanJobFactory.materializeJob(testBeanJob);
    runner.run();
    assertTrue("Wrong run count, singleton bean should have been incremented to 3",
        ((TestBeanJob) runner).runCount.equals(3));
  }

  @Test
  public void wrapFallbackJobInLoggingProxyRunnable() throws Exception {
    beanJobFactory.setFallbackJobFactory(new TestJobJobFactory());
    beanJobFactory.setAddLoggingProxy(true);
    Job testJob = new Job("com.tjhruska.spring.jesque.testJobs.TestJob");
    Object runner = beanJobFactory.materializeJob(testJob);
    beanJobFactory.setAddLoggingProxy(false);

    assertTrue("Job was not logging proxy runnable", (runner instanceof LoggingProxyRunnable));
    assertTrue("Job was not a TestJob", (((LoggingProxyRunnable) runner).getDelegate() instanceof TestJob));
  }

  @Test
  public void wrapJobInLoggingProxyCallable() throws Exception {
    beanJobFactory.setAddLoggingProxy(true);
    Job testJobCallable = new BeanJob("testBeanJobCallable");
    Object runner = beanJobFactory.materializeJob(testJobCallable);
    beanJobFactory.setAddLoggingProxy(false);

    assertTrue("Job was not logging proxy callable", (runner instanceof LoggingProxyCallable));
    assertTrue("Job was not a TestJobCallable",
        (((LoggingProxyCallable) runner).getDelegate() instanceof TestBeanJobCallable));
  }
}