package org.litespring.beans;

/**
 * Exception thrown on a type mismatch when trying to set a bean property.
 *
 */
public class TypeMismatchException extends BeansException{
    private transient Object value;

    private Class<?> requiredType;

    public TypeMismatchException( Object value, Class<?> requiredType) {
        super("Failed to convert value :"+value + "to type "+requiredType);
        this.value = value;
        this.requiredType = requiredType;
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getRequiredType() {
        return requiredType;
    }
}