package exaltedcombat.models;

import java.util.Collection;

/**
 * The interface to listen for changes in the entity position on the
 * combat timeline. The timeline of the combat is zero based.
 *
 * @param <EntityType> the type of the combat entities in this model
 *
 * @see CombatPositionModel
 * @author Kelemen Attila
 */
public interface CombatPosEventListener<EntityType> {
    /**
     * Invoked when an entity enters the combat at a tick. Although the rules
     * of Exalted only allow to an entity to enter a combat by rolling a
     * join combat roll in ExaltedCombat it is possible to enter without an
     * associated join combat roll.
     * <P>
     * When this method is called the entity has already joined the combat and
     * this method may cause them to act in combat.
     *
     * @param entity the entity entering the combat. This argument cannot be
     *   {@code null}.
     * @param tick the tick when the entity will act first. Note that in
     *   ExaltedCombat the entity can be moved to a different tick even if it
     *   is not its turn. This argument must be greater or equals to zero.
     */
    public void enterCombat(EntityType entity, int tick);

    /**
     * Invoked when some entities are removed from the combat. The entities
     * need to reenter the combat if they are to act again.
     * <P>
     * When this method is called the entities has already left the combat and
     * this method can reenter any of them.
     *
     * @param entities the non-empty collection of entities leaving the combat.
     *   This argument cannot be {@code null}.
     */
    public void leaveCombat(Collection<? extends EntityType> entities);

    /**
     * Invoked when an entity is moved from one tick to another in combat. This
     * method is only invoked if the entity was already in combat. If it has
     * not yet entered the combat the
     * {@link #enterCombat(java.lang.Object, int) enterCombat} method will
     * be called instead.
     * <P>
     * When this method is called the entities was already moved and this method
     * may cause them to act in combat again.
     *
     * @param entity the entity which was moved from one tick to another. This
     *   argument cannot be {@code null}.
     * @param srcTick the tick from which the entity was moved. This argument
     *   must be greater than or equal to zero.
     * @param destTick the tick to which the entity was moved. This argument
     *   must be greater than or equal to zero.
     */
    public void move(EntityType entity, int srcTick, int destTick);
}
