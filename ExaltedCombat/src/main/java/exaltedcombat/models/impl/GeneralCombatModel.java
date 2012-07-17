package exaltedcombat.models.impl;

import exaltedcombat.models.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a combat model implementation with a user specified
 * {@link CombatPositionModel position model}.
 * <P>
 * This implementation allows to set
 * {@link #getPreJoinRoll(exaltedcombat.models.impl.CombatEntity) join combat rolls}
 * for combat entities not actually in combat. This can be useful when loading
 * previously saved state of the combat. Use the
 * {@link #setPreStartJoinRoll(exaltedcombat.models.impl.CombatEntity, int) setPreStartJoinRoll(CombatEntity, int)}
 * method to set the join combat roll for an entity.
 *
 * <h3>Models in ExaltedCombat</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 *
 * @see exaltedcombat.combatmanagers.HorizontalCombatPanel
 * @see exaltedcombat.combatmanagers.TickBasedEventManager
 * @author Kelemen Attila
 */
public final class GeneralCombatModel implements CombatModel<CombatEntity> {
    private static final int HIGHEST_START_TICK_OFFSET = 6;

    private final ListenerManager<CombatStateChangeListener, Void> listeners;

    private CombatPositionModel<CombatEntity> positionModel;

    private final Map<CombatEntity, Integer> joinPhaseRolls;
    private int maxRoll; // only valid in COMBAT_PHASE
    private int minimumHighestRoll;

    private CombatState combatState;

    /**
     * Creates a new combat model with the given position model. Note that this
     * constructor also needs to register itself to listen for changes in the
     * position model.
     *
     * @param positionModel the position model to be used and returned by the
     *   {@link #getPositionModel() getPositionModel()} method call. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified position model is
     *   {@code null}
     */
    public GeneralCombatModel(CombatPositionModel<CombatEntity> positionModel) {
        ExceptionHelper.checkNotNullArgument(positionModel, "positionModel");

        this.listeners = new CopyOnTriggerListenerManager<>();
        this.joinPhaseRolls = new HashMap<>();
        this.maxRoll = 0;
        this.minimumHighestRoll = 0;
        this.combatState = CombatState.JOIN_PHASE;
        this.positionModel = positionModel;

        registerCombatPosListener();
    }

    private void registerCombatPosListener() {
        positionModel.addCombatPosListener(new CombatPosEventListener<CombatEntity>() {
            @Override
            public void enterCombat(CombatEntity entity, int tick) {
            }

            @Override
            public void leaveCombat(Collection<? extends CombatEntity> entities) {
                for (CombatEntity entity: entities) {
                    joinPhaseRolls.remove(entity);
                }

                if (getCombatState() == CombatState.JOIN_PHASE) {
                    reJoin();
                }
            }

            @Override
            public void move(CombatEntity entity, int srcTick, int destTick) {
            }
        });
    }

    private void reJoin() {
        assert getCombatState() == CombatState.JOIN_PHASE;
        CombatPositionModel<CombatEntity> posModel = getPositionModel();
        if (joinPhaseRolls.isEmpty()) {
            return;
        }

        int highest = getHighestJoinRoll();
        Map<CombatEntity, Integer> newTicks = CollectionsEx.newHashMap(joinPhaseRolls.size());
        for (Map.Entry<CombatEntity, Integer> entry: joinPhaseRolls.entrySet()) {
            CombatEntity entity = entry.getKey();
            int joinRoll = entry.getValue();
            if (joinRoll >= 0) {
                int newTick = Math.min(highest - joinRoll, HIGHEST_START_TICK_OFFSET);
                if (newTick != posModel.getTickOfEntity(entity)) {
                    newTicks.put(entity, newTick);
                }
            }
        }

        for (Map.Entry<CombatEntity, Integer> entry: newTicks.entrySet()) {
            posModel.moveToTick(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Sets the {@link #getPreJoinRoll(exaltedcombat.models.impl.CombatEntity) join combat roll}
     * for an entity not necessarily in this combat. The entity is assumed to be
     * joined the combat in the {@link CombatState#JOIN_PHASE join phase}.
     * <P>
     * In case the entity already joined the combat this method will override
     * its join combat roll value.
     * <P>
     * This method is useful if restoring a previously saved state of a combat.
     *
     * @param entity the entity to which the roll is to be associated. This
     *   argument cannot be {@code null}.
     * @param roll the join combat roll value to be associated with the given
     *   entity. This value can be negative to represent a botched roll.
     *
     * @throws NullPointerException thrown if the specified combat entity is
     *   {@code null}
     */
    public void setPreStartJoinRoll(CombatEntity entity, int roll) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");

        joinPhaseRolls.put(entity, roll);
        maxRoll = Math.max(maxRoll, roll);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CombatPositionModel<CombatEntity> getPositionModel() {
        CombatPositionModel<CombatEntity> result = positionModel;
        if (result == null) {
            throw new IllegalStateException("PositionModel was not yet initialized.");
        }
        return result;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public ListenerRef addCombatStateChangeListener(
            CombatStateChangeListener listener) {
        return listeners.registerListener(listener);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public CombatState getCombatState() {
        return combatState;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getHighestJoinRoll() {
        int result;
        if (combatState == CombatState.COMBAT_PHASE) {
            result = maxRoll;
        }
        else {
            result = 0;
            for (Integer roll: joinPhaseRolls.values()) {
                result = Math.max(result, roll);
            }
        }
        return Math.max(minimumHighestRoll, result);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getPreJoinRoll(CombatEntity entity) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");

        Integer result = joinPhaseRolls.get(entity);
        return result != null ? result.intValue() : 0;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void joinCombat(CombatEntity entity, int roll) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");

        CombatPositionModel<CombatEntity> posModel = getPositionModel();
        if (posModel.getTickOfEntity(entity) >= 0) {
            throw new IllegalStateException("The entity is already in combat.");
        }

        switch (combatState) {
            case JOIN_PHASE:
            {
                if (roll < 0) {
                    posModel.moveToTick(entity, HIGHEST_START_TICK_OFFSET);
                }
                else {
                    int highest = getHighestJoinRoll();
                    assert highest >= 0;
                    Map<Integer, List<CombatEntity>> others = posModel.getEntities();

                    if (highest >= roll) {
                        int dRoll = Math.min(highest - roll, HIGHEST_START_TICK_OFFSET);
                        posModel.moveToTick(entity, dRoll);
                    }
                    else {
                        // the min is here to eliminate integer overflow
                        int dRoll = Math.min(roll - highest, HIGHEST_START_TICK_OFFSET);
                        for (Map.Entry<Integer, List<CombatEntity>> entry: others.entrySet()) {
                            int tick = entry.getKey();
                            int toTick = Math.min(tick + dRoll, HIGHEST_START_TICK_OFFSET);
                            if (toTick != tick) {
                                for (CombatEntity toMove: entry.getValue()) {
                                    posModel.moveToTick(toMove, toTick);
                                }
                            }
                        }
                        posModel.moveToTick(entity, 0);
                    }
                }

                break;
            }
            case COMBAT_PHASE:
            {
                int highest = getHighestJoinRoll();
                if (highest <= roll) {
                    posModel.moveToTick(entity, posModel.getCurrentTick());
                }
                else {
                    int dTick = Math.min(highest - roll, HIGHEST_START_TICK_OFFSET);
                    posModel.moveToTick(entity, posModel.getCurrentTick() + dTick);
                }
                break;
            }
            default:
                throw new IllegalStateException("Unexpected combat state: " + combatState);
        }

        if (getCombatState() == CombatState.JOIN_PHASE) {
            joinPhaseRolls.put(entity, roll);
        }
    }

    private void dispatchStateChangeEvent(final CombatState newState) {
        listeners.onEvent(new EventDispatcher<CombatStateChangeListener, Void>() {
            @Override
            public void onEvent(CombatStateChangeListener eventListener, Void arg) {
                eventListener.onChangeCombatState(newState);
            }
        }, null);
    }

    private void setCombatState(CombatState newState) {
        if (combatState != newState) {
            if (newState == CombatState.COMBAT_PHASE) {
                maxRoll = getHighestJoinRoll();
            }

            combatState = newState;
            dispatchStateChangeEvent(newState);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void endJoinPhase() {
        if (combatState != CombatState.JOIN_PHASE) {
            throw new IllegalStateException("The combat is not in the join phase.");
        }

        setCombatState(CombatState.COMBAT_PHASE);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void revertToJoinPhase() {
        if (combatState != CombatState.COMBAT_PHASE) {
            throw new IllegalStateException("The combat is not in the combat phase.");
        }

        setCombatState(CombatState.JOIN_PHASE);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void resetCombat() {
        minimumHighestRoll = 0;
        getPositionModel().removeAllEntities();
        // This clear() method should do nothing due to the listeners removing
        // them but call it just in case.
        joinPhaseRolls.clear();
        setCombatState(CombatState.JOIN_PHASE);
    }
}
