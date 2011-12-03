package exaltedcombat.actions;

import exaltedcombat.utils.*;

import java.io.*;

import org.jtrim.utils.*;

import resources.strings.*;

/**
 * A convenient base class for implementing the {@link CombatEntityAction}
 * interface. This class implements the {@link #getActionTick() getActionTick()}
 * and the {@link #getUserDescription() getUserDescription()} methods adhering
 * to their contracts. This class also contains a default overridable
 * implementation for the {@link #getPresentationText() getPresentationText()}
 * method.
 * <P>
 * Note that none of the states of {@code AbstractEntityAction} can be changed,
 * so this class can be a base class for immutable implementations.
 * <P>
 * For subclasses to be serializable neeed to retrieve the object returned by
 * the {@link #getAbstractContent() getAbstractContent()} method and serialize
 * the {@link AbstractContent} object. To deserialize use the protected
 * {@link #AbstractEntityAction(AbstractEntityAction.AbstractContent) AbstractEntityAction(AbstractEntityAction.AbstractContent)}
 * constructor with the serialized {@code AbstractContent}.
 *
 * @author Kelemen Attila
 */
@SuppressWarnings("serial")
public abstract class AbstractEntityAction implements CombatEntityAction {
    private static final LocalizedString OCCURED_AT_TICK = StringContainer.getDefaultString("OCCURED_AT_TICK");
    private static final LocalizedString USER_DESCR_TEXT_CAPTION = StringContainer.getDefaultString("USER_DESCR_TEXT_CAPTION");

    private final int actionTick;
    private final String userDescription;

    /**
     * Initializes this entity action with the given action tick value and
     * user description. The provided values will be returned by the appropriate
     * methods.
     *
     * @param actionTick the value to be returned by the
     *   {@link #getActionTick() getActionTick()} method. This argument cannot
     *   be a negative integer.
     * @param userDescription the description provided by the user. This string
     *   will be returned by the {@link #getUserDescription() getUserDescription()}
     *   method, so it cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code actionTick} is a
     *   negative integer
     * @throws NullPointerException thrown if the provided user description is
     *   {@code null}
     */
    public AbstractEntityAction(int actionTick, String userDescription) {
        ExceptionHelper.checkArgumentInRange(actionTick, 0, Integer.MAX_VALUE, "actionTick");
        ExceptionHelper.checkNotNullArgument(userDescription, "userDescription");

        this.actionTick = actionTick;
        this.userDescription = userDescription;
    }

    /**
     * Initializes this entity action with the given action tick value and
     * user description provided by an {@code AbstractContent}. This constructor
     * is provided for convenience to be used when the class is deserialized.
     * <P>
     * This constructor is equivalent to:
     * {@code AbstractEntityAction(content.getActionTick(), content.getUserDescription())}.
     *
     * @param content the abstract content containing the action tick and
     *   the user defined description. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the provided argument is
     *   {@code null}
     */
    protected AbstractEntityAction(AbstractContent content) {
        this(content.actionTick, content.userDescription);
    }

    /**
     * Returns all the information stored by {@code AbstractEntityAction} in
     * a serializable object. This returned object must be used by subclasses
     * to be able to serialize/deserialize themselves.
     *
     * @return all the information stored by {@code AbstractEntityAction} in
     *   a serializable object. This method never returns {@code null}.
     */
    protected final AbstractContent getAbstractContent() {
        return new AbstractContent(this);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final int getActionTick() {
        return actionTick;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public final String getUserDescription() {
        return userDescription;
    }

    /**
     * Returns a string containing the {@link #getPresentationCaption() caption},
     * {@link #getActionTick() action tick} and the user defined description.
     * The string is localized except for the user defined string which is
     * contained in the returned string in an unmodified state.
     *
     * @return a localized description of this entity action as described in the
     *   documentation of this method. This method never returns {@code null}.
     */
    @Override
    public String getPresentationText() {
        StringBuilder result = new StringBuilder(256);

        result.append(getPresentationCaption());
        result.append("\n");

        int displayedTick = getActionTick() + ExaltedConsts.TICK_OFFSET;
        result.append(OCCURED_AT_TICK.format(displayedTick));

        String userDescr = getUserDescription();
        if (!"".equals(userDescr)) {
            result.append("\n\n");
            result.append(USER_DESCR_TEXT_CAPTION.toString());
            result.append("\n\n");
            result.append(userDescr);
        }

        return result.toString();
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("This abstract class cannot be deserialized.");
    }

    /**
     * Defines a serializable class storing all the information stored by
     * {@code AbstractEntityAction}. From instances of this class
     * {@code AbstractEntityAction} can be completely restored (not counting
     * data added by subclasses).
     * <P>
     * Instances of this class are immutable (therefore thread-safe) and
     * transparent to any synchronization.
     */
    protected static final class AbstractContent implements Serializable {
        private static final long serialVersionUID = -8435041501732038625L;

        private final int actionTick;
        private final String userDescription;

        private AbstractContent(AbstractEntityAction abstractAction) {
            this(abstractAction.actionTick, abstractAction.userDescription);
        }

        private AbstractContent(int actionTick, String userDescription) {
            ExceptionHelper.checkArgumentInRange(actionTick, 0, Integer.MAX_VALUE, "actionTick");
            ExceptionHelper.checkNotNullArgument(userDescription, "userDescription");

            this.actionTick = actionTick;
            this.userDescription = userDescription;
        }

        /**
         * Returns the action tick as defined by
         * {@link CombatEntityAction#getActionTick()}.
         *
         * @return the action tick. This method never returns a negative
         *   integer.
         */
        public int getActionTick() {
            return actionTick;
        }

        /**
         * Returns the user description as defined by
         * {@link CombatEntityAction#getUserDescription()}.
         *
         * @return the description provided by the user. This method never
         *   returns {@code null}.
         */
        public String getUserDescription() {
            return userDescription;
        }

        private Object writeReplace() {
           return new Format1(this);
        }

        private void readObject(ObjectInputStream stream) throws InvalidObjectException {
            throw new InvalidObjectException("This abstract class cannot be deserialized.");
        }

        private static class Format1 implements Serializable {
            private static final long serialVersionUID = 1921669388410599406L;

            private final int actionTick;
            private final String userDescription;

            public Format1(AbstractContent content) {
                this.actionTick = content.actionTick;
                this.userDescription = content.userDescription;
            }

            private Object readResolve() throws InvalidObjectException {
                return new AbstractContent(actionTick, userDescription);
            }
        }
    }
}
