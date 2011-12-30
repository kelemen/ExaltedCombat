package exaltedcombat.actions;

import java.io.*;

import resources.strings.*;

/**
 * Defines the action which was taken by a combat entity to enter the combat.
 * Usually if entering the combat occurred at zero
 * {@link #getActionTick() action tick} it means that it was during the first
 * join phase of the combat. Note that this is not strictly required because
 * it is technically possible to enter after the first join phase and still do
 * that at tick zero. Note that if the action tick is greater than zero, the
 * action must have occurred after the first join phase.
 * <P>
 * Besides the required properties this action also contains the number of
 * successes rolled on a join battle roll. Specifying a negative integer for it
 * means botching.
 * <P>
 * Instances of this class are immutable and transparent to any synchronization
 * as required by the {@link CombatEntityAction} interface.
 *
 * @author Kelemen Attila
 */
public final class EntityJoinCombatAction extends AbstractEntityAction {
    private static final long serialVersionUID = 2154003604372802223L;

    private static final LocalizedString CAPTION_FORMAT = StringContainer.getDefaultString("JOIN_COMBAT_ACTION_CAPTION");
    private static final LocalizedString BOTCH = StringContainer.getDefaultString("ROLL_BOTCH");

    private final int joinCombatRoll;
    private final String caption;

    /**
     * Initializes this entity action with the given action tick value,
     * user description and join combat roll. The provided values will be
     * returned by the appropriate methods. Note that the action tick does not
     * necessarily equals to the tick of the first action of the entity but the
     * time when she/he entered the combat.
     *
     * @param actionTick the value to be returned by the
     *   {@link #getActionTick() getActionTick()} method. This argument cannot
     *   be a negative integer.
     * @param userDescription the description provided by the user. This string
     *   will be returned by the {@link #getUserDescription() getUserDescription()}
     *   method, so it cannot be {@code null}.
     * @param joinCombatRoll the number of success rolled on a join battle roll.
     *   A negative value means botching and the user may specify a negative
     *   integer which absolute value equals the number of ones rolled
     *   (e.g.: -2 means that two ones were rolled and no successes). This is
     *   not strictly required, it is the decision of the user to use this value
     *   this way. However negative values always mean a botched roll.
     *
     * @throws IllegalArgumentException thrown if {@code actionTick} is a
     *   negative integer
     * @throws NullPointerException thrown if the provided user description is
     *   {@code null}
     */
    public EntityJoinCombatAction(int actionTick,
            String userDescription,
            int joinCombatRoll) {
        super(actionTick, userDescription);
        this.joinCombatRoll = joinCombatRoll;
        this.caption = CAPTION_FORMAT.format(getRollValue(joinCombatRoll));
    }

    /**
     * Returns the number of successes rolled on a join battle roll. Negative
     * value means a botched roll. By the decision of the user (the creator of
     * this instance) if botching the absolute value can mean the number of
     * ones rolled but this is not strictly required.
     *
     * @return the number of successes rolled on a join battle roll and a
     *   negative integer if the roll was botched
     */
    public int getJoinCombatRoll() {
        return joinCombatRoll;
    }

    private static Object getRollValue(int roll) {
        return roll >= 0
                ? Integer.valueOf(roll)
                : BOTCH.format(Math.abs((long)roll)); // it cannot overflow even in case of Integer.MIN_VALUE
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getPresentationCaption() {
        return caption;
    }

    private Object writeReplace() {
        return new Format1(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("This object cannot be deserialized directly.");
    }

    private static class Format1 implements Serializable {
        private static final long serialVersionUID = -8113895743701746161L;

        private final AbstractContent abstractContent;
        private final int joinCombatRoll;

        public Format1(EntityJoinCombatAction action) {
            this.abstractContent = action.getAbstractContent();
            this.joinCombatRoll = action.getJoinCombatRoll();
        }

        private Object readResolve() throws InvalidObjectException {
            return new EntityJoinCombatAction(
                    abstractContent.getActionTick(),
                    abstractContent.getUserDescription(),
                    joinCombatRoll);
        }
    }
}
