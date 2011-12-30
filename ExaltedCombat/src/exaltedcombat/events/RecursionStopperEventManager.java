package exaltedcombat.events;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import org.jtrim.event.*;
import org.jtrim.utils.*;

/**
 * An {@code EventManager} implementation which ignores
 * {@link #triggerEvent(java.lang.Object, java.lang.Object) triggerEvent} call
 * if it was caused by the same event.
 * <P>
 * This implementation can remember the causes without tracking executors if
 * handlers during the {@code triggerEvent} call synchronously invoke
 * {@code triggerEvent} on the same thread. If they submit a task to a different
 * thread this implementation will not be able to detect that that task was
 * caused by the submitting event.
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
 * @see LocalEventManager
 * @author Kelemen Attila
 */
public final class RecursionStopperEventManager<EventType>
implements
        EventManager<EventType> {

    private final ThreadLocal<EventCauses<EventType>> currentCauses;
    private final Lock registerLock;
    private final ConcurrentMap<EventType, EventHandlerContainer<GeneralEventListener<EventType>>> listeners;

    /**
     * Creates a new event manager with a given expected different event count.
     * The expected event count is not required for correctness but supplying a
     * large enough value can improve performance. However specifying too large
     * value can cause unnecessary memory overhead.
     * <P>
     * The expected number of event count is the number of different (defined by
     * the {@code equals} method) {@code event} argument that can be passed
     * to the {@link #registerListener(Object, exaltedcombat.events.GeneralEventListener) registerListener}
     * method call.
     *
     * @param expectedEventTypeCount the number of expected different event
     *   count. This argument must be greater than or equal to zero.
     *
     * @throws IllegalArgumentException thrown if the argument is a negative
     *   integer
     */
    public RecursionStopperEventManager(int expectedEventTypeCount) {
        this.listeners = new ConcurrentHashMap<>(expectedEventTypeCount);
        this.registerLock = new ReentrantLock();
        this.currentCauses = new ThreadLocal<>();
    }

    private EventCauses<EventType> getCurrentCauses(EventType directCause) {
        EventCauses<EventType> causes = currentCauses.get();
        if (causes == null) {
            return new SingleCause<>(directCause);
        }
        else {
            return new ChainedCause<>(directCause, causes);
        }
    }

    private static <EventType> boolean isCause(EventCauses<EventType> causes, EventType event) {
        if (Objects.equals(event, causes.getDirectCause())) {
            return true;
        }

        return causes.isIndirectCause(event);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void triggerEvent(EventType event, Object eventArg) {
        ExceptionHelper.checkNotNullArgument(event, "event");

        EventHandlerContainer<GeneralEventListener<EventType>> currentListeners;
        currentListeners = listeners.get(event);
        if (currentListeners == null) {
            return;
        }

        EventCauses<EventType> previousCauses = currentCauses.get();
        if (previousCauses != null) {
            if (isCause(previousCauses, event)) {
                return;
            }
        }

        try {
            EventCauses<EventType> newCauses = getCurrentCauses(event);
            currentCauses.set(newCauses);
            currentListeners.onEvent(
                    new InternalDispatcher<>(newCauses, eventArg));
        } finally {
            if (previousCauses != null) {
                currentCauses.set(previousCauses);
            }
            else {
                currentCauses.remove();
            }
        }

    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void registerListener(EventType event, GeneralEventListener<EventType> listener) {
        ExceptionHelper.checkNotNullArgument(event, "event");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        boolean registered;
        do {
            EventHandlerContainer<GeneralEventListener<EventType>> currentListeners;
            currentListeners = listeners.get(event);
            if (currentListeners == null) {
                EventHandlerContainer<GeneralEventListener<EventType>> newListeners;
                newListeners = new LifoEventHandlerContainer<>();
                currentListeners = listeners.putIfAbsent(event, newListeners);
                if (currentListeners == null) {
                    currentListeners = newListeners;
                }
            }

            registerLock.lock();
            try {
                registered = listeners.get(event) == currentListeners;
                if (registered) {
                    currentListeners.registerListener(listener);
                }
            } finally {
                registerLock.unlock();
            }
        } while (!registered);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void unregisterListener(EventType event, GeneralEventListener<EventType> listener) {
        ExceptionHelper.checkNotNullArgument(event, "event");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        EventHandlerContainer<GeneralEventListener<EventType>> currentListeners;
        currentListeners = listeners.get(event);

        if (currentListeners != null) {
            registerLock.lock();
            try {
                currentListeners.removeListener(listener);
                if (currentListeners.getListenerCount() == 0) {
                    Object prevListeners = listeners.remove(event);
                    assert prevListeners == currentListeners;
                }
            } finally {
                registerLock.unlock();
            }
        }
    }

    private void setCauseAndRun(EventCauses<EventType> cause, Runnable command) {
        EventCauses<EventType> prevCause = currentCauses.get();
        try {
            currentCauses.set(cause);
            command.run();
        } finally {
            if (prevCause != null) {
                currentCauses.set(prevCause);
            }
            else {
                currentCauses.remove();
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Executor createTrackedExecutor(final Executor wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

        return new Executor() {
            @Override
            public void execute(Runnable command) {
                EventCauses<EventType> cause = currentCauses.get();
                wrapped.execute(new TaskWrapperRunnable(cause, command));
                setCauseAndRun(cause, command);
            }
        };
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ExecutorService createTrackedExecutor(ExecutorService wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

        return new TaskWrapperExecutor(wrapped) {
            @Override
            protected <V> Callable<V> wrapTask(final Callable<V> task) {
                ExceptionHelper.checkNotNullArgument(task, "task");

                EventCauses<EventType> cause = currentCauses.get();
                return new TaskWrapperCallable<>(cause, task);
            }

            @Override
            protected Runnable wrapTask(final Runnable task) {
                ExceptionHelper.checkNotNullArgument(task, "task");

                EventCauses<EventType> cause = currentCauses.get();
                return new TaskWrapperRunnable(cause, task);
            }
        };
    }

    private class TaskWrapperRunnable implements Runnable {
        private final EventCauses<EventType> cause;
        private final Runnable task;

        public TaskWrapperRunnable(EventCauses<EventType> cause, Runnable task) {
            assert task != null;

            this.cause = cause;
            this.task = task;
        }

        @Override
        public void run() {
            EventCauses<EventType> prevCause = currentCauses.get();
            try {
                currentCauses.set(cause);
                task.run();
            } finally {
                if (prevCause != null) {
                    currentCauses.set(prevCause);
                }
                else {
                    currentCauses.remove();
                }
            }
        }
    }

    private class TaskWrapperCallable<V> implements Callable<V> {
        private final EventCauses<EventType> cause;
        private final Callable<V> task;

        public TaskWrapperCallable(EventCauses<EventType> cause, Callable<V> task) {
            assert task != null;

            this.cause = cause;
            this.task = task;
        }

        @Override
        public V call() throws Exception {
            EventCauses<EventType> prevCause = currentCauses.get();
            try {
                currentCauses.set(cause);
                return task.call();
            } finally {
                if (prevCause != null) {
                    currentCauses.set(prevCause);
                }
                else {
                    currentCauses.remove();
                }
            }
        }
    }

    private static class InternalDispatcher<EventType>
    implements
            EventDispatcher<GeneralEventListener<EventType>> {

        private final EventCauses<EventType> causes;
        private final Object eventArg;

        public InternalDispatcher(EventCauses<EventType> causes, Object eventArg) {
            this.causes = causes;
            this.eventArg = eventArg;
        }

        @Override
        public void onEvent(GeneralEventListener<EventType> eventListener) {
            eventListener.onEvent(causes, eventArg);
        }
    }

    private static class ChainedCause<EventType> implements EventCauses<EventType> {
        private final EventType directCause;
        private final EventCauses<EventType> previousCauses;

        public ChainedCause(EventType directCause, EventCauses<EventType> previousCauses) {
            this.directCause = directCause;
            this.previousCauses = previousCauses;
        }

        @Override
        public boolean isIndirectCause(EventType event) {
            return Objects.equals(event, previousCauses.getDirectCause())
                    || previousCauses.isIndirectCause(event);
        }

        @Override
        public EventType getDirectCause() {
            return directCause;
        }

    }

    private static class SingleCause<EventType> implements EventCauses<EventType> {
        private final EventType directCause;

        public SingleCause(EventType directCause) {
            this.directCause = directCause;
        }

        @Override
        public boolean isIndirectCause(Object event) {
            return false;
        }

        @Override
        public EventType getDirectCause() {
            return directCause;
        }
    }

    private static abstract class TaskWrapperExecutor implements ExecutorService {
        private final ExecutorService wrapped;

        public TaskWrapperExecutor(ExecutorService wrapped) {
            ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

            this.wrapped = wrapped;
        }

        protected abstract <V> Callable<V> wrapTask(Callable<V> task);
        protected abstract Runnable wrapTask(Runnable task);

        private <T> Collection<Callable<T>> wrapManyTasks(Collection<? extends Callable<T>> tasks) {
            List<Callable<T>> result = new ArrayList<>(tasks.size());
            for (Callable<T> task: tasks) {
                result.add(wrapTask(task));
            }
            return result;
        }

        @Override
        public void shutdown() {
            wrapped.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return wrapped.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return wrapped.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return wrapped.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return wrapped.isTerminated();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return wrapped.submit(wrapTask(task));
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return wrapped.submit(wrapTask(task), result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return wrapped.submit(wrapTask(task));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return wrapped.invokeAll(wrapManyTasks(tasks));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return wrapped.invokeAll(wrapManyTasks(tasks), timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return wrapped.invokeAny(wrapManyTasks(tasks));
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return wrapped.invokeAny(wrapManyTasks(tasks), timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            wrapped.execute(wrapTask(command));
        }
    }
}
