package exaltedcombat.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import org.jtrim.utils.ExceptionHelper;

/**
 * Defines a three layered border for Swing components. The three layers of the
 * border are roughly the same size and the outer and inner layer are of the
 * same color while the middle layer can be specified to use a different color.
 * <P>
 * This border is intended to use for indicating that a component is selected.
 * This implementation has the advantage over simple borders that it can be
 * visible on any background if the colors of its layers are different.
 * <P>
 * Note that this border can be defined by specifying its thickness (i.e.: the
 * width of the border in pixels) therefore there are three notable special case
 * where the above specifications for the border cannot be met:
 * <ul>
 *  <li>Thickness == 0 means that there is no border at all.</li>
 *  <li>Thickness == 1 means that there is a one pixel wide border having the
 *   color specified for the outer border.</li>
 *  <li>Thickness == 2 means that there is no inner border but a one pixel wide
 *   outer border and a one pixel wide middle border. The middle border having
 *   the inner color.</li>
 * </ul>
 * <P>
 * Instances of this border are explicitly allowed to be used by multiple
 * Swing components.
 * <P>
 * The implementation of this border is thread-safe but invoking any of its
 * methods will cause the arguments of the method in question to be accessed.
 * These arguments are usually can only be accessed from the AWT event
 * dispatching thread, therefore it is recommended to only use instances of this
 * class on the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public final class BorderedBorder implements Border {
    private final Border wrappedBorder;

    /**
     * Creates a new border with the specified thickness and colors.
     *
     * @param thickness the width of the border in pixels. This argument cannot
     *   be a negative integer.
     * @param outerColor the color of the first and last layer of this border.
     *   This argument cannot be {@code null}.
     * @param innerColor the color of the middle layer of this border. This
     *   argument cannot be {@code null}.
     *
     * @throws IllegalArgumentException thrown if the specified thickness is a
     *   negative integer
     * @throws NullPointerException thrown if any of the specified colors is
     *   {@code null}
     */
    public BorderedBorder(int thickness, Color outerColor, Color innerColor) {
        ExceptionHelper.checkArgumentInRange(thickness, 0, Integer.MAX_VALUE, "thickness");

        Objects.requireNonNull(outerColor, "outerColor");
        Objects.requireNonNull(innerColor, "innerColor");

        switch (thickness) {
            case 0:
                this.wrappedBorder = BorderFactory.createEmptyBorder();
                break;
            case 1:
                this.wrappedBorder = BorderFactory.createLineBorder(outerColor, 1);
                break;
            case 2:
            {
                Border border1 = BorderFactory.createLineBorder(innerColor, 1);
                Border border2 = BorderFactory.createLineBorder(outerColor, 1);
                this.wrappedBorder = BorderFactory.createCompoundBorder(border1, border2);
                break;
            }
            default:
                int size = thickness / 3;
                int rem = thickness % 3;

                int size1 = size;
                int size2 = size;
                int size3 = size;

                if (rem == 1) {
                    size2++;
                }
                else if (rem == 2) {
                    size1++;
                    size2++;
                }

                Border border1 = BorderFactory.createLineBorder(outerColor, size1);
                Border border2 = BorderFactory.createLineBorder(innerColor, size2);
                Border border3 = BorderFactory.createLineBorder(outerColor, size3);

                Border inner = BorderFactory.createCompoundBorder(border1, border2);
                this.wrappedBorder = BorderFactory.createCompoundBorder(inner, border3);
                break;
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        wrappedBorder.paintBorder(c, g, x, y, width, height);
    }

    /**
     * {@inheritDoc }
     *
     * @return the insets of the border
     */
    @Override
    public Insets getBorderInsets(Component c) {
        return wrappedBorder.getBorderInsets(c);
    }

    /**
     * {@inheritDoc }
     *
     * @return {@code true} if this border is opaque, {@code false} otherwise
     */
    @Override
    public boolean isBorderOpaque() {
        return wrappedBorder.isBorderOpaque();
    }

}
