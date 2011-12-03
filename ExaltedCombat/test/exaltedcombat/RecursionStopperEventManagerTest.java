/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package exaltedcombat;

import exaltedcombat.events.EventManager;
import exaltedcombat.events.GeneralEventListener;
import exaltedcombat.events.EventCauses;
import exaltedcombat.events.RecursionStopperEventManager;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class RecursionStopperEventManagerTest {

    public RecursionStopperEventManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of triggerEvent method, of class RecursionStopperEventManager.
     */
    @Test
    public void testSingleEvent() {
        final Object testArg = new Object();
        final AtomicBoolean triggered = new AtomicBoolean();

        EventManager<EventTypes> manager = new RecursionStopperEventManager<>(10);

        manager.registerListener(EventTypes.EVENT1, new TestListener() {
            @Override
            public void onEvent(EventCauses<EventTypes> managerArg, Object eventArg) {
                assertFalse(managerArg.isIndirectCause(EventTypes.EVENT1));
                assertFalse(managerArg.isIndirectCause(EventTypes.EVENT2));
                assertFalse(managerArg.isIndirectCause(EventTypes.EVENT3));
                assertEquals(managerArg.getDirectCause(), EventTypes.EVENT1);
                assertSame(eventArg, testArg);
                triggered.set(true);
            }
        });

        triggered.set(false);
        manager.triggerEvent(EventTypes.EVENT1, testArg);
        assertTrue(triggered.get());

        triggered.set(false);
        manager.triggerEvent(EventTypes.EVENT2, testArg);
        assertFalse(triggered.get());

        triggered.set(false);
        manager.triggerEvent(EventTypes.EVENT3, testArg);
        assertFalse(triggered.get());
    }

    private enum EventTypes {
        EVENT1, EVENT2, EVENT3
    }

    private static interface TestListener extends GeneralEventListener<EventTypes> {

    }
}
