package exaltedcombat.actions;

import java.io.*;

import resources.strings.*;

/**
 * Defines the action which was taken by a combat entity to leave the combat.
 * Leaving combat can occur for various reasons like death, fleeing, etc.
 * <P>
 * Instances of this class are immutable and transparent to any synchronization
 * as required by the {@link CombatEntityAction} interface.
 *
 * @author Kelemen Attila
 */
public final class EntityLeaveCombatAction extends AbstractEntityAction {
    private static final long serialVersionUID = -8937110781707600462L;

    private static final LocalizedString CAPTION_FORMAT = StringContainer.getDefaultString("LEAVE_COMBAT_ACTION_CAPTION");

    /**
     * Initializes this entity action with the given action tick value and
     * user description. The provided values will be returned by the appropriate
     * methods. Note that the action tick does not necessarily equals to the
     * tick when the entity next turn would have been prior leaving the combat
     * but the time when she/he did.
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
    public EntityLeaveCombatAction(int actionTick, String userDescription) {
        super(actionTick, userDescription);
    }

    private EntityLeaveCombatAction(AbstractContent content) {
        super(content);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getPresentationCaption() {
        return CAPTION_FORMAT.toString();
    }

    private Object writeReplace() {
        return new Format1(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("This object cannot be deserialized directly.");
    }

    private static class Format1 implements Serializable {
        private static final long serialVersionUID = 6841020651002122986L;

        private final AbstractContent abstractContent;

        public Format1(EntityLeaveCombatAction action) {
            this.abstractContent = action.getAbstractContent();
        }

        private Object readResolve() throws InvalidObjectException {
            return new EntityLeaveCombatAction(abstractContent);
        }
    }
}
