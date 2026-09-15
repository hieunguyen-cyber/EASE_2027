package org.jdom2.output.support;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.StringWriter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.support.FormatStack;
import org.jdom2.util.NamespaceStack;
import java.io.Writer;
import java.io.IOException;
import org.jdom2.Content;
import org.jdom2.Comment;
import org.jdom2.ProcessingInstruction;
import org.jdom2.Text;
import org.jdom2.CDATA;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class AbstractXMLOutputProcessor_printDocument_Test {

    private Writer out;

    private FormatStack fstack;

    private NamespaceStack nstack;

    private Document doc;

    @Nested
    class PrintDocumentWithTextContentTests {

        @Test
        void testPrintDocument_withSingleTextNode() throws Exception {
            // Given
            Element root = new Element("root");
            root.addContent(new Text("This is a text node"));
            doc.setRootElement(root);
            // When
            getProcessor().printDocument(out, fstack, nstack, doc);
            // Then
            String expectedOutput = "<root>This is a text node</root>";
            assertEquals(expectedOutput, out.toString().replaceAll("\\s+", ""));
        }

        @Test
        void testPrintDocument_withMultipleTextNodes() throws Exception {
            // Given
            Element root = new Element("root");
            root.addContent(new Text("First text"));
            root.addContent(new Text("Second text"));
            doc.setRootElement(root);
            // When
            getProcessor().printDocument(out, fstack, nstack, doc);
            // Then
            String expectedOutput = "<root>First textSecond text</root>";
            assertEquals(expectedOutput, out.toString().replaceAll("\\s+", ""));
        }
    }

    @Nested
    class PrintDocumentWithVariousContentTests {

        @Test
        void testPrintDocument_withEmptyDocument() throws Exception {
            // Given
            Document emptyDoc = new Document();
            // When
            getProcessor().printDocument(out, fstack, nstack, emptyDoc);
            // Then
            assertEquals("", out.toString().trim());
        }

        @Test
        void testPrintDocument_withCommentContent() throws Exception {
            // Given
            Element root = new Element("root");
            root.addContent(new Comment("This is a comment"));
            doc.setRootElement(root);
            // When
            getProcessor().printDocument(out, fstack, nstack, doc);
            // Then
            String expectedOutput = "<root><!--This is a comment--></root>";
            assertEquals(expectedOutput, out.toString().replaceAll("\\s+", ""));
        }

        @Test
        void testPrintDocument_withProcessingInstruction() throws Exception {
            // Given
            Element root = new Element("root");
            root.addContent(new ProcessingInstruction("php", "echo 'Hello';"));
            doc.setRootElement(root);
            // When
            getProcessor().printDocument(out, fstack, nstack, doc);
            // Then
            String expectedOutput = "<root><?php echo 'Hello'; ?></root>";
            assertEquals(expectedOutput, out.toString().replaceAll("\\s+", ""));
        }

        @Test
        void testPrintDocument_withCDATAContent() throws Exception {
            // Given
            Element root = new Element("root");
            root.addContent(new CDATA("This is CDATA content"));
            doc.setRootElement(root);
            // When
            getProcessor().printDocument(out, fstack, nstack, doc);
            // Then
            String expectedOutput = "<root><![CDATA[This is CDATA content]]></root>";
            assertEquals(expectedOutput, out.toString().replaceAll("\\s+", ""));
        }
    }

    @Nested
    class PrintDocumentWithDifferentDocumentStructure {

        @Test
        void testPrintDocument_withDocumentWithoutRoot() throws Exception {
            // Given
            Document documentWithoutRoot = new Document();
            // When
            getProcessor().printDocument(out, fstack, nstack, documentWithoutRoot);
            // Then
            assertEquals("", out.toString().trim());
        }

        @Test
        void testPrintDocument_withTextAndComment() throws Exception {
            // Given
            Element root = new Element("root");
            root.addContent(new Text("Some text before comment"));
            root.addContent(new Comment("A comment here"));
            root.addContent(new Text("Some text after comment"));
            doc.setRootElement(root);
            // When
            getProcessor().printDocument(out, fstack, nstack, doc);
            // Then
            String expectedOutput = "<root>Some text before comment<!--A comment here-->Some text after comment</root>";
            assertEquals(expectedOutput, out.toString().replaceAll("\\s+", ""));
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        out = new StringWriter();
        fstack = new FormatStack(new Format());
        nstack = new NamespaceStack();
        doc = new Document();
    }

    private AbstractXMLOutputProcessor getProcessor() throws Exception {
        return new AbstractXMLOutputProcessor() {

            @Override
            protected void printElement(Writer out, FormatStack fstack, NamespaceStack nstack, Element element) throws IOException {
                super.printElement(out, fstack, nstack, element);
            }
        };
    }

    @Test
    void testPrintDocument_withValidInput() throws Exception {
        // Given
        // Setup is done in @BeforeEach
        // When
        getProcessor().printDocument(out, fstack, nstack, doc);
        // Then
        String expectedOutput = "<root/>";
        assertEquals(expectedOutput, out.toString().replaceAll("\\s+", ""));
    }

    @Test
    void testPrintDocument_withEmptyDocument() throws Exception {
        // Given
        // Create an empty Document
        Document emptyDoc = new Document();
        // When
        AbstractXMLOutputProcessor processor = Mockito.mock(AbstractXMLOutputProcessor.class);
        processor.printDocument(out, fstack, nstack, emptyDoc);
        // Then
        // Expect an empty output (no root element)
        assertEquals("", out.toString().trim());
    }

    @Test
    void testPrintDocument_withCommentContent() throws Exception {
        // Given
        Element root = new Element("root");
        root.addContent(new Comment("This is a comment"));
        doc.setRootElement(root);
        // When
        AbstractXMLOutputProcessor processor = Mockito.mock(AbstractXMLOutputProcessor.class);
        processor.printDocument(out, fstack, nstack, doc);
        // Then
        String expectedOutput = "<root><!--This is a comment--></root>";
        assertEquals(expectedOutput, out.toString().trim());
    }

    @Test
    void testPrintDocument_withProcessingInstruction() throws Exception {
        // Given
        Element root = new Element("root");
        root.addContent(new ProcessingInstruction("php", "echo 'Hello';"));
        doc.setRootElement(root);
        // When
        AbstractXMLOutputProcessor processor = Mockito.mock(AbstractXMLOutputProcessor.class);
        processor.printDocument(out, fstack, nstack, doc);
        // Then
        String expectedOutput = "<root><?php echo 'Hello'; ?></root>";
        assertEquals(expectedOutput, out.toString().trim());
    }

    @Test
    void testPrintDocument_withCDATAContent() throws Exception {
        // Given
        Element root = new Element("root");
        root.addContent(new CDATA("This is CDATA content"));
        doc.setRootElement(root);
        // When
        AbstractXMLOutputProcessor processor = Mockito.mock(AbstractXMLOutputProcessor.class);
        processor.printDocument(out, fstack, nstack, doc);
        // Then
        String expectedOutput = "<root><![CDATA[This is CDATA content]]></root>";
        assertEquals(expectedOutput, out.toString().trim());
    }

    @Test
    void testPrintDocument_throwsIOException_whenWriterFails() throws Exception {
        // Given
        Document sampleDoc = new Document(new Element("root"));
        // Simulate IOException using a mock Writer
        Writer failingWriter = Mockito.mock(Writer.class);
        Mockito.doThrow(new IOException("Writer error")).when(failingWriter).write(Mockito.anyString());
        // When
        AbstractXMLOutputProcessor processor = getProcessor();
        // Then
        IOException thrown = assertThrows(IOException.class, () -> {
            processor.printDocument(failingWriter, fstack, nstack, sampleDoc);
        });
        assertEquals("Writer error", thrown.getMessage());
    }

    @Test
    void testPrintDocument_throwsIOException_whenFormatStackIsNull() throws Exception {
        // Given
        Document sampleDoc = new Document(new Element("root"));
        // When
        AbstractXMLOutputProcessor processor = getProcessor();
        // Then
        assertThrows(NullPointerException.class, () -> {
            processor.printDocument(out, null, nstack, sampleDoc);
        });
    }

    @Test
    void testPrintDocument_throwsIOException_whenNamespaceStackIsNull() throws Exception {
        // Given
        Document sampleDoc = new Document(new Element("root"));
        // When
        AbstractXMLOutputProcessor processor = getProcessor();
        // Then
        assertThrows(NullPointerException.class, () -> {
            processor.printDocument(out, fstack, null, sampleDoc);
        });
    }
}
