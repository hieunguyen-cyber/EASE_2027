package org.jdom2.output.support;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.StringWriter;
import java.io.Writer;
import org.jdom2.output.Format;
import org.jdom2.util.NamespaceStack;
import org.jdom2.output.support.Walker;
import org.jdom2.Content;
import org.jdom2.CDATA;
import org.jdom2.Comment;
import org.jdom2.DocType;
import org.jdom2.Element;
import org.jdom2.EntityRef;
import org.jdom2.ProcessingInstruction;
import org.jdom2.Text;
import java.util.*;
import java.io.IOException;
import java.lang.reflect.Method;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.ParameterizedTest;
import org.jdom2.output.support.FormatStack;
import java.util.stream.Stream;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class AbstractXMLOutputProcessor_printContent_Test {

    private AbstractXMLOutputProcessor processor;

    private Format format;

    private StringWriter stringWriter;

    private FormatStack fstack;

    private NamespaceStack nstack;

    private Walker walker;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize necessary fields and dependencies
        processor = Mockito.spy(new AbstractXMLOutputProcessor() {

            @Override
            protected void printText(Writer out, FormatStack fstack, Text text) throws IOException {
                out.write(text.getText());
            }

            protected void printCDATA(Writer out, FormatStack fstack, CDATA cdata) throws IOException {
                out.write("<![CDATA[" + cdata.getText() + "]]>");
            }

            protected void printComment(Writer out, FormatStack fstack, Comment comment) throws IOException {
                out.write("<!-- " + comment.getText() + " -->");
            }

            protected void printElement(Writer out, FormatStack fstack, NamespaceStack nstack, Element element) throws IOException {
                out.write("<" + element.getName() + ">" + element.getText() + "</" + element.getName() + ">");
            }

            protected void printDocType(Writer out, FormatStack fstack, DocType docType) throws IOException {
                out.write("<!DOCTYPE " + docType.getPublicID() + ">");
            }

            protected void printEntityRef(Writer out, FormatStack fstack, EntityRef entity) throws IOException {
                out.write("&" + entity.getName() + ";");
            }

            protected void printProcessingInstruction(Writer out, FormatStack fstack, ProcessingInstruction pi) throws IOException {
                out.write("<?" + pi.getTarget() + " " + pi.getData() + "?>");
            }
        });
        format = Format.getRawFormat();
        stringWriter = new StringWriter();
        fstack = new FormatStack(format);
        nstack = new NamespaceStack();
    }

    @AfterEach
    void teardownAfterEach() {
        stringWriter.getBuffer().setLength(0);
    }

    private Walker buildWalker(List<Content> contents) {
        // Provide a working implementation or mock the walker based on the provided contents for testing
        Walker walker = mock(Walker.class);
        when(walker.hasNext()).thenReturn(true, false);
        // Adjust to return the appropriate content
        when(walker.next()).thenAnswer(invocation -> contents.get(0));
        return walker;
    }

    private static Stream<Arguments> provideMixedContent() {
        return Stream.of(Arguments.of(Arrays.asList(new Text("Text1"), new CDATA("CDATA1"), new Comment("Comment1")), "Text1<![CDATA[CDATA1]]><!-- Comment1 -->"), Arguments.of(Arrays.asList(new EntityRef("entityRef"), new ProcessingInstruction("target", "data")), "&entityRef;<?target data?>"));
    }

    @Test
    void testPrintContent_withText() throws IOException {
        List<Content> contents = Collections.singletonList(new Text("example text"));
        walker = buildWalker(contents);
        processor.printContent(stringWriter, fstack, nstack, walker);
        assertEquals("example text", stringWriter.toString());
    }

    @Test
    void testPrintContent_withCDATA() throws IOException {
        List<Content> contents = Collections.singletonList(new CDATA("example CDATA"));
        walker = buildWalker(contents);
        processor.printContent(stringWriter, fstack, nstack, walker);
        assertEquals("<![CDATA[example CDATA]]>", stringWriter.toString());
    }

    @Test
    void testPrintContent_withComment() throws IOException {
        List<Content> contents = Collections.singletonList(new Comment("example comment"));
        walker = buildWalker(contents);
        processor.printContent(stringWriter, fstack, nstack, walker);
        assertEquals("<!-- example comment -->", stringWriter.toString());
    }

    @Test
    void testPrintContent_withElement() throws IOException {
        Element element = new Element("example").addContent("content");
        List<Content> contents = Collections.singletonList(element);
        walker = buildWalker(contents);
        processor.printContent(stringWriter, fstack, nstack, walker);
        assertEquals("<example>content</example>", stringWriter.toString());
    }

    @Test
    void testPrintContent_withDocumentType() throws IOException {
        DocType docType = new DocType("example");
        List<Content> contents = Collections.singletonList(docType);
        walker = buildWalker(contents);
        processor.printContent(stringWriter, fstack, nstack, walker);
        assertEquals("<!DOCTYPE example>", stringWriter.toString());
    }

    @Test
    void testPrintContent_withEntityRef() throws IOException {
        EntityRef entityRef = new EntityRef("exampleEntity");
        List<Content> contents = Collections.singletonList(entityRef);
        walker = buildWalker(contents);
        processor.printContent(stringWriter, fstack, nstack, walker);
        assertEquals("&exampleEntity;", stringWriter.toString());
    }

    @Test
    void testPrintContent_withProcessingInstruction() throws IOException {
        ProcessingInstruction pi = new ProcessingInstruction("target", "data");
        List<Content> contents = Collections.singletonList(pi);
        walker = buildWalker(contents);
        processor.printContent(stringWriter, fstack, nstack, walker);
        assertEquals("<?target data?>", stringWriter.toString());
    }

    @ParameterizedTest
    @MethodSource("provideMixedContent")
    void testPrintContent_withMixedContent(List<Content> contents, String expectedOutput) throws IOException {
        walker = buildWalker(contents);
        processor.printContent(stringWriter, fstack, nstack, walker);
        assertEquals(expectedOutput, stringWriter.toString());
    }

//     @Test
//     void testPrintContent_withNullWriter() {
//         assertThrows(InvocationTargetException.class, () -> {
//             Method method = AbstractXMLOutputProcessor.class.getDeclaredMethod("printContent", Writer.class, FormatStack.class, NamespaceStack.class, Walker.class);
//             method.setAccessible(true);
//             method.invoke(processor, null, fstack, nstack, walker);
//         });
//     }

//     @Test
//     void testPrintContent_withNullWalker() throws IOException {
//         assertThrows(InvocationTargetException.class, () -> {
//             Method method = AbstractXMLOutputProcessor.class.getDeclaredMethod("printContent", Writer.class, FormatStack.class, NamespaceStack.class, Walker.class);
//             method.setAccessible(true);
//             method.invoke(processor, stringWriter, fstack, nstack, null);
//         });
//     }

    @Test
    void testPrintContent_withWalkerThatThrowsIOException() throws IOException {
        Walker faultyWalker = mock(Walker.class);
        when(faultyWalker.hasNext()).thenReturn(true);
        when(faultyWalker.next()).thenThrow(new IOException("Walker IOException"));
        assertThrows(IOException.class, () -> {
            Method method = AbstractXMLOutputProcessor.class.getDeclaredMethod("printContent", Writer.class, FormatStack.class, NamespaceStack.class, Walker.class);
            method.setAccessible(true);
            method.invoke(processor, stringWriter, fstack, nstack, faultyWalker);
        });
    }
}