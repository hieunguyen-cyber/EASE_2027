package org.jdom2;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class Element_getNamespace_Test {

    // target class instance
    private Element element;

    // namespace dependency
    private Namespace namespace;

    // Prefix for the default namespace used in tests
    private static final String DEFAULT_NAMESPACE_PREFIX = "default";

    private static final String DEFAULT_NAMESPACE_URI = "http://example.com/default";

    @BeforeEach
    void setupBeforeEach() {
        // Obtain a Namespace instance through a static method (assuming such a method exists)
        namespace = Namespace.getNamespace(DEFAULT_NAMESPACE_PREFIX, DEFAULT_NAMESPACE_URI);
        // create an Element instance with namespace
        element = new Element("TestElement", namespace);
    }

    // Reflection method to set the parent of an Element since it is not exposed
//     private void setParent(Element child, Element parent) {
//         try {
//             Method setParentMethod = Element.class.getDeclaredMethod("setParent", Parent.class);
//             setParentMethod.setAccessible(true);
//             setParentMethod.invoke(child, parent);
//         } catch (NoSuchMethodException e) {
//             fail("setParent method not found: " + e.getMessage());
//         } catch (IllegalAccessException e) {
//             fail("setParent method cannot be accessed: " + e.getMessage());
//         } catch (Exception e) {
//             fail("Failed to set parent for element using reflection: " + e.getMessage());
//         }
//     }

    @Test
    void testGetNamespaceWithNullPrefix() {
        Namespace result = element.getNamespace(null);
        assertNull(result, "Expected null for null prefix");
    }

    @Test
    void testGetNamespaceWithXMLPrefix() {
        Namespace result = element.getNamespace(JDOMConstants.NS_PREFIX_XML);
        assertEquals(Namespace.XML_NAMESPACE, result, "Expected to return XML namespace");
    }

    @Test
    void testGetNamespaceWithDefaultNamespace() {
        element.addNamespaceDeclaration(namespace);
        Namespace result = element.getNamespace(DEFAULT_NAMESPACE_PREFIX);
        assertEquals(namespace, result, "Expected to return the associated namespace");
    }

    @Test
    void testGetNamespaceWithUnknownPrefix() {
        Namespace result = element.getNamespace("unknownPrefix");
        assertNull(result, "Expected null for unknown prefix");
    }

    @Test
    void testGetNamespaceWithElementNamespace() {
        Namespace result = element.getNamespace(namespace.getPrefix());
        assertEquals(namespace, result, "Expected to return the element's own namespace");
    }

    @Test
    void testGetNamespaceWithAdditionalNamespace() {
        Namespace additionalNamespace = Namespace.getNamespace("additional", "http://example.com/additional");
        element.addNamespaceDeclaration(additionalNamespace);
        Namespace result = element.getNamespace(additionalNamespace.getPrefix());
        assertEquals(additionalNamespace, result, "Expected to return the additional namespace");
    }

    @Test
    void testGetNamespaceWithAttributeNamespace() {
        Namespace attributeNamespace = Namespace.getNamespace("attr", "http://example.com/attr");
        Attribute attribute = new Attribute("attrName", "attrValue", attributeNamespace);
        element.setAttribute(attribute);
        Namespace result = element.getNamespace(attributeNamespace.getPrefix());
        assertEquals(attributeNamespace, result, "Expected to return the attribute's namespace");
    }

    @Test
    void testGetNamespaceWithParentNamespace() {
        // Create a parent element with a specific namespace
        Namespace parentNamespace = Namespace.getNamespace("parent", "http://example.com/parent");
        Element parentElement = new Element("ParentElement", parentNamespace);
        // Set the parent of the current element using the public method of the Element class
        // Assuming parentElement can take element as child
        parentElement.addContent(element);
        // Test with the parent's namespace prefix
        Namespace result = element.getNamespace(parentNamespace.getPrefix());
        assertEquals(parentNamespace, result, "Expected to return the parent element's namespace");
    }

    @Test
    void testGetNamespaceWithDetachedElement() {
        Element detachedElement = element.clone();
        assertNull(detachedElement.getNamespace("parent"), "Expected null for namespace in detached element");
    }
}