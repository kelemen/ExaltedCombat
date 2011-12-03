package exaltedcombat.undo;

import java.util.*;

import javax.swing.undo.*;

/**
 * Defines an {@link UndoableEdit UndoableEdit} aggregating multiple
 * {@code UndoableEdit} instances. Instances of this class redo and undo the
 * aggregated {@code UndoableEdit} instances as a single edit action and assume
 * that if the first edit can be performed (first edit in redo and last edit
 * in undo), all the following edits can as well.
 *
 * @author Kelemen Attila
 */
public class SafeCompoundEdit extends AbstractUndoableEdit {
    private static final long serialVersionUID = -1190878482381509816L;

    private static final UndoableEdit[] EMPTY_EDIT_ARRAY = new UndoableEdit[0];

    private final UndoableEdit[] edits;

    /**
     * Creates a new aggregated {@code UndoableEdit} with the given
     * {@code UndoableEdit} subedits. If no subedits were specified this
     * composite edit is a no-op.
     *
     * @param edits the subedits in the order they are to be preformed
     *   (i.e.: the order the redo action should perform them). This argument
     *   cannot be {@code null} but can contain {@code null} elements and those
     *   elements will be ignored. The array is allowed to be empty.
     *
     * @throws NullPointerException thrown if the {@code edits} argument
     *   is {@code null}
     */
    public SafeCompoundEdit(UndoableEdit... edits) {
        List<UndoableEdit> editList = new LinkedList<>();
        for (UndoableEdit edit: edits) {
            if (edit != null) {
                editList.add(edit);
            }
        }

        this.edits = editList.toArray(EMPTY_EDIT_ARRAY);
    }

    /**
     * Returns the number of subedits this composite edit contains.
     *
     * @return the number of subedits this composite edit contains. This method
     *   always returns an integer greater than or equal to zero.
     */
    public int getEditCount() {
        return edits.length;
    }

    /**
     * Returns {@code true} if this edit is {@code alive}, {@code hasBeenDone}
     * is {@code false} and the first subedit can be redone (if there are any).
     *
     * @return {@code true} if this edit is {@code alive}, {@code hasBeenDone}
     *   is {@code false} and the first subedit can be redone (if there are any)'
     *   {@code false} otherwise
     */
    @Override
    public boolean canRedo() {
        if (edits.length == 0) {
            return super.canRedo();
        }
        else {
            return super.canRedo() && edits[0].canRedo();
        }
    }

    /**
     * Returns {@code true} if this edit is {@code alive}, {@code hasBeenDone}
     * is {@code true} and the last subedit can be undone (if there are any).
     *
     * @return {@code true} if this edit is {@code alive}, {@code hasBeenDone}
     *   is {@code true} and the last subedit can be undone (if there are any);
     *   {@code false} otherwise
     *
     * @see javax.swing.undo.AbstractUndoableEdit#canUndo() AbstractUndoableEdit.canUndo()
     */
    @Override
    public boolean canUndo() {
        if (edits.length == 0) {
            return super.canUndo();
        }
        else {
            return super.canUndo() && edits[edits.length - 1].canUndo();
        }
    }

    /**
     * Returns the presentation name of the last subedit. If there are no
     * subedits this methods returns an empty string.
     *
     * @return the name of the {@code UndoableEdit} which can be presented to
     *   the user. If the last subedit cannot return a {@code null} value for
     *   this presentation name, this method will not return {@code null} as
     *   well.
     */
    @Override
    public String getPresentationName() {
        if (edits.length == 0) {
            return "";
        }
        else {
            return edits[edits.length - 1].getPresentationName();
        }
    }

    /**
     * Returns the redo presentation name of the last subedit. If there are no
     * subedits this method returns the value inherited from
     * {@link AbstractUndoableEdit#getRedoPresentationName() AbstractUndoableEdit.getRedoPresentationName()}.
     *
     * @return a string value which can presented to the user as a redo button
     *   caption. If the last subedit cannot return a {@code null} value for
     *   this redo presentation name, this method will not return {@code null}
     *   as well.
     */
    @Override
    public String getRedoPresentationName() {
        if (edits.length == 0) {
            return super.getRedoPresentationName();
        }
        else {
            return edits[edits.length - 1].getRedoPresentationName();
        }
    }

    /**
     * Returns the undo presentation name of the last subedit. If there are no
     * subedits this method returns the value inherited from
     * {@link AbstractUndoableEdit#getUndoPresentationName() AbstractUndoableEdit.getUndoPresentationName()}.
     *
     * @return a string value which can presented to the user as a undo button
     *   caption. If the last subedit cannot return a {@code null} value for
     *   this undo presentation name, this method will not return {@code null}
     *   as well.
     */
    @Override
    public String getUndoPresentationName() {
        if (edits.length == 0) {
            return super.getUndoPresentationName();
        }
        else {
            return edits[edits.length - 1].getUndoPresentationName();
        }
    }

    /**
     * Returns {@code true} if at least one of the subedits'
     * {@code isSignificant()} method returns {@code true}. Notice that if there
     * are no subedits, this method will always return {@code false}.
     *
     * @return {@code true} if at least one of the subedits is significant,
     *   {@code false} otherwise
     */
    @Override
    public boolean isSignificant() {
        for (UndoableEdit edit: edits) {
            if (edit.isSignificant()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets {@code hasBeenDone} to {@code true} and calls the {@code redo()}
     * method of every subedits in order.
     *
     * @throws CannotRedoException thrown if {@link #canRedo() canRedo()}
     *   returns {@code false} or any of the subedits throw a
     *   {@code CannotRedoException}
     */
    @Override
    public void redo() {
        super.redo();
        for (UndoableEdit edit: edits) {
            edit.redo();
        }
    }

    /**
     * Sets {@code hasBeenDone} to {@code false} and calls the {@code undo()}
     * method of every subedits in reversed order.
     *
     * @throws CannotUndoException thrown if {@link #canUndo() canUndo()}
     *   returns {@code false} or any of the subedits throw a
     *   {@code CannotUndoException}
     */
    @Override
    public void undo() {
        super.undo();
        for (int i = edits.length - 1; i >= 0; i--) {
            edits[i].undo();
        }
    }
}
