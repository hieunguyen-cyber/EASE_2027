package org.jdom2;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Element_getNamespacesInScope_Test {

    private Element element;

    private Namespace namespace;

    private Attribute attributeWithNamespace;

    private Namespace attributeNamespace;

    @BeforeEach
    void setupBeforeEach() {
        namespace = Namespace.getNamespace("prefix", "http://example.com");
        element = new Element("testElement", namespace);
        attributeNamespace = Namespace.getNamespace("attrPrefix", "http://attr.example.com");
    }

    @AfterEach
    void teardownAfterEach() {
        element = null;
        namespace = null;
        attributeNamespace = null;
    }

    // Reflection helper method to set parent element
    private void setParentElement(Element element, Element parent) {
        try {
            java.lang.reflect.Field parentField = Element.class.getDeclaredField("parent");
            parentField.setAccessible(true);
            parentField.set(element, parent);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // Helper method to invoke the private getNamespacesInScope method
//     private List<Namespace> invokeGetNamespacesInScope() throws Exception {
//         Method method = Element.class.getDeclaredMethod("getNamespacesInScope");
//         method.setAccessible(true);
//         return (List<Namespace>) method.invoke(element);
//     }

    @Test
    void testGetNamespacesInScope() {
        List<Namespace> namespaces = element.getNamespacesInScope();
        assertNotNull(namespaces);
    }

    @Test
    void testGetNamespacesInScope_WithNoAdditionalNamespaces_AndNoParent() {
        List<Namespace> namespaces = element.getNamespacesInScope();
        assertNotNull(namespaces);
        assertTrue(namespaces.contains(Namespace.XML_NAMESPACE));
        assertTrue(namespaces.contains(Namespace.NO_NAMESPACE));
        assertEquals(2, namespaces.size());
    }

    @Test
    void testGetNamespacesInScope_WithAdditionalNamespaces() {
        Namespace additionalNamespace = Namespace.getNamespace("anotherPrefix", "http://another.example.com");
        element.addNamespaceDeclaration(additionalNamespace);
        List<Namespace> namespaces = element.getNamespacesInScope();
        assertTrue(namespaces.contains(additionalNamespace));
        assertEquals(3, namespaces.size());
    }

    @Test
    void testGetNamespacesInScope_WithAttributes() {
        Attribute attributeWithNamespace = new Attribute("att", "value", attributeNamespace);
        element.setAttribute(attributeWithNamespace);
        List<Namespace> namespaces = element.getNamespacesInScope();
        assertTrue(namespaces.contains(attributeWithNamespace.getNamespace()));
        // Updated expected size based on the context
        assertEquals(4, namespaces.size());
    }

    @Test
    void testGetNamespacesInScope_WithParentElement() {
        Element parentElement = new Element("parent", Namespace.getNamespace("parentPrefix", "http://parent.example.com"));
        // Assume a setParent method exists in Element class
        element.setParent(parentElement);
        List<Namespace> namespaces = element.getNamespacesInScope();
        assertTrue(namespaces.contains(parentElement.getNamespace()));
        assertTrue(namespaces.contains(Namespace.XML_NAMESPACE));
        assertTrue(namespaces.contains(Namespace.NO_NAMESPACE));
        // Updated expected size based on context
        assertEquals(4, namespaces.size());
    }

    @Test
    void testGetNamespacesInScope_AsRootElement() {
        // Assume a setParent method exists for this to be null
        element.setParent(null);
        List<Namespace> namespaces = element.getNamespacesInScope();
        assertTrue(namespaces.contains(Namespace.NO_NAMESPACE));
        assertEquals(2, namespaces.size());
    }

    @Test
    void testGetNamespacesInScope_WithConflictingNamespaces() {
        Namespace conflictingNamespace = Namespace.getNamespace("prefix", "http://different.example.com");
        assertThrows(IllegalAddException.class, () -> element.addNamespaceDeclaration(conflictingNamespace));
    }

    @Test
    void testGetNamespacesInScope_WithNullAdditionalNamespaces() throws Exception {
        List<Namespace> namespaces = element.getNamespacesInScope();
        assertNotNull(namespaces);
        assertFalse(namespaces.contains(null));
    }

    @Test
    void testGetNamespacesInScope_WithAttributes_ThrowsException() throws Exception {
        Element spyElement = Mockito.spy(element);
        doThrow(new RuntimeException("Get Attributes Failed")).when(spyElement).getAttributes();
        Exception exception = assertThrows(RuntimeException.class, spyElement::getNamespacesInScope);
        assertEquals("Get Attributes Failed", exception.getMessage());
    }

    @Test
    void testGetNamespacesInScope_WhenParentElementIsNull_ShouldIncludeNoNamespace() throws Exception {
        // Assuming setParent sets merging to null parent
        element.setParent(null);
        List<Namespace> namespaces = element.getNamespacesInScope();
        assertTrue(namespaces.contains(Namespace.NO_NAMESPACE));
    }

    @Test
    void testGetNamespacesInScope_WithInvalidAttributeNamespace() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            Attribute invalidAttribute = new Attribute("invalidAtt", "value", Namespace.getNamespace("invalidPrefix", ""));
            element.setAttribute(invalidAttribute);
        });
    }
}