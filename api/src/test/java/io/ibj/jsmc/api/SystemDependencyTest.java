package io.ibj.jsmc.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link SystemDependency}
 *
 * @author Joseph Hirschfeld [Ichbinjoe] (joe@ibj.io)
 * @since 1/12/17
 */
public class SystemDependencyTest extends DependencyContractTest {

    @Test
    public void testDependencyLifecycleGetExportReturnsConstructorObject() throws Exception {
        DependencyConsumer consumer = mock(DependencyConsumer.class);
        Object object = new Object();

        Dependency dependency = new SystemDependency(object);

        DependencyLifecycle lifecycle = dependency.depend(consumer);

        assertEquals(object, lifecycle.getDependencyExports());
    }

    @Override
    public Dependency createNewTestable() {
        return new SystemDependency(new Object());
    }
}
