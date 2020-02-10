package org.litespring.core.annotation.AnnotationAttributes;

import org.litespring.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * {@link LinkedHashMap} subclass representing annotation attribute key/value pairs
 * as read by Spring's reflection- or ASM-based {@link
 * org.springframework.core.type.AnnotationMetadata AnnotationMetadata} implementations.
 * Provides 'pseudo-reification' to avoid noisy Map generics in the calling code as well
 * as convenience methods for looking up annotation attributes in a type-safe fashion.
 *
 * @author Chris Beams
 * @since 3.1.1
 */
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

    /**
     * Create a new, empty {@link AnnotationAttributes} instance.
     */
    public AnnotationAttributes() {
    }

    /**
     * Create a new, empty {@link AnnotationAttributes} instance with the given initial
     * capacity to optimize performance.
     *
     * @param initialCapacity initial size of the underlying map
     */
    public AnnotationAttributes(int initialCapacity) {
        super(initialCapacity);
    }


    public AnnotationAttributes(Map<String, Object> map) {
        super(map);
    }


    public String getString(String attributeName) {
        return doGet(attributeName, String.class);
    }

    public String[] getStringArray(String attributeName) {
        return doGet(attributeName, String[].class);
    }

    public boolean getBoolean(String attributeName) {
        return doGet(attributeName, Boolean.class);
    }

    @SuppressWarnings("unchecked")
    public <N extends Number> N getNumber(String attributeName) {
        return (N) doGet(attributeName, Integer.class);
    }

    @SuppressWarnings("unchecked")
    public <E extends Enum<?>> E getEnum(String attributeName) {
        return (E) doGet(attributeName, Enum.class);
    }

    @SuppressWarnings("unchecked")
    public <T> Class<? extends T> getClass(String attributeName) {
        return doGet(attributeName, Class.class);
    }

    public Class<?>[] getClassArray(String attributeName) {
        return doGet(attributeName, Class[].class);
    }


    @SuppressWarnings("unchecked")
    private <T> T doGet(String attributeName, Class<T> expectedType) {

        Object value = this.get(attributeName);
        Assert.notNull(value, format("Attribute '%s' not found", attributeName));
        return (T) value;
    }
}