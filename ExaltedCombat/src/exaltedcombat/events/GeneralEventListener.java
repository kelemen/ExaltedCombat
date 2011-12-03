package exaltedcombat.events;

/**
 * The interface of event handlers in {@link EventManager}s. The events are
 * triggered by a {@link EventManager#triggerEvent(Object, Object) EventManager.triggerEvent}
 * call and the {@link #onEvent(exaltedcombat.events.EventCauses, Object) onEvent}
 * method is called synchronously on the calling thread of {@code EventManager.triggerEvent}.
 * <P>
 * In general there is no restriction on what thread the event handler can
 * be invoked. However in certain cases it is expected that the event handlers
 * be invoked from a certain context. In a gui related EventManager it is
 * usually expected that the {@code EventManager.triggerEvent} method be called
 * from the AWT event dispatching thread.
 * <P>
 * Note that this interface will be redesigned in the future and be moved to the
 * jtrim library.
 *
 * @param <EventType> the type of the possible events including the possible
 *   causes of event this event handler expects
 *
 * @see EventManager
 * @author Kelemen Attila
 */
public interface GeneralEventListener<EventType> {
    /**
     * The method to be called when a certain event has been triggered.
     * This method is called from within a
     * {@link EventManager#triggerEvent(Object, Object) EventManager.triggerEvent}
     * method call.
     *
     * @param causes the causes of this current event. These events occured
     *   before the current event leading to the call to this event. This
     *   argument cannot be {@code null}.
     * @param eventArg a user defined argument of this event. This argument
     *   was passed to the
     *   {@link EventManager#triggerEvent(Object, Object) EventManager.triggerEvent}
     *   method. This argument can be {@code null} if the definition of the
     *   current event allows {@code null} argument.
     */
    public void onEvent(EventCauses<EventType> causes, Object eventArg);
}
