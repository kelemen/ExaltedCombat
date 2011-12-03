package resources.strings;

import java.text.*;
import java.util.*;
import java.util.logging.*;

import org.jtrim.utils.*;

/**
 * Provides localization support for ExaltedCombat.
 * <P>
 * This class allows localized string to be read from a {@link ResourceBundle}
 * and provides other local specific information.
 * <P>
 * Note that this class cannot be instantiated directly, to get an instance of
 * this class use the static {@link #getDefault() getDefault()} method.
 * <P>
 * Methods of this class (both static and non-static) are allowed to be called
 * from multiple threads concurrently and they are required to be
 * synchronization transparent.
 *
 * @see LocalizedString
 * @author Kelemen Attila
 */
public final class StringContainer {
    private static final Logger LOGGER = Logger.getLogger(StringContainer.class.getName());

    private static final String DEFAULT_RESOURCE_NAME = "resources.strings.textconsts";
    private static final ResourceBundle DEFAULT_RESOURCE_BUNDLE = ResourceBundle.getBundle(DEFAULT_RESOURCE_NAME, Locale.getDefault());

    private static StringContainer loadDefaultContainer() {
        Locale locale = null;
        String resourceName = null;

        try {
            ResourceBundle userConfig = ResourceBundle.getBundle("exaltedcombat-config");
            String localeStr = tryGetStringFromResource(userConfig, "LOCALE");
            locale = localeStr != null
                    ? Locale.forLanguageTag(localeStr)
                    : null;

            resourceName = tryGetStringFromResource(userConfig, "TEXTCONSTS");
        } catch (Throwable ex) {
            // There was probably no "exaltedcombat-config.properties" file
            // So we need to use the default resource and locale
        }

        if (locale == null && resourceName == null) {
            return new StringContainer(DEFAULT_RESOURCE_BUNDLE);
        }

        if (locale == null) {
            locale = Locale.getDefault();
        }

        if (resourceName == null) {
            resourceName = DEFAULT_RESOURCE_NAME;
        }

        StringContainer result = null;
        try {
            result = new StringContainer(ResourceBundle.getBundle(resourceName, locale));
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, "Failed to get resource bundle.", ex);
        }

        return result != null
                ? result
                : new StringContainer(DEFAULT_RESOURCE_BUNDLE);
    }

    private static final StringContainer DEFAULT = loadDefaultContainer();

    private final ResourceBundle resource;
    private final Collator stringCollator;

    private StringContainer(ResourceBundle resource) {
        assert resource != null;
        this.resource = resource;
        this.stringCollator = Collator.getInstance(resource.getLocale());
    }

    /**
     * Returns a string {@code Collator} which can be used to compate strings in
     * a local specific way. The returned {@code Collator} can be shared across
     * multiple threads concurrently.
     * <P>
     * The returned {@code Collator} is based on the locale returned by the
     * {@link #getLocale() getLocale()} method.
     *
     * @return a string {@code Collator} which can be used to compate strings in
     *   a local specific way. This method never
     */
    public Collator getStringCollator() {
        return stringCollator;
    }

    /**
     * Returns the locale associated with this {@code StringContainer}. The
     * strings returned by this {@code StringContainer} should be in the
     * language of the returned locale.
     *
     * @return the locale associated with this {@code StringContainer}. This
     *   methdo never returns {@code null}.
     */
    public Locale getLocale() {
        return resource.getLocale();
    }

    /**
     * Returns a string associated with a non-localized key (possibly not even
     * human readable). The returned string should be translated to the language
     * defined by the locale returned by {@link #getLocale() getLocale()}.
     * <P>
     * If the specified key is not associated with any strings in the given
     * locale, this method may try to return a string using a different locale.
     * If even this fails, this method will return the key itself.
     *
     * @param key the key to be used to retrieve the localized string. This
     *   argument cannot be {@code null}.
     * @return the string associated with a non-localized key. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified key is {@code null}
     */
    public LocalizedString getLocalizedString(String key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        return new CachedString(this, key);
    }

    /**
     * Returns the default (and currently the only one) {@code StringContainer}
     * instance to use.
     * <P>
     * The following logic is used to find the backing {@link ResourceBundle}:
     * <ol>
     *  <li>
     *   If the "exaltedcombat-config" resource bundle can be loaded, the
     *   value of "LOCALE" in the "exaltedcombat-config" resource bundle will
     *   determine the locale to be used and "TEXTCONSTS" the name of the
     *   resource bundle.
     *  </li>
     *  <li>
     *   If in the previous point the value of "LOCALE" cannot be retrieved for
     *   any reason, the default value for it is the
     *   {@link Locale#getDefault() default locale of the JVM}. In case the
     *   value of "TEXTCONSTS" cannot be retrieved the default value for the
     *   name of the resource bundle is "resources.strings.textconsts".
     *  </li>
     *  <li>
     *   The locale in the previous point can still be changed if there is no
     *   available resource bundle based on that locale.
     *  </li>
     *  <li>
     *   The result of this method will be based on the previously defined
     *   resource bundle but may fall back to the default resource bundle
     *   if a requested string cannot be retrieved. The default fallback
     *   resource bundle in this case is: "resources.strings.textconsts".
     *  </li>
     * </ol>
     *
     * @return the default (and currently the only one) {@code StringContainer}
     *   instance to use. This method never returns {@code null}.
     */
    public static StringContainer getDefault() {
        return DEFAULT;
    }

    /**
     * Equivalent to calling: {@code getDefault().getLocalizedString(key)}.
     *
     * @param key the key to be used to retrieve the localized string. This
     *   argument cannot be {@code null}.
     * @return the string associated with a non-localized key. This method
     *   never returns {@code null}.
     *
     * @throws NullPointerException thrown if the specified key is {@code null}
     */
    public static LocalizedString getDefaultString(String key) {
        return getDefault().getLocalizedString(key);
    }

    private static String tryGetStringFromResource(ResourceBundle resource, String key) {
        assert resource != null;
        assert key != null;

        String result = null;
        if (resource.containsKey(key)) {
            try {
                result = resource.getString(key);
            } catch (Throwable ex) {
            }
        }
        return result;
    }

    private static String getStringFromResource(ResourceBundle resource, String key) {
        assert resource != null;
        assert key != null;

        String result = null;
        try {
            result = resource.getString(key);
        } catch (Throwable ex) {
            try {
                result = DEFAULT_RESOURCE_BUNDLE.getString(key);
            } catch (Throwable ex2) {
            }
        }

        return result != null ? result : key;
    }

    private static class CachedString implements LocalizedString {
        private final ResourceBundle resource;
        private final String key;
        private volatile String value;

        public CachedString(StringContainer source, String key) {
            assert source != null;
            assert key != null;

            this.resource = source.resource;
            this.key = key;
            this.value = null;
        }

        private String fetchString() {
            return getStringFromResource(resource, key);
        }

        @Override
        public String toString() {
            String result = value;
            if (result == null) {
                result = fetchString();
                value = result;
            }
            return result;
        }

        @Override
        public String format(Object... arguments) {
            String formatStr = toString();
            return MessageFormat.format(formatStr, arguments);
        }
    }
}
