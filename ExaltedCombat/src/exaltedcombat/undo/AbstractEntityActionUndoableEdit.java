package exaltedcombat.undo;

import exaltedcombat.models.impl.*;
import exaltedcombat.actions.*;

import org.jtrim.utils.*;

/**
 * Defines a base class for {@link javax.swing.undo.UndoableEdit UndoableEdit}s
 * backed by a {@link CombatEntityAction CombatEntityAction}. Instances of this
 * class are intended to be used to undo the backing {@code CombatEntityAction}.
 * <P>
 * This class defines a default implementation for the
 * {@link #canRedo() canRedo()} and {@link #canUndo() canUndo()} methods and the
 * methods derived from
 * {@link AbstractExaltedUndoableEdit AbstractExaltedUndoableEdit}.
 *
 * @author Kelemen Attila
 */
public abstract class AbstractEntityActionUndoableEdit extends AbstractExaltedUndoableEdit {
    private static final long serialVersionUID = -3843700513421025477L;

    private final CombatEntities populationModel;
    private final CombatEntity entity;
    private final CombatEntityAction action;

    /**
     * Initializes this {@code UndoableEdit} with the given action and models
     * used by the action.
     *
     * @param populationModel the population model affected by the backing
     *   action. This argument cannot be {@code null}.
     * @param entity the entity who is defined to perform the backing action of
     *   the new {@code UndoableEdit}. This argument cannot be {@code null}.
     * @param action the backing action of the new {@code UndoableEdit}. This
     *   argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public AbstractEntityActionUndoableEdit(
            CombatEntities populationModel,
            CombatEntity entity,
            CombatEntityAction action) {
        ExceptionHelper.checkNotNullArgument(populationModel, "combatEntities");
        ExceptionHelper.checkNotNullArgument(entity, "entity");
        ExceptionHelper.checkNotNullArgument(action, "action");

        this.populationModel = populationModel;
        this.entity = entity;
        this.action = action;
    }

    /**
     * Returns {@code true} if this edit is {@code alive}, {@code hasBeenDone}
     * is {@code false} and the population contains the
     * {@link #getEntity() entity} performing the {@link #getAction() action}.
     *
     * @return {@code true} if this edit is {@code alive}, {@code hasBeenDone}
     *   is {@code false} and the population contains the
     *   {@link #getEntity() entity} performing the {@link #getAction() action};
     *   {@code false} otherwise
     *
     * @see javax.swing.undo.AbstractUndoableEdit#canRedo() AbstractUndoableEdit.canRedo()
     */
    @Override
    public boolean canRedo() {
        return super.canRedo()
                && populationModel.containsEntity(entity);
    }

    /**
     * Returns {@code true} if this edit is {@code alive}, {@code hasBeenDone}
     * is {@code true}, the population contains the
     * {@link #getEntity() entity} performing the {@link #getAction() action}
     * and the last action of this entity is the backing action.
     *
     * @return {@code true} if this edit is {@code alive}, {@code hasBeenDone}
     *   is {@code true}, the population contains the
     *   {@link #getEntity() entity} performing the {@link #getAction() action}
     *   and the last action of this entity is the backing action;
     *   {@code false} otherwise
     *
     * @see javax.swing.undo.AbstractUndoableEdit#canUndo() AbstractUndoableEdit.canUndo()
     */
    @Override
    public boolean canUndo() {
        return super.canUndo()
                && populationModel.containsEntity(entity)
                && entity.getLastAction() == action;
    }

    /**
     * Returns the action specified at construction time. This
     * {@code UndoableEdit} is defined to undo the action returned by this
     * method.
     *
     * @return the backing action of this {@code UndoableEdit}. This method
     *   never returns {@code null}.
     */
    public CombatEntityAction getAction() {
        return action;
    }

    /**
     * Returns the entity specified at construction time. This is the entity who
     * is defined to perform the backing action of this {@code UndoableEdit}.
     *
     * @return the entity who is defined to perform the backing action of this
     *   {@code UndoableEdit}. This argument cannot be {@code null}.
     */
    public CombatEntity getEntity() {
        return entity;
    }
}
