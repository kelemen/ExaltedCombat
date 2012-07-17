package exaltedcombat.events;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.jtrim.event.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * An {@code EventTracker} implementation which ignores event notifications if
 * it was caused by the same kind of event. That is, if the {@code onEvent}
 * method call of a {@code TrackedListenerManager} created by the
 * {@code RecursionStopperEventTracker} is caused by the same kind of event as
 * the kind of event handled by the {@code TrackedListenerManager}: the
 * {@code onEvent} call will be ignored.
 * <P>
 * The tracking of the events is actually handled by an {@code EventTracker}
 * specified at construction time.
 *
 * <h3>Thread safety</h3>
 * Methods of this class are safe to be accessed by multiple threads
 * concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Methods of this class are <I>synchronization transparent</I>. Note that only
 * the methods provided by this class are <I>synchronization transparent</I>,
 * the {@code TrackedListenerManager} is not.
 *
 * @author Kelemen Attila
 */
public final class RecursionStopperEventTracker implements EventTracker {
    private final EventTracker wrappedTracker;

    /**
     * Creates a new {@code RecursionStopperEventTracker} with the given wrapped
     * {@code EventTracker}. Every call of the {@code RecursionStopperEventTracker}
     * will be forwarded to this {@code EventTracker}.
     *
     * @param wrappedTracker he {@code EventTracker} to which calls will be
     *   forwarded. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified argument is
     *   {@code null}
     */
    public RecursionStopperEventTracker(EventTracker wrappedTracker) {
        ExceptionHelper.checkNotNullArgument(wrappedTracker, "wrappedTracker");
        this.wrappedTracker = wrappedTracker;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public <ArgType> TrackedListenerManager<ArgType> getManagerOfType(
            Object eventKind, Class<ArgType> argType) {
        return new RecursionStopperListenerManager<>(eventKind,
                wrappedTracker.getManagerOfType(eventKind, argType));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Executor createTrackedExecutor(Executor executor) {
        return wrappedTracker.createTrackedExecutor(executor);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ExecutorService createTrackedExecutorService(ExecutorService executor) {
        return wrappedTracker.createTrackedExecutorService(executor);
    }

    private class RecursionStopperListenerManager<ArgType>
    implements
            TrackedListenerManager<ArgType> {

        private final Object eventKind;
        private final TrackedListenerManager<ArgType> wrappedManager;

        public RecursionStopperListenerManager(Object eventKind,
                TrackedListenerManager<ArgType> wrappedManager) {
            this.eventKind = eventKind;
            this.wrappedManager = wrappedManager;
        }

        @Override
        public void onEvent(ArgType arg) {
            wrappedManager.onEvent(arg);
        }

        @Override
        public ListenerRef registerListener(final TrackedEventListener<ArgType> listener) {
            return wrappedManager.registerListener(new TrackedEventListener<ArgType>() {
                @Override
                public void onEvent(TrackedEvent<ArgType> trackedEvent) {
                    if (!trackedEvent.getCauses().isCausedByKind(eventKind)) {
                        listener.onEvent(trackedEvent);
                    }
                }
            });
        }

        @Override
        public int getListenerCount() {
            return wrappedManager.getListenerCount();
        }
    }
}
