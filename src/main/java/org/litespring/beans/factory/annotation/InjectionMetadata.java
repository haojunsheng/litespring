package org.litespring.beans.factory.annotation;

import java.util.List;

/**
 * Internal class for managing injection metadata.
 * Not intended for direct use in applications.
 *
 * <p>Used by {@link AutowiredAnnotationBeanPostProcessor},
 * {@link org.springframework.context.annotation.CommonAnnotationBeanPostProcessor} and
 * {@link org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor}.
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class InjectionMetadata {

    private final Class<?> targetClass;
    private List<InjectionElement> injectionElements;

    public InjectionMetadata(Class<?> targetClass, List<InjectionElement> injectionElements) {
        this.targetClass = targetClass;
        this.injectionElements = injectionElements;
    }

    public List<InjectionElement> getInjectionElements() {
        return injectionElements;
    }

     /**
     * Either this or {@link #getResourceToInject} needs to be overridden.
     */
    public void inject(Object target) {
        if (injectionElements == null || injectionElements.isEmpty()) {
            return;
        }
        for (InjectionElement ele : injectionElements) {

            ele.inject(target);
        }
    }
}