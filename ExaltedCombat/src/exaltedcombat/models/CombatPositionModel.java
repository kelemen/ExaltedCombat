package exaltedcombat.models;

import java.util.*;

/**
 * Defines a model for the position of entities on a tick based timeline.
 * <P>
 * Entities on the timeline has a certain tick value. This tick value defines
 * on which tick will they act. It is possible that on a certain tick there are
 * multiple entities in this case these entities acts simultenously.
 * <P>
 * The current time is defined by the entity with the lowest tick value. If
 * there are no entities defined, the time is the last time when there were
 * entities defined. In case the event manager never had an entity defined the
 * current time is defined to be zero.
 *
 * <h3>Models in ExaltedCombat</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 *
 * @param <EntityType> the type of the combat entities in this model
 *
 * @see exaltedcombat.combatmanagers.HorizontalCombatPanel
 * @see exaltedcombat.combatmanagers.TickBasedEventManager
 * @author Kelemen Attila
 */
public interface CombatPositionModel<EntityType> {
    /**
     * Registers a new listener to listen for changes in this tick based
     * timeline. Changes occur when an entity enters, leaves or acts in combat.
     * <P>
     * Note that in case the listener was already registered implementations
     * may ignore this call as a no-op.
     *
     * @param listener the listener to be notified when changes occur in this
     *   tick based timeline. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public void addCombatPosListener(CombatPosEventListener<EntityType> listener);

    /**
     * Unregisters a previously registered position change listener. If the
     * listener was not registered or was already unregistered this call does
     * nothing.
     *
     * @param listener the listener to be removed and no longer be notified of
     *   the changes in this tick based timeline. This argument cannot be
     *   {@code null}
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public void removeCombatPosListener(CombatPosEventListener<EntityType> listener);

    /**
     * Returns the number of entities currently in combat.
     *
     * @return the number of entities currently in combat. The returned value
     *   is always greater or equal to zero.
     */
    public int getNumberOfEntities();

    /**
     * Returns map containing the entities in this tick based timeline. The
     * returned map maps ticks to the entities currently on that tick; it does
     * not have association with ticks containing no entities.
     * <P>
     * Note that the sum of the length of the list in the returned map equals
     * to the {@link #getNumberOfEntities() number of entities} in this model.
     *
     * @return the map containing the entities in this tick based timeline. The
     *   returned map is independent of this model and is not required to be
     *   mutable. Modifying this model has ne effect on the returned map. Also
     *   notice that the lists in the returned map are never empty. This method
     *   never returns {@code null}.
     */
    public Map<Integer, List<EntityType>> getEntities();

    /**
     * Returns the list of entities on the specified tick. This method is
     * equivalent to {@code getEntities().get(tick)} except that this method
     * never returns {@code null}.
     *
     * @param tick the tick to be queried. This argument must greate than or
     *   equal to zero.
     * @return the list of entities on the specified tick. This method never
     *   returns {@code null} if there are no entities on the given tick, this
     *   method returns an empty list. Note that the returned list is not
     *   required to be mutable and does not reflect subsequent modifications to
     *   this model.
     */
    public List<EntityType> getEntities(int tick);

    /**
     * Returns {@code Iterable} containing the entities on the ticks from the
     * given starting tick. The returned {@code Iterable} can be iterated
     * endlessly. The {@code Iterator} of the returned {@code Iterable} steps
     * one tick at a time (i.e.: a call to {@link Iterator#next()}).
     * <P>
     * Note that the returned {@code Iterable} will reflect the changes made to
     * this model, also iterating over it will access this model. Therefore the
     * returned iterator can only be used safely where a
     * {@link #getEntities(int) getEntities(int)} can be used.
     *
     * @param startTick the tick from where to start iterating. This argument
     *   must be greater than or equal to zero.
     * @return the {@code Iterable} containing the entities on the ticks from
     *   the given starting tick. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code startTick} is a
     *   negative integer
     */
    public Iterable<List<EntityType>> getTicks(int startTick);

    /**
     * Returns {@code Iterable} containing the entities on the ticks from the
     * current tick. The returned {@code Iterable} can be iterated
     * endlessly. The {@code Iterator} of the returned {@code Iterable} steps
     * one tick at a time (i.e.: a call to {@link Iterator#next()}).
     * <P>
     * Note that the returned {@code Iterable} will reflect the changes made to
     * this model, also iterating over it will access this model. Therefore the
     * returned iterator can only be used safely where a
     * {@link #getEntities(int) getEntities(int)} can be used.
     * <P>
     * Note that this method is just a shorthand for
     * {@code getTicks(model.getCurrentTick())}.
     *
     * @return the {@code Iterable} containing the entities on the ticks from
     *   the current tick. This method never returns {@code null}.
     *
     * @throws IllegalArgumentException thrown if {@code startTick} is a
     *   negative integer
     */
    public Iterable<List<EntityType>> getTicks();

    /**
     * Returns the current time of this combat. The current time is defined to
     * be the lowest tick when an entity acts. If there is no entity in the
     * combat, the current time is defined to be zero.
     *
     * @return the current time of this combat. This method always returns a
     *   non-negative integer.
     */
    public int getCurrentTick();

    /**
     * Returns the tick when the given entity will act first.
     *
     * @param entity the requested entity. This argument cannot be {@code null}.
     * @return the tick when the given entity will act first or -1 if there
     *   is no such entity in the combat
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public int getTickOfEntity(EntityType entity);

    /**
     * Moves an entity to a given tick on this tick based timeline. The entity
     * will enter into the combat if it has not yet entered. After a successful
     * call to this method
     * {@link #getTickOfEntity(Object) getTickOfEntity(entity)} will return
     * the specified tick for the specified entity.
     * <P>
     * This method will notify
     * {@link #addCombatPosListener(CombatPosEventListener) registered}
     * position listeners of the changes.
     *
     * @param entity the entity to be moved to the given tick. This argument
     *   cannot be {@code null}
     * @param tick the tick to which the given entity is to be moved. This
     *   argument must be greater than or equal to zero.
     * @return the previous tick where the entity was before this method call,
     *   that is, the value {@code getTickOfEntity(entity)} would have returned
     *   before this method call.
     *
     * @throws IllegalArgumentException thrown if the specified tick is a
     *   negative integer
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public int moveToTick(EntityType entity, int tick);

    /**
     * Removes every entity from the combat. After a successful call to this
     * method {@link #getNumberOfEntities() getNumberOfEntities()} will return
     * zero.
     * <P>
     * This method will notify
     * {@link #addCombatPosListener(CombatPosEventListener) registered}
     * position listeners of the changes.
     */
    public void removeAllEntities();

    /**
     * Removes the specified entity from the combat. In case the entity was not
     * in combat, this method does nothing.
     * <P>
     * This method will notify
     * {@link #addCombatPosListener(CombatPosEventListener) registered}
     * position listeners of the changes.
     *
     * @param entity the entity to be removed from the combat. This argument
     *   cannot be {@code null}.
     * @return the tick where the entity was before this method call,
     *   that is, the value {@code getTickOfEntity(entity)} would have returned
     *   before this method call.
     */
    public int removeEntity(EntityType entity);
}
