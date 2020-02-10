package org.litespring.beans.factory.config;

import java.lang.reflect.Field;

import org.litespring.util.Assert;

/**
 * Descriptor for a specific dependency that is about to be injected.
 * Wraps a constructor parameter, a method parameter or a field,
 * allowing unified access to their metadata.
 *
 * 对依赖的描述符，只实现了对字段的描述，未实现对方法的描述
 * @author Juergen Hoeller
 * @since 2.5
 */
public class DependencyDescriptor {
    private Field field;
    private boolean required;

    /**
     * Create a new descriptor for a field.
     * Considers the dependency as 'eager'.
     *
     * @param field    the field to wrap
     * @param required whether the dependency is required
     */
    public DependencyDescriptor(Field field, boolean required) {
        Assert.notNull(field, "Field must not be null");
        this.field = field;
        this.required = required;

    }

    /**
     * Determine the declared (non-generic) type of the wrapped parameter/field.
     *
     * @return the declared type (never {@code null})
     */
    public Class<?> getDependencyType() {
        if (this.field != null) {
            return field.getType();
        }
        throw new RuntimeException("only support field dependency");
    }

    /**
     * Return whether this dependency is required.
     */
    public boolean isRequired() {
        return this.required;
    }
}