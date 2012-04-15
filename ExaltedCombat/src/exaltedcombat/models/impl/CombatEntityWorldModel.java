package exaltedcombat.models.impl;

import exaltedcombat.models.CombatModel;

/**
 * Defines the model for the "world" in ExaltedCombat. The world of
 * ExaltedCombat contains a population of entities and a model for the combat.
 * This implies that in this world there can be only one combat concurrently.
 * <P>
 * It is also possible to move currently unused entities to a hidden storage,
 * this storage can be accessed in an implementation defined way.
 * <P>
 * Note that it is not required by this model that the entities in the
 * {@link #getCombatModel() combat model} be part of the
 * {@link #getPopulationModel() population}. Although in mostly this is the
 * case. In practice if an entity is not part of the population but part of
 * the combat, only means that it cannot be selected.
 *
 * <h3>Models in ExaltedCombat</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public interface CombatEntityWorldModel {
    /**
     * Returns the model containing the population of the ExaltedCombat world.
     * The population can be independently modified to the
     * {@link #getCombatModel() combat model} but usually it is recommended to
     * contain all the entities in the combat model.
     *
     * @return the model containing the population of the world. This method
     *   never returns {@code null}.
     */
    public CombatEntities getPopulationModel();

    /**
     * Returns the model of the combat of the ExaltedCombat world. The combat
     * model can be independently modified to the
     * {@link #getPopulationModel() population model} but usually it is
     * recommended that all entities in the combat model be part of the
     * population model.
     *
     * @return the model of the combat of the ExaltedCombat world. This method
     *   never returns {@code null}.
     */
    public CombatModel<CombatEntity> getCombatModel();

    /**
     * Removes an entity from the {@link #getPopulationModel() population} and
     * moves it to an implementation specific container. This method can be used
     * to removed currently unused entities, so that they don't clutter the
     * user interface.
     * <P>
     * In case the specified entity is not in the population this method does
     * nothing.
     * <P>
     * Since this method removes the entity from the population, it will notify
     * the appropriate listeners that the entity was removed.
     *
     * @param entity the entity to be moved to an implementation specific
     *   container. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public void hideEntity(CombatEntity entity);
}
