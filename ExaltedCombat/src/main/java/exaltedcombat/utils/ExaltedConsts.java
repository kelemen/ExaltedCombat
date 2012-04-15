package exaltedcombat.utils;

/**
 * Defines static constants for ExaltedCombat.
 * <P>
 * This class cannot be inherited or instantiated and does not contain public
 * methods.
 *
 * @author Kelemen Attila
 */
public final class ExaltedConsts {
    /**
     * First tick of the combat as it should be presented to the user. Note that
     * the classes in ExaltedCombat use a zero based timeline for a combat in
     * Exalted but the user should see so that the first tick is actually this
     * value.
     */
    public static final int TICK_OFFSET = 1;

    private ExaltedConsts() {
        throw new AssertionError();
    }
}
