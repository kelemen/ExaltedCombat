package exaltedcombat.components;

import java.util.*;

import javax.swing.*;

/**
 * A subclass of {@link DefaultListModel DefaultListModel} allowing users to
 * send notification about changed contents. This list model is useful if
 * the elements of the list are mutable so if they change one of the
 * {@code updateContent} must be called which will in turn notify the owner
 * list.
 *
 * @param <E> the type of the elements in the list
 *
 * @author Kelemen Attila
 */
public final class UpdatableListModel<E> extends DefaultListModel<E> {
    private static final long serialVersionUID = 3795395945955504317L;

    /**
     * Creates a new list model preinitialized with the specified elements.
     * The model will contain the specified elements in the order they are
     * returned by the iterator of the specified collection.
     *
     * @param elements the initial elements of the list model. This argument
     *   cannot be {@code null} nut can be an empty list instead.
     *
     * @throws NullPointerException if the specified collection is {@code null}
     */
    public UpdatableListModel(Collection<? extends E> elements) {
        for (E element: elements) {
            addElement(element);
        }
    }

    /**
     * Notifies registered listeners that the contents of all the elements
     * in this list model might have changed.
     */
    public void updateContent() {
        if (!isEmpty()) {
            fireContentsChanged(this, 0, getSize() - 1);
        }
    }

    /**
     * Notifies registered listeners that the content of the element at the
     * specified index has changed. The specified index should be a valid index
     * in this model because registered listeners may not expect an out of range
     * index.
     *
     * @param index the index of the element whose content has changed
     */
    public void updateContent(int index) {
        fireContentsChanged(this, index, index);
    }

    /**
     * Notifies registered listeners that the content of the elements between
     * specified indexes has changed. The specified indexes should be a valid
     * indexes in this model because registered listeners may not expect an out
     * of range index. Not however that {@code index0} and {@code index1} can
     * be exchanged (i.e.: it is not required that {@code index0 <= index1}).
     *
     * @param index0 one end of the new interval (inclusive)
     * @param index1 the other end of the new interval (inclusive)
     */
    public void updateContent(int index0, int index1) {
        fireContentsChanged(this, index0, index1);
    }
}
