package org.json;

/*
Public Domain.
*/

import java.lang.annotation.*;

/**
 * Use this annotation on a getter method to override the Bean name
 * parser for Bean -&gt; JSONObject mapping. A value set to empty string <code>""</code>
 * will have the Bean parser fall back to the default field name processing.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface JSONPropertyName {
    /**
     * The value of the JSON property.
     * @return The name of the property as to be used in the JSON Object.
     */
    String value();
}
