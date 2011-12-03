package exaltedcombat.save;

import exaltedcombat.actions.*;
import exaltedcombat.models.impl.*;

import java.io.*;
import java.util.*;

import org.jtrim.utils.*;

/**
 * Defines a serializable class storing the basic properties of an entity and
 * its history of actions. This class is an extension of the
 * {@link SavedEntityInfo} class.
 * <P>
 * Note that this class is completely immutable (assuming
 * {@link CombatEntityAction actions} are immutable) so instances of this class
 * are safe to be shared across multiple threads concurrently.
 * <P>
 * The serialization of this class is intended to be kept backward compatible,
 * so the serialized form of this class is appropriate for long term storage.
 *
 * @see CombatEntity
 * @author Kelemen Attila
 */
public final class SavedActiveEntityInfo implements Serializable {
    private static final long serialVersionUID = 5754054511925955187L;

    private final SavedEntityInfo entityInfo;
    private final List<CombatEntityAction> previousActions;

    /**
     * Creates serializable instance storing the properties of an entity and its
     * history of actions.
     *
     * @param entity the entity whose properties are to be copied. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public SavedActiveEntityInfo(CombatEntity entity) {
        this(new SavedEntityInfo(entity), entity.getPreviousActions());
    }

    /**
     * Creates serializable instance storing the properties of an entity
     * (given a {@link SavedEntityInfo} instance) and its history of actions.
     *
     * @param entityInfo the object storing the basic properties of the
     *   entity. This argument cannot be {@code null}.
     * @param previousActions the history of actions of the entity in the order
     *   the actions occured. The passed list is copied by this constructor,
     *   modifying the passed list after this constructor returns will have no
     *   effect on the new instance. This argument cannot be {@code null} but
     *   can be an empty list.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public SavedActiveEntityInfo(
            SavedEntityInfo entityInfo,
            List<CombatEntityAction> previousActions) {
        ExceptionHelper.checkNotNullArgument(entityInfo, "entityInfo");
        ExceptionHelper.checkNotNullArgument(previousActions, "previousActions");

        this.entityInfo = entityInfo;
        this.previousActions = new ArrayList<>(previousActions);
        ExceptionHelper.checkNotNullElements(this.previousActions, "previousActions");
    }

    /**
     * Creates a new unique combat entity with its history of actions.
     * Every call to this method returns a new different entity with the
     * same properties.
     * <P>
     * The returned instance is not used by this {@link SavedActiveEntityInfo}
     * instance and can be used by the client as it wishes.
     *
     * @return a new unique combat entity with its history of actions.
     *   This method never returns {@code null}.
     */
    public CombatEntity toCombatEntity() {
        CombatEntity result = entityInfo.toCombatEntity();
        result.addActions(previousActions);
        return result;
    }

    /**
     * Returns a serializable object storing the basic properties of an entity.
     *
     * @return a serializable object storing the basic properties of an entity.
     *   This method never returns {@code null}.
     */
    public SavedEntityInfo getEntityInfo() {
        return entityInfo;
    }

    /**
     * Returns the history of actions of an entity specified at construction
     * time. The returned list contains actions in the order they occured.
     *
     * @return  the history of actions of an entity specified at construction
     *   time. The returned list readonly and is never {@code null}.
     */
    public List<CombatEntityAction> getPreviousActions() {
        return Collections.unmodifiableList(previousActions);
    }

    private Object writeReplace() {
        return new Format1(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("This object cannot be deserialized directly.");
    }

    private static List<CombatEntityAction> getCheckedActionList(Collection<CombatEntityAction> actions)
            throws InvalidObjectException {

        List<CombatEntityAction> result = new ArrayList<>(actions);
        for (Object action: actions) {
            Objects.requireNonNull(action, "action");
            if (!(action instanceof CombatEntityAction)) {
                throw new InvalidObjectException("The action list contains an object with an invalid class: " + action.getClass());
            }
        }
        return result;
    }

    private static class Format1 implements Serializable {
        private static final long serialVersionUID = 9008883401582315791L;

        private final SavedEntityInfo entityInfo;
        private final List<CombatEntityAction> previousActions;

        public Format1(SavedActiveEntityInfo info) {
            this.entityInfo = info.entityInfo;
            this.previousActions = info.previousActions;
        }

        private Object readResolve() throws InvalidObjectException {
            return new SavedActiveEntityInfo(entityInfo,
                    getCheckedActionList(previousActions));
        }
    }
}
