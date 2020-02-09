package org.litespring.beans;

import java.util.LinkedList;
import java.util.List;

/**
 * Holder for constructor argument values, typically as part of a bean definition.
 */
public class ConstructorArgument {
    private final List<ValueHolder> argumentValues = new LinkedList<ValueHolder>();

    /**
     * Create a new empty ConstructorArgumentValues object.
     */
    public ConstructorArgument() {

    }
    /**
     * Copy all given argument values into this object, using separate holder
     * instances to keep the values independent from the original object.
     * <p>Note: Identical ValueHolder instances will only be registered once,
     * to allow for merging and re-merging of argument value definitions. Distinct
     * ValueHolder instances carrying the same content are of course allowed.
     */
    public void addArgumentValue(ValueHolder valueHolder) {
        this.argumentValues.add(valueHolder);
    }

    /**
     * Look for an argument value that either corresponds to the given index
     * in the constructor argument list or generically matches by type.
     * @return the ValueHolder for the argument, or {@code null} if none set
     */
    public List<ValueHolder> getArgumentValues() {
        return argumentValues;
    }

    /**
     * Return the number of argument values held in this instance,
     * counting both indexed and generic argument values.
     */
    public int getArgumentCount() {
        return this.argumentValues.size();
    }

    /**
     * Return if this holder does not contain any argument values,
     * neither indexed ones nor generic ones.
     */
    public boolean isEmpty() {
        return this.argumentValues.isEmpty();
    }

    /**
     * Clear this holder, removing all argument values.
     */
    public void clear() {
        this.argumentValues.clear();
    }

    /**
     * Holder for a constructor argument value, with an optional type
     * attribute indicating the target type of the actual constructor argument.
     */
    public static class ValueHolder {

        private Object value;

        private String type;

        private String name;

        public ValueHolder(Object value) {
            this.value = value;
        }

        public ValueHolder(Object value, String type) {
            this.value = value;
            this.type = type;
        }


        public ValueHolder(Object value, String type, String name) {
            this.value = value;
            this.type = type;
            this.name = name;
        }


        public void setValue(Object value) {
            this.value = value;
        }


        public Object getValue() {
            return this.value;
        }


        public void setType(String type) {
            this.type = type;
        }


        public String getType() {
            return this.type;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}