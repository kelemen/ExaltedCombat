package exaltedcombat.save;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a serializable class storing the basic properties of an entity
 * participating in a combat and its history of actions. This class is an
 * extension of the {@link SavedEntityInfo} class.
 * <P>
 * Note that this class is completely immutable so instances of this class
 * are safe to be shared across multiple threads concurrently.
 * <P>
 * The serialization of this class is intended to be kept backward compatible,
 * so the serialized form of this class is appropriate for long term storage.
 *
 * @see exaltedcombat.models.impl.CombatEntity CombatEntity
 * @author Kelemen Attila
 */
public final class SavedCombatEntity implements Serializable {
    private static final long serialVersionUID = 4996906677693866299L;

    private final SavedActiveEntityInfo activeInfo;
    private final int tick;
    private final int preStartRoll;

    /**
     * Creates serializable instance storing the properties and history of
     * actions of an entity (given a {@link SavedActiveEntityInfo} instance) and
     * its current position in the combat.
     *
     * @param activeInfo the object storing the basic properties and the history
     *   of actions of the entity. This argument cannot be {@code null}.
     * @param tick the tick when the given entity were to act first in the
     *   combat. This argument must be greater than or equal to zero.
     * @param preStartRoll the number of successes rolled by an entity on the
     *   join combat roll in the
     *   {@link exaltedcombat.models.CombatState#JOIN_PHASE join phase} of the
     *   combat. Specify zero if the entity joined in the combat phase of the
     *   combat. See {@link exaltedcombat.models.CombatModel#getPreJoinRoll(Object) CombatModel.getPreJoinRoll(EntityType)}
     *   for further details on this value.
     *
     * @throws NullPointerException thrown if {@code activeInfo} is {@code null}
     */
    public SavedCombatEntity(SavedActiveEntityInfo activeInfo, int tick, int preStartRoll) {
        ExceptionHelper.checkNotNullArgument(activeInfo, "activeInfo");
        ExceptionHelper.checkArgumentInRange(tick, 0, Integer.MAX_VALUE, "tick");

        this.activeInfo = activeInfo;
        this.tick = tick;
        this.preStartRoll = preStartRoll;
    }

    /**
     * Returns a serializable object storing the basic properties and the
     * history of actions of the entity.
     *
     * @return a serializable object storing the basic properties and the
     *   history of actions of the entity. This method never returns
     *   {@code null}.
     */
    public SavedActiveEntityInfo getActiveInfo() {
        return activeInfo;
    }

    /**
     * Returns the number of successes rolled by an entity on the join combat
     * roll in the {@link exaltedcombat.models.CombatState#JOIN_PHASE join phase}
     * of the combat.
     *
     * @return the number of successes rolled by an entity on the join combat
     *   roll in the
     *   {@link exaltedcombat.models.CombatState#JOIN_PHASE join phase} of the
     *   combat. Returns a negative integer a botched roll and zero if the
     *   entity joined after the join phase of the combat.
     *
     * @see exaltedcombat.models.CombatModel#getPreJoinRoll(Object) CombatModel.getPreJoinRoll(EntityType)
     */
    public int getPreStartRoll() {
        return preStartRoll;
    }

    /**
     * Returns the tick when the given entity were to act first in the combat.
     *
     * @return the tick when the given entity were to act first in the combat.
     *   This methods always returns a value greater than or equals to zero.
     */
    public int getTick() {
        return tick;
    }

    private Object writeReplace() {
        return new Format1(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("This object cannot be deserialized directly.");
    }

    private static class Format1 implements Serializable {
        private static final long serialVersionUID = -2677822764593298396L;

        private final SavedActiveEntityInfo activeInfo;
        private final int tick;
        private final int preStartRoll;

        public Format1(SavedCombatEntity info) {
            this.activeInfo = info.activeInfo;
            this.tick = info.tick;
            this.preStartRoll = info.preStartRoll;

        }

        private Object readResolve() throws InvalidObjectException {
            return new SavedCombatEntity(activeInfo, tick, preStartRoll);
        }
    }
}
