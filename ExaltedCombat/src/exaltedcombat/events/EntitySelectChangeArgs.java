package exaltedcombat.events;

import exaltedcombat.models.impl.CombatEntity;

/**
 * The type of the argument associated with
 * {@link WorldEvent#ENTITY_SELECT_CHANGE} events. This event argument stores
 * the previously selected combat entity and the new entity which was just
 * selected.
 * <P>
 * Instances of this class are immutable (therefore thread-safe) and
 * transparent to any synchronization. Note however that the referenced combat
 * entities are not thread-safe, see {@link CombatEntity} for reference.
 *
 * @see EventManager
 * @see WorldEvent#ENTITY_SELECT_CHANGE
 * @author Kelemen Attila
 */
public final class EntitySelectChangeArgs {
    private final CombatEntity oldSelection;
    private final CombatEntity newSelection;

    /**
     * Creates a new event argument with the specified combat entities.
     * The new object will store references to the specified entities.
     *
     * @param oldSelection the combat entity which was previously selected
     *   or {@code null} if there was no entity selected previously
     * @param newSelection the combat entity which is now selected
     *   or {@code null} if there is no entity selected currently
     */
    public EntitySelectChangeArgs(CombatEntity oldSelection, CombatEntity newSelection) {
        this.oldSelection = oldSelection;
        this.newSelection = newSelection;
    }

    /**
     * Returns the entity which became selected when the associated event was
     * triggered.
     *
     * @return the entity which became selected when the associated event was
     *   triggered or {@code null} if there was no entity selected
     */
    public CombatEntity getNewSelection() {
        return newSelection;
    }

    /**
     * Returns the entity which was selected previously when the associated
     * event was triggered.
     *
     * @return the entity which was selected previously when the associated
     *   event was triggered or {@code null} if there was no entity selected
     *   previously
     */
    public CombatEntity getOldSelection() {
        return oldSelection;
    }
}
