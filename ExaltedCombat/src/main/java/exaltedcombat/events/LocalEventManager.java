package exaltedcombat.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.event.ListenerRef;
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
    private final RefList<ListenerRef<GeneralEventListener<EventType>>> registrations;

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
        this.registrations = new RefLinkedList<>();
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
    public ListenerRef<GeneralEventListener<EventType>> registerListener(
            final EventType event, final GeneralEventListener<EventType> listener) {
        final ListenerRef<GeneralEventListener<EventType>> listenerRef;
        listenerRef = wrappedManager.registerListener(event, listener);

        final RefList.ElementRef<?> registrationRef;
        mainLock.lock();
        try {
            registrationRef = registrations.addLastGetReference(listenerRef);
        } finally {
            mainLock.unlock();
        }

        return new ListenerRef<GeneralEventListener<EventType>>() {
            private final AtomicBoolean registered = new AtomicBoolean(true);

            @Override
            public boolean isRegistered() {
                return registered.get();
            }

            @Override
            public void unregister() {
                if (registered.getAndSet(false)) {
                    try {
                        listenerRef.unregister();
                    } finally {
                        mainLock.lock();
                        try {
                            registrationRef.remove();
                        } finally {
                            mainLock.unlock();
                        }
                    }
                }
            }

            @Override
            public GeneralEventListener<EventType> getListener() {
                return listener;
            }
        };
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
        List<ListenerRef<GeneralEventListener<EventType>>> toRemove;
        mainLock.lock();
        try {
            toRemove = new ArrayList<>(registrations);
            registrations.clear();
        } finally {
            mainLock.unlock();
        }

        for (ListenerRef<GeneralEventListener<EventType>> listenerRef: toRemove) {
            listenerRef.unregister();
        }
    }
}
