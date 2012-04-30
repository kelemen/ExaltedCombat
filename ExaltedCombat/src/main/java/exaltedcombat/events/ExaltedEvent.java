package exaltedcombat.events;

import org.jtrim.event.EventTracker;
import org.jtrim.event.TrackedEventListener;
import org.jtrim.utils.ExceptionHelper;

/**
 * The base interface for every event kind in ExaltedCombat.
 * <P>
 * Events in ExaltedCombat are expected to implement this interface.
 *
 * @param <ArgType> the type of the event argument of this kind of event
 *
 * @author Kelemen Attila
 */
public interface ExaltedEvent<ArgType> {
    /**
     * Returns the type of the event argument of this kind of event.
     *
     * @return the type of the event argument of this kind of event. This method
     *   never returns {@code null}.
     */
    public Class<ArgType> getArgClass();

    /**
     * Contains the static helper method
     * {@link #register(EventTracker, ExaltedEvent, TrackedEventListener)}
     * which helps registering listeners to an {@code EventTracker}.
     */
    public class Helper {
        /**
         * A convenient helper method to retrieve the appropriate
         * {@code TrackedListenerManager} and register the specified listener
         * with it.
         *
         * @param <ArgType> the type of the event argument which is used by
         *   the {@code TrackedListenerManager} to which the listener is
         *   registered with
         * @param eventTracker the {@code EventTracker} from which the
         *   {@code TrackedListenerManager} is retrieved to register the
         *   specified listener. This argument cannot be {@code null}.
         * @param eventKind the kind of event used to retrieve the
         *   {@code TrackedListenerManager} to which the listener is
         *   registered with. This argument cannot be {@code null}.
         * @param eventListener the listener which is to be notified of the
         *   specified kind of event. This argument cannot be {@code null}.
         *
         * @throws NullPointerException thrown if any of the arguments is
         *   {@code null}
         */
        public static <ArgType> void register(
                EventTracker eventTracker,
                ExaltedEvent<ArgType> eventKind,
                TrackedEventListener<ArgType> eventListener) {
            eventTracker.getManagerOfType(eventKind, eventKind.getArgClass())
                    .registerListener(eventListener);
        }

        /**
         * A convenient helper method to trigger an event using the appropriate
         * {@code TrackedListenerManager}. That is, this method requests
         * the appropriate {@code TrackedListenerManager} from the
         * {@code EventTracker} based on the event kind and invokes its
         * {@code onEvent} method.
         *
         * @param <ArgType> the type of the event argument which is used by
         *   the {@code TrackedListenerManager} and to be used to trigger the
         *   event
         * @param eventTracker the {@code EventTracker} from which the
         *   {@code TrackedListenerManager} is retrieved to trigger the event.
         *   This argument cannot be {@code null}.
         * @param eventKind the kind of event used to retrieve the
         *   {@code TrackedListenerManager} to trigger the event. This argument
         *   cannot be {@code null}.
         * @param eventArgument the event argument to be passed to the
         *   {@code onEvent} method. This argument can be {@code null} if this
         *   kind of event allows {@code null} event arguments.
         *
         * @throws NullPointerException thrown if {@code eventTracker} or
         *   {@code eventKind} is {@code null}
         */
        public static <ArgType> void triggerEvent(
                EventTracker eventTracker,
                ExaltedEvent<ArgType> eventKind,
                ArgType eventArgument) {
            eventTracker.getManagerOfType(eventKind, eventKind.getArgClass())
                    .onEvent(eventArgument);
        }

        /**
         * Creates a new {@code ExaltedEvent} instance which does not equal to
         * any other {@code ExaltedEvent} instance and has the given event
         * argument type.
         *
         * @param <ArgType> the type of the event argument allowed for this kind
         *   of event
         * @param argType the class of the event argument allowed for this kind
         *   of event. This argument cannot be {@code null}.
         * @return the new {@code ExaltedEvent} instance which does not equal to
         *   any other {@code ExaltedEvent} instance and has the given event
         *   argument type. This method never returns {@code null}.
         *
         * @throws NullPointerException thrown if the specified class is
         *   {@code null}
         */
        public static <ArgType> ExaltedEvent<ArgType> createExaltedEvent(
                final Class<ArgType> argType) {
            ExceptionHelper.checkNotNullArgument(argType, "argType");

            return new ExaltedEvent<ArgType>() {
                @Override
                public Class<ArgType> getArgClass() {
                    return argType;
                }
            };
        }
    }
}
