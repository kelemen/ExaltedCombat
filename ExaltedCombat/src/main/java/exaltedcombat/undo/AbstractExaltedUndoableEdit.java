package exaltedcombat.undo;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

/**
 * Defines a base class to be used for {@link UndoableEdit}s in ExaltedCombat.
 * This class defines a default implementation for the
 * {@link #getRedoPresentationName() getRedoPresentationName()} and
 * {@link #getUndoPresentationName() getUndoPresentationName()} methods and the
 * methods derived from {@link AbstractUndoableEdit AbstractUndoableEdit}.
 *
 * @see AbstractEntityActionUndoableEdit
 * @author Kelemen Attila
 */
public abstract class AbstractExaltedUndoableEdit extends AbstractUndoableEdit {
    private static final long serialVersionUID = -3843700513421025477L;

    private static final LocalizedString UNDO_ACTION_TEXT = StringContainer.getDefaultString("UNDO_ACTION_TEXT");
    private static final LocalizedString REDO_ACTION_TEXT = StringContainer.getDefaultString("REDO_ACTION_TEXT");

    /**
     * Returns a localized name for the redo action, intended to be used
     * as a caption for redo actions. The returned string also contains the
     * value returned by the {@link #getPresentationName() getPresentationName()}.
     *
     * @return the localized name for the redo action. This method never returns
     *   {@code null}.
     */
    @Override
    public String getRedoPresentationName() {
        return REDO_ACTION_TEXT.format(getPresentationName());
    }

    /**
     * Returns a localized name for the undo action, intended to be used
     * as a caption for undo actions. The returned string also contains the
     * value returned by the {@link #getPresentationName() getPresentationName()}.
     *
     * @return the localized name for the undo action. This method never returns
     *   {@code null}.
     */
    @Override
    public String getUndoPresentationName() {
        return UNDO_ACTION_TEXT.format(getPresentationName());
    }
}
