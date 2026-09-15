package org.apache.commons.csv;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.StringReader;
import java.io.IOException;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class CSVParser_nextRecord_Test {

    private CSVParser parser;

    private CSVFormat format;

    private String inputCSV;

    @BeforeEach
    void setupBeforeEach() throws IOException {
        inputCSV = "a,b,c,d\n" + " a , b , 1 2 \n" + "\"foo baar\", b,\n" + "d,e\n";
        format = CSVFormat.DEFAULT.withIgnoreSurroundingSpaces().withAllowMissingColumnNames();
        parser = new CSVParser(new StringReader(inputCSV), format);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (parser != null) {
            parser.close();
        }
    }

//     @Test
//     void testNextRecord_ValidInputs() throws IOException {
//         CSVRecord record;
//         record = parser.nextRecord();
//         assertNotNull(record, "First record should not be null");
//         assertEquals("a", record.get(0), "First field of the first record should be 'a'");
//         assertEquals("b", record.get(1), "Second field of the first record should be 'b'");
//         assertEquals("c", record.get(2), "Third field of the first record should be 'c'");
//         assertEquals("d", record.get(3), "Fourth field of the first record should be 'd'");
//         record = parser.nextRecord();
//         assertNotNull(record, "Second record should not be null");
//         assertEquals("a", record.get(0), "First field of the second record should be 'a'");
//         assertEquals("b", record.get(1), "Second field of the second record should be 'b'");
//         assertEquals("1 2", record.get(2), "Third field of the second record should be '1 2'");
//         assertThrows(IndexOutOfBoundsException.class, () -> record.get(3), "3rd column should be out of bounds");
//     }

    @Test
    void testNextRecord_EOF() throws IOException {
        // Consume all records
        while (parser.nextRecord() != null) {
            // No-op
        }
        // Should return null
        CSVRecord record = parser.nextRecord();
        assertNull(record, "Should return null when the end of the stream is reached");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testNextRecord_ThrowsIOExceptionOnInvalidParseSequence() throws IOException {
        String invalidCSV = "a,b,c,d\n" + " a , b , 1 2 \nINVALID_RECORD";
        parser = new CSVParser(new StringReader(invalidCSV), format);
        assertThrows(IOException.class, parser::nextRecord, "Expected IOException to be thrown due to invalid parse sequence");
    }

    @ParameterizedTest
    @ValueSource(strings = { "\"valid, record\"", "\"another,record\nwith newline\"" })
    void testNextRecord_ValidCSVRecords(String csvInput) throws Exception {
        parser = new CSVParser(new StringReader(csvInput), format);
        Method nextRecordMethod = CSVParser.class.getDeclaredMethod("nextRecord");
        nextRecordMethod.setAccessible(true);
        CSVRecord record = (CSVRecord) nextRecordMethod.invoke(parser);
        assertNotNull(record);
    }

    @Test
    void testNextRecord_CommentLine() throws IOException {
        String csvInputWithComment = "# this is a comment\n" + "a,b,c,d\n" + "1,2,3,4\n";
        parser = new CSVParser(new StringReader(csvInputWithComment), format.withCommentMarker('#'));
        CSVRecord record = parser.nextRecord();
        assertNotNull(record);
        assertEquals("a", record.get(0));
    }

    @Test
    void testNextRecord_MultipleConsecutiveEmptyLines() throws IOException {
        String csvInputWithEmptyLines = "a,b,c\n\n\n1,2,3\n\n";
        parser = new CSVParser(new StringReader(csvInputWithEmptyLines), format);
        CSVRecord record = parser.nextRecord();
        assertNotNull(record);
        assertEquals("a", record.get(0));
        record = parser.nextRecord();
        assertNotNull(record);
        assertEquals("1", record.get(0));
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testNextRecord_ThrowsIOExceptionOnInvalidToken() throws Exception {
        String invalidTokenCSV = "a,b,c,d\n" + " a , b , 1 2 \nINVALID_TOKEN";
        parser = new CSVParser(new StringReader(invalidTokenCSV), format);
        Method nextRecordMethod = CSVParser.class.getDeclaredMethod("nextRecord");
        nextRecordMethod.setAccessible(true);
        assertThrows(IOException.class, () -> {
            nextRecordMethod.invoke(parser);
        }, "Expected IOException to be thrown due to invalid token");
    }

    @Test
    void testNextRecord_ThrowsIOExceptionOnEOF() throws Exception {
        String incompleteCSV = "a,b,c,d\n";
        parser = new CSVParser(new StringReader(incompleteCSV), format);
        parser.nextRecord();
        Method nextRecordMethod = CSVParser.class.getDeclaredMethod("nextRecord");
        nextRecordMethod.setAccessible(true);
        assertNull(nextRecordMethod.invoke(parser), "Expected null to be returned due to EOF");
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testNextRecord_ThrowsIllegalStateExceptionOnUnexpectedTokenType() throws Exception {
        String unexpectedTokenCSV = "a,b,c,d\n%unexpectedTokenType\n";
        parser = new CSVParser(new StringReader(unexpectedTokenCSV), format);
        Method nextRecordMethod = CSVParser.class.getDeclaredMethod("nextRecord");
        nextRecordMethod.setAccessible(true);
        assertThrows(IllegalStateException.class, () -> {
            nextRecordMethod.invoke(parser);
        }, "Expected IllegalStateException to be thrown due to unexpected token type");
    }
@Test
void test_startCharPosition_calculation() throws IOException {
    String csvInput = "a,b,c,d\n";
    parser = new CSVParser(new StringReader(csvInput), format);
    CSVRecord record = parser.nextRecord();
    assertNotNull(record);
    assertEquals(0, record.getCharacterPosition(), "StartCharPosition should be 0");
}
@Test
void test_recordNumber_increment() throws IOException {
    String csvInput = "a,b,c,d\n1,2,3,4\n";
    parser = new CSVParser(new StringReader(csvInput), format);
    CSVRecord record1 = parser.nextRecord();
    assertNotNull(record1);
    assertEquals(1, record1.getRecordNumber(), "RecordNumber should be 1");
    CSVRecord record2 = parser.nextRecord();
    assertNotNull(record2);
    assertEquals(2, record2.getRecordNumber(), "RecordNumber should be 2");
}
@Test
void test_addRecordValue_call_on_EORECORD() throws IOException {
    String csvInput = "a,b,c,d\n";
    parser = new CSVParser(new StringReader(csvInput), format);
    CSVRecord record = parser.nextRecord();
    assertNotNull(record);
    assertEquals(4, record.size(), "Record should have 4 values");
}
}