package exaltedcombat.combatmanagers;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.*;

import javax.swing.*;

/**
 * A simple default implementation for the
 * {@link CombatPanelCreator#createEmptyPanel()} method.
 * <P>
 * This panel is completely opaque and contains a few diagonal lines. The
 * lines are using the {@link #setForeground(java.awt.Color) foreground color}
 * and the background uses the specified
 * {@link #setBackground(java.awt.Color) background color}.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class DefaultEmptyTickPanel extends JPanel {
    private static final long serialVersionUID = 3109256773218160485L;

    /**
     * {@inheritDoc }
     * Note that additionally this method will draw the diagonal lines and
     * the background of this component.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        BufferedImage backingImage = null;
        Graphics2D g2d;
        if (g instanceof Graphics2D) {
            g2d = (Graphics2D)((Graphics2D)g).create();
        }
        else {
            backingImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            g2d = backingImage.createGraphics();
            g2d.setBackground(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(getForeground());

            int height = getHeight();
            int width = getWidth();

            int lineCount = 4;
            int stepX = Math.max(width / (lineCount + 1), 1);
            int stepY = Math.max(height / (lineCount + 1), 1);

            int x = stepX;
            int y = stepY;
            while (x < width && y < height) {
                g2d.drawLine(0, y, x, 0);
                g2d.drawLine(x, height - 1, width - 1, y);

                x += stepX;
                y += stepY;
            }

            g2d.drawLine(0, height - 1, width - 1, 0);
        } finally {
            g2d.dispose();

            if (backingImage != null) {
                g.drawImage(backingImage, 0, 0, null);
            }
        }
    }
}