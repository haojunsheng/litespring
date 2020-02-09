package org.litespring.beans.factory.config;

/**
 * Holder for a typed String value. Can be added to bean definitions
 * in order to explicitly specify a target type for a String value,
 * for example for collection elements.
 *
 * <p>This holder will just store the String value and the target type.
 * The actual conversion will be performed by the bean factory.
 *
 */
public class TypedStringValue{
    private String value;
    /**
     * Create a new {@link TypedStringValue} for the given String value.
     * @param value the String value
     */
    public TypedStringValue(String value) {
        this.value = value;
    }
    /**
     * Return the String value.
     */
    public String getValue() {
        return this.value;
    }
}