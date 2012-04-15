package exaltedcombat.events;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.utils.ExceptionHelper;

/**
 * An {@code EventManager} implementation allowing to remove all the registered
 * event handlers in a single method call. Other than this, this class only
 * forwards its method calls to an {@code EventManager} specified at
 * construction time.
 * <P>
 * This class is intended to be used when an {@code EventManager} can be
 * replaced and when it was replaced the event handlers need to be reregistered.
 * This class is expected to be used the following way:
 * <pre>
 * LocalEventManager&lt;EventType&gt; eventManager = null;
 * void setEventManager(EventManager&lt;EventType&gt;newEventManager) {
 *   if (eventManager != null) {
 *     eventManager.removeAllListeners();
 *   }
 *   eventManager = new LocalEventManager<>(newEventManager);
 *   // Register events with "eventManager" ...
 * }
 * </pre>
 * <P>
 * As required by the {@code EventManager} interface methods of this call are
 * safe to call from multiple threads concurrently but they are not transparent
 * to their synchronization.
 * <P>
 * Note that this class will be redesigned in the future and be moved to the
 * jtrim library.
 *
 * @param <EventType> the type of the possible events. It is recommended
 *   (but not required) that the events themselves be an instance of an
 *   {@code enum}.
 *
 * @author Kelemen Attila
 */
public class LocalEventManager<EventType> implements EventManager<EventType> {
    private final EventManager<EventType> wrappedManager;
    private final Lock mainLock;
    private final Map<Registration<EventType>, AtomicInteger> registrations;

    /**
     * Creates a new event manager delegating its calls to the specified
     * {@code EventManager}.
     *
     * @param wrappedManager the {@code EventManager} to which calls will be
     *   forwarded. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public LocalEventManager(EventManager<EventType> wrappedManager) {
        ExceptionHelper.checkNotNullArgument(wrappedManager, "wrappedManager");
        this.wrappedManager = wrappedManager;
        this.mainLock = new ReentrantLock();
        this.registrations = new HashMap<>();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Executor createTrackedExecutor(Executor wrapped) {
        return wrappedManager.createTrackedExecutor(wrapped);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ExecutorService createTrackedExecutor(ExecutorService wrapped) {
        return wrappedManager.createTrackedExecutor(wrapped);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void triggerEvent(EventType event, Object eventArg) {
        wrappedManager.triggerEvent(event, eventArg);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void registerListener(EventType event, GeneralEventListener<EventType> listener) {
        wrappedManager.registerListener(event, listener);

        Registration<EventType> reg = new Registration<>(event, listener);
        mainLock.lock();
        try {
            AtomicInteger counter = registrations.get(reg);
            if (counter == null) {
                counter = new AtomicInteger(0);
                registrations.put(reg, counter);
            }
            counter.incrementAndGet();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void unregisterListener(EventType event, GeneralEventListener<EventType> listener) {
        wrappedManager.unregisterListener(event, listener);

        Registration<EventType> reg = new Registration<>(event, listener);
        mainLock.lock();
        try {
            AtomicInteger counter = registrations.get(reg);
            if (counter != null) {
                int regCount = counter.decrementAndGet();
                if (regCount <= 0) {
                    registrations.remove(reg);
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * Unregisters all the event handlers
     * {@link #registerListener(java.lang.Object, exaltedcombat.events.GeneralEventListener) registered through this event manager}.
     * This method will call
     * {@link #unregisterListener(Object, exaltedcombat.events.GeneralEventListener) unregisterListener}
     * as many times as it were called for a certain event handler considering
     * the previous explicit {@code unregisterListener} calls.
     * <P>
     * Note that this method may ignore event handlers registered or
     * unregistered concurrently with this method call.
     */
    public void removeAllListeners() {
        List<Map.Entry<Registration<EventType>, AtomicInteger>> toRemove;
        mainLock.lock();
        try {
            toRemove = new ArrayList<>(registrations.entrySet());
            registrations.clear();
        } finally {
            mainLock.unlock();
        }

        for (Map.Entry<Registration<EventType>, AtomicInteger> entry: toRemove) {
            EventType event = entry.getKey().getEvent();
            GeneralEventListener<EventType> listener = entry.getKey().getListener();

            int removeCount = entry.getValue().get();
            for (int i = 0; i < removeCount; i++) {
                wrappedManager.unregisterListener(event, listener);
            }
        }
    }

    private static class Registration<EventType> {
        private final EventType event;
        private final GeneralEventListener<EventType> listener;

        public Registration(EventType event, GeneralEventListener<EventType> listener) {
            this.event = event;
            this.listener = listener;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Registration<?> other = (Registration<?>) obj;
            if (!Objects.equals(this.event, other.event)) {
                return false;
            }
            if (this.listener != other.listener) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(event);
            hash = 59 * hash + System.identityHashCode(listener);
            return hash;
        }

        public EventType getEvent() {
            return event;
        }

        public GeneralEventListener<EventType> getListener() {
            return listener;
        }
    }
}
