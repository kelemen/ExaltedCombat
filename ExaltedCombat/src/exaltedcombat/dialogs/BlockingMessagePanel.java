package exaltedcombat.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;
import org.jtrim.utils.*;

/**
 * Defines a panel with a single message text in its center. The background of
 * this panel can be transparent making this panel useful to draw over another
 * component to display that it is disabled. The text message can describe
 * the task disabling the component.
 * <P>
 * The message of this panel can be set by the
 * {@link #setMessage(java.lang.String) setMessage(String)} method and its color
 * by the {@link #setPanelColor(java.awt.Color, int) setPanelColor(Color, int)}.
 * <P>
 * Note that like most Swing components this component is not thread-safe and
 * can only be accessed from the AWT event dispatching thread.
 *
 * @see AutoComponentBlocker
 * @author Kelemen Attila
 */
public class BlockingMessagePanel extends JPanel {
    private static final long serialVersionUID = 4035271302733827005L;

    private final JLabel jMessageLabel;

    private static Font deriveFont(Font baseFont, int style, float size) {
        return baseFont != null ? baseFont.deriveFont(style, size) : null;
    }

    /**
     * Creates a new panel with an empty message. The default background is gray
     * and transparent.
     */
    public BlockingMessagePanel() {
        setLayout(new GridBagLayout());

        jMessageLabel = new JLabel();
        jMessageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jMessageLabel.setOpaque(true);
        jMessageLabel.setFont(deriveFont(jMessageLabel.getFont(), Font.BOLD, 16.0f));
        add(jMessageLabel, new GridBagConstraints());

        setMessage("");
        setPanelColor(Color.GRAY, 128);
    }

    /**
     * Sets the displayed message of this panel. The message will be displayed
     * in the center of this panel.
     *
     * @param message the message to be displayed on this dialog. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if the specified message is
     *   {@code null}
     */
    public final void setMessage(String message) {
        ExceptionHelper.checkNotNullArgument(message, "message");
        jMessageLabel.setText(message);
    }

    /**
     * Sets the background color of this panel. The panel will have the
     * specified color as its background with the specified transparancy level
     * (ignoring the alpha component of the specified color). Unlike the
     * background of this panel the text message on this panel will have a
     * completely opaque background of the same color as the color specified
     * for the background. The color of the text message will be either white
     * or black depending on which is more visible on the specified background.
     *
     * @param color the background color of this component. This argument
     *   cannot be {@code null} and its {@link Color#getAlpha() alpha} component
     *   is ignored.
     * @param transparency the transparency level of the specified background.
     *   This argument must be between 0 and 255 (inclusive), 255 meaning
     *   completely opaque and 0 meaning completely transparent background.
     *
     * @throws IllegalArgumentException thrown if the transparency level is not
     *   within 0 and 255 (inclusive)
     * @throws NullPointerException thrown if the specified background color is
     *   {@code null}
     */
    public final void setPanelColor(Color color, int transparency) {
        setBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), transparency));

        Color textBckg = new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        jMessageLabel.setBackground(textBckg);
        jMessageLabel.setForeground(ExaltedDialogHelper.getVisibleTextColor(textBckg));
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public boolean isOpaque() {
        return super.isOpaque() && getBackground().getAlpha() == 0xFF;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        Color oldColor = g.getColor();
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(oldColor);
    }
}
