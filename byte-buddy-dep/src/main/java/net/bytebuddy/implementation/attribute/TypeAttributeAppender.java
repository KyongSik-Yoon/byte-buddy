package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.objectweb.asm.ClassVisitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An appender that writes attributes or annotations to a given ASM {@link org.objectweb.asm.ClassVisitor}.
 */
public interface TypeAttributeAppender {

    /**
     * Applies this type attribute appender.
     *
     * @param classVisitor     The class visitor to which the annotations of this visitor should be written to.
     * @param instrumentedType A description of the instrumented type that is target of the ongoing instrumentation.
     */
    void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter);

    /**
     * A type attribute appender that does not append any attributes.
     */
    enum NoOp implements TypeAttributeAppender {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            /* do nothing */
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.NoOp." + name();
        }
    }

    /**
     * An attribute appender that writes all annotations that are found on a given target type to the
     * instrumented type this type attribute appender is applied onto. The visibility for the annotation
     * will be inferred from the annotations' {@link java.lang.annotation.RetentionPolicy}.
     */
    enum ForInstrumentedType implements TypeAttributeAppender {

        INSTANCE;

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (AnnotationDescription annotation : instrumentedType.getDeclaredAnnotations()) {
                appender = appender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation), annotationValueFilter);
            }
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.ForInstrumentedType." + name();
        }

        public static class Excluding implements TypeAttributeAppender {

            private final Set<? extends TypeDescription> excludedTypes;

            public Excluding(TypeDescription originalType) {
                this(new HashSet<TypeDescription>(originalType.getDeclaredAnnotations().asTypeList()));
            }

            public Excluding(Set<? extends TypeDescription> excludedTypes) {
                this.excludedTypes = excludedTypes;
            }

            @Override
            public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
                AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
                for (AnnotationDescription annotation : instrumentedType.getDeclaredAnnotations()) {
                    if (!excludedTypes.contains(annotation.getAnnotationType())) {
                        appender = appender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation), annotationValueFilter);
                    }
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && excludedTypes.equals(((Excluding) other).excludedTypes);
            }

            @Override
            public int hashCode() {
                return excludedTypes.hashCode();
            }

            @Override
            public String toString() {
                return "TypeAttributeAppender.ForInstrumentedType.Excluding{" +
                        "excludedTypes=" + excludedTypes +
                        '}';
            }
        }
    }

    /**
     * An attribute appender that appends a single annotation to a given type. The visibility for the annotation
     * will be inferred from the annotation's {@link java.lang.annotation.RetentionPolicy}.
     */
    class Explicit implements TypeAttributeAppender {

        /**
         * The annotations to write to the given type.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new annotation attribute appender for explicit annotation values.
         *
         * @param annotations The annotations to write to the given type.
         */
        public Explicit(List<? extends AnnotationDescription> annotations) {
            this.annotations = annotations;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            AnnotationAppender appender = new AnnotationAppender.Default(new AnnotationAppender.Target.OnType(classVisitor));
            for (AnnotationDescription annotation : annotations) {
                appender = appender.append(annotation, AnnotationAppender.AnnotationVisibility.of(annotation), annotationValueFilter);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotations.equals(((Explicit) other).annotations);
        }

        @Override
        public int hashCode() {
            return annotations.hashCode();
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.Explicit{" +
                    "annotations=" + annotations +
                    '}';
        }
    }

    /**
     * A compound type attribute appender that concatenates a number of other attribute appenders.
     */
    class Compound implements TypeAttributeAppender {

        /**
         * The type attribute appenders this compound appender represents in their application order.
         */
        private final List<? extends TypeAttributeAppender> typeAttributeAppenders;

        /**
         * Creates a new compound attribute appender.
         *
         * @param typeAttributeAppender The type attribute appenders to concatenate in the order of their application.
         */
        public Compound(TypeAttributeAppender... typeAttributeAppender) {
            this(Arrays.asList(typeAttributeAppender));
        }

        public Compound(List<? extends TypeAttributeAppender> typeAttributeAppenders) {
            this.typeAttributeAppenders = typeAttributeAppenders;
        }

        @Override
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            for (TypeAttributeAppender typeAttributeAppender : typeAttributeAppenders) {
                typeAttributeAppender.apply(classVisitor, instrumentedType, annotationValueFilter);
            }
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && typeAttributeAppenders.equals(((Compound) other).typeAttributeAppenders);
        }

        @Override
        public int hashCode() {
            return typeAttributeAppenders.hashCode();
        }

        @Override
        public String toString() {
            return "TypeAttributeAppender.Compound{typeAttributeAppenders=" + typeAttributeAppenders + '}';
        }
    }
}
