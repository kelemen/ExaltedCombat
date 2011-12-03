package exaltedcombat.components;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.swing.*;

/**
 * Defines a panel which is completely painted with its
 * {@link #getBackground() background color}.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @author Kelemen Attila
 */
public class JSolidPanel extends JPanel {
    private static final long serialVersionUID = 5967551428876103602L;

    /**
     * Creates a new double buffered panel with a Look and Feel specific
     * background color using {@link java.awt.FlowLayout FlowLayout}.
     */
    public JSolidPanel() {
    }

    /**
     * Creates a new panel with a Look and Feel specific background color,
     * and the specified buffering strategy using
     * {@link java.awt.FlowLayout FlowLayout}.
     *
     * @param isDoubleBuffered  a boolean, true for double-buffering, which
     *        uses additional memory space to achieve fast, flicker-free
     *        updates
     */
    public JSolidPanel(boolean isDoubleBuffered) {
        super(isDoubleBuffered);
    }

    /**
     * Creates a new double buffered panel with a Look and Feel specific
     * background color using the specified layout manager.
     *
     * @param layout the {@code LayoutManager} to use
     */
    public JSolidPanel(LayoutManager layout) {
        super(layout);
    }

    /**
     * Creates a new panel with a Look and Feel specific background color,
     * and the specified buffering strategy using the specified layout manager.
     *
     * @param layout the {@code LayoutManager} to use
     * @param isDoubleBuffered  a boolean, true for double-buffering, which
     *        uses additional memory space to achieve fast, flicker-free
     *        updates
     */
    public JSolidPanel(LayoutManager layout, boolean isDoubleBuffered) {
        super(layout, isDoubleBuffered);
    }

    /**
     * {@inheritDoc }
     * <P>
     * Note that this method will return {@code false} if a not completely
     * opaque {@link #getBackground() background color} is used so there is
     * no reason to explicitly set the opaque property to {@code false}.
     */
    @Override
    public boolean isOpaque() {
        return super.isOpaque() && getBackground().getAlpha() >= 0xFF;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void paintComponent(Graphics g) {
        Color prevColor = g.getColor();

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(prevColor);
    }
}
