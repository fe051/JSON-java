package org.json;

/*
Public Domain.
 */

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * A JSONArray is an ordered sequence of values. Its external text form is a
 * string wrapped in square brackets with commas separating the values. The
 * internal form is an object having <code>get</code> and <code>opt</code>
 * methods for accessing the values by index, and <code>put</code> methods for
 * adding or replacing values. The values can be any of these types:
 * <code>Boolean</code>, <code>JSONArray</code>, <code>JSONObject</code>,
 * <code>Number</code>, <code>String</code>, or the
 * <code>JSONObject.NULL object</code>.
 * <p>
 * The constructor can convert a JSON text into a Java object. The
 * <code>toString</code> method converts to JSON text.
 * <p>
 * A <code>get</code> method returns a value if one can be found, and throws an
 * exception if one cannot be found. An <code>opt</code> method returns a
 * default value instead of throwing an exception, and so is useful for
 * obtaining optional values.
 * <p>
 * The generic <code>get()</code> and <code>opt()</code> methods return an
 * object which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * coercion for you.
 * <p>
 * The texts produced by the <code>toString</code> methods strictly conform to
 * JSON syntax rules. The constructors are more forgiving in the texts they will
 * accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
 * before the closing bracket.</li>
 * <li>The <code>null</code> value will be inserted when there is <code>,</code>
 * &nbsp;<small>(comma)</small> elision.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
 * quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote
 * or single quote, and if they do not contain leading or trailing spaces, and
 * if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , #</code> and if they do not look like numbers and
 * if they are not the reserved words <code>true</code>, <code>false</code>, or
 * <code>null</code>.</li>
 * </ul>
 *
 * @author JSON.org
 * @version 2016-08/15
 */
public class JSONArray implements Iterable<Object> {

    /**
     * The arrayList where the JSONArray's properties are kept.
     */
    private final ArrayList<Object> myArrayList;

    /**
     * Construct an empty JSONArray.
     */
    public JSONArray() {
        this.myArrayList = new ArrayList<>();
    }

    /**
     * Construct a JSONArray from a JSONTokener.
     *
     * @param x
     *            A JSONTokener
     * @throws JSONException
     *             If there is a syntax error.
     */
    public JSONArray(final JSONTokener x) throws JSONException {
        this(x, x.getJsonParserConfiguration());
    }

    /**
     * Constructs a JSONArray from a JSONTokener and a JSONParserConfiguration.
     *
     * @param x                       A JSONTokener instance from which the JSONArray is constructed.
     * @param jsonParserConfiguration A JSONParserConfiguration instance that controls the behavior of the parser.
     * @throws JSONException If a syntax error occurs during the construction of the JSONArray.
     */
    public JSONArray(final JSONTokener x, final JSONParserConfiguration jsonParserConfiguration) throws JSONException {
        this();

        final boolean isInitial = x.getPrevious() == 0;
        if (x.nextClean() != '[') {
            throw x.syntaxError("A JSONArray text must start with '['");
        }

        char nextChar = x.nextClean();
        if (nextChar == 0) {
            // array is unclosed. No ']' found, instead EOF
            throw x.syntaxError("Expected a ',' or ']'");
        }
        if (nextChar != ']') {
            x.back();
            for (;;) {
                if (x.nextClean() == ',') {
                    x.back();
                    this.myArrayList.add(JSONObject.NULL);
                } else {
                    x.back();
                    this.myArrayList.add(x.nextValue());
                }
                switch (x.nextClean()) {
                case 0:
                    // array is unclosed. No ']' found, instead EOF
                    throw x.syntaxError("Expected a ',' or ']'");
                case ',':
                    nextChar = x.nextClean();
                    if (nextChar == 0) {
                        // array is unclosed. No ']' found, instead EOF
                        throw x.syntaxError("Expected a ',' or ']'");
                    }
                    if (nextChar == ']') {
                        // trailing commas are not allowed in strict mode
                        if (jsonParserConfiguration.isStrictMode()) {
                            throw x.syntaxError("Strict mode error: Expected another array element");
                        }
                        return;
                    }
                    if (nextChar == ',') {
                        // consecutive commas are not allowed in strict mode
                        if (jsonParserConfiguration.isStrictMode()) {
                            throw x.syntaxError("Strict mode error: Expected a valid array element");
                        }
                        return;
                    }
                    x.back();
                    break;
                case ']':
                    if (isInitial && jsonParserConfiguration.isStrictMode() &&
                            x.nextClean() != 0) {
                        throw x.syntaxError("Strict mode error: Unparsed characters found at end of input text");
                    }
                    return;
                default:
                    throw x.syntaxError("Expected a ',' or ']'");
                }
            }
        } else {
            if (isInitial && jsonParserConfiguration.isStrictMode() && x.nextClean() != 0) {
                throw x.syntaxError("Strict mode error: Unparsed characters found at end of input text");
            }
        }
    }

    /**
     * Construct a JSONArray from a source JSON text.
     *
     * @param source
     *            A string that begins with <code>[</code>&nbsp;<small>(left
     *            bracket)</small> and ends with <code>]</code>
     *            &nbsp;<small>(right bracket)</small>.
     * @throws JSONException
     *             If there is a syntax error.
     */
    public JSONArray(final String source) throws JSONException {
        this(source, new JSONParserConfiguration());
    }

    /**
     * Construct a JSONArray from a source JSON text.
     *
     * @param source
     *            A string that begins with <code>[</code>&nbsp;<small>(left
     *            bracket)</small> and ends with <code>]</code>
     *            &nbsp;<small>(right bracket)</small>.
     * @param jsonParserConfiguration the parser config object
     * @throws JSONException
     *             If there is a syntax error.
     */
    public JSONArray(final String source, final JSONParserConfiguration jsonParserConfiguration) throws JSONException {
        this(new JSONTokener(source, jsonParserConfiguration), jsonParserConfiguration);
    }

    /**
     * Construct a JSONArray from a Collection.
     *
     * @param collection
     *            A Collection.
     */
    public JSONArray(final Collection<?> collection) {
      this(collection, 0, new JSONParserConfiguration());
    }

    /**
     * Construct a JSONArray from a Collection.
     *
     * @param collection
     *            A Collection.
     * @param jsonParserConfiguration
     *            Configuration object for the JSON parser
     */
    public JSONArray(final Collection<?> collection, final JSONParserConfiguration jsonParserConfiguration) {
        this(collection, 0, jsonParserConfiguration);
    }

