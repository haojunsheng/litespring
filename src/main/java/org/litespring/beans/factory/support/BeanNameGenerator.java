package org.litespring.beans.factory.support;

import org.litespring.beans.BeanDefinition;
/**
 * Strategy interface for generating bean names for bean definitions.
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 */
public interface BeanNameGenerator {
    /**
     * Generate a bean name for the given bean definition.
     *
     * @param definition the bean definition to generate a name for
     * @param registry   the bean definition registry that the given definition
     *                   is supposed to be registered with
     * @return the generated bean name
     */
    String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry);
}