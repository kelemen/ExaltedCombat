package exaltedcombat.models.impl;

import java.util.Collection;

/**
 * Defines a storage for combat entities. This storage is a simplified
 * collection of entities.
 * <P>
 * The intended use of this interface is to store entities for later retrieval.
 * Implementations may however define other ways to access the stored entities.
 *
 * <h3>Models in ExaltedCombat</h3>
 * Although this interface does not define a model, the same holds for this
 * interface as for models in ExaltedCombat.
 * <P>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 *
 * @see exaltedcombat.dialogs.EntityStorageFrame
 * @author Kelemen Attila
 */
public interface EntityStorage {
    /**
     * Returns the {@code Collection} of the currently stored entities. The
     * returned {@code Collection} does not need to be mutable and must be
     * independent of this storage. So modifying the content of this storage
     * does not affect the returned {@code Collection}.
     *
     * @return the currently stored entities. This method never returns
     *   {@code null} and does not contain {@code null} elements.
     */
    public Collection<CombatEntity> getStoredEntities();

    /**
     * Removes every entity from this storage. After this method returns
     * successfully {@link #getStoredEntities() getStoredEntities()} will return
     * an empty {@code Collection}.
     */
    public void clearEntities();

    /**
     * Adds a given entity to this storage. This method may or may not add
     * entities if they are already in this storage.
     * <P>
     * After a successfully call to this method, the {@code Collection} returned
     * by a subsequent call to {@link #getStoredEntities() getStoredEntities()}
     * will now contain the currently stored entity.
     * <P>
     * This method is effectively equivalent to calling:
     * {@code storeEntities(Collections.singleton(entity))}.
     *
     * @param entity the entity to be added to this storage. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public void storeEntity(CombatEntity entity);

    /**
     * Adds a {@code Collection} of given entities to this storage. This method
     * may or may not add entities if they are already in this storage.
     * <P>
     * After a successfully call to this method, the {@code Collection} returned
     * by a subsequent call to {@link #getStoredEntities() getStoredEntities()}
     * will now contain the currently stored entities.
     *
     * @param entities the entity to be added to this storage. This argument
     *   cannot be {@code null} and cannot contain {@code null} elements.
     *
     * @throws NullPointerException thrown if the specified {@code Collection}
     *   is {@code null} or contains {@code null} elements
     */
    public void storeEntities(Collection<? extends CombatEntity> entities);
}
