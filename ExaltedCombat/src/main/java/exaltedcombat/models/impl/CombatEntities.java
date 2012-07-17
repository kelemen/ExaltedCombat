package exaltedcombat.models.impl;

import exaltedcombat.models.impl.CombatEntity.UpdateListener;
import java.util.*;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a model and implementation for a collection of combat entities in
 * ExaltedCombat.
 * <P>
 * This collection of combat entities is called population. The population of
 * entities differs from a collection in that it can have at most one
 * {@link #getSelection() selected entity}. The selected entity is the default
 * entity to refer to when an action must be done on an specific entity.
 * <P>
 * As with all models, user can be notified when changes occur to the model.
 * There are two set of possible changes:
 * <ul>
 *  <li>
 *   Entities can be added to and removed from the population. The selected
 *   entity of the population can change as well. These changes can be detected
 *   using a {@link #addChangeListener(ChangeListener) change listener}.
 *  </li>
 *  <li>
 *   The properties of individual entities can change. These changes can be
 *   detected using a {@link #addUpdateListener(CombatEntity.UpdateListener) update listener}.
 *  </li>
 * </ul>
 *
 * <h3>Models in ExaltedCombat</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 *
 * @see CombatEntity
 * @see CombatEntityWorldModel
 * @author Kelemen Attila
 */
public final class CombatEntities {
    /**
     * The interface to listen for changes in the population model.
     */
    public static interface ChangeListener {
        /**
         * Invoked when a new entity was selected or an old one was deselected.
         * <P>
         * When this method is called the new entity has already been selected
         * and this method can modify the underlying model.
         * <P>
         * <B>Warning</B>: Changing the selection in this method is not
         * strictly forbidden but is strongly discouraged because it may cause
         * removing an entity to fail with an {@code IllegaStateException}.
         *
         * @param prevSelected the previously selected entity. This argument
         *   can be {@code null} if there was no entity selected previously.
         * @param newSelected the newly selected entity. This argument can be
         *   {@code null} if there is no entity selected currently.
         */
        public void onChangedSelection(CombatEntity prevSelected, CombatEntity newSelected);

        /**
         * Invoked when new entities were added to the population or entities
         * were removed from it.
         * <P>
         * When this method is called the population is already in the new
         * state and this method can modify the underlying model.
         * <P>
         * <B>Warning</B>: Removing entities from the underlying model in this
         * method is not strictly forbidden but is strongly discouraged because
         * it may cause selecting an entity to fail with an
         * {@code IllegaStateException}.
         */
        public void onChangedEntities();
    }

    private final Map<CombatEntity, ListenerRef> entities;
    private CombatEntity selected;

    private final ListenerManager<UpdateListener, Void> updateListeners;
    private final ListenerManager<ChangeListener, Void> changeListeners;
    private final EventDispatcher<ChangeListener, Void> changedEntitiesDispatcher;
    private final UpdateListener entityUpdateListener;

    /**
     * Creates a new empty population with an expected maximum size. The
     * specified expected maximum size does not affect correctness but
     * specifying too low value may affect performance somewhat and too high
     * value may cause unnecessary memory overhead.
     *
     * @param expectedEntityCount the maximum expected number of entities in
     *   this population. This argument must be greater than or equal to zero.
     *
     * @throws IllegalArgumentException thrown if {@code expectedEntityCount}
     *   is a negative integer
     */
    public CombatEntities(int expectedEntityCount) {
        this.updateListeners = new CopyOnTriggerListenerManager<>();
        this.changeListeners = new CopyOnTriggerListenerManager<>();
        this.entities = CollectionsEx.newHashMap(expectedEntityCount);
        this.selected = null;

        this.entityUpdateListener = new CombatEntity.UpdateListener() {
            @Override
            public void onChangedShortName(final CombatEntity entity) {
                updateListeners.onEvent(new EventDispatcher<UpdateListener, Void>() {
                    @Override
                    public void onEvent(UpdateListener eventListener, Void arg) {
                        eventListener.onChangedShortName(entity);
                    }
                }, null);
            }

            @Override
            public void onChangedColor(final CombatEntity entity) {
                updateListeners.onEvent(new EventDispatcher<UpdateListener, Void>() {
                    @Override
                    public void onEvent(UpdateListener eventListener, Void arg) {
                        eventListener.onChangedColor(entity);
                    }
                }, null);
            }

            @Override
            public void onChangedDescription(final CombatEntity entity) {
                updateListeners.onEvent(new EventDispatcher<UpdateListener, Void>() {
                    @Override
                    public void onEvent(UpdateListener eventListener, Void arg) {
                        eventListener.onChangedDescription(entity);
                    }
                }, null);
            }

            @Override
            public void onChangedPreviousAction(final CombatEntity entity) {
                updateListeners.onEvent(new EventDispatcher<UpdateListener, Void>() {
                    @Override
                    public void onEvent(UpdateListener eventListener, Void arg) {
                        eventListener.onChangedPreviousAction(entity);
                    }
                }, null);
            }
        };
        this.changedEntitiesDispatcher = new EventDispatcher<ChangeListener, Void>() {
            @Override
            public void onEvent(ChangeListener eventListener, Void arg) {
                eventListener.onChangedEntities();
            }
        };
    }

    /**
     * Registers a new listener to listen for changes in the population
     * (add, remove entities or change selection). The registered listener can
     * be removed using the returned reference.
     * <P>
     * Note that in case the listener was already registered implementations
     * may ignore this call as a no-op.
     *
     * @param listener the listener to be notified when the population changes.
     *   This argument cannot be {@code null}.
     * @return the reference through which the currently added listener can be
     *   removed. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public ListenerRef addChangeListener(ChangeListener listener) {
        return changeListeners.registerListener(listener);
    }

    /**
     * Registers a new listener to listen for changes in the properties of the
     * entities of this population. The registered listener can be removed using
     * the returned reference.
     * <P>
     * Note that in case the listener was already registered implementations
     * may ignore this call as a no-op.
     * <P>
     * To listen for changes in a particular entity: Register a listener with
     * that particular entity using: {@link CombatEntity#addUpdateListener(CombatEntity.UpdateListener)}.
     *
     * @param listener the listener to be notified when the property of an
     *   entity of this population changes. This argument cannot be
     *   {@code null}.
     * @return the reference through which the currently added listener can be
     *   removed. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public ListenerRef addUpdateListener(UpdateListener listener) {
        return updateListeners.registerListener(listener);
    }

    private void dispatchChangedEntites() {
        changeListeners.onEvent(changedEntitiesDispatcher, null);
    }

    /**
     * Returns all the entities in this population.
     *
     * @return the entities of this population. The returned {@code Collection}
     *   is independent of this model (i.e.: adding or removing entities from
     *   this population has no effect on the returned {@code Collection}. Note
     *   however that modifying the elements of the returned {@code Collection}
     *   will actually modify the entities in this population and will cause
     *   appropriate events to be fired. This method never returns {@code null},
     *   in case this population is empty, this method returns an empty
     *   {@code Collection}. The returned collection is not necessarily
     *   mutable.
     */
    public Collection<CombatEntity> getEntities() {
        return new ArrayList<>(entities.keySet());
    }

    /**
     * Checks whether the given entity is in this population. The equality of
     * entities is based on the {@link CombatEntity#equals(Object) equals}
     * method. If the specified entity is in this population trying to
     * {@link #addEntity(CombatEntity) add} it to this population will have
     * no effect.
     *
     * @param entity the entity to be checked if it is in this population. This
     *   argument cannot be {@code null}.
     * @return {@code true} if the specified entity is in this population,
     *   {@code false} otherwise
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public boolean containsEntity(CombatEntity entity) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");

        return entities.containsKey(entity);
    }

    /**
     * Removes every entity from this population. This method will notify the
     * appropriate listeners of the changes in this population. Note that this
     * method will also cause the currently selected entity to be deselected.
     *
     * @throws IllegalStateException thrown if an underlying listener reselects
     *   an entity after the currently selected entity was deselected.
     */
    public void clear() {
        if (entities.isEmpty()) {
            return;
        }

        setSelection(null);

        CombatEntity currentSelection = selected;
        if (currentSelection == null) {
            for (ListenerRef listenerRef: entities.values()) {
                listenerRef.unregister();
            }
            entities.clear();
            dispatchChangedEntites();
        }
        else {
            throw new IllegalStateException("The entities cannot be removed because a listener reselected an entity.");
        }
    }

    /**
     * Removes the given entity from this population. This method will notify
     * the appropriate listeners of the changes in this population. Note that
     * this method will also cause the currently selected entity to be
     * deselected in case the selected entity is the same as the entity being
     * removed.
     * <P>
     * The equality of entities is based on the
     * {@link CombatEntity#equals(Object) equals} method.
     *
     * @param entity the entity to be removed from this population. This
     *   argument cannot be {@code null}.
     * @return {@code true} if the specified entity was in this population
     *   and needed to be removed, {@code false} otherwise
     *
     * @throws IllegalStateException thrown if an underlying listener reselects
     *   the entity being removed after it was deselected.
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public boolean removeEntity(CombatEntity entity) {
        ExceptionHelper.checkNotNullArgument(entity, "entity");

        if (Objects.equals(entity, selected)) {
            setSelection(null);
            if (Objects.equals(entity, selected)) {
                throw new IllegalStateException("The entity cannot be removed because a listener reselected it.");
            }
        }

        ListenerRef listenerRef = entities.remove(entity);
        if (listenerRef != null) {
            listenerRef.unregister();
            dispatchChangedEntites();
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Adds a single entity to this population if the entity is not in the
     * population already. This method will notify the appropriate listeners of
     * the changes in this population.
     * <P>
     * The equality of entities is based on the
     * {@link CombatEntity#equals(Object) equals} method.
     * <P>
     * Note that this method is effectively equivalent to calling:
     * {@code addEntities(Collections.singleton(entity))}.
     *
     * @param entity the new entity to be added to this population. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified entity is {@code null}
     */
    public void addEntity(CombatEntity entity) {
        addEntities(Collections.singleton(entity));
    }

    /**
     * Adds a some entities to this population if they are not in the
     * population already. This method will notify the appropriate listeners of
     * the changes in this population.
     * <P>
     * The equality of entities is based on the
     * {@link CombatEntity#equals(Object) equals} method.
     *
     * @param newEntities the entities to be added to this population. This
     *   argument cannot be {@code null} and cannot contain {@code null}
     *   entities but can be an empty {@code Collection}.
     *
     * @throws NullPointerException thrown if the specified {@code Collection}
     *   is {@code null} or contains {@code null} elements
     */
    public void addEntities(Collection<? extends CombatEntity> newEntities) {
        if (!newEntities.isEmpty()) {
            ExceptionHelper.checkNotNullElements(newEntities, "newEntities");
            boolean changed = false;
            for (CombatEntity entity: newEntities) {
                // Although this should always succeed, buggy (or malicious)
                // implementations of the provided collection may return
                // different entities. So to be on the safe side, we check
                // the entity again.
                //
                // Note that in case of concurrent modifications, even with
                // this check, we may add a {@code null} element. Although it
                // is highly unlikely to happen and we cannot offer any
                // guarantees in the face of concurrent modifications.
                ExceptionHelper.checkNotNullArgument(entity, "newEntities[?]");

                ListenerRef listenerRef = entity.addUpdateListener(entityUpdateListener);
                ListenerRef prevRef = entities.put(entity, listenerRef);
                if (prevRef != null) {
                    prevRef.unregister();
                }
                else {
                    changed = true;
                }
            }

            if (changed) {
                dispatchChangedEntites();
            }
        }
    }

    /**
     * Returns the currently selected entity. The selected entity is always part
     * of this population.
     *
     * @return the currently selected entity or {@code null} if there is no
     *   entity currently selected.
     */
    public CombatEntity getSelection() {
        return selected;
    }

    private void dispatchChangeSelection(
            final CombatEntity oldSelection,
            final CombatEntity newSelection) {

        if (Objects.equals(oldSelection, newSelection)) {
            return;
        }

        changeListeners.onEvent(new EventDispatcher<ChangeListener, Void>() {
            @Override
            public void onEvent(ChangeListener eventListener, Void arg) {
                eventListener.onChangedSelection(oldSelection, newSelection);
            }
        }, null);
    }

    /**
     * Sets the given entity as the selected entity of this population. The
     * previously selected entity will be deselected. In case the specified
     * entity is not part of this population it will be added before actually
     * being selected. This method will notify the appropriate listeners of the
     * changes in this population and the new selection.
     *
     * @param toSelect the entity to be selected. If this argument is
     *   {@code null} no entity will be selected.
     *
     * @throws IllegalStateException thrown if a listener removed the entity
     *   to be selected after being added
     */
    public void setSelection(CombatEntity toSelect) {
        if (Objects.equals(toSelect, selected)) {
            // Already selected, there is nothing to do.
            return;
        }

        if (toSelect != null && !entities.containsKey(toSelect)) {
            addEntity(toSelect);

            if (!entities.containsKey(toSelect)) {
               throw new IllegalStateException("The entity was removed by a listener after being added.");
            }
        }

        CombatEntity oldSelection = selected;
        selected = toSelect;
        dispatchChangeSelection(oldSelection, toSelect);
    }
}
