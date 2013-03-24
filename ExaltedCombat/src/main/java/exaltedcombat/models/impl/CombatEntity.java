package exaltedcombat.models.impl;

import exaltedcombat.actions.CombatEntityAction;
import java.awt.Color;
import java.util.*;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a model and an implementation of an entity in ExaltedCombat.
 * A combat entity in ExaltedCombat has the following properties:
 * <ul>
 *  <li>
 *   A {@link #getShortName() short name} to be displayed as a caption or title.
 *  </li>
 *  <li>
 *   A {@link #getColor() color} associated with the entity to provide easily
 *   recognized visual distinction to entities.
 *  </li>
 *  <li>
 *   A possibly longer {@link #getDescription() description} of the entities
 *   containing more detailed information about it.
 *  </li>
 *  <li>
 *   The {@link #getPreviousActions() history of actions} of an entity.
 *  </li>
 * </ul>
 * <P>
 * Two combat entities are considered equivalent if they are actually the same
 * objects (the same as "=="). So even if they have the same properties they can
 * be different entities.
 * <P>
 * As with all models, user can be notified when changes occur to the model
 * (i.e.: some properties of it were changed) by
 * {@link #addUpdateListener(CombatEntity.UpdateListener) registering} a
 * listener.
 *
 * <h3>Models in ExaltedCombat</h3>
 * In general models in ExaltedCombat are not safe to use by multiple threads
 * concurrently and may not transparent to synchronization. In practice this
 * usually means that the use of a model must be restricted to a certain thread.
 * Since models need to interact with the GUI: ExaltedCombat requires that all
 * the models be used only from the AWT event dispatching thread.
 *
 * @see CombatEntities
 * @author Kelemen Attila
 */
public final class CombatEntity {
    /**
     * The interface to listen for changes in the properties of an entity.
     */
    public static interface UpdateListener {
        /**
         * Invoked when the {@link #getShortName() short name} of an entity
         * has been changed.
         * <P>
         * When this method is called the name of the entity has already been
         * changed.
         *
         * @param entity the entity whose name was changed. This argument cannot
         *   be {@code null}.
         */
        public void onChangedShortName(CombatEntity entity);

        /**
         * Invoked when the {@link #getColor() color} of an entity
         * has been changed.
         * <P>
         * When this method is called the color of the entity has already been
         * changed.
         *
         * @param entity the entity whose color was changed. This argument
         *   cannot be {@code null}.
         */
        public void onChangedColor(CombatEntity entity);

        /**
         * Invoked when the {@link #getDescription() description} of an entity
         * has been changed.
         * <P>
         * When this method is called the description of the entity has already
         * been changed.
         *
         * @param entity the entity whose description was changed. This argument
         *   cannot be {@code null}.
         */
        public void onChangedDescription(CombatEntity entity);

        /**
         * Invoked when the {@link #getPreviousActions() history of actions} of
         * an entity has been changed.
         * <P>
         * When this method is called the history of actions of the entity has
         * already been changed.
         *
         * @param entity the entity whose history of actions was changed.
         *   This argument cannot be {@code null}.
         */
        public void onChangedPreviousAction(CombatEntity entity);
    }

    private final ListenerManager<UpdateListener> listeners;
    private String shortName;
    private Color color;
    private String description;
    private final List<CombatEntityAction> previousAction;

    private static final EventDispatcher<UpdateListener, CombatEntity> changeShortNameDispatcher;
    private static final EventDispatcher<UpdateListener, CombatEntity> changeColorDispatcher;
    private static final EventDispatcher<UpdateListener, CombatEntity> changeDescriptionDispatcher;
    private static final EventDispatcher<UpdateListener, CombatEntity> changeActionDispatcher;

    static {
        changeShortNameDispatcher = new EventDispatcher<UpdateListener, CombatEntity>() {
            @Override
            public void onEvent(UpdateListener eventListener, CombatEntity entity) {
                eventListener.onChangedShortName(entity);
            }
        };
        changeColorDispatcher = new EventDispatcher<UpdateListener, CombatEntity>() {
            @Override
            public void onEvent(UpdateListener eventListener, CombatEntity entity) {
                eventListener.onChangedColor(entity);
            }
        };
        changeDescriptionDispatcher = new EventDispatcher<UpdateListener, CombatEntity>() {
            @Override
            public void onEvent(UpdateListener eventListener, CombatEntity entity) {
                eventListener.onChangedDescription(entity);
            }
        };
        changeActionDispatcher = new EventDispatcher<UpdateListener, CombatEntity>() {
            @Override
            public void onEvent(UpdateListener eventListener, CombatEntity entity) {
                eventListener.onChangedPreviousAction(entity);
            }
        };
    }

    /**
     * Creates a new combat entity with the same properties as the specified
     * entity but without its history of actions. The new entity will have an
     * empty history of actions when created.
     *
     * @param other the entity whose properties are to be copied. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified entity is
     *   {@code null}
     */
    public CombatEntity(CombatEntity other) {
        this(other.getShortName(), other.getColor(), other.getDescription());
    }

    /**
     * Creates a new combat entity with the specified properties. The new entity
     * will have an empty history of actions when created.
     *
     * @param shortName the initial {@link #getShortName() short name} of the
     *   new entity. This argument cannot be {@code null}.
     * @param color the initial {@link #getColor() color} of the new entity.
     *   This argument cannot be {@code null}.
     * @param description the initial {@link #getDescription() description}
     *   of the new entity. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public CombatEntity(String shortName, Color color, String description) {
        ExceptionHelper.checkNotNullArgument(shortName, "shortName");
        ExceptionHelper.checkNotNullArgument(color, "color");
        ExceptionHelper.checkNotNullArgument(description, "description");

        this.listeners = new CopyOnTriggerListenerManager<>();
        this.shortName = shortName;
        this.color = color;
        this.description = description;
        this.previousAction = new LinkedList<>();
    }

    /**
     * Registers a new listener to listen for changes in the properties of this
     * entity.  The registered listener can be removed using the returned
     * reference.
     * <P>
     * Note that in case the listener was already registered implementations
     * may ignore this call as a no-op.
     *
     * @param listener the listener to be notified when a property of this
     *   entity changes. This argument cannot be {@code null}.
     * @return the reference through which the newly added listener can be
     *   removed. This method never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified listener is
     *   {@code null}
     */
    public ListenerRef addUpdateListener(UpdateListener listener) {
        return listeners.registerListener(listener);
    }

    /**
     * Returns the color of this entity. The color of an entity is intended to
     * be used to provide easily recognizable visual look to entities.
     *
     * @return the color of this entity. This method never returns {@code null}.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Associates a new color with this entity. The associated color can be
     * retrieved by a {@link #getColor() getColor()} method call.
     * <P>
     * This method will notify the
     * {@link #addUpdateListener(CombatEntity.UpdateListener) registered}
     * property change listeners.
     *
     * @param color the new color of this entity. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified color is
     *   {@code null}
     */
    public void setColor(Color color) {
        ExceptionHelper.checkNotNullArgument(color, "color");

        if (!Objects.equals(this.color, color)) {
            this.color = color;
            listeners.onEvent(changeColorDispatcher, this);
        }
    }

    /**
     * Returns the description of this entity. The description is intended to be
     * used to give detailed information about this entity.
     *
     * @return the description of this entity. This method never returns
     *   {@code null}.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Associates a new description with this entity. The associated description
     * can be retrieved by a {@link #getDescription() getDescription()} method
     * call.
     * <P>
     * This method will notify the
     * {@link #addUpdateListener(CombatEntity.UpdateListener) registered}
     * property change listeners.
     *
     * @param description the new description of this entity. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified description is
     *   {@code null}
     */
    public void setDescription(String description) {
        ExceptionHelper.checkNotNullArgument(description, "description");

        if (!Objects.equals(this.description, description)) {
            this.description = description;
            listeners.onEvent(changeDescriptionDispatcher, this);
        }
    }

    /**
     * Returns the short name of this entity. The short name is intended to be
     * used as a caption or title.
     *
     * @return the short name of this entity. This method never returns
     *   {@code null}.
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Associates a new short name with this entity. The associated short name
     * can be retrieved by a {@link #getShortName() getShortName()} method call.
     * <P>
     * This method will notify the
     * {@link #addUpdateListener(CombatEntity.UpdateListener) registered}
     * property change listeners.
     *
     * @param shortName the new short name of this entity. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified short name is
     *   {@code null}
     */
    public void setShortName(String shortName) {
        ExceptionHelper.checkNotNullArgument(shortName, "shortName");

        if (!Objects.equals(this.shortName, shortName)) {
            this.shortName = shortName;
            listeners.onEvent(changeShortNameDispatcher, this);
        }
    }

    /**
     * Removes the last action from the
     * {@link #getPreviousActions() history of actions} of this entity.
     * <P>
     * This method will notify the
     * {@link #addUpdateListener(CombatEntity.UpdateListener) registered}
     * property change listeners.
     *
     * @throws NoSuchElementException thrown if there are no actions in the
     *   history of actions of this entity
     */
    public void removeLastAction() {
        if (previousAction.isEmpty()) {
            throw new NoSuchElementException();
        }

        previousAction.remove(previousAction.size() - 1);
        listeners.onEvent(changeActionDispatcher, this);
    }

    /**
     * Appends a new action to the end of the
     * {@link #getPreviousActions() history of actions} of this entity.
     * <P>
     * This method will notify the
     * {@link #addUpdateListener(CombatEntity.UpdateListener) registered}
     * property change listeners.
     *
     * @param action the new action to be appended to the end of the history
     *   of actions of this entity. This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the passed action is {@code null}
     */
    public void addAction(CombatEntityAction action) {
        addActions(Collections.singleton(action));
    }

    /**
     * Appends a {@code Collection} of new actions to the end of the
     * {@link #getPreviousActions() history of actions} of this entity. The
     * actions will be appended in the order the
     * {@link Collection#iterator() iterator} of the {@code Collection} returns
     * them.
     * <P>
     * This method will notify the
     * {@link #addUpdateListener(CombatEntity.UpdateListener) registered}
     * property change listeners.
     *
     * @param actions the new actions to be appended to the end of the history
     *   of actions of this entity. This argument cannot be {@code null} and
     *   cannot contain {@code null} elements.
     *
     * @throws NullPointerException thrown if the passed {@code Collection} is
     *   {@code null} or contains {@code null} elements
     */
    public void addActions(Collection<? extends CombatEntityAction> actions) {
        if (!actions.isEmpty()) {
            ExceptionHelper.checkNotNullElements(actions, "actions");

            for (CombatEntityAction action: actions) {
                // This should only fail if the passed {@code Collection} is
                // broken.
                ExceptionHelper.checkNotNullArgument(action, "action");
                previousAction.add(action);
            }
            listeners.onEvent(changeActionDispatcher, this);
        }
    }

    /**
     * Returns the last action in the
     * {@link #getPreviousActions() history of actions} of this entity or
     * {@code null} if the history is empty.
     *
     * @return the last action in the history of actions of this entity or
     *   {@code null} if the history is empty
     */
    public CombatEntityAction getLastAction() {
        int actionCount = previousAction.size();
        return actionCount > 0 ? previousAction.get(actionCount - 1) : null;
    }

    /**
     * Returns the history of actions of this entity. This property is intended
     * to record the previous actions of this entity as a reference to the user.
     * The returned list will contain the actions in the order they occurred
     * (was added by the
     * {@link #addAction(CombatEntityAction) addAction(CombatEntityAction)} or
     * the {@link #addActions(Collection) addActions(Collection)} method.
     *
     * @return the history of actions of this entity. This method never returns
     *   {@code null} but may return an empty list.
     */
    public List<CombatEntityAction> getPreviousActions() {
        if (previousAction.isEmpty()) {
            return Collections.emptyList();
        }
        else {
            return new ArrayList<>(previousAction);
        }
    }
}
