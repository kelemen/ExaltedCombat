package exaltedcombat.models;

import org.jtrim.event.ListenerRef;

/**
 * Defines the model of a combat in Exalted. The model defines the current
 * {@link #getCombatState() state of the combat} and the position of the
 * entities on the timeline of the combat.
 * <P>
 * A combat in Exalted starts by
 * {@link #joinCombat(Object, int) rolling join combat} for all the entities
 * participating in the combat from the beginning. Once everyone joined the
 * combat: the combat {@link #endJoinPhase() starts}. In this second phase
 * new entities may join the combat and entities may leave it (possibly because
 * of dying). Most entities in the second phase of the combat however perform
 * various actions and these actions can be done through the
 * {@link #getPositionModel() position model} (a submodel of this combat model).
 * <P>
 * Note that although entities can only perform actions during the second
 * phase of the combat, this model allows to perform actions through the
 * position model for convenience.
 * <P>
 * To be notified for changes in the state of the combat this model
 * {@link #addCombatStateChangeListener(CombatStateChangeListener) register}
 * a new event handler for state changes.
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
 * @see exaltedcombat.models.impl.CombatEntity CombatEntity
 * @see exaltedcombat.models.impl.GeneralCombatModel GeneralCombatModel
 * @author Kelemen Attila
 */
public interface CombatModel<EntityType> {
    /**
     * Returns the model of the position of entities on the timeline of the
     * combat. The entities on the returned model are allowed be modified in
     * every combat state (not only in the join phase).
     * <P>
     * Note that entities may enter the combat through the position model. If
     * they do so they will not have a join combat roll but otherwise can act
     * normally.
     *
     * @return the model of the position of entities on the timeline of the
     *   combat. This method never returns {@code null}.
     */
    public CombatPositionModel<EntityType> getPositionModel();

    /**
     * Registers a new listener to listen for changes in the state of the
     * combat. The registered listener can be removed using the returned
     * reference.
     * <P>
     * Note that in case the listener was already registered implementations
     * may ignore this call as a no-op.
     *
     * @param listener the listener to be notified when the state of the combat
     *   changes. This argument cannot be {@code null}.
     * @return the reference through which the newly added listener can be
     *   removed. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public ListenerRef<CombatStateChangeListener> addCombatStateChangeListener(
            CombatStateChangeListener listener);

    /**
     * Returns the current state of the combat. The combat can be either the
     * join phase or in the start combat state. Changes in the state of combat
     * can be detected by
     * {@link #addCombatStateChangeListener(CombatStateChangeListener) registering}
     * a combat state change listener.
     *
     * @return the current state of the combat. This method never returns
     *   {@code null}.
     */
    public CombatState getCombatState();

    /**
     * Returns the highest roll in the join phase for join combat. For this
     * method, botches count as zero success rolls.
     *
     * @return the highest roll in the join phase for join combat. This method
     *   returns zero if no one joined the combat in the join phase.
     */
    public int getHighestJoinRoll();

    /**
     * Returns the number of successes rolled by an entity in the join combat
     * phase.
     *
     * @param entity the entity whose roll is to be returned. This argument
     *   cannot be {@code null}.
     * @return the number of successes rolled by an entity in the join combat
     *   phase. In case the entity joined the combat after the join
     *   phase this method returns zero. Note that this method may return a
     *   negative integer in case the entity rolled a botch when entering the
     *   combat in the join phase.
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public int getPreJoinRoll(EntityType entity);

    /**
     * Makes an entity join the combat. This method will modify the
     * {@link #getPositionModel() position model} according to the specified
     * join combat roll. In case the combat is in the join phase
     * (see: {@link #getCombatState() getCombatState()}), this method may need
     * to adjust the position of other, previously registered entities in the
     * position model.
     * <P>
     * In case the entity already joined the combat (even if directly through
     * the position model), this method will throw an
     * {@code IllegalStateException}.
     * <P>
     * The entity can be removed from the combat using the
     * {@link #getPositionModel() position model}.
     *
     * @param entity the entity to join the combat. This argument cannot be
     *   {@code null}.
     * @param roll the number of successes rolled by the entity. This argument
     *   can be negative if the entity rolled a botch. For specifying botch any
     *   negative integer is allowed and the user may define additional meaning
     *   to different negative values (e.g.: number of ones rolled).
     *
     * @throws IllegalStateException thrown if the entity is already in combat
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public void joinCombat(EntityType entity, int roll);

    /**
     * Ends the join phase and enters the combat into the combat phase.
     * Note that Exalted defines different rules for entities to join the combat
     * after the join phase and the implementation must be aware of it.
     * <P>
     * After this method returns successfully
     * {@link #getCombatState() getCombatState()} will return
     * {@link CombatState#COMBAT_PHASE}.
     *
     * @throws IllegalStateException thrown if the combat is not in the
     *   {@link CombatState#JOIN_PHASE join phase}
     */
    public void endJoinPhase();

    /**
     * Revert the combat from the {@link CombatState#COMBAT_PHASE combat phase}
     * to the {@link CombatState#JOIN_PHASE join phase}. Subsequent join combat
     * rolls will be done with the rules for the join combat phase.
     * <P>
     * If there is an entity joining the combat in the combat phase, that entity
     * will appear as if it entered the combat without an actual join combat
     * roll.
     * <P>
     * After this method returns successfully
     * {@link #getCombatState() getCombatState()} will return
     * {@link CombatState#JOIN_PHASE}.
     *
     * @throws IllegalStateException thrown if the combat is not in the
     *   {@link CombatState#COMBAT_PHASE combat phase}
     */
    public void revertToJoinPhase();

    /**
     * Removes every entity from the combat and sets the combat state to
     * {@link CombatState#JOIN_PHASE}. This method call may result in several
     * events to be fired.
     * <P>
     * After this method returns successfully
     * {@link #getCombatState() getCombatState()} will return
     * {@link CombatState#JOIN_PHASE}.
     */
    public void resetCombat();
}
