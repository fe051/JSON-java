package org.json;

/*
Public Domain.
*/

import java.lang.annotation.*;

/**
 * Use this annotation on a getter method to override the Bean name
 * parser for Bean -&gt; JSONObject mapping. If this annotation is
 * present at any level in the class hierarchy, then the method will
 * not be serialized from the bean into the JSONObject.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSONPropertyIgnore { }
