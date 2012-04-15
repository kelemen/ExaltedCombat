package exaltedcombat.events;

/**
 * Defines the causing events leading to a specific event. The causes are
 * tracked by an {@link EventManager}. The causes are events themselves and
 * every event has zero or one direct cause. So causes form a list of events
 * leading to the last event.
 * <P>
 * Implementations of this interface are expected to be immutable in general
 * and be transparent to any synchronization.
 * <P>
 * Note that this interface will be redesigned in the future and be moved to the
 * jtrim library.
 *
 * @param <EventType> the type of the possible events. It is recommended
 *   (but not required) that the events themselves be an instance of an
 *   {@code enum}.
 *
 * @see EventManager
 * @author Kelemen Attila
 */
public interface EventCauses<EventType> {
    /**
     * Checks whether the specified event (without considering its argument)
     * is an indirect cause in this causality list. This method does not
     * consider the last cause returned by
     * {@link #getDirectCause() getDirectCause()}.
     *
     * @param event the event without its argument to be checked. This argument
     *   cannot be {@code null}
     * @return {@code true} if the specified event is an indirect cause in this
     *   causality list, {@code false} otherwise
     *
     * @throws NullPointerException implementations may throw this exception if
     *   the specified event is {@code null}
     *
     */
    public boolean isIndirectCause(EventType event);

    /**
     * Returns the last event in this causality list. Note that the event
     * returned by this method is not an indirect cause and is not considered
     * by the
     * {@link #isIndirectCause(java.lang.Object) isIndirectCause(EventType)}.
     *
     * @return the last event in this causality list or {@code null} if there
     *   was no cause at all
     */
    public EventType getDirectCause();
}
