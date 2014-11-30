package com.tjhruska.spring.jesque;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class SpringConfiguredJesqueServerUnitTest {
  JesqueContainer mockContainer1;
  JesqueContainer mockContainer2;
  List<JesqueContainer> jesqueContainers;

  SpringConfiguredJesqueServer springConfiguredJesqueServer;

  @Before
  public void setup() {
    mockContainer1 = mock(JesqueContainer.class);
    mockContainer2 = mock(JesqueContainer.class);
    jesqueContainers = new ArrayList<JesqueContainer>();
    jesqueContainers.add(mockContainer1);
    jesqueContainers.add(mockContainer2);

    springConfiguredJesqueServer = new SpringConfiguredJesqueServer(jesqueContainers, 1);
    springConfiguredJesqueServer.setBeanName("springConfiguredJesqueServer1");
  }

  @Test
  public void testAfterPropertiesSet() throws Exception {
    springConfiguredJesqueServer.afterPropertiesSet();
    Thread.sleep(50);
    verify(mockContainer1, atLeastOnce()).checkWorkers();
    verify(mockContainer2, atLeastOnce()).checkWorkers();
    assertFalse("Shutdown was true", springConfiguredJesqueServer.isShutdown());
    assertTrue("Thread not alive", springConfiguredJesqueServer.isAlive());
  }

  @Test
  public void testDestroy() throws Exception {
    springConfiguredJesqueServer.afterPropertiesSet();
    Thread.sleep(50);
    verify(mockContainer1, atLeastOnce()).checkWorkers();
    verify(mockContainer2, atLeastOnce()).checkWorkers();
    springConfiguredJesqueServer.destroy();
    verify(mockContainer1).stop(false);
    verify(mockContainer2).stop(false);
    verify(mockContainer1).join(0);
    verify(mockContainer2).join(0);
    assertTrue("Shutdown was false", springConfiguredJesqueServer.isShutdown());
    assertFalse("Thread alive", springConfiguredJesqueServer.isAlive());
  }
}
