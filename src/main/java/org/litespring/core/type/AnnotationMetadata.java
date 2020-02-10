package org.litespring.core.type;

import java.util.Set;

import org.litespring.core.annotation.AnnotationAttributes;

/**
 * Interface that defines abstract access to the annotations of a specific
 * class, in a form that does not require that class to be loaded yet.
 *
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @see StandardAnnotationMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getAnnotationMetadata()
 * @since 2.5
 */
public interface AnnotationMetadata extends ClassMetadata {
    /**
     * Return the names of all annotation types defined on the underlying class.
     *
     * @return the annotation type names
     */
    Set<String> getAnnotationTypes();

    /**
     * Determine whether the underlying class has an annotation of the given
     * type defined.
     *
     * @param annotationType the annotation type to look for
     * @return whether a matching annotation is defined
     */
    boolean hasAnnotation(String annotationType);

    /**
     * Retrieve the attributes of the annotation of the given type,
     * if any (i.e. if defined on the underlying class, as direct
     * annotation or as meta-annotation).
     *
     * @param annotationType the annotation type to look for
     * @return a Map of attributes, with the attribute name as key (e.g. "value")
     * and the defined attribute value as Map value. This return value will be
     * {@code null} if no matching annotation is defined.
     */
    AnnotationAttributes getAnnotationAttributes(String annotationType);
}