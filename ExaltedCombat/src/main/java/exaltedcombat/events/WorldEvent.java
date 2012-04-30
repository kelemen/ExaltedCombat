package exaltedcombat.events;

import exaltedcombat.models.impl.CombatEntity;
import java.util.Collection;

/**
 * The possible general events which can occur in ExaltedCombat. The arguments
 * these events expect are documented in the documentation of their respective
 * instance which is the same as the one returned by their {@code getArgClass()}
 * method.
 *
 * @author Kelemen Attila
 */
public final class WorldEvent {
    /**
     * The event occurs when the
     * {@link exaltedcombat.models.impl.CombatEntity#getShortName() name}
     * of an entity has changed. When this event occurs the name has already
     * been changed and from the referenced combat entity the new name can be
     * retrieved.
     * <P>
     * The required type of the argument for this event is
     * {@link exaltedcombat.models.impl.CombatEntity CombatEntity} and cannot be
     * {@code null}. The argument is the combat entity whose name has been
     * changed.
     */
    public static final ExaltedEvent<CombatEntity> ENTITY_NAME_CHANGE
            = ExaltedEvent.Helper.createExaltedEvent(CombatEntity.class);

    /**
     * The event occurs when the
     * {@link exaltedcombat.models.impl.CombatEntity#getColor() color}
     * of an entity has changed. When this event occurs the color has already
     * been changed and from the referenced combat entity the new color can be
     * retrieved.
     * <P>
     * The required type of the argument for this event is
     * {@link exaltedcombat.models.impl.CombatEntity CombatEntity} and cannot be
     * {@code null}. The argument is the combat entity whose color has been
     * changed.
     */
    public static final ExaltedEvent<CombatEntity> ENTITY_COLOR_CHANGE
            = ExaltedEvent.Helper.createExaltedEvent(CombatEntity.class);

    /**
     * The event occurs when the
     * {@link exaltedcombat.models.impl.CombatEntity#getDescription() description}
     * of an entity has changed. When this event occurs the description has
     * already been changed and from the referenced combat entity the new
     * description can be retrieved.
     * <P>
     * The required type of the argument for this event is
     * {@link exaltedcombat.models.impl.CombatEntity CombatEntity} and cannot be
     * {@code null}. The argument is the combat entity whose description has
     * been changed.
     */
    public static final ExaltedEvent<CombatEntity> ENTITY_DESCRIPTION_CHANGE
            = ExaltedEvent.Helper.createExaltedEvent(CombatEntity.class);

    /**
     * The event occurs when the
     * {@link exaltedcombat.models.impl.CombatEntity#getPreviousActions() history of actions}
     * of an entity have changed. When this event occurs the actions have
     * already been changed and from the referenced combat entity the new
     * action history can be retrieved.
     * <P>
     * The required type of the argument for this event is
     * {@link exaltedcombat.models.impl.CombatEntity CombatEntity} and cannot be
     * {@code null}. The argument is the combat entity whose history of actions
     * have been changed.
     */
    public static final ExaltedEvent<CombatEntity> ENTITY_PREV_ACTION_CHANGE
            = ExaltedEvent.Helper.createExaltedEvent(CombatEntity.class);

    /**
     * The event occurs when a new entity was selected. Only a single combat
     * entity can be selected at a time. When this event occurs the new entity
     * was already selected.
     * <P>
     * The required type of the argument for this event is
     * {@link EntitySelectChangeArgs} and cannot be {@code null}. The argument
     * contains both the previously and newly selected entities.
     */
    public static final ExaltedEvent<EntitySelectChangeArgs> ENTITY_SELECT_CHANGE
            = ExaltedEvent.Helper.createExaltedEvent(EntitySelectChangeArgs.class);

    /**
     * The event occurs when the list of entities in ExaltedCombat changes.
     * This event can occur when an entity was removed from ExaltedCombat or
     * a new entity was added. Note however that this event can also be fired
     * when multiple entities enter or leave the world of ExaltedCombat, so the
     * whole list of entities must be checked.
     * <P>
     * The argument for this event is {@code Void}.
     */
    public static final ExaltedEvent<Void> ENTITY_LIST_CHANGE
            = ExaltedEvent.Helper.createExaltedEvent(Void.class);

    /**
     * The event occurs when an entity of ExaltedCombat joins a combat. When
     * this event occurs the entity is already in combat.
     * <P>
     * The required type of the argument for this event is
     * {@link exaltedcombat.models.impl.CombatEntity CombatEntity} and cannot be
     * {@code null}. The argument is the combat entity who joined a combat.
     */
    public static final ExaltedEvent<CombatEntity> ENTITY_ENTER_COMBAT
            = ExaltedEvent.Helper.createExaltedEvent(CombatEntity.class);

    /**
     * The event occurs when an entity of ExaltedCombat takes an action in a
     * combat with a greater than zero speed.
     * <P>
     * The required type of the argument for this event is
     * {@link exaltedcombat.models.impl.CombatEntity CombatEntity} and cannot be
     * {@code null}. The argument is the combat entity who acted in a combat.
     */
    public static final ExaltedEvent<CombatEntity> ENTITY_MOVE_IN_COMBAT
            = ExaltedEvent.Helper.createExaltedEvent(CombatEntity.class);

    /**
     * The event occurs when some entities of ExaltedCombat leave a combat.
     * <P>
     * The required type of the argument for this event is
     * {@link java.util.Collection Collection&lt;CombatEntity&gt;} and cannot be
     * {@code null} or empty. The argument is the collection of entities leaving
     * the combat.
     */
    @SuppressWarnings("unchecked") // Obviously, the below cast cannot fail.
    public static final ExaltedEvent<Collection<?>> ENTITIES_LEAVE_COMBAT
            = ExaltedEvent.Helper.createExaltedEvent((Class<Collection<?>>)(Class<?>)Collection.class);

    /**
     * The event occurs when the phase of a combat in ExaltedCombat changes.
     * ExaltedCombat defines two phases for a combat:
     * <ol>
     *  <li>Join phase</li>
     *  <li>Combat phase</li>
     * </ol>
     * In the first phase (join phase), the entities will enter the combat but
     * will not yet act. The second phase of the combat is when the combat
     * really starts and entities start taking their actions.
     * <P>
     * The argument for this event is {@code Void}.
     */
    public static final ExaltedEvent<Void> ENTITY_COMBAT_PHASE_CHANGE
            = ExaltedEvent.Helper.createExaltedEvent(Void.class);
}
