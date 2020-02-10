package org.litespring.core.type.classreading;

import org.litespring.core.annotation.AnnotationAttributes.AnnotationAttributes;
import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.SpringAsmInfo;

import java.util.Map;
/**
 * ASM visitor which looks for the annotations defined on a class or method, including
 * tracking meta-annotations.
 *
 * <p>As of Spring 3.1.1, this visitor is fully recursive, taking into account any nested
 * annotations or nested annotation arrays. These annotations are in turn read into
 * {@link AnnotationAttributes} map structures.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 3.0
 */
final class AnnotationAttributesReadingVisitor extends AnnotationVisitor {

    private final String annotationType;

    private final Map<String, AnnotationAttributes> attributesMap;

    AnnotationAttributes attributes = new AnnotationAttributes();


    public AnnotationAttributesReadingVisitor(
            String annotationType, Map<String, AnnotationAttributes> attributesMap) {
        super(SpringAsmInfo.ASM_VERSION);

        this.annotationType = annotationType;
        this.attributesMap = attributesMap;

    }
    @Override
    public final void visitEnd(){
        this.attributesMap.put(this.annotationType, this.attributes);
    }

    @Override
    public void visit(String attributeName, Object attributeValue) {
        this.attributes.put(attributeName, attributeValue);
    }
}