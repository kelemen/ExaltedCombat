package exaltedcombat.events;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Forwards events to listeners and keeps track of which event was caused
 * by which. The event manager can only keep track of events triggered by
 * a {@link #triggerEvent(java.lang.Object, java.lang.Object) triggerEvent(EventType, Object)}
 * method call on the same event manager. Every implementation is required to
 * be able to detect that the {@code triggerEvent} method was called by a
 * {@link #registerListener(java.lang.Object, exaltedcombat.events.GeneralEventListener) registered}
 * event handler method. In this case ({@code triggerEvent} was called by the
 * registered event handler method) the newly triggered event is said to be
 * caused by the event which the registered event handler now handles.
 * <P>
 * Note that sometimes an event handler does not directly invoke
 * {@code triggerEvent} but calls it in an asynchronously (or simply executed
 * later) executed task. To be able to keep track of the causality even in
 * this case use one of the {@code createTrackedExecutor} methods.
 * <P>
 * Implementations of this interface are required to be safe to call from
 * multiple threads concurrently but they are not required to transparent to
 * their synchronization.
 * <P>
 * Note that this interface will be redesigned in the future and be moved to the
 * jtrim library.
 *
 * @param <EventType> the type of the possible events. It is recommended
 *   (but not required) that the events themselves be an instance of an
 *   {@code enum}.
 *
 * @see LocalEventManager
 * @see RecursionStopperEventManager
 * @author Kelemen Attila
 */
public interface EventManager<EventType> {
    /**
     * Invokes the registered event handlers with the specified event arguments.
     * The event handlers are invoked synchronously on the current thread. In
     * case a called event handler registers a new handler in this event, the
     * newly registered handler will not be notified during this event.
     * <P>
     * In case an event handler throws an unchecked exception this method will
     * throw that exception. If multiple event handlers throw exceptions, the
     * first will be thrown and the others will be
     * {@link Throwable#addSuppressed(java.lang.Throwable) suppressed}.
     *
     * @param event the event to be triggered. Event handler registered to this
     *   event will be notified. This argument cannot be {@code null}.
     * @param eventArg the argument associated with the specified event. The
     *   specified event may define the type of this argument, however this
     *   method is unable verify that the type of this argument is correct.
     *   This argument can be {@code null} if it is allowed for the triggered
     *   event.
     *
     * @throws NullPointerException thrown if the specified event is
     *   {@code null}
     */
    public void triggerEvent(EventType event, Object eventArg);

    /**
     * Registers an event handler to listen for a specific event. The event
     * handler will be notified when a
     * {@link #triggerEvent(java.lang.Object, java.lang.Object) triggerEvent}
     * is invoked with the specified event.
     * <P>
     * If the same listener is registered to the same event twice without
     * unregistering, the following two behaviours are allowed:
     * <ul>
     *  <li>The method call is silently ignored.</li>
     *  <li>The listener will be registered twice and will be
     *   called twice if the specified event is triggered.</li>
     * </ul>
     * So to avoid confusion clients are advised against registering
     * the same listener multiple times.
     * <P>
     * The registered event handler should be removed when no longer needed to
     * avoid unnecessary object retention. A registered event handler can be
     * removed by a {@link #unregisterListener(java.lang.Object, exaltedcombat.events.GeneralEventListener) unregisterListener}
     * method call.
     *
     * @param event the event of which the event handler is notified of. This
     *   argument cannot be {@code null}.
     * @param listener the event handler which will be notified when the
     *   specified event occur due to a {@code triggerEvent} method call. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public void registerListener(EventType event, GeneralEventListener<EventType> listener);

    /**
     * Removes a previously
     * {@link #registerListener(java.lang.Object, exaltedcombat.events.GeneralEventListener) registered}
     * event handler.
     * <P>
     * The equality of event handlers are determined by reference
     * comparison (==). If the implementation allows registering a listener
     * multiple times, it will remove the given listener exactly once. Note that
     * in case the listener was not registered (or was already unregistered)
     * this method returns silently, doing nothing.
     *
     * @param event the event to no longer be notified of through the given
     *   event handler. This argument cannot be {@code null}.
     * @param listener the listener no longer notified when the specified event
     *   occurs. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public void unregisterListener(EventType event, GeneralEventListener<EventType> listener);

    /**
     * Creates an {@link Executor Executor} which delegates tasks to the
     * specified executor but remebers the causing events. The tasks submitted
     * to the returned executor will have the same causing events as the ones
     * causing submitting the task.
     *
     * @param wrapped the {@code Executor} to which the returned executor
     *   delegates tasks. This argument cannot be {@code null}.
     * @return the {@code Executor} which will remember the causing events when
     *   submitting tasks. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the argument specified is
     *   {@code null}
     */
    public Executor createTrackedExecutor(Executor wrapped);

    /**
     * Creates an {@link ExecutorService ExecutorService} which delegates tasks
     * to the specified executor but remebers the causing events. The tasks
     * submitted to the returned executor will have the same causing events as
     * the ones causing submitting the task.
     *
     * @param wrapped the {@code ExecutorService} to which the returned executor
     *   delegates tasks. This argument cannot be {@code null}.
     * @return the {@code ExecutorService} which will remember the causing
     *   events when submitting tasks. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the argument specified is
     *   {@code null}
     */
    public ExecutorService createTrackedExecutor(ExecutorService wrapped);
}