    /**
     * Construct a JSONArray from a collection with recursion depth.
     *
     * @param collection
     *             A Collection.
     * @param recursionDepth
     *             Variable for tracking the count of nested object creations.
     * @param jsonParserConfiguration
     *             Configuration object for the JSON parser
     */
    JSONArray(final Collection<?> collection, final int recursionDepth, final JSONParserConfiguration jsonParserConfiguration) {
        if (recursionDepth > jsonParserConfiguration.getMaxNestingDepth()) {
          throw new JSONException("JSONArray has reached recursion depth limit of " + jsonParserConfiguration.getMaxNestingDepth());
        }
        if (collection == null) {
            this.myArrayList = new ArrayList<>();
        } else {
            this.myArrayList = new ArrayList<>(collection.size());
            this.addAll(collection, true, recursionDepth, jsonParserConfiguration);
        }
    }

    /**
     * Construct a JSONArray from an Iterable. This is a shallow copy.
     *
     * @param iter
     *            A Iterable collection.
     */
    public JSONArray(final Iterable<?> iter) {
        this();
        if (iter == null) {
            return;
        }
        this.addAll(iter, true);
    }

    /**
     * Construct a JSONArray from another JSONArray. This is a shallow copy.
     *
     * @param array
     *            A array.
     */
    public JSONArray(final JSONArray array) {
        if (array == null) {
            this.myArrayList = new ArrayList<>();
        } else {
            // shallow copy directly the internal array lists as any wrapping
            // should have been done already in the original JSONArray
            this.myArrayList = new ArrayList<>(array.myArrayList);
        }
    }

    /**
     * Construct a JSONArray from an array.
     *
     * @param array
     *            Array. If the parameter passed is null, or not an array, an
     *            exception will be thrown.
     *
     * @throws JSONException
     *            If not an array or if an array value is non-finite number.
     * @throws NullPointerException
     *            Thrown if the array parameter is null.
     */
    public JSONArray(final Object array) throws JSONException {
        this();
        if (!array.getClass().isArray()) {
            throw new JSONException(
                    "JSONArray initial value should be a string or collection or array.");
        }
        this.addAll(array, true, 0);
    }

    /**
     * Construct a JSONArray with the specified initial capacity.
     *
     * @param initialCapacity
     *            the initial capacity of the JSONArray.
     * @throws JSONException
     *             If the initial capacity is negative.
     */
    public JSONArray(final int initialCapacity) throws JSONException {
    	if (initialCapacity < 0) {
            throw new JSONException(
                    "JSONArray initial capacity cannot be negative.");
    	}
    	this.myArrayList = new ArrayList<>(initialCapacity);
    }

    @Override
    public Iterator<Object> iterator() {
        return this.myArrayList.iterator();
    }

    /**
     * Get the object value associated with an index.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return An object value.
     * @throws JSONException
     *             If there is no value for the index.
     */
    public Object get(final int index) throws JSONException {
        final Object object = this.opt(index);
        if (object == null) {
            throw new JSONException("JSONArray[" + index + "] not found.");
        }
        return object;
    }

    /**
     * Get the boolean value associated with an index. The string values "true"
     * and "false" are converted to boolean.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The truth.
     * @throws JSONException
     *             If there is no value for the index or if the value is not
     *             convertible to boolean.
     */
    public boolean getBoolean(final int index) throws JSONException {
        final Object object = this.get(index);
        if (object.equals(Boolean.FALSE)
                || (object instanceof String && "false"
                        .equalsIgnoreCase((String) object))) {
            return false;
        } else if (object.equals(Boolean.TRUE)
                || (object instanceof String && "true"
                        .equalsIgnoreCase((String) object))) {
            return true;
        }
        throw JSONArray.wrongValueFormatException(index, "boolean", object, null);
    }

    /**
     * Get the double value associated with an index.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     * @throws JSONException
     *             If the key is not found or if the value cannot be converted
     *             to a number.
     */
    public double getDouble(final int index) throws JSONException {
        final Object object = this.get(index);
        if(object instanceof Number) {
            return ((Number)object).doubleValue();
        }
        try {
            return Double.parseDouble(object.toString());
        } catch (final Exception e) {
            throw JSONArray.wrongValueFormatException(index, "double", object, e);
        }
    }

    /**
     * Get the float value associated with a key.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The numeric value.
     * @throws JSONException
     *             if the key is not found or if the value is not a Number
     *             object and cannot be converted to a number.
     */
    public float getFloat(final int index) throws JSONException {
        final Object object = this.get(index);
        if(object instanceof Number) {
            return ((Number)object).floatValue();
        }
        try {
            return Float.parseFloat(object.toString());
        } catch (final Exception e) {
            throw JSONArray.wrongValueFormatException(index, "float", object, e);
        }
    }

    /**
     * Get the Number value associated with a key.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The numeric value.
     * @throws JSONException
     *             if the key is not found or if the value is not a Number
     *             object and cannot be converted to a number.
     */
    public Number getNumber(final int index) throws JSONException {
        final Object object = this.get(index);
        try {
            if (object instanceof Number) {
                return (Number)object;
            }
            return JSONObject.stringToNumber(object.toString());
        } catch (final Exception e) {
            throw JSONArray.wrongValueFormatException(index, "number", object, e);
        }
    }

    /**
     * Get the enum value associated with an index.
     * 
     * @param <E>
     *            Enum Type
     * @param clazz
     *            The type of enum to retrieve.
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The enum value at the index location
     * @throws JSONException
     *            if the key is not found or if the value cannot be converted
     *            to an enum.
     */
    public <E extends Enum<E>> E getEnum(final Class<E> clazz, final int index) throws JSONException {
        final E val = optEnum(clazz, index);
        if(val==null) {
            // JSONException should really take a throwable argument.
            // If it did, I would re-implement this with the Enum.valueOf
            // method and place any thrown exception in the JSONException
            throw JSONArray.wrongValueFormatException(index, "enum of type "
                    + JSONObject.quote(clazz.getSimpleName()), opt(index), null);
        }
        return val;
    }

    /**
     * Get the BigDecimal value associated with an index. If the value is float
     * or double, the {@link BigDecimal#BigDecimal(double)} constructor
     * will be used. See notes on the constructor for conversion issues that
     * may arise.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     * @throws JSONException
     *             If the key is not found or if the value cannot be converted
     *             to a BigDecimal.
     */
    public BigDecimal getBigDecimal (final int index) throws JSONException {
        final Object object = this.get(index);
        final BigDecimal val = JSONObject.objectToBigDecimal(object, null);
        if(val == null) {
            throw JSONArray.wrongValueFormatException(index, "BigDecimal", object, null);
        }
        return val;
    }

