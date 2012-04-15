package resources.strings;

/**
 * The interface for storing a localized, human readable string. The string may
 * contain arguments to be replaced as defined by the
 * {@link java.text.MessageFormat} class.
 * <P>
 * Instances of this interface can be expected to be safe to share across
 * multiple concurrent threads and synchronization transparent.
 *
 * @author Kelemen Attila
 */
public interface LocalizedString {
    /**
     * Returns the value of this localized string. If it contained arguments to
     * be replaced, the returned string will still contain these patterns.
     *
     * @return the value of this localized string. This method never returns
     *   {@code null}.
     */
    @Override
    public String toString();

    /**
     * Returns the value of this localized string with its arguments replaced.
     * The format is the underlying string is the same as defined by the
     * {@link java.text.MessageFormat} class.
     *
     * @param arguments the arguments to be replaced in the underlying localized
     *   string. This argument cannot be {@code null} and cannot contain
     *   {@code null} elements.
     * @return the value of this localized string with its arguments replaced.
     *   This method never returns {@code null}.
     */
    public String format(Object... arguments);
}
