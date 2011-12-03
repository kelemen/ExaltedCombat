package exaltedcombat.models.impl;

import exaltedcombat.models.*;

import org.jtrim.utils.*;

/**
 * Defines a "world" model which composes user defined population and combat
 * model implementations and will also have a user defined
 * {@link EntityStorage storage} for hidden entities.
 *
 * <h3>Models in ExaltedCombat</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 *
 * @see CombatEntities
 * @see GeneralCombatModel
 * @see exaltedcombat.dialogs.EntityStorageFrame
 * @author Kelemen Attila
 */
public final class GeneralCombatEntityWorldModel implements CombatEntityWorldModel {
    private final CombatEntities populationModel;
    private final CombatModel<CombatEntity> combatModel;
    private final EntityStorage hiddenStorage;

    /**
     * Creates a new "world" model using the given submodels.
     *
     * @param populationModel the model to be returned by the
     *   {@link #getPopulationModel() getPopulationModel()} method call. This
     *   argument cannot be {@code null}.
     * @param combatModel the model to be returned by the
     *   {@link #getCombatModel() getCombatModel()} method call. This
     *   argument cannot be {@code null}.
     * @param hiddenStorage the storage where the
     *   {@link #hideEntity(CombatEntity) hideEntity(CombatEntity)} method
     *   will store entities after removed from the population. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public GeneralCombatEntityWorldModel(
            CombatEntities populationModel,
            CombatModel<CombatEntity> combatModel,
            EntityStorage hiddenStorage) {

        ExceptionHelper.checkNotNullArgument(populationModel, "populationModel");
        ExceptionHelper.checkNotNullArgument(combatModel, "combatModel");
        ExceptionHelper.checkNotNullArgument(hiddenStorage, "hiddenStorage");

        this.populationModel = populationModel;
        this.combatModel = combatModel;
        this.hiddenStorage = hiddenStorage;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CombatEntities getPopulationModel() {
        return populationModel;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CombatModel<CombatEntity> getCombatModel() {
        return combatModel;
    }

    /**
     * {@inheritDoc }
     * <P>
     * Implementation note: This method will store the entity in the
     * {@link EntityStorage storage} specified at construction time after
     * removing the entity from the population.
     */
    @Override
    public void hideEntity(CombatEntity entity) {
        if (populationModel.removeEntity(entity)) {
            hiddenStorage.storeEntity(entity);
        }
    }
}
