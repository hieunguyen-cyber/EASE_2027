package org.jdom2.output.support;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.util.XMLEventConsumer;
import org.jdom2.output.Format;
import org.jdom2.output.support.FormatStack;
import org.jdom2.util.NamespaceStack;
import org.jdom2.output.support.Walker;
import javax.xml.stream.XMLStreamException;
import org.jdom2.Content;
import org.jdom2.Content.CType;
import org.jdom2.CDATA;
import org.jdom2.Comment;
import org.jdom2.Element;
import org.jdom2.Text;
import java.util.Collections;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class AbstractStAXEventProcessor_printContent_Test {

    // Mocks for dependencies
    private XMLEventConsumer out;

    private FormatStack fstack;

    private NamespaceStack nstack;

    private XMLEventFactory eventfactory;

    private Walker walker;

    @BeforeEach
    void setupBeforeEach() {
        // Preparing mocks and any other setup for each test
        out = mock(XMLEventConsumer.class);
        Format format = mock(Format.class);
        fstack = new FormatStack(format);
        nstack = new NamespaceStack();
        eventfactory = mock(XMLEventFactory.class);
        walker = mock(Walker.class);
    }

    @Test
    void testPrintContent() throws XMLStreamException {
        // Arrange: Set up any needed state
        when(walker.hasNext()).thenReturn(true).thenReturn(false);
        when(walker.next()).thenReturn(new Text("Some text"));
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act: Call the printContent method
        processor.printContent(out, fstack, nstack, eventfactory, walker);
        // Assert: Verify expected outcomes
        verify(out).add(any());
    }

    @Test
    void testPrintContentWithNullContent() throws XMLStreamException {
        when(walker.hasNext()).thenReturn(true).thenReturn(false);
        when(walker.next()).thenReturn(null);
        when(walker.isCDATA()).thenReturn(true);
        when(walker.text()).thenReturn("Some CDATA text");
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act
        processor.printContent(out, fstack, nstack, eventfactory, walker);
        // Assert
        verify(processor).printCDATA(out, fstack, eventfactory, new CDATA("Some CDATA text"));
    }

    @Test
    void testPrintContentWithComment() throws XMLStreamException {
        Comment comment = new Comment("This is a comment");
        when(walker.hasNext()).thenReturn(true).thenReturn(false);
        when(walker.next()).thenReturn(comment);
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act
        processor.printContent(out, fstack, nstack, eventfactory, walker);
        // Assert
        verify(processor).printComment(out, fstack, eventfactory, comment);
    }

    @Test
    void testPrintContentWithText() throws XMLStreamException {
        Text textContent = new Text("This is text");
        when(walker.hasNext()).thenReturn(true).thenReturn(false);
        when(walker.next()).thenReturn(textContent);
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act
        processor.printContent(out, fstack, nstack, eventfactory, walker);
        // Assert
        verify(processor).printText(out, fstack, eventfactory, textContent);
    }

    @Test
    void testPrintContentWithElement() throws XMLStreamException {
        Element element = new Element("element");
        when(walker.hasNext()).thenReturn(true).thenReturn(false);
        when(walker.next()).thenReturn(element);
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act
        processor.printContent(out, fstack, nstack, eventfactory, walker);
        // Assert
        verify(processor).printElement(out, fstack, nstack, eventfactory, element);
    }

    @Test
    void testPrintContentWithUnexpectedContentType() {
        when(walker.hasNext()).thenReturn(true).thenReturn(false);
        Content content = mock(Content.class);
        when(content.getCType()).thenReturn(null);
        when(walker.next()).thenReturn(content);
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            processor.printContent(out, fstack, nstack, eventfactory, walker);
        });
    }

    @Test
    void testPrintContentWithNullContentAsCDATA() throws XMLStreamException {
        // Scenario: Walker returns null content, and it indicates CDATA type
        when(walker.hasNext()).thenReturn(true).thenReturn(false);
        when(walker.next()).thenReturn(null);
        when(walker.isCDATA()).thenReturn(true);
        when(walker.text()).thenReturn("Some CDATA text");
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act
        processor.printContent(out, fstack, nstack, eventfactory, walker);
        // Assert
        verify(processor).printCDATA(out, fstack, eventfactory, new CDATA("Some CDATA text"));
    }

    @Test
    void testPrintContentWithNullEventFactory() {
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            processor.printContent(out, fstack, nstack, null, walker);
        });
    }

    @Test
    void testPrintContentWithWalkerThrowingException() throws XMLStreamException {
        when(walker.hasNext()).thenReturn(true);
        when(walker.next()).thenThrow(new RuntimeException("Walker exception"));
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            processor.printContent(out, fstack, nstack, eventfactory, walker);
        });
    }

    @Test
    void testPrintContentWithUnexpectedContentTypeException() throws XMLStreamException {
        when(walker.hasNext()).thenReturn(true).thenReturn(false);
        Content mockContent = mock(Content.class);
        when(mockContent.getCType()).thenReturn(CType.Comment);
        when(walker.next()).thenReturn(mockContent);
        AbstractStAXEventProcessor processor = Mockito.mock(AbstractStAXEventProcessor.class, Mockito.CALLS_REAL_METHODS);
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            processor.printContent(out, fstack, nstack, eventfactory, walker);
        });
    }
}
