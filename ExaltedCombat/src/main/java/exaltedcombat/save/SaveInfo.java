package exaltedcombat.save;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a serializable class storing the state of ExaltedCombat.
 * <P>
 * The state contains three set of combat entities:
 * <ul>
 *  <li>
 *   Entities currently participating in the combat: These entities can be
 *   retrieved by a {@link #getCombatEntities() getCombatEntities()} method call.
 *  </li>
 *  <li>
 *   Entities not currently participating in the combat but are in the
 *   population of the world: These entities can be retrieved by a
 *   {@link #getReadyEntities() getReadyEntities()} method call.
 *  </li>
 *  <li>
 *   Entities not participating in the combat and not part of the population:
 *   These entities can be retrieved by a
 *   {@link #getHiddenEntities() getHiddenEntities()} method call.
 *  </li>
 * <ul>
 * These collection of entities usually expected to be distinct. However it is
 * possible that a saved file contains non distinct sets of these entities, in
 * this case the user of this class can decide in which group it believes the
 * entity is in (but cannot choose a group it is not in).
 * <P>
 * The saved state also defines the current
 * {@link exaltedcombat.models.CombatState state} of the combat.
 * <P>
 * The serialization of this class is intended to be kept backward compatible,
 * so the serialized form of this class is appropriate for long term storage.
 *
 * @see ExaltedSaveHelper#createSaveInfo(exaltedcombat.models.impl.CombatEntityWorldModel, java.util.Collection) ExaltedSaveHelper.createSaveInfo(CombatEntityWorldModel, Collection)
 * @see SavedActiveEntityInfo
 * @see SavedCombatEntity
 * @see SavedEntityInfo
 * @author Kelemen Attila
 */
public final class SaveInfo implements Serializable {
    private static final long serialVersionUID = -6336671693202386107L;

    private final boolean combatWasStarted;
    private final Collection<SavedCombatEntity> combatEntities;
    private final Collection<SavedActiveEntityInfo> readyEntities;
    private final Collection<SavedEntityInfo> hiddenEntities;

    /**
     * Creates a state of ExaltedCombat without any entities. Entities to this
     * state can be added by the {@code addXXX} method calls.
     *
     * @param combatWasStarted {@code true} if the combat is already in the
     *   {@link exaltedcombat.models.CombatState#COMBAT_PHASE combat phase},
     *   {@code false} if it is still in the
     *   {@link exaltedcombat.models.CombatState#JOIN_PHASE join phase}
     */
    public SaveInfo(boolean combatWasStarted) {
        this.combatWasStarted = combatWasStarted;
        this.combatEntities = new LinkedList<>();
        this.readyEntities = new LinkedList<>();
        this.hiddenEntities = new LinkedList<>();
    }

    /**
     * Checks whether the combat has already advanced to the second phase
     * ({@link exaltedcombat.models.CombatState#COMBAT_PHASE COMBAT_PHASE}).
     *
     * @return {@code true} if the combat of this saved state is in the
     *   {@link exaltedcombat.models.CombatState#COMBAT_PHASE COMBAT_PHASE},
     *   {@code false} if it is still in the
     *   {@link exaltedcombat.models.CombatState#JOIN_PHASE JOIN_PHASE}
     */
    public boolean isCombatWasStarted() {
        return combatWasStarted;
    }

    private <E> void addAllAndCheckForNulls(Collection<? super E> dest, Collection<? extends E> source) {
        ExceptionHelper.checkNotNullElements(source, "source");
        for (E element: source) {
            // This should not fail if the source collection was properly implemented.
            ExceptionHelper.checkNotNullArgument(element, "element");
            dest.add(element);
        }
    }

    /**
     * Adds the given entities to the entities participating in the combat.
     * These entities can be later retrieved by a call to
     * {@link #getCombatEntities() getCombatEntities()}.
     *
     * @param entities the entities to be added to the entities participating in
     *   the combat. This argument cannot be {@code null} and cannot contain
     *   {@code null} elements.
     *
     * @throws NullPointerException thrown if {@code entities} or any of its
     *   elements is {@code null}
     *
     * @see SavedCombatEntity
     */
    public void addCombatEntities(Collection<? extends SavedCombatEntity> entities) {
        addAllAndCheckForNulls(combatEntities, entities);
    }

    /**
     * Adds the given entities to the entities not participating in the combat
     * and not part of the population. These entities can be later retrieved by
     * a call to {@link #getHiddenEntities() getHiddenEntities()}.
     *
     * @param entities the entities to be added to the entities not
     *   participating in the combat and not part of the population. This
     *   argument cannot be {@code null} and cannot contain {@code null}
     *   elements.
     *
     * @throws NullPointerException thrown if {@code entities} or any of its
     *   elements is {@code null}
     *
     * @see SavedEntityInfo
     */
    public void addHiddenEntities(Collection<? extends SavedEntityInfo> entities) {
        addAllAndCheckForNulls(hiddenEntities, entities);
    }

    /**
     * Adds the given entities to the entities not participating in the combat
     * but are part of the population. These entities can be later retrieved by
     * a call to {@link #getReadyEntities() getReadyEntities()}.
     *
     * @param entities the entities to be added to the entities not
     *   participating in the combat but are part of the population. This
     *   argument cannot be {@code null} and cannot contain {@code null}
     *   elements.
     *
     * @throws NullPointerException thrown if {@code entities} or any of its
     *   elements is {@code null}
     *
     * @see SavedActiveEntityInfo
     */
    public void addReadyEntities(Collection<? extends SavedActiveEntityInfo> entities) {
        addAllAndCheckForNulls(readyEntities, entities);
    }

    /**
     * Returns the entities participating in the combat. These entities are
     * also expected to be part of the population. The returned
     * {@code Collection} is a view of the entities stored in this collection
     * and cannot be modified. However the returned {@code Collection} will
     * reflect any changes made to this {@code SaveInfo} instance.
     *
     * @return the entities participating in the combat. This method never
     *   returns {@code null} but may return an empty {@code Collection}.
     *
     * @see SavedCombatEntity
     */
    public Collection<SavedCombatEntity> getCombatEntities() {
        return Collections.unmodifiableCollection(combatEntities);
    }

    /**
     * Returns the entities not participating in the combat and not part of the
     * population. The returned {@code Collection} is a view of the entities
     * stored in this collection and cannot be modified. However the returned
     * {@code Collection} will reflect any changes made to this {@code SaveInfo}
     * instance.
     *
     * @return the entities not participating in the combat and not part of the
     *   population. This method never returns {@code null} but may return an
     *   empty {@code Collection}.
     *
     * @see SavedEntityInfo
     */
    public Collection<SavedEntityInfo> getHiddenEntities() {
        return Collections.unmodifiableCollection(hiddenEntities);
    }

    /**
     * Returns the entities not participating in the combat but are part of the
     * population. The returned {@code Collection} is a view of the entities
     * stored in this collection and cannot be modified. However the returned
     * {@code Collection} will reflect any changes made to this {@code SaveInfo}
     * instance.
     *
     * @return the entities not participating in the combat but are part of the
     *   population. This method never returns {@code null} but may return an
     *   empty {@code Collection}.
     *
     * @see SavedActiveEntityInfo
     */
    public Collection<SavedActiveEntityInfo> getReadyEntities() {
        return Collections.unmodifiableCollection(readyEntities);
    }

    private static Collection<SavedCombatEntity> getCheckedCombatEntities(Collection<SavedCombatEntity> entities)
            throws InvalidObjectException {

        List<SavedCombatEntity> result = new ArrayList<>(entities);
        for (Object entity: entities) {
            Objects.requireNonNull(entity, "entity");
            if (!(entity instanceof SavedCombatEntity)) {
                throw new InvalidObjectException("The entity list contains an object with an invalid class: " + entity.getClass());
            }
        }
        return result;
    }

    private static Collection<SavedActiveEntityInfo> getCheckedReadyEntities(Collection<SavedActiveEntityInfo> entities)
            throws InvalidObjectException {

        List<SavedActiveEntityInfo> result = new ArrayList<>(entities);
        for (Object entity: entities) {
            Objects.requireNonNull(entity, "entity");
            if (!(entity instanceof SavedActiveEntityInfo)) {
                throw new InvalidObjectException("The entity list contains an object with an invalid class: " + entity.getClass());
            }
        }
        return result;
    }

    private static Collection<SavedEntityInfo> getCheckedHiddenEntities(Collection<SavedEntityInfo> entities)
            throws InvalidObjectException {

        List<SavedEntityInfo> result = new ArrayList<>(entities);
        for (Object entity: entities) {
            Objects.requireNonNull(entity, "entity");
            if (!(entity instanceof SavedEntityInfo)) {
                throw new InvalidObjectException("The entity list contains an object with an invalid class: " + entity.getClass());
            }
        }
        return result;
    }

    private Object writeReplace() {
        ExceptionHelper.checkNotNullElements(combatEntities, "combatEntities");
        ExceptionHelper.checkNotNullElements(readyEntities, "readyEntities");
        ExceptionHelper.checkNotNullElements(hiddenEntities, "hiddenEntities");

        return new Format1(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("This object cannot be deserialized directly.");
    }

    private static class Format1 implements Serializable {
        private static final long serialVersionUID = -1898224196617635466L;

        private final Collection<SavedCombatEntity> combatEntities;
        private final Collection<SavedActiveEntityInfo> readyEntities;
        private final Collection<SavedEntityInfo> hiddenEntities;
        private final boolean combatWasStarted;

        public Format1(SaveInfo info) {
            this.combatEntities = info.combatEntities;
            this.readyEntities = info.readyEntities;
            this.hiddenEntities = info.hiddenEntities;
            this.combatWasStarted = info.combatWasStarted;
        }

        private Object readResolve() throws InvalidObjectException {
            SaveInfo result = new SaveInfo(combatWasStarted);
            result.addCombatEntities(getCheckedCombatEntities(combatEntities));
            result.addHiddenEntities(getCheckedHiddenEntities(hiddenEntities));
            result.addReadyEntities(getCheckedReadyEntities(readyEntities));
            return result;
        }
    }
}
