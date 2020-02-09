package org.litespring.beans;

/**
 * Object to hold information and value for an individual bean property.
 * Using an object here, rather than just storing all properties in
 * a map keyed by property name, allows for more flexibility, and the
 * ability to handle indexed properties etc in an optimized way.
 *
 * <p>Note that the value doesn't need to be the final required type:
 * A {@link BeanWrapper} implementation should handle any necessary conversion,
 * as this object doesn't know anything about the objects it will be applied to.
 *
 * PropertyValue来表示property属性
 */
public class PropertyValue {
    private final String name;

    private final Object value;
    //标志着把value转化为convertedValue，即把RuntimeBeanReference/TypedStringValue 转化为PropertyValue
    private boolean converted = false;

    private Object convertedValue;

    public PropertyValue(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Return the name of the property.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the value of the property.
     * <p>Note that type conversion will <i>not</i> have occurred here.
     * It is the responsibility of the BeanWrapper implementation to
     * perform type conversion.
     */
    public Object getValue() {
        return this.value;
    }
    /**
     * Return whether this holder contains a converted value already ({@code true}),
     * or whether the value still needs to be converted ({@code false}).
     */
    public synchronized boolean isConverted() {
        return this.converted;
    }

    /**
     * Set the converted value of the constructor argument,
     * after processed type conversion.
     */
    public synchronized void setConvertedValue(Object value) {
        this.converted = true;
        this.convertedValue = value;
    }

    /**
     * Return the converted value of the constructor argument,
     * after processed type conversion.
     */
    public synchronized Object getConvertedValue() {
        return this.convertedValue;
    }
}