    /**
     * Get the BigInteger value associated with an index.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     * @throws JSONException
     *             If the key is not found or if the value cannot be converted
     *             to a BigInteger.
     */
    public BigInteger getBigInteger (final int index) throws JSONException {
        final Object object = this.get(index);
        final BigInteger val = JSONObject.objectToBigInteger(object, null);
        if(val == null) {
            throw JSONArray.wrongValueFormatException(index, "BigInteger", object, null);
        }
        return val;
    }

    /**
     * Get the int value associated with an index.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     * @throws JSONException
     *             If the key is not found or if the value is not a number.
     */
    public int getInt(final int index) throws JSONException {
        final Object object = this.get(index);
        if(object instanceof Number) {
            return ((Number)object).intValue();
        }
        try {
            return Integer.parseInt(object.toString());
        } catch (final Exception e) {
            throw JSONArray.wrongValueFormatException(index, "int", object, e);
        }
    }

    /**
     * Get the JSONArray associated with an index.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return A JSONArray value.
     * @throws JSONException
     *             If there is no value for the index. or if the value is not a
     *             JSONArray
     */
    public JSONArray getJSONArray(final int index) throws JSONException {
        final Object object = this.get(index);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        throw JSONArray.wrongValueFormatException(index, "JSONArray", object, null);
    }

    /**
     * Get the JSONObject associated with an index.
     *
     * @param index
     *            subscript
     * @return A JSONObject value.
     * @throws JSONException
     *             If there is no value for the index or if the value is not a
     *             JSONObject
     */
    public JSONObject getJSONObject(final int index) throws JSONException {
        final Object object = this.get(index);
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        throw JSONArray.wrongValueFormatException(index, "JSONObject", object, null);
    }

    /**
     * Get the long value associated with an index.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     * @throws JSONException
     *             If the key is not found or if the value cannot be converted
     *             to a number.
     */
    public long getLong(final int index) throws JSONException {
        final Object object = this.get(index);
        if(object instanceof Number) {
            return ((Number)object).longValue();
        }
        try {
            return Long.parseLong(object.toString());
        } catch (final Exception e) {
            throw JSONArray.wrongValueFormatException(index, "long", object, e);
        }
    }

    /**
     * Get the string associated with an index.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return A string value.
     * @throws JSONException
     *             If there is no string value for the index.
     */
    public String getString(final int index) throws JSONException {
        final Object object = this.get(index);
        if (object instanceof String) {
            return (String) object;
        }
        throw JSONArray.wrongValueFormatException(index, "String", object, null);
    }

    /**
     * Determine if the value is <code>null</code>.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return true if the value at the index is <code>null</code>, or if there is no value.
     */
    public boolean isNull(final int index) {
        return JSONObject.NULL.equals(this.opt(index));
    }

    /**
     * Make a string from the contents of this JSONArray. The
     * <code>separator</code> string is inserted between each element. Warning:
     * This method assumes that the data structure is acyclical.
     *
     * @param separator
     *            A string that will be inserted between the elements.
     * @return a string.
     * @throws JSONException
     *             If the array contains an invalid number.
     */
    public String join(final String separator) throws JSONException {
        final int len = this.length();
        if (len == 0) {
            return "";
        }
        
        final StringBuilder sb = new StringBuilder(
                   JSONObject.valueToString(this.myArrayList.get(0)));

        for (int i = 1; i < len; i++) {
            sb.append(separator)
              .append(JSONObject.valueToString(this.myArrayList.get(i)));
        }
        return sb.toString();
    }

    /**
     * Get the number of elements in the JSONArray, included nulls.
     *
     * @return The length (or size).
     */
    public int length() {
        return this.myArrayList.size();
    }

    /**
     * Removes all of the elements from this JSONArray.
     * The JSONArray will be empty after this call returns.
     */
    public void clear() {
        this.myArrayList.clear();
    }

    /**
     * Get the optional object value associated with an index.
     *
     * @param index
     *            The index must be between 0 and length() - 1. If not, null is returned.
     * @return An object value, or null if there is no object at that index.
     */
    public Object opt(final int index) {
        return (index < 0 || index >= this.length()) ? null : this.myArrayList
                .get(index);
    }

    /**
     * Get the optional boolean value associated with an index. It returns false
     * if there is no value at that index, or if the value is not Boolean.TRUE
     * or the String "true".
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The truth.
     */
    public boolean optBoolean(final int index) {
        return this.optBoolean(index, false);
    }

