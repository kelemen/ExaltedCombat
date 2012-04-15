package exaltedcombat.actions;

import exaltedcombat.utils.ExaltedConsts;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import org.jtrim.utils.ExceptionHelper;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

/**
 * Defines a generic action which was taken by a combat entity. In more general
 * sense: The entity was delayed by a certain amount of ticks. Two additional
 * properties define this action: the start tick and the speed. Start tick means
 * the tick when the next action of the entity would have occurred, the speed
 * is the speed of the action which was taken (i.e.: the entity was delayed
 * by {@code speed} number of ticks).
 * <P>
 * Note that usually an entity acts while she/he is in the current tick of the
 * combat, there are a few exceptions however such as the water based effect
 * of the dragon graced weapon can delay anyone.
 * <P>
 * Instances of this class are immutable and transparent to any synchronization
 * as required by the {@link CombatEntityAction} interface.
 *
 * @author Kelemen Attila
 */
public final class EntityMoveAction extends AbstractEntityAction {
    private static final long serialVersionUID = -987653882642499274L;

    private static final LocalizedString CAPTION_FORMAT = StringContainer.getDefaultString("MOVE_ACTION_CAPTION");
    private static final LocalizedString OCCURRED_AT_TICK = StringContainer.getDefaultString("OCCURRED_AT_TICK");
    private static final LocalizedString USER_DESCR_TEXT_CAPTION = StringContainer.getDefaultString("USER_DESCR_TEXT_CAPTION");
    private static final LocalizedString TICK_STR = StringContainer.getDefaultString("TICK_STR");
    private static final LocalizedString MOVE_TICK_TO_TICK_TEXT = StringContainer.getDefaultString("MOVE_TICK_TO_TICK_TEXT");

    private final int startTick;
    private final int speed;
    private final String caption;

    /**
     * Initializes this entity action with the given action tick value,
     * user description, start tick and speed. The provided values will be
     * returned by the appropriate methods. Note that the action tick does not
     * necessarily equals to the start tick but the time when she/he was delayed
     * in the combat.
     *
     * @param actionTick the value to be returned by the
     *   {@link #getActionTick() getActionTick()} method. This argument cannot
     *   be a negative integer.
     * @param userDescription the description provided by the user. This string
     *   will be returned by the {@link #getUserDescription() getUserDescription()}
     *   method, so it cannot be {@code null}.
     * @param startTick the tick when the next action of the entity would have
     *   occurred. This argument cannot be negative.
     * @param speed the speed of the action taken. This argument can be negative
     *   to allow manual corrections. Note that although rules does not allow
     *   a speed to be greater than 6, this argument is explicitly allowed to
     *   exceed this limit.
     *
     * @throws IllegalArgumentException thrown if {@code startTick} is a
     *   negative integer or {@code startTick + speed} is a negative integer
     *   which would result in the entity being on a negative tick
     * @throws NullPointerException thrown if the provided user description is
     *   {@code null}
     */
    public EntityMoveAction(int actionTick,
            String userDescription,
            int startTick,
            int speed) {

        super(actionTick, userDescription);

        ExceptionHelper.checkArgumentInRange(startTick, 0, Integer.MAX_VALUE, "startTick");
        ExceptionHelper.checkArgumentInRange(startTick + speed, 0, Integer.MAX_VALUE, "startTick + speed");

        this.startTick = startTick;
        this.speed = speed;
        this.caption = CAPTION_FORMAT.format(speed);
    }

    /**
     * Returns the speed of the action taken. This is the number of ticks
     * required to do the specified action. Note that this value can be
     * negative to allow manual corrections.
     *
     * @return the speed of the action taken
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Returns the tick when the next action of the entity would have occurred.
     * This value cannot be negative.
     *
     * @return the tick when the next action of the entity would have occurred.
     *   This method never returns a negative integer.
     */
    public int getStartTick() {
        return startTick;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getPresentationCaption() {
        return caption;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Note that the returned string will also contain the speed and the
     * start tick of this action.
     */
    @Override
    public String getPresentationText() {
        StringBuilder result = new StringBuilder(256);

        result.append(getPresentationCaption());
        result.append("\n");

        int displayedTick = getActionTick() + ExaltedConsts.TICK_OFFSET;
        result.append(OCCURRED_AT_TICK.format(displayedTick));
        result.append("\n");

        String tick0 = TICK_STR.format(getStartTick() + ExaltedConsts.TICK_OFFSET);
        String tick1 = TICK_STR.format(getStartTick() + getSpeed() + ExaltedConsts.TICK_OFFSET);
        result.append(MOVE_TICK_TO_TICK_TEXT.format(tick0, tick1));

        String userDescr = getUserDescription();
        if (!"".equals(userDescr)) {
            result.append("\n\n");
            result.append(USER_DESCR_TEXT_CAPTION.toString());
            result.append("\n\n");
            result.append(userDescr);
        }

        return result.toString();
    }

    private Object writeReplace() {
        return new Format1(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("This object cannot be deserialized directly.");
    }

    private static class Format1 implements Serializable {
        private static final long serialVersionUID = -5936294303333624926L;

        private final AbstractContent abstractContent;
        private final int startTick;
        private final int speed;

        public Format1(EntityMoveAction action) {
            this.abstractContent = action.getAbstractContent();
            this.startTick = action.startTick;
            this.speed = action.speed;
        }

        private Object readResolve() throws InvalidObjectException {
            return new EntityMoveAction(
                    abstractContent.getActionTick(),
                    abstractContent.getUserDescription(),
                    startTick,
                    speed);
        }
    }
}
