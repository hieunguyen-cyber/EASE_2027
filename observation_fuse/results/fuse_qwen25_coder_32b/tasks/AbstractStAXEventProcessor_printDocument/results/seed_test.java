package org.jdom2.output.support;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.xml.stream.util.XMLEventConsumer;
import javax.xml.stream.XMLEventFactory;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.util.NamespaceStack;
import javax.xml.stream.XMLStreamException;
import java.util.Collections;
import java.lang.reflect.Method;
import java.util.List;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.DocType;
import org.jdom2.Comment;
import org.jdom2.output.support.FormatStack;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class AbstractStAXEventProcessor_printDocument_Test {

    private XMLEventConsumer out;

    private FormatStack formatStack;

    private NamespaceStack namespaceStack;

    private XMLEventFactory eventFactory;

    private Document document;

    // Target class instance
    private AbstractStAXEventProcessor processor;

    @BeforeEach
    void setupBeforeEach() {
        out = mock(XMLEventConsumer.class);
        Format format = mock(Format.class);
        formatStack = new FormatStack(format);
        namespaceStack = new NamespaceStack();
        eventFactory = mock(XMLEventFactory.class);
        // Initialize with a simple Document instance
        document = new Document();
        processor = new AbstractStAXEventProcessor() {

            @Override
            public void process(XMLEventConsumer out, Format format, XMLEventFactory eventFactory, List<? extends Content> list) throws XMLStreamException {
                // Dummy implementation
            }
        };
    }

    // Utility method to invoke the private printDocument method using reflection
    private void invokePrintDocument() throws Exception {
        Method method = AbstractStAXEventProcessor.class.getDeclaredMethod("printDocument", XMLEventConsumer.class, FormatStack.class, NamespaceStack.class, XMLEventFactory.class, Document.class);
        method.setAccessible(true);
        method.invoke(processor, out, formatStack, namespaceStack, eventFactory, document);
    }

    @Test
    void testPrintDocument() throws Exception {
        invokePrintDocument();
        verify(out).add(any());
    }

    @Test
    void testPrintDocument_WithOmitDeclaration() throws Exception {
        // Given phase: Setting up format stack to omit declaration
        when(formatStack.isOmitDeclaration()).thenReturn(true);
        // Mocking the return value for getTextMode
        when(formatStack.getTextMode()).thenReturn(Format.TextMode.NORMALIZE);
        invokePrintDocument();
        verify(out).add(eventFactory.createStartDocument(null, null));
        verify(out).add(eventFactory.createEndDocument());
    }

    @Test
    void testPrintDocument_WithOmitEncoding() throws Exception {
        when(formatStack.isOmitEncoding()).thenReturn(true);
        when(formatStack.getLineSeparator()).thenReturn("\n");
        when(formatStack.getTextMode()).thenReturn(Format.TextMode.NORMALIZE);
        invokePrintDocument();
        verify(out).add(eventFactory.createStartDocument(null, "1.0"));
        verify(out).add(eventFactory.createCharacters("\n"));
        verify(out).add(eventFactory.createEndDocument());
    }

    @Test
    void testPrintDocument_WithEncodingAndSeparator() throws Exception {
        when(formatStack.isOmitDeclaration()).thenReturn(false);
        when(formatStack.getEncoding()).thenReturn("UTF-8");
        when(formatStack.getLineSeparator()).thenReturn("\n");
        when(formatStack.getTextMode()).thenReturn(Format.TextMode.NORMALIZE);
        invokePrintDocument();
        verify(out).add(eventFactory.createStartDocument("UTF-8", "1.0"));
        verify(out).add(eventFactory.createCharacters("\n"));
        verify(out).add(eventFactory.createEndDocument());
    }

    @Test
    void testPrintDocument_EmptyDocument() throws Exception {
        document = new Document();
        invokePrintDocument();
        verify(out).add(eventFactory.createEndDocument());
    }

    @Test
    void testPrintDocument_WithRootElement() throws Exception {
        Element root = new Element("root");
        document.setRootElement(root);
        when(formatStack.getTextMode()).thenReturn(Format.TextMode.NORMALIZE);
        invokePrintDocument();
        verify(out).add(any());
    }

    @Test
    void testPrintDocument_WithMultipleContents() throws Exception {
        Element root = new Element("root");
        document.setRootElement(root);
        document.addContent(new Comment("This is a comment"));
        document.setDocType(new DocType("root"));
        when(formatStack.getTextMode()).thenReturn(Format.TextMode.NORMALIZE);
        invokePrintDocument();
        verify(out, atLeast(1)).add(any());
    }

    @Test
    void testPrintDocument_ThrowsXMLStreamException_WhenOutputConsumerFails() throws Exception {
        doThrow(new XMLStreamException("Output consumer failure")).when(out).add(any());
        Exception exception = assertThrows(XMLStreamException.class, this::invokePrintDocument);
        assertEquals("Output consumer failure", exception.getMessage());
    }

    @Test
    void testPrintDocument_ThrowsXMLStreamException_WhenEventFactoryFails() throws Exception {
        when(eventFactory.createStartDocument(null, null)).thenThrow(new XMLStreamException("Event factory failure"));
        Exception exception = assertThrows(XMLStreamException.class, this::invokePrintDocument);
        assertEquals("Event factory failure", exception.getMessage());
    }

    @Test
    void testPrintDocument_ThrowsXMLStreamException_OnEmptyDocument() {
        document = new Document();
        Exception exception = assertThrows(XMLStreamException.class, this::invokePrintDocument);
        assertNotNull(exception);
    }

    @Test
    void testPrintDocument_WithMultipleContents_ThrowsXMLStreamException_WhenWalkerFails() throws Exception {
        document.setRootElement(new Element("root"));
        document.addContent(new Comment("This is a comment"));
        when(eventFactory.createCharacters(anyString())).thenThrow(new XMLStreamException("Walker failure"));
        Exception exception = assertThrows(XMLStreamException.class, this::invokePrintDocument);
        assertEquals("Walker failure", exception.getMessage());
    }
}