    /**
     * Get the optional boolean value associated with an index. It returns the
     * defaultValue if there is no value at that index or if it is not a Boolean
     * or the String "true" or "false" (case insensitive).
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            A boolean default.
     * @return The truth.
     */
    public boolean optBoolean(final int index, final boolean defaultValue) {
        try {
            return this.getBoolean(index);
        } catch (final Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get the optional Boolean object associated with an index. It returns false
     * if there is no value at that index, or if the value is not Boolean.TRUE
     * or the String "true".
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The truth.
     */
    public Boolean optBooleanObject(final int index) {
        return this.optBooleanObject(index, false);
    }

    /**
     * Get the optional Boolean object associated with an index. It returns the
     * defaultValue if there is no value at that index or if it is not a Boolean
     * or the String "true" or "false" (case insensitive).
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            A boolean default.
     * @return The truth.
     */
    public Boolean optBooleanObject(final int index, final Boolean defaultValue) {
        try {
            return this.getBoolean(index);
        } catch (final Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get the optional double value associated with an index. NaN is returned
     * if there is no value for the index, or if the value is not a number and
     * cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     */
    public double optDouble(final int index) {
        return this.optDouble(index, Double.NaN);
    }

    /**
     * Get the optional double value associated with an index. The defaultValue
     * is returned if there is no value for the index, or if the value is not a
     * number and cannot be converted to a number.
     *
     * @param index
     *            subscript
     * @param defaultValue
     *            The default value.
     * @return The value.
     */
    public double optDouble(final int index, final double defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.doubleValue();
    }

    /**
     * Get the optional Double object associated with an index. NaN is returned
     * if there is no value for the index, or if the value is not a number and
     * cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The object.
     */
    public Double optDoubleObject(final int index) {
        return this.optDoubleObject(index, Double.NaN);
    }

    /**
     * Get the optional double value associated with an index. The defaultValue
     * is returned if there is no value for the index, or if the value is not a
     * number and cannot be converted to a number.
     *
     * @param index
     *            subscript
     * @param defaultValue
     *            The default object.
     * @return The object.
     */
    public Double optDoubleObject(final int index, final Double defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.doubleValue();
    }

    /**
     * Get the optional float value associated with an index. NaN is returned
     * if there is no value for the index, or if the value is not a number and
     * cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     */
    public float optFloat(final int index) {
        return this.optFloat(index, Float.NaN);
    }

    /**
     * Get the optional float value associated with an index. The defaultValue
     * is returned if there is no value for the index, or if the value is not a
     * number and cannot be converted to a number.
     *
     * @param index
     *            subscript
     * @param defaultValue
     *            The default value.
     * @return The value.
     */
    public float optFloat(final int index, final float defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.floatValue();
    }

    /**
     * Get the optional Float object associated with an index. NaN is returned
     * if there is no value for the index, or if the value is not a number and
     * cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The object.
     */
    public Float optFloatObject(final int index) {
        return this.optFloatObject(index, Float.NaN);
    }

    /**
     * Get the optional Float object associated with an index. The defaultValue
     * is returned if there is no value for the index, or if the value is not a
     * number and cannot be converted to a number.
     *
     * @param index
     *            subscript
     * @param defaultValue
     *            The default object.
     * @return The object.
     */
    public Float optFloatObject(final int index, final Float defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.floatValue();
    }

    /**
     * Get the optional int value associated with an index. Zero is returned if
     * there is no value for the index, or if the value is not a number and
     * cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     */
    public int optInt(final int index) {
        return this.optInt(index, 0);
    }

    /**
     * Get the optional int value associated with an index. The defaultValue is
     * returned if there is no value for the index, or if the value is not a
     * number and cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default value.
     * @return The value.
     */
    public int optInt(final int index, final int defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.intValue();
    }

    /**
     * Get the optional Integer object associated with an index. Zero is returned if
     * there is no value for the index, or if the value is not a number and
     * cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The object.
     */
    public Integer optIntegerObject(final int index) {
        return this.optIntegerObject(index, 0);
    }

    /**
     * Get the optional Integer object associated with an index. The defaultValue is
     * returned if there is no value for the index, or if the value is not a
     * number and cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default object.
     * @return The object.
     */
    public Integer optIntegerObject(final int index, final Integer defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.intValue();
    }

    /**
     * Get the enum value associated with a key.
     * 
     * @param <E>
     *            Enum Type
     * @param clazz
     *            The type of enum to retrieve.
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The enum value at the index location or null if not found
     */
    public <E extends Enum<E>> E optEnum(final Class<E> clazz, final int index) {
        return this.optEnum(clazz, index, null);
    }

    /**
     * Get the enum value associated with a key.
     * 
     * @param <E>
     *            Enum Type
     * @param clazz
     *            The type of enum to retrieve.
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default in case the value is not found
     * @return The enum value at the index location or defaultValue if
     *            the value is not found or cannot be assigned to clazz
     */
    public <E extends Enum<E>> E optEnum(final Class<E> clazz, final int index, final E defaultValue) {
        try {
            final Object val = this.opt(index);
            if (JSONObject.NULL.equals(val)) {
                return defaultValue;
            }
            if (clazz.isAssignableFrom(val.getClass())) {
                // we just checked it!
                @SuppressWarnings("unchecked") final E myE = (E) val;
                return myE;
            }
            return Enum.valueOf(clazz, val.toString());
        } catch (final IllegalArgumentException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Get the optional BigInteger value associated with an index. The 
     * defaultValue is returned if there is no value for the index, or if the 
     * value is not a number and cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default value.
     * @return The value.
     */
    public BigInteger optBigInteger(final int index, final BigInteger defaultValue) {
        final Object val = this.opt(index);
        return JSONObject.objectToBigInteger(val, defaultValue);
    }

    /**
     * Get the optional BigDecimal value associated with an index. The 
     * defaultValue is returned if there is no value for the index, or if the 
     * value is not a number and cannot be converted to a number. If the value
     * is float or double, the {@link BigDecimal#BigDecimal(double)}
     * constructor will be used. See notes on the constructor for conversion
     * issues that may arise.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default value.
     * @return The value.
     */
    public BigDecimal optBigDecimal(final int index, final BigDecimal defaultValue) {
        final Object val = this.opt(index);
        return JSONObject.objectToBigDecimal(val, defaultValue);
    }

    /**
     * Get the optional JSONArray associated with an index. Null is returned if
     * there is no value at that index or if the value is not a JSONArray.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return A JSONArray value.
     */
    public JSONArray optJSONArray(final int index) {
        return this.optJSONArray(index, null);
    }

    /**
     * Get the optional JSONArray associated with an index. The defaultValue is returned if
     * there is no value at that index or if the value is not a JSONArray.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default.
     * @return A JSONArray value.
     */
    public JSONArray optJSONArray(final int index, final JSONArray defaultValue) {
        final Object object = this.opt(index);
        return object instanceof JSONArray ? (JSONArray) object : defaultValue;
    }

    /**
     * Get the optional JSONObject associated with an index. Null is returned if
     * there is no value at that index or if the value is not a JSONObject.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return A JSONObject value.
     */
    public JSONObject optJSONObject(final int index) {
        return this.optJSONObject(index, null);
    }

    /**
     * Get the optional JSONObject associated with an index. The defaultValue is returned if
     * there is no value at that index or if the value is not a JSONObject.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default.
     * @return A JSONObject value.
     */
    public JSONObject optJSONObject(final int index, final JSONObject defaultValue) {
        final Object object = this.opt(index);
        return object instanceof JSONObject ? (JSONObject) object : defaultValue;
    }

    /**
     * Get the optional long value associated with an index. Zero is returned if
     * there is no value for the index, or if the value is not a number and
     * cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The value.
     */
    public long optLong(final int index) {
        return this.optLong(index, 0);
    }

    /**
     * Get the optional long value associated with an index. The defaultValue is
     * returned if there is no value for the index, or if the value is not a
     * number and cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default value.
     * @return The value.
     */
    public long optLong(final int index, final long defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.longValue();
    }

    /**
     * Get the optional Long object associated with an index. Zero is returned if
     * there is no value for the index, or if the value is not a number and
     * cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return The object.
     */
    public Long optLongObject(final int index) {
        return this.optLongObject(index, 0L);
    }

    /**
     * Get the optional Long object associated with an index. The defaultValue is
     * returned if there is no value for the index, or if the value is not a
     * number and cannot be converted to a number.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default object.
     * @return The object.
     */
    public Long optLongObject(final int index, final Long defaultValue) {
        final Number val = this.optNumber(index, null);
        if (val == null) {
            return defaultValue;
        }
        return val.longValue();
    }

    /**
     * Get an optional {@link Number} value associated with a key, or <code>null</code>
     * if there is no such key or if the value is not a number. If the value is a string,
     * an attempt will be made to evaluate it as a number ({@link BigDecimal}). This method
     * would be used in cases where type coercion of the number value is unwanted.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return An object which is the value.
     */
    public Number optNumber(final int index) {
        return this.optNumber(index, null);
    }

    /**
     * Get an optional {@link Number} value associated with a key, or the default if there
     * is no such key or if the value is not a number. If the value is a string,
     * an attempt will be made to evaluate it as a number ({@link BigDecimal}). This method
     * would be used in cases where type coercion of the number value is unwanted.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default.
     * @return An object which is the value.
     */
    public Number optNumber(final int index, final Number defaultValue) {
        final Object val = this.opt(index);
        if (JSONObject.NULL.equals(val)) {
            return defaultValue;
        }
        if (val instanceof Number){
            return (Number) val;
        }
        
        if (val instanceof String) {
            try {
                return JSONObject.stringToNumber((String) val);
            } catch (final Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Get the optional string value associated with an index. It returns an
     * empty string if there is no value at that index. If the value is not a
     * string and is not null, then it is converted to a string.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @return A String value.
     */
    public String optString(final int index) {
        return this.optString(index, "");
    }

    /**
     * Get the optional string associated with an index. The defaultValue is
     * returned if the key is not found.
     *
     * @param index
     *            The index must be between 0 and length() - 1.
     * @param defaultValue
     *            The default value.
     * @return A String value.
     */
    public String optString(final int index, final String defaultValue) {
        final Object object = this.opt(index);
        return JSONObject.NULL.equals(object) ? defaultValue : object
                .toString();
    }

    /**
     * Append a boolean value. This increases the array's length by one.
     *
     * @param value
     *            A boolean value.
     * @return this.
     */
    public JSONArray put(final boolean value) {
        return this.put(value ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Put a value in the JSONArray, where the value will be a JSONArray which
     * is produced from a Collection.
     *
     * @param value
     *            A Collection value.
     * @return this.
     * @throws JSONException
     *            If the value is non-finite number.
     */
    public JSONArray put(final Collection<?> value) {
        return this.put(new JSONArray(value));
    }

    /**
     * Append a double value. This increases the array's length by one.
     *
     * @param value
     *            A double value.
     * @return this.
     * @throws JSONException
     *             if the value is not finite.
     */
    public JSONArray put(final double value) throws JSONException {
        return this.put(Double.valueOf(value));
    }
    
    /**
     * Append a float value. This increases the array's length by one.
     *
     * @param value
     *            A float value.
     * @return this.
     * @throws JSONException
     *             if the value is not finite.
     */
    public JSONArray put(final float value) throws JSONException {
        return this.put(Float.valueOf(value));
    }

    /**
     * Append an int value. This increases the array's length by one.
     *
     * @param value
     *            An int value.
     * @return this.
     */
    public JSONArray put(final int value) {
        return this.put(Integer.valueOf(value));
    }

    /**
     * Append an long value. This increases the array's length by one.
     *
     * @param value
     *            A long value.
     * @return this.
     */
    public JSONArray put(final long value) {
        return this.put(Long.valueOf(value));
    }

    /**
     * Put a value in the JSONArray, where the value will be a JSONObject which
     * is produced from a Map.
     *
     * @param value
     *            A Map value.
     * @return this.
     * @throws JSONException
     *            If a value in the map is non-finite number.
     * @throws NullPointerException
     *            If a key in the map is <code>null</code>
     */
    public JSONArray put(final Map<?, ?> value) {
        return this.put(new JSONObject(value));
    }

    /**
     * Append an object value. This increases the array's length by one.
     *
     * @param value
     *            An object value. The value should be a Boolean, Double,
     *            Integer, JSONArray, JSONObject, Long, or String, or the
     *            JSONObject.NULL object.
     * @return this.
     * @throws JSONException
     *            If the value is non-finite number.
     */
    public JSONArray put(final Object value) {
        JSONObject.testValidity(value);
        this.myArrayList.add(value);
        return this;
    }

    /**
     * Put or replace a boolean value in the JSONArray. If the index is greater
     * than the length of the JSONArray, then null elements will be added as
     * necessary to pad it out.
     *
     * @param index
     *            The subscript.
     * @param value
     *            A boolean value.
     * @return this.
     * @throws JSONException
     *             If the index is negative.
     */
    public JSONArray put(final int index, final boolean value) throws JSONException {
        return this.put(index, value ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Put a value in the JSONArray, where the value will be a JSONArray which
     * is produced from a Collection.
     *
     * @param index
     *            The subscript.
     * @param value
     *            A Collection value.
     * @return this.
     * @throws JSONException
     *             If the index is negative or if the value is non-finite.
     */
    public JSONArray put(final int index, final Collection<?> value) throws JSONException {
        return this.put(index, new JSONArray(value));
    }

    /**
     * Put or replace a double value. If the index is greater than the length of
     * the JSONArray, then null elements will be added as necessary to pad it
     * out.
     *
     * @param index
     *            The subscript.
     * @param value
     *            A double value.
     * @return this.
     * @throws JSONException
     *             If the index is negative or if the value is non-finite.
     */
    public JSONArray put(final int index, final double value) throws JSONException {
        return this.put(index, Double.valueOf(value));
    }

    /**
     * Put or replace a float value. If the index is greater than the length of
     * the JSONArray, then null elements will be added as necessary to pad it
     * out.
     *
     * @param index
     *            The subscript.
     * @param value
     *            A float value.
     * @return this.
     * @throws JSONException
     *             If the index is negative or if the value is non-finite.
     */
    public JSONArray put(final int index, final float value) throws JSONException {
        return this.put(index, Float.valueOf(value));
    }

    /**
     * Put or replace an int value. If the index is greater than the length of
     * the JSONArray, then null elements will be added as necessary to pad it
     * out.
     *
     * @param index
     *            The subscript.
     * @param value
     *            An int value.
     * @return this.
     * @throws JSONException
     *             If the index is negative.
     */
    public JSONArray put(final int index, final int value) throws JSONException {
        return this.put(index, Integer.valueOf(value));
    }

    /**
     * Put or replace a long value. If the index is greater than the length of
     * the JSONArray, then null elements will be added as necessary to pad it
     * out.
     *
     * @param index
     *            The subscript.
     * @param value
     *            A long value.
     * @return this.
     * @throws JSONException
     *             If the index is negative.
     */
    public JSONArray put(final int index, final long value) throws JSONException {
        return this.put(index, Long.valueOf(value));
    }

    /**
     * Put a value in the JSONArray, where the value will be a JSONObject that
     * is produced from a Map.
     *
     * @param index
     *            The subscript.
     * @param value
     *            The Map value.
     * @return
     *             reference to self
     * @throws JSONException
     *             If the index is negative or if the value is an invalid
     *             number.
     * @throws NullPointerException
     *             If a key in the map is <code>null</code>
     */
    public JSONArray put(final int index, final Map<?, ?> value) throws JSONException {
        this.put(index, new JSONObject(value, new JSONParserConfiguration()));
        return this;
    }

    /**
     * Put a value in the JSONArray, where the value will be a JSONObject that
     * is produced from a Map.
     *
     * @param index
     *          The subscript
     * @param value
     *          The Map value.
     * @param jsonParserConfiguration
     *          Configuration object for the JSON parser
     * @return reference to self
     * @throws JSONException
     *          If the index is negative or if the value is an invalid
     *          number.
     */
    public JSONArray put(final int index, final Map<?, ?> value, final JSONParserConfiguration jsonParserConfiguration) throws JSONException {
        this.put(index, new JSONObject(value, jsonParserConfiguration));
        return this;
    }

    /**
     * Put or replace an object value in the JSONArray. If the index is greater
     * than the length of the JSONArray, then null elements will be added as
     * necessary to pad it out.
     *
     * @param index
     *            The subscript.
     * @param value
     *            The value to put into the array. The value should be a
     *            Boolean, Double, Integer, JSONArray, JSONObject, Long, or
     *            String, or the JSONObject.NULL object.
     * @return this.
     * @throws JSONException
     *             If the index is negative or if the value is an invalid
     *             number.
     */
    public JSONArray put(final int index, final Object value) throws JSONException {
        if (index < 0) {
            throw new JSONException("JSONArray[" + index + "] not found.");
        }
        if (index < this.length()) {
            JSONObject.testValidity(value);
            this.myArrayList.set(index, value);
            return this;
        }
        if(index == this.length()){
            // simple append
            return this.put(value);
        }
        // if we are inserting past the length, we want to grow the array all at once
        // instead of incrementally.
        this.myArrayList.ensureCapacity(index + 1);
        while (index != this.length()) {
            // we don't need to test validity of NULL objects
            this.myArrayList.add(JSONObject.NULL);
        }
        return this.put(value);
    }

    /**
     * Put a collection's elements in to the JSONArray.
     *
     * @param collection
     *            A Collection.
     * @return this. 
     */
    public JSONArray putAll(final Collection<?> collection) {
        this.addAll(collection, false);
        return this;
    }
    
    /**
     * Put an Iterable's elements in to the JSONArray.
     *
     * @param iter
     *            An Iterable.
     * @return this. 
     */
    public JSONArray putAll(final Iterable<?> iter) {
        this.addAll(iter, false);
        return this;
    }

    /**
     * Put a JSONArray's elements in to the JSONArray.
     *
     * @param array
     *            A JSONArray.
     * @return this. 
     */
    public JSONArray putAll(final JSONArray array) {
        // directly copy the elements from the source array to this one
        // as all wrapping should have been done already in the source.
        this.myArrayList.addAll(array.myArrayList);
        return this;
    }

    /**
     * Put an array's elements in to the JSONArray.
     *
     * @param array
     *            Array. If the parameter passed is null, or not an array or Iterable, an
     *            exception will be thrown.
     * @return this. 
     *
     * @throws JSONException
     *            If not an array, JSONArray, Iterable or if an value is non-finite number.
     * @throws NullPointerException
     *            Thrown if the array parameter is null.
     */
    public JSONArray putAll(final Object array) throws JSONException {
        this.addAll(array, false);
        return this;
    }
    
    /**
     * Creates a JSONPointer using an initialization string and tries to 
     * match it to an item within this JSONArray. For example, given a
     * JSONArray initialized with this document:
     * <pre>
     * [
     *     {"b":"c"}
     * ]
     * </pre>
     * and this JSONPointer string: 
     * <pre>
     * "/0/b"
     * </pre>
     * Then this method will return the String "c"
     * A JSONPointerException may be thrown from code called by this method.
     *
     * @param jsonPointer string that can be used to create a JSONPointer
     * @return the item matched by the JSONPointer, otherwise null
     */
    public Object query(final String jsonPointer) {
        return query(new JSONPointer(jsonPointer));
    }
    
    /**
     * Uses a user initialized JSONPointer  and tries to 
     * match it to an item within this JSONArray. For example, given a
     * JSONArray initialized with this document:
     * <pre>
     * [
     *     {"b":"c"}
     * ]
     * </pre>
     * and this JSONPointer: 
     * <pre>
     * "/0/b"
     * </pre>
     * Then this method will return the String "c"
     * A JSONPointerException may be thrown from code called by this method.
     *
     * @param jsonPointer string that can be used to create a JSONPointer
     * @return the item matched by the JSONPointer, otherwise null
     */
    public Object query(final JSONPointer jsonPointer) {
        return jsonPointer.queryFrom(this);
    }
    
    /**
     * Queries and returns a value from this object using {@code jsonPointer}, or
     * returns null if the query fails due to a missing key.
     * 
     * @param jsonPointer the string representation of the JSON pointer
     * @return the queried value or {@code null}
     * @throws IllegalArgumentException if {@code jsonPointer} has invalid syntax
     */
    public Object optQuery(final String jsonPointer) {
    	return optQuery(new JSONPointer(jsonPointer));
    }
    
    /**
     * Queries and returns a value from this object using {@code jsonPointer}, or
     * returns null if the query fails due to a missing key.
     * 
     * @param jsonPointer The JSON pointer
     * @return the queried value or {@code null}
     * @throws IllegalArgumentException if {@code jsonPointer} has invalid syntax
     */
    public Object optQuery(final JSONPointer jsonPointer) {
        try {
            return jsonPointer.queryFrom(this);
        } catch (final JSONPointerException e) {
            return null;
        }
    }

    /**
     * Remove an index and close the hole.
     *
     * @param index
     *            The index of the element to be removed.
     * @return The value that was associated with the index, or null if there
     *         was no value.
     */
    public Object remove(final int index) {
        return index >= 0 && index < this.length()
            ? this.myArrayList.remove(index)
            : null;
    }

    /**
     * Determine if two JSONArrays are similar.
     * They must contain similar sequences.
     *
     * @param other The other JSONArray
     * @return true if they are equal
     */
    public boolean similar(final Object other) {
        if (!(other instanceof JSONArray)) {
            return false;
        }
        final int len = this.length();
        if (len != ((JSONArray)other).length()) {
            return false;
        }
        for (int i = 0; i < len; i += 1) {
            final Object valueThis = this.myArrayList.get(i);
            final Object valueOther = ((JSONArray)other).myArrayList.get(i);
            if(valueThis == valueOther) {
            	continue;
            }
            if(valueThis == null) {
            	return false;
            }
            if (valueThis instanceof JSONObject) {
                if (!((JSONObject)valueThis).similar(valueOther)) {
                    return false;
                }
            } else if (valueThis instanceof JSONArray) {
                if (!((JSONArray)valueThis).similar(valueOther)) {
                    return false;
                }
            } else if (valueThis instanceof Number && valueOther instanceof Number) {
                if (!JSONObject.isNumberSimilar((Number)valueThis, (Number)valueOther)) {
                	return false;
                }
            } else if (valueThis instanceof JSONString && valueOther instanceof JSONString) {
                if (!((JSONString) valueThis).toJSONString().equals(((JSONString) valueOther).toJSONString())) {
                    return false;
                }
            } else if (!valueThis.equals(valueOther)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Produce a JSONObject by combining a JSONArray of names with the values of
     * this JSONArray.
     *
     * @param names
     *            A JSONArray containing a list of key strings. These will be
     *            paired with the values.
     * @return A JSONObject, or null if there are no names or if this JSONArray
     *         has no values.
     * @throws JSONException
     *             If any of the names are null.
     */
    public JSONObject toJSONObject(final JSONArray names) throws JSONException {
        if (names == null || names.isEmpty() || this.isEmpty()) {
            return null;
        }
        final JSONObject jo = new JSONObject(names.length());
        for (int i = 0; i < names.length(); i += 1) {
            jo.put(names.getString(i), this.opt(i));
        }
        return jo;
    }

    /**
     * Make a JSON text of this JSONArray. For compactness, no unnecessary
     * whitespace is added. If it is not possible to produce a syntactically
     * correct JSON text then null will be returned instead. This could occur if
     * the array contains an invalid number.
     * <p><b>
     * Warning: This method assumes that the data structure is acyclical.
     * </b>
     *
     * @return a printable, displayable, transmittable representation of the
     *         array.
     */
    @Override
    public String toString() {
        try {
            return this.toString(0);
        } catch (final Exception e) {
            return null;
        }
    }

    /**
     * Make a pretty-printed JSON text of this JSONArray.
     * 
     * <p>If <pre> {@code indentFactor > 0}</pre> and the {@link JSONArray} has only
     * one element, then the array will be output on a single line:
     * <pre>{@code [1]}</pre>
     * 
     * <p>If an array has 2 or more elements, then it will be output across
     * multiple lines: <pre>{@code
     * [
     * 1,
     * "value 2",
     * 3
     * ]
     * }</pre>
     * <p><b>
     * Warning: This method assumes that the data structure is acyclical.
     * </b>
     * 
     * @param indentFactor
     *            The number of spaces to add to each level of indentation.
     * @return a printable, displayable, transmittable representation of the
     *         object, beginning with <code>[</code>&nbsp;<small>(left
     *         bracket)</small> and ending with <code>]</code>
     *         &nbsp;<small>(right bracket)</small>.
     * @throws JSONException if a called function fails
     */
    @SuppressWarnings("resource")
    public String toString(final int indentFactor) throws JSONException {
        // each value requires a comma, so multiply the count by 2
        // We don't want to oversize the initial capacity
        final int initialSize = myArrayList.size() * 2;
        final Writer sw = new StringBuilderWriter(Math.max(initialSize, 16));
        return this.write(sw, indentFactor, 0).toString();
    }

    /**
     * Write the contents of the JSONArray as JSON text to a writer. For
     * compactness, no whitespace is added.
     * <p><b>
     * Warning: This method assumes that the data structure is acyclical.
     *</b>
     * @param writer the writer object
     * @return The writer.
     * @throws JSONException if a called function fails
     */
    public Writer write(final Writer writer) throws JSONException {
        return this.write(writer, 0, 0);
    }

    /**
     * Write the contents of the JSONArray as JSON text to a writer.
     * 
     * <p>If <pre>{@code indentFactor > 0}</pre> and the {@link JSONArray} has only
     * one element, then the array will be output on a single line:
     * <pre>{@code [1]}</pre>
     * 
     * <p>If an array has 2 or more elements, then it will be output across
     * multiple lines: <pre>{@code
     * [
     * 1,
     * "value 2",
     * 3
     * ]
     * }</pre>
     * <p><b>
     * Warning: This method assumes that the data structure is acyclical.
     * </b>
     *
     * @param writer
     *            Writes the serialized JSON
     * @param indentFactor
     *            The number of spaces to add to each level of indentation.
     * @param indent
     *            The indentation of the top level.
     * @return The writer.
     * @throws JSONException if a called function fails or unable to write
     */
    @SuppressWarnings("resource")
    public Writer write(final Writer writer, final int indentFactor, final int indent)
            throws JSONException {
        try {
            boolean needsComma = false;
            final int length = this.length();
            writer.write('[');

            if (length == 1) {
                try {
                    JSONObject.writeValue(writer, this.myArrayList.get(0),
                            indentFactor, indent);
                } catch (final Exception e) {
                    throw new JSONException("Unable to write JSONArray value at index: 0", e);
                }
            } else if (length != 0) {
                final int newIndent = indent + indentFactor;

                for (int i = 0; i < length; i += 1) {
                    if (needsComma) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    JSONObject.indent(writer, newIndent);
                    try {
                        JSONObject.writeValue(writer, this.myArrayList.get(i),
                                indentFactor, newIndent);
                    } catch (final Exception e) {
                        throw new JSONException("Unable to write JSONArray value at index: " + i, e);
                    }
                    needsComma = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                JSONObject.indent(writer, indent);
            }
            writer.write(']');
            return writer;
        } catch (final IOException e) {
            throw new JSONException(e);
        }
    }

    /**
     * Returns a java.util.List containing all of the elements in this array.
     * If an element in the array is a JSONArray or JSONObject it will also
     * be converted to a List and a Map respectively.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return a java.util.List containing the elements of this array
     */
    public List<Object> toList() {
        final List<Object> results = new ArrayList<>(this.myArrayList.size());
        for (final Object element : this.myArrayList) {
            if (element == null || JSONObject.NULL.equals(element)) {
                results.add(null);
            } else if (element instanceof JSONArray) {
                results.add(((JSONArray) element).toList());
            } else if (element instanceof JSONObject) {
                results.add(((JSONObject) element).toMap());
            } else {
                results.add(element);
            }
        }
        return results;
    }

    /**
     * Check if JSONArray is empty.
     *
     * @return true if JSONArray is empty, otherwise false.
     */
    public boolean isEmpty() {
        return this.myArrayList.isEmpty();
    }

    /**
     * Add a collection's elements to the JSONArray.
     *
     * @param collection
     *            A Collection.
     * @param wrap
     *            {@code true} to call {@link JSONObject#wrap(Object)} for each item,
     *            {@code false} to add the items directly
     * @param recursionDepth
     *            Variable for tracking the count of nested object creations.
     */
    private void addAll(final Collection<?> collection, final boolean wrap, final int recursionDepth, final JSONParserConfiguration jsonParserConfiguration) {
        this.myArrayList.ensureCapacity(this.myArrayList.size() + collection.size());
        if (wrap) {
            for (final Object o: collection){
                this.put(JSONObject.wrap(o, recursionDepth + 1, jsonParserConfiguration));
            }
        } else {
            for (final Object o: collection){
                this.put(o);
            }
        }
    }

    /**
     * Add an Iterable's elements to the JSONArray.
     *
     * @param iter
     *            An Iterable.
     * @param wrap
     *            {@code true} to call {@link JSONObject#wrap(Object)} for each item,
     *            {@code false} to add the items directly
     */
    private void addAll(final Iterable<?> iter, final boolean wrap) {
        if (wrap) {
            for (final Object o: iter){
                this.put(JSONObject.wrap(o));
            }
        } else {
            for (final Object o: iter){
                this.put(o);
            }
        }
    }

    /**
     * Add an array's elements to the JSONArray.
     *
     * @param array
     *          Array. If the parameter passed is null, or not an array,
     *          JSONArray, Collection, or Iterable, an exception will be
     *          thrown.
     * @param wrap
     *          {@code true} to call {@link JSONObject#wrap(Object)} for each item,
     *          {@code false} to add the items directly
     * @throws JSONException
     *          If not an array or if an array value is non-finite number.
     */
    private void addAll(final Object array, final boolean wrap) throws JSONException {
      this.addAll(array, wrap, 0);
    }

    /**
     * Add an array's elements to the JSONArray.
     *
     * @param array
     *            Array. If the parameter passed is null, or not an array,
     *            JSONArray, Collection, or Iterable, an exception will be
     *            thrown.
     * @param wrap
     *          {@code true} to call {@link JSONObject#wrap(Object)} for each item,
     *          {@code false} to add the items directly
     * @param recursionDepth
     *          Variable for tracking the count of nested object creations.
     */
    private void addAll(final Object array, final boolean wrap, final int recursionDepth) {
        addAll(array, wrap, recursionDepth, new JSONParserConfiguration());
    }
    /**
     * Add an array's elements to the JSONArray.
     *`
     * @param array
     *            Array. If the parameter passed is null, or not an array,
     *            JSONArray, Collection, or Iterable, an exception will be
     *            thrown.
     * @param wrap
     *            {@code true} to call {@link JSONObject#wrap(Object)} for each item,
     *            {@code false} to add the items directly
     * @param recursionDepth
     *            Variable for tracking the count of nested object creations.
     * @param jsonParserConfiguration
     *            Variable to pass parser custom configuration for json parsing.
     * @throws JSONException
     *            If not an array or if an array value is non-finite number.
     * @throws NullPointerException
     *            Thrown if the array parameter is null.
     */
    private void addAll(final Object array, final boolean wrap, final int recursionDepth, final JSONParserConfiguration jsonParserConfiguration) throws JSONException {
        if (array.getClass().isArray()) {
            final int length = Array.getLength(array);
            this.myArrayList.ensureCapacity(this.myArrayList.size() + length);
            if (wrap) {
                for (int i = 0; i < length; i += 1) {
                    this.put(JSONObject.wrap(Array.get(array, i), recursionDepth + 1, jsonParserConfiguration));
                }
            } else {
                for (int i = 0; i < length; i += 1) {
                    this.put(Array.get(array, i));
                }
            }
        } else if (array instanceof JSONArray) {
            // use the built in array list `addAll` as all object
            // wrapping should have been completed in the original
            // JSONArray
            this.myArrayList.addAll(((JSONArray)array).myArrayList);
        } else if (array instanceof Collection) {
            this.addAll((Collection<?>)array, wrap, recursionDepth, jsonParserConfiguration);
        } else if (array instanceof Iterable) {
            this.addAll((Iterable<?>)array, wrap);
        } else {
            throw new JSONException(
                    "JSONArray initial value should be a string or collection or array.");
        }
    }
    
    /**
     * Create a new JSONException in a common format for incorrect conversions.
     * @param idx index of the item
     * @param valueType the type of value being coerced to
     * @param cause optional cause of the coercion failure
     * @return JSONException that can be thrown.
     */
    private static JSONException wrongValueFormatException(
            final int idx,
            final String valueType,
            final Object value,
            final Throwable cause) {
        if(value == null) {
            return new JSONException(
                    "JSONArray[" + idx + "] is not a " + valueType + " (null)."
                    , cause);
        }
        // don't try to toString collections or known object types that could be large.
        if(value instanceof Map || value instanceof Iterable || value instanceof JSONObject) {
            return new JSONException(
                    "JSONArray[" + idx + "] is not a " + valueType + " (" + value.getClass() + ")."
                    , cause);
        }
        return new JSONException(
                "JSONArray[" + idx + "] is not a " + valueType + " (" + value.getClass() + " : " + value + ")."
                , cause);
    }

}
