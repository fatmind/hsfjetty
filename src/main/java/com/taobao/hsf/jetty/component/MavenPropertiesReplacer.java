package com.taobao.hsf.jetty.component;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * not used
 * @auther: zheshan
 * Time: 13-7-16 下午7:55
 */
@Deprecated
public class MavenPropertiesReplacer implements PlaceholderReplacer{
    @Override
    public void replace() {

    }


    /** Default placeholder prefix: "${" */
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "${";

    /** Default placeholder suffix: "}" */
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";


    /** Never check system properties. */
    public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;

    /**
     * Check system properties if not resolvable in the specified properties.
     * This is the default.
     */
    public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;

    /**
     * Check system properties first, before trying the specified properties.
     * This allows system properties to override any other property source.
     */
    public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;

    private String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

    private String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

    private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

    private boolean searchSystemEnvironment = true;

    private boolean ignoreUnresolvablePlaceholders = true;



    /**
     * @param args
     */
    public static void main(String[] args) {
        MavenPropertiesReplacer place = new MavenPropertiesReplacer();
        Properties props = new Properties();
        props.put("bbbb", "FFFF");
//		props.put("aaaa", "EEEEE");
        String strVal = "aagaljgklajgkljalglaafaf,,bkkkk";
        Set<String> visitedPlaceholders = new HashSet<String>();
        String s = place.parseStringValue(strVal, props, visitedPlaceholders);
        System.out.println(s);
//		System.out.println(place.hasPlaceHolder(strVal));
    }

    public boolean hasPlaceHolder(String strVal){
        int startIndex = strVal.indexOf(this.placeholderPrefix);
        if(startIndex == -1){
            return false;
        }
        int endIndex = findPlaceholderEndIndex(strVal, startIndex);
        if(endIndex > 0){
            return true;
        }
        return false;
    }

    public String parseStringValue(String strVal, Properties props, Set visitedPlaceholders){

        StringBuffer buf = new StringBuffer(strVal);

        int startIndex = strVal.indexOf(this.placeholderPrefix);
        while (startIndex != -1) {
            int endIndex = findPlaceholderEndIndex(buf, startIndex);
            if (endIndex != -1) {
                String placeholder = buf.substring(startIndex + this.placeholderPrefix.length(), endIndex);
                visitedPlaceholders.add(placeholder);
                // Recursive invocation, parsing placeholders contained in the placeholder key.
                placeholder = parseStringValue(placeholder, props, visitedPlaceholders);
                // Now obtain the value for the fully resolved key...
                String propVal = resolvePlaceholder(placeholder, props, this.systemPropertiesMode);
                if (propVal != null) {
                    // Recursive invocation, parsing placeholders contained in the
                    // previously resolved placeholder value.
                    propVal = parseStringValue(propVal, props, visitedPlaceholders);
                    buf.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);

                    startIndex = buf.indexOf(this.placeholderPrefix, startIndex + propVal.length());
                }
                else if (this.ignoreUnresolvablePlaceholders) {
                    // Proceed with unprocessed value.
                    startIndex = buf.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
                }else{

                }

                visitedPlaceholders.remove(placeholder);
            }
            else {
                startIndex = -1;
            }
        }

        return buf.toString();
    }

    private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + this.placeholderPrefix.length();
        int withinNestedPlaceholder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, this.placeholderSuffix)) {
                if (withinNestedPlaceholder > 0) {
                    withinNestedPlaceholder--;
                    index = index + this.placeholderSuffix.length();
                }
                else {
                    return index;
                }
            }
            else if (substringMatch(buf, index, this.placeholderPrefix)) {
                withinNestedPlaceholder++;
                index = index + this.placeholderPrefix.length();
            }
            else {
                index++;
            }
        }
        return -1;
    }

    /**
     * Resolve the given placeholder using the given properties, performing
     * a system properties check according to the given mode.
     * <p>Default implementation delegates to <code>resolvePlaceholder
     * (placeholder, props)</code> before/after the system properties check.
     * <p>Subclasses can override this for custom resolution strategies,
     * including customized points for the system properties check.
     * @param placeholder the placeholder to resolve
     * @param props the merged properties of this configurer
     * @param systemPropertiesMode the system properties mode,
     * according to the constants in this class
     * @return the resolved value, of null if none
     * @see System#getProperty
     * @see #resolvePlaceholder(String, java.util.Properties)
     */
    protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
        String propVal = null;
        if (systemPropertiesMode == SYSTEM_PROPERTIES_MODE_OVERRIDE) {
            propVal = resolveSystemProperty(placeholder);
        }
        if (propVal == null) {
            propVal = resolvePlaceholder(placeholder, props);
        }
        if (propVal == null && systemPropertiesMode == SYSTEM_PROPERTIES_MODE_FALLBACK) {
            propVal = resolveSystemProperty(placeholder);
        }
        return propVal;
    }

    /**
     * Resolve the given placeholder using the given properties.
     * The default implementation simply checks for a corresponding property key.
     * <p>Subclasses can override this for customized placeholder-to-key mappings
     * or custom resolution strategies, possibly just using the given properties
     * as fallback.
     * <p>Note that system properties will still be checked before respectively
     * after this method is invoked, according to the system properties mode.
     * @param placeholder the placeholder to resolve
     * @param props the merged properties of this configurer
     * @return the resolved value, of <code>null</code> if none
     */
    protected String resolvePlaceholder(String placeholder, Properties props) {
        return props.getProperty(placeholder);
    }

    /**
     * Resolve the given key as JVM system property, and optionally also as
     * system environment variable if no matching system property has been found.
     * @param key the placeholder to resolve as system property key
     * @return the system property value, or <code>null</code> if not found
     * @see java.lang.System#getProperty(String)
     * @see java.lang.System#getenv(String)
     */
    protected String resolveSystemProperty(String key) {
        try {
            String value = System.getProperty(key);
            if (value == null && this.searchSystemEnvironment) {
                value = System.getenv(key);
            }
            return value;
        }
        catch (Throwable ex) {

            return null;
        }
    }

    private boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        for (int j = 0; j < substring.length(); j++) {
            int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }

}
