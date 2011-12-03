package exaltedcombat.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

import javax.swing.border.*;

import org.jtrim.utils.*;

/**
 * Defines a border with dashed lines. The dashes alternate between the colors
 * specified at construction time.
 * <P>
 * Note that it is unlikely to be able to completly fill the border with equal
 * dashes. As of the current implementation the bottom right corner of this
 * border may seem out of order. A future implementation may change to instead
 * dishonour the dash length.
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
public final class DashedBorder implements Border {

    private final int thickness;
    private final int dashLength;
    private final Color[] colors;

    /**
     * Creates a new border with the given thickness, dash length and colors.
     * <P>
     * Note that future implementation may not completely honour the specified
     * dash length to create more appealing border.
     *
     * @param thickness the width of the border in pixels. This argument cannot
     *   be a negative integer.
     * @param dashLength the length of the dashes in pixels. This argument must
     *   be a positive integer.
     * @param colors the colors of the dashes. The dashes will alternate between
     *   these colors. Note that there can be more than two colors defined but
     *   there must be at least two colors defined.
     *
     * @throws IllegalArgumentException thrown if any of the arguments is
     *   out of its specified range
     * @throws NullPointerException thrown if {@code colors} is {@code null}
     */
    public DashedBorder(int thickness, int dashLength, Color... colors) {
        ExceptionHelper.checkArgumentInRange(thickness, 0, Integer.MAX_VALUE, "thickness");
        ExceptionHelper.checkArgumentInRange(dashLength, 1, Integer.MAX_VALUE, "dashLength");
        ExceptionHelper.checkArgumentInRange(colors.length, 2, Integer.MAX_VALUE, "colors.length");

        for (Color c: colors) {
            ExceptionHelper.checkNotNullArgument(c, "color[i]");
        }

        this.thickness = thickness;
        this.dashLength = dashLength;
        this.colors = colors.clone();
    }

    private static int moveToNextColor(Graphics2D g, Color[] colors, int currentIndex) {
        g.setColor(colors[currentIndex]);
        return (currentIndex + 1) % colors.length;
    }

    private static int moveToPrevColor(Graphics2D g, Color[] colors, int currentIndex) {
        g.setColor(colors[currentIndex]);
        return (currentIndex + colors.length + 1) % colors.length;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final Color[] colors = this.colors;

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final int thickness = this.thickness;

        @SuppressWarnings("LocalVariableHidesMemberVariable")
        final int dashLength = this.dashLength;

        if ((thickness > 0) && (g instanceof Graphics2D)) {


            Graphics2D g2d = (Graphics2D) g;
            Color oldColor = g2d.getColor();

            int w = c.getWidth();
            int h = c.getHeight();

            int colorIndex = 0;
            int startX = 0;
            int startY = 0;

            g2d.setColor(colors[colorIndex]);

            // Top
            while (startX < w) {
                colorIndex = moveToNextColor(g2d, colors, colorIndex);
                g2d.fillRect(startX, startY, dashLength, thickness);
                startX += dashLength;
            }

            // Right
            int rem = w % dashLength + thickness;
            startX = w - thickness;
            startY = 0;
            if (rem > 0) {
                g2d.fillRect(startX, startY, thickness, rem);
                startY += rem;
            }
            while (startY < h) {
                colorIndex = moveToNextColor(g2d, colors, colorIndex);
                g2d.fillRect(startX, startY, thickness, dashLength);
                startY += dashLength;
            }

            // Left
            colorIndex = colors.length - 1;
            g2d.setColor(colors[colorIndex]);
            startX = 0;
            startY = thickness;
            while (startY < h) {
                colorIndex = moveToPrevColor(g2d, colors, colorIndex);
                g2d.fillRect(startX, startY, thickness, dashLength);
                startY += dashLength;
            }

            // Bottom
            rem = Math.max((h - thickness) % dashLength, 0) + thickness;
            startX = 0;
            startY = h - thickness;
            int endX = w - thickness;
            if (rem > 0) {
                g2d.fillRect(startX, startY, rem, thickness);
                startX += rem;
            }
            while (startX < endX) {
                colorIndex = moveToPrevColor(g2d, colors, colorIndex);
                int remLength = endX - startX;
                g2d.fillRect(startX, startY, Math.min(dashLength, remLength), thickness);
                startX += dashLength;
            }

            g2d.setColor(oldColor);
        }
    }

    /**
     * {@inheritDoc }
     *
     * @return the insets of the border
     */
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness, thickness, thickness, thickness);
    }

    /**
     * {@inheritDoc }
     *
     * @return {@code true} if this border is opaque, {@code false} otherwise
     */
    @Override
    public boolean isBorderOpaque() {
        // The colors of the border can be transparent so return false just in
        // case. It would be possible to check the alpha component of colors but
        // until there is no need for it return false.
        return false;
    }
}
