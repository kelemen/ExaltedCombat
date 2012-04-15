package exaltedcombat.components;

import java.awt.Color;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import org.jtrim.utils.ExceptionHelper;

/**
 * A {@link ListCellRenderer ListCellRenderer} for {@link JList JList}s
 * displaying a solid colored square along the caption of the entities. Entries
 * of the list must implement {@link ColoredListCell} so they can provide the
 * required color and caption to be displayed. Each list element can have
 * different colors so they can be more easily distinguished by the user.
 * <P>
 * When elements of a list is selected rendered by this cell renderer, the
 * selected element will have simple border. The color and the thickness of the
 * border can be specified at construction time.
 * <P>
 * This class is only intended to use as a cell renderer for {@code JList}s
 * which can be set by {@link JList#setCellRenderer(javax.swing.ListCellRenderer)}.
 * <P>
 * Instances of this class must be used and created only on the AWT event
 * dispatching thread and are not required to be transparent to any
 * synchronization.
 *
 * @param <E> the type of the elements in the rendered list. Note that this
 *   model requires a type implementing {@link ColoredListCell}.
 *
 * @author Kelemen Attila
 */
public class ColoredListCellRenderer<E extends ColoredListCell>
implements
        ListCellRenderer<E> {

    private final ColorAndCaptionPanel renderer;

    private final Border selectedBorder;
    private final Border unselectedBorder;

    /**
     * Creates a new cell render with the specified selection color and border
     * thickness.
     *
     * @param selectionColor the color of the border of selected items.
     *   This argument cannot be {@code null}.
     * @param borderSize the thickness of the border of selected items. This
     *   argument must not be a negative integer.
     */
    public ColoredListCellRenderer(Color selectionColor, int borderSize) {
        ExceptionHelper.checkNotNullArgument(selectionColor, "selectionColor");
        ExceptionHelper.checkArgumentInRange(borderSize, 0, Integer.MAX_VALUE, "borderSize");

        this.renderer = new ColorAndCaptionPanel();
        this.selectedBorder = BorderFactory.createLineBorder(selectionColor, borderSize);
        this.unselectedBorder = BorderFactory.createEmptyBorder(borderSize, borderSize, borderSize, borderSize);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Component getListCellRendererComponent(
            JList<? extends E> list,
            E value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        Color color;
        String caption;

        if (value == null) {
            color = new Color(0, 0, 0, 0);
            caption = "";
        }
        else {
            color = value.getColor();
            caption = value.getCaption();
        }

        renderer.setComponentOrientation(list.getComponentOrientation());
        renderer.setFont(list.getFont());
        renderer.setBackground(list.getBackground());
        renderer.setForeground(list.getForeground());
        renderer.setPanelColor(color);
        renderer.setPanelCaption(caption);
        renderer.setBorder(isSelected ? selectedBorder : unselectedBorder);

        return renderer;
    }
}
