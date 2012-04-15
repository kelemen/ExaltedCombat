package exaltedcombat.dialogs;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JOptionPane;
import org.jtrim.utils.ExceptionHelper;
import resources.strings.LocalizedString;
import resources.strings.StringContainer;

/**
 * Contains static utility methods for dialogs and Swing components in
 * ExaltedCombat.
 * <P>
 * Note that this class cannot be initiated.
 *
 * @author Kelemen Attila
 */
public final class ExaltedDialogHelper {
    private static final LocalizedString EXCEPTION_MESSAGE_TEXT = StringContainer.getDefaultString("EXCEPTION_MESSAGE_TEXT");
    private static final LocalizedString BUTTON_CAPTION_YES = StringContainer.getDefaultString("BUTTON_CAPTION_YES");
    private static final LocalizedString BUTTON_CAPTION_NO = StringContainer.getDefaultString("BUTTON_CAPTION_NO");

    /**
     * Defines a completely transparent color. The
     * {@link Color#getAlpha() alpha} component of this color is zero. The
     * color of other components is undefined.
     */
    public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    /**
     * Shows a modal error message dialog displaying an exception.
     *
     * @param parent the parent component of the dialog to be shown. This
     *   argument can be {@code null} if the dialog has no parent.
     * @param caption the title of the dialog to be shown. This argument
     *   cannot be {@code null}.
     * @param error the error to displayed in the dialog. This argument cannot
     *   be {@code null}.
     *
     * @throws NullPointerException thrown if the specified caption or error is
     *   {@code null}
     */
    public static void displayError(Component parent, String caption, Throwable error) {
        ExceptionHelper.checkNotNullArgument(caption, "caption");

        JOptionPane.showMessageDialog(parent,
            EXCEPTION_MESSAGE_TEXT.format(error.toString()),
            caption,
            JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a modal dialog with a yes and no answer. The captions of the
     * buttons are localized and the button selected by default can be
     * specified.
     * <P>
     * Note that this method can only be called on the AWT event dispatching
     * thread.
     *
     * @param parent determines the {@code Frame} in which the dialog is
     *   displayed; if {@code null}, or if the {@code parent} has no
     *   {@code Frame}, a default {@code Frame} is used
     * @param caption the title of the dialog to be displayed. This argument
     *   cannot be {@code null}.
     * @param question the text displayed in the dialog. This argument cannot be
     *   {@code null}.
     * @param defaultAnswer the default button to be selected. If this argument
     *   is {@code true}, the yes button is selected by default; otherwise the
     *   no button is selected.
     * @return {@code true} if the user choose the "yes" option, {@code false}
     *   in any other case
     *
     * @throws java.awt.HeadlessException if
     *   {@code GraphicsEnvironment.isHeadless} returns {@code true}
     */
    public static boolean askYesNoQuestion(
            Component parent,
            String caption,
            String question,
            boolean defaultAnswer) {

        ExceptionHelper.checkNotNullArgument(caption, "caption");
        ExceptionHelper.checkNotNullArgument(question, "question");

        String yesOption = BUTTON_CAPTION_YES.toString();
        String noOption = BUTTON_CAPTION_NO.toString();
        return JOptionPane.showOptionDialog(parent,
                question, caption,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{yesOption, noOption},
                defaultAnswer ? yesOption : noOption) == 0;
    }

    /**
     * Returns the luminance of the specified color. The luminance is returned
     * in the [0.0, 1.0] range and lower values are darker.
     * <P>
     * Note that this method ignores the {@link Color#getAlpha() alpha}
     * component of the specified color.
     * <P>
     * This method is safe to call from multiple threads and is transparent
     * to any synchronization it may use.
     *
     * @param color the color whose luminance is to be calculated. This argument
     *   cannot be {@code null}.
     * @return the luminance of the specified color. The return value is within
     *   the [0.0, 1.0] range.
     *
     * @throws NullPointerException thrown if the specified color is
     *   {@code null}
     */
    public static double getLuminance(Color color) {
        double luminance = (0.30 * (double)color.getRed()
                + 0.59 * (double)color.getGreen()
                + 0.11 * (double)color.getBlue()) / 255.0;

        // Rounding errors may occur because the above constants may not
        // sum to 1.0 after they are converted to the binary representation.
        if (luminance >= 1.0) {
            return 1.0;
        }
        else if (luminance <= 0) {
            return 0.0;
        }
        else {
            return luminance;
        }
    }

    /**
     * Returns a color which can easily be read on a given background. The
     * returned color can therefore be used as a text on the given background.
     * <P>
     * This method relies on the {@link #getLuminance(java.awt.Color) luminance}
     * of the color and the model used by the
     * <A href="ftp://medical.nema.org/medical/dicom/2008/08_14pu.pdf">
     * DICOM standard 2008 in part 14</A>.
     * <P>
     * Note that this method ignores the {@link Color#getAlpha() alpha}
     * component of the specified color.
     * <P>
     * This method is safe to call from multiple threads and is transparent
     * to any synchronization it may use.
     *
     * @param bckgColor the background color to be used. This argument cannot be
     *   {@code null}.
     * @return either black or white depending on which can be more easily
     *   distinguished on the given background color. This method never returns
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified background color
     *   is {@code null}
     */
    public static Color getVisibleTextColor(Color bckgColor) {
        double luminance = getLuminance(bckgColor);

        // Note that the human eye can distinguish brighter light more
        // so the comparison point to determine which color to use is not 0.5

        // Derived from DICOM standard 2008 Part 14
        // ftp://medical.nema.org/medical/dicom/2008/08_14pu.pdf
        double middlePoint = 0.28179295499080444408168701432807;

        return luminance < middlePoint
                ? Color.WHITE
                : Color.BLACK;
    }

    private ExaltedDialogHelper() {
        throw new AssertionError();
    }
}
