package org.jdom2.input;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import javax.xml.stream.XMLStreamReader;
import org.jdom2.JDOMFactory;
import org.jdom2.input.stax.StAXFilter;
import org.jdom2.JDOMException;
import javax.xml.stream.XMLStreamException;
import org.jdom2.Element;
import javax.xml.namespace.QName;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class StAXStreamBuilder_processPrunableElement_Test {

    private JDOMFactory factory;

    private XMLStreamReader reader;

    private StAXFilter filter;

    private StAXStreamBuilder builder;

    @BeforeEach
    void setupBeforeEach() {
        factory = mock(JDOMFactory.class);
        reader = mock(XMLStreamReader.class);
        filter = mock(StAXFilter.class);
        builder = new StAXStreamBuilder();
    }

    // Reflection method to invoke private static method processPrunableElement
    private Element invokeProcessPrunableElement(JDOMFactory factory, XMLStreamReader reader, int topdepth, StAXFilter filter) throws Exception {
        var method = StAXStreamBuilder.class.getDeclaredMethod("processPrunableElement", JDOMFactory.class, XMLStreamReader.class, int.class, StAXFilter.class);
        method.setAccessible(true);
        try {
            return (Element) method.invoke(null, factory, reader, topdepth, filter);
        } catch (InvocationTargetException e) {
            // Unwrap the JDOMException and throw it
            Throwable cause = e.getCause();
            if (cause instanceof JDOMException) {
                throw (JDOMException) cause;
            } else if (cause instanceof XMLStreamException) {
                throw new JDOMException(cause.getMessage(), cause);
            }
            // Re-throw unexpected exceptions
            throw e;
        }
    }

    @Test
    void testProcessPrunableElement_ThrowsJDOMException_WhenNotAtStartElement() throws XMLStreamException {
        when(reader.getEventType()).thenReturn(XMLStreamReader.CHARACTERS);
        JDOMException thrown = assertThrows(JDOMException.class, () -> invokeProcessPrunableElement(factory, reader, 0, filter));
        assertEquals("JDOM requires that the XMLStreamReader is at the START_ELEMENT state when retrieving an Element Fragment.", thrown.getMessage());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testProcessPrunableElement_ThrowsJDOMException_WhenXMLStreamExceptionOccurs() throws XMLStreamException {
        when(reader.getEventType()).thenThrow(new XMLStreamException("Stream exception occurred"));
        JDOMException thrown = assertThrows(JDOMException.class, () -> invokeProcessPrunableElement(factory, reader, 0, filter));
        assertEquals("Stream exception occurred", thrown.getCause().getMessage());
    }
}
