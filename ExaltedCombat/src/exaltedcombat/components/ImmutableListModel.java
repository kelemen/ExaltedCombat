package exaltedcombat.components;

import java.util.*;

import javax.swing.*;

/**
 * Defines an unmodifiable list model. The elements of this list model can
 * be specified at construction time and cannot be modified later.
 * <P>
 * Note that instances of this class are safe to shared by multiple list
 * controls and this class is safe to use by multiple concurrent threads. Note
 * however that this class is not required to be transparent to any
 * synchronization.
 *
 * @param <E> the type of the elements in the list
 *
 * @author Kelemen Attila
 */
public final class ImmutableListModel<E> extends AbstractListModel<E> {
    private static final long serialVersionUID = -8863763911057478839L;

    private final List<E> elements;

    /**
     * Creates a list model with no elements.
     */
    public ImmutableListModel() {
        this(Collections.<E>emptyList());
    }

    /**
     * Creates a list model containing the elements specified by the passed
     * array in order.
     * <P>
     * Note that the content of the passed array will be copied and modifying
     * this array after this constructor returns will have no effect on the
     * newly created list model.
     *
     * @param elements the elements of the list model in order. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if {@code elements} is {@code null}
     */
    public ImmutableListModel(E[] elements) {
        this(Arrays.asList(elements));
    }

    /**
     * Creates a list model containing the elements specified by the passed
     * collection. The order of the elements in the new list model will be
     * ordered as they were returned by the {@link Iterator iterator} of the
     * passed collection.
     * <P>
     * Note that the content of the passed collection will be copied and
     * modifying this collection after this constructor returns will have no
     * effect on the newly created list model.
     *
     * @param elements the elements of the list model in order. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if {@code elements} is {@code null}
     */
    public ImmutableListModel(Collection<? extends E> elements) {
        this.elements = new ArrayList<>(elements);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public int getSize() {
        return elements.size();
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public E getElementAt(int index) {
        return elements.get(index);
    }
}
