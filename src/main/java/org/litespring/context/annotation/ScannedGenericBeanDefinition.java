package org.litespring.context.annotation;

import org.litespring.beans.factory.annotation.AnnotatedBeanDefinition;
import org.litespring.beans.factory.support.GenericBeanDefinition;
import org.litespring.core.type.AnnotationMetadata;

/**
 * Extension of the {@link org.springframework.beans.factory.support.GenericBeanDefinition}
 * class, based on an ASM ClassReader, with support for annotation metadata exposed
 * through the {@link AnnotatedBeanDefinition} interface.
 *
 * <p>This class does <i>not</i> load the bean {@code Class} early.
 * It rather retrieves all relevant metadata from the ".class" file itself,
 * parsed with the ASM ClassReader. It is functionally equivalent to
 * {@link AnnotatedGenericBeanDefinition#AnnotatedGenericBeanDefinition(AnnotationMetadata)}
 * but distinguishes by type beans that have been <em>scanned</em> vs those that have
 * been otherwise registered or detected by other means.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.5
 * @see #getMetadata()
 * @see #getBeanClassName()
 * @see org.springframework.core.type.classreading.MetadataReaderFactory
 * @see AnnotatedGenericBeanDefinition
 */
public class ScannedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {
    private final AnnotationMetadata metadata;

    /**
     * Create a new ScannedGenericBeanDefinition for the class that the
     * given MetadataReader describes.
     * @param metadata the MetadataReader for the scanned target class
     */
    public ScannedGenericBeanDefinition(AnnotationMetadata metadata) {
        super();
        this.metadata = metadata;
        setBeanClassName(this.metadata.getClassName());
    }

    public AnnotationMetadata getMetadata() {
        return this.metadata;
    }
}