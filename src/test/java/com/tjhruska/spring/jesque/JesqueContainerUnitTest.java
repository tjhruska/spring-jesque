package com.tjhruska.spring.jesque;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import net.greghaines.jesque.worker.Worker;
import net.greghaines.jesque.worker.WorkerImpl;

import org.junit.Before;
import org.junit.Test;

public class JesqueContainerUnitTest {

  private Callable<Worker> mockWorkerFactory;
  private WorkerImpl mockWorker;
  private JesqueContainer jesqueContainer;

  @SuppressWarnings("unchecked")
  @Before
  public void setup() throws Exception {
    mockWorkerFactory = mock(Callable.class);
    mockWorker = mock(WorkerImpl.class);
    when(mockWorkerFactory.call()).thenReturn(mockWorker);
    when(mockWorker.getName()).thenReturn("mock worker");

    jesqueContainer = new JesqueContainer(mockWorkerFactory);
    jesqueContainer.setBeanName("testJesqueContainer");
  }

  @Test
  public void testIsPaused() {
    jesqueContainer.checkWorkers();
    assertFalse("Expected paused = false", jesqueContainer.isPaused());
    jesqueContainer.togglePause(true);
    assertTrue("Expected paused = true", jesqueContainer.isPaused());
  }

  @Test
  public void testIsPausedUninitialized() {
    jesqueContainer.isPaused();
  }

  @Test
  public void testInitOne() {
    jesqueContainer.checkWorkers();
    assertNotNull("Worker was null", jesqueContainer.getWorkers()[0]);
    assertNotNull("WorkerThread was null", jesqueContainer.getWorkerThreads()[0]);
    assertFalse("Was daemon thread", jesqueContainer.getWorkerThreads()[0].isDaemon());
    assertTrue("Thread wasn't started", jesqueContainer.getWorkerThreads()[0].isAlive());
  }

  @Test
  public void testInitThree() {
    jesqueContainer = new JesqueContainer(mockWorkerFactory, 3);
    jesqueContainer.setBeanName("testJesqueContainerWithMaxJobCount");
    jesqueContainer.checkWorkers();
    assertTrue("Expected 3 workers", (jesqueContainer.getWorkers().length == 3));

    for (int i = 0; i < 3; i++) {
      assertNotNull("Worker was null", jesqueContainer.getWorkers()[0]);
      assertNotNull("WorkerThread was null", jesqueContainer.getWorkerThreads()[0]);
      assertFalse("Was daemon thread", jesqueContainer.getWorkerThreads()[0].isDaemon());

      // mocked worker doesn't stay running, this is covered by functional
      // tests, so skipping here
      // assertTrue("Thread wasn't started",
      // jesqueContainer.getWorkerThreads()[0].isAlive());
    }
  }

  @Test
  public void testStop() {
    jesqueContainer.checkWorkers();
    jesqueContainer.stop(true);
    verify(mockWorker).end(true);
  }

  @Test
  public void testJoin() {
    jesqueContainer.checkWorkers();
    try {
      jesqueContainer.join(0);
      verify(mockWorker).join(0);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}