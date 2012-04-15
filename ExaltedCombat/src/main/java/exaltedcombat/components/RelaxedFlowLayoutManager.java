package exaltedcombat.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import org.jtrim.utils.ExceptionHelper;

/**
 * A layout manager positioning components in a single horizontal row next to
 * each other. Unlike {@link java.awt.FlowLayout FlowLayout} this layout manager
 * does not modify the size of the components and its minimum size is not
 * dependent on the components.
 * <P>
 * Note that as of the current implementation this layout manager always orders
 * its elements from left to right.
 * <P>
 * Note that like most Swing components this class is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class RelaxedFlowLayoutManager implements LayoutManager {
    private final int gap;

    /**
     * Creates a layout manager using five pixel wide gaps between components.
     */
    public RelaxedFlowLayoutManager() {
        this(5);
    }

    /**
     * Creates a layout manager using the specified width for gaps between
     * components.
     *
     * @param gap the width of the gap between components in pixels. This
     *   argument cannot be a negative integer.
     *
     * @throws IllegalArgumentException thrown if the specified gap is a
     *   negative integer
     */
    public RelaxedFlowLayoutManager(int gap) {
        ExceptionHelper.checkArgumentInRange(gap, 0, Integer.MAX_VALUE, "gap");
        this.gap = gap;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void removeLayoutComponent(Component comp) {
    }

    /**
     * {@inheritDoc }
     *
     * @return the preferred size dimensions for the specified container. This
     *   method never returns {@code null}.
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int height = 0;
        int width = 0;

        int cCount = parent.getComponentCount();
        for (int i = 0; i < cCount; i++) {
            Component c = parent.getComponent(i);
            height = Math.max(height, c.getHeight());
            width += gap;
            width += c.getWidth();
        }
        width += gap;

        return new Dimension(Math.max(1, width), Math.max(1, height));
    }

    /**
     * {@inheritDoc }
     *
     * @return the minimum size dimensions for the specified container. This
     *   method never returns {@code null}.
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(1, 1);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void layoutContainer(Container parent) {
        int parentHeight = parent.getHeight();

        int x = gap;
        int cCount = parent.getComponentCount();
        for (int i = 0; i < cCount; i++) {
            Component c = parent.getComponent(i);
            int width = c.getWidth();
            int height = c.getHeight();

            int y;
            int baseLine = c.getBaseline(width, height);
            if (baseLine >= 0) {
                y = parentHeight / 2 - baseLine;
            }
            else {
                y = (parentHeight - height) / 2;
            }

            c.setLocation(x, y);
            x += c.getWidth();
            x += gap;
        }
    }

}
