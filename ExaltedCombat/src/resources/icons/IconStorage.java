package resources.icons;

import java.awt.Image;
import java.net.URL;
import javax.imageio.ImageIO;

/**
 * Provides static methods to load icons for ExaltedCombat.
 * <P>
 * This class cannot be inherited and instantiated.
 *
 * @author Kelemen Attila
 */
public final class IconStorage {
    private static Image tryLoadImageFromResource(String resourceName) {
        try {
            URL resourceUrl = ClassLoader.getSystemClassLoader().getResource(resourceName);
            return ImageIO.read(resourceUrl);
        } catch (Throwable ex) {
        }

        return null;
    }

    private static Image tryLoadIcon(String fileName) {
        return tryLoadImageFromResource("resources/icons/" + fileName);
    }

    private static class MainIconHolder {
        private static Image ICON = tryLoadIcon("startle_quiet.png");
    }

    /**
     * Returns the main icon for ExaltedCombat used by the main frame.
     * <P>
     * The icon will be loaded the first time this method is called.
     *
     * @return the main icon for ExaltedCombat used by the main frame.
     *   This method will only return {@code true} if the icon is unavailable
     *   or due to some serious error cannot be read. Note however if this
     *   method returns {@code null} it implies a serious failure.
     */
    public static Image getMainIcon() {
        return MainIconHolder.ICON;
    }

    private static class SecondaryIconHolder {
        private static Image ICON = tryLoadIcon("hero_sword.png");
    }

    /**
     * Returns a secondary icon for ExaltedCombat used by the dialogs and frames
     * other than the main frame.
     * <P>
     * The icon will be loaded the first time this method is called.
     *
     * @return the secondary icon for ExaltedCombat used by the dialogs and
     *   frames other than the main frame. This method will only return
     *   {@code true} if the icon is unavailable or due to some serious error
     *   cannot be read. Note however if this method returns {@code null} it
     *   implies a serious failure.
     */
    public static Image getSecondaryIcon() {
        return SecondaryIconHolder.ICON;
    }

    private IconStorage() {
        throw new AssertionError();
    }
}
