package exaltedcombat.components;

import java.awt.Color;

/**
 * This interface need to be implemented by elements of list components
 * rendered by {@link ColoredListCellRenderer ColoredListCellRenderer&lt;E&gt;}.
 * <P>
 * Such elements must have color and the text caption so both of these
 * properties can be displayed.
 * <P>
 * Note that as with most Swing components instances of this interface are not
 * required to be thread-safe and in general can only be accessed from the AWT
 * event dispatching thread.
 *
 * @author Kelemen Attila
 */
public interface ColoredListCell {

    /**
     * Returns the color associated with this list element.
     *
     * @return the color associated with this list element. This method
     *   must never return {@code null}.
     */
    public Color getColor();

    /**
     * Returns the caption associated with this list element.
     *
     * @return the caption associated with this list element. This method
     *   must never return {@code null}.
     */
    public String getCaption();
}
