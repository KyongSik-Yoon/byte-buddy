package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeWriterFieldPoolRecordTest {

    private static final int MODIFIER = 42;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldAttributeAppender fieldAttributeAppender;

    @Mock
    private AnnotationValueFilter valueFilter;

    @Mock
    private AnnotationValueFilter.Factory annotationValueFilterFactory;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private FieldVisitor fieldVisitor;

    @Mock
    private FieldDescription fieldDescription;

    @Mock
    private Object defaultValue;

    @Before
    public void setUp() throws Exception {
        when(fieldDescription.getModifiers()).thenReturn(MODIFIER);
        when(fieldDescription.getInternalName()).thenReturn(FOO);
        when(fieldDescription.getDescriptor()).thenReturn(BAR);
        when(fieldDescription.getGenericSignature()).thenReturn(QUX);
        when(classVisitor.visitField(MODIFIER, FOO, BAR, QUX, defaultValue)).thenReturn(fieldVisitor);
        when(classVisitor.visitField(MODIFIER, FOO, BAR, QUX, null)).thenReturn(fieldVisitor);
        when(annotationValueFilterFactory.on(fieldDescription)).thenReturn(valueFilter);
    }

    @Test
    public void testRichFieldEntryProperties() throws Exception {
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForRichField(fieldAttributeAppender, defaultValue, fieldDescription);
        assertThat(record.getFieldAppender(), is(fieldAttributeAppender));
        assertThat(record.resolveDefault(FieldDescription.NO_DEFAULT_VALUE), is(defaultValue));
    }

    @Test
    public void testRichFieldEntryWritesField() throws Exception {
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForRichField(fieldAttributeAppender, defaultValue, fieldDescription);
        record.apply(classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitField(MODIFIER, FOO, BAR, QUX, defaultValue);
        verify(fieldAttributeAppender).apply(fieldVisitor, fieldDescription, valueFilter);
        verifyNoMoreInteractions(fieldAttributeAppender);
        verifyNoMoreInteractions(classVisitor);
        verify(fieldVisitor).visitEnd();
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testSimpleFieldEntryWritesField() throws Exception {
        when(fieldDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForSimpleField(fieldDescription); /// TODO: Annotation
        record.apply(classVisitor, annotationValueFilterFactory);
        verify(classVisitor).visitField(MODIFIER, FOO, BAR, QUX, null);
        verifyNoMoreInteractions(classVisitor);
        verify(fieldVisitor).visitEnd();
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testSimpleFieldEntryProperties() throws Exception {
        TypeWriter.FieldPool.Record record = new TypeWriter.FieldPool.Record.ForSimpleField(fieldDescription);
        assertThat(record.getFieldAppender(), is((FieldAttributeAppender) FieldAttributeAppender.ForInstrumentedField.INSTANCE));
        assertThat(record.resolveDefault(defaultValue), is(defaultValue));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.FieldPool.Record.ForSimpleField.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.FieldPool.Record.ForRichField.class).apply();
    }
}
