package com.apple.spark.api;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.apple.spark.operator.SparkApplication;
import com.apple.spark.AppConfig;
import com.apple.spark.operator.SparkApplicationSpec;
import java.lang.reflect.Method;
// Add other imports if necessary
import java.util.HashMap;
import java.util.Map;
import com.apple.spark.operator.DriverSpec;
import com.apple.spark.core.Constants;
import static com.apple.spark.core.Constants.*;
import com.apple.spark.operator.ExecutorSpec;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class SubmissionSummary_copyFrom_Test {

    private SubmissionSummary submissionSummary;

    private SparkApplication sparkApplicationResource;

    private AppConfig.SparkCluster sparkCluster;

    private AppConfig appConfig;

    @BeforeEach
    void setupBeforeEach() {
        submissionSummary = new SubmissionSummary();
        sparkApplicationResource = mock(SparkApplication.class);
        sparkCluster = mock(AppConfig.SparkCluster.class);
        appConfig = mock(AppConfig.class);
        // Mock the Metadata and its methods
        var metadata = mock(io.fabric8.kubernetes.api.model.ObjectMeta.class);
        when(metadata.getName()).thenReturn("testSubmissionId");
        // Return empty map by default
        when(metadata.getLabels()).thenReturn(new HashMap<>());
        when(sparkApplicationResource.getMetadata()).thenReturn(metadata);
    }

    @Test
    void testCopyFrom() {
        // Given
        when(sparkApplicationResource.getMetadata().getName()).thenReturn("testSubmissionId");
        // When
        submissionSummary.copyFrom(sparkApplicationResource);
        // Then
        assertEquals("testSubmissionId", submissionSummary.getSubmissionId());
    }

    @Test
    void testCopyFrom_WithLabels_AndDriverSpecs() {
        // Given
        SparkApplicationSpec spec = mock(SparkApplicationSpec.class);
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.PROXY_USER_LABEL, "testUser");
        labels.put(Constants.QUEUE_LABEL, "testQueue");
        labels.put(Constants.DAG_NAME_LABEL, "testDag");
        when(sparkApplicationResource.getMetadata().getLabels()).thenReturn(labels);
        when(sparkApplicationResource.getSpec()).thenReturn(spec);
        when(spec.getDriver()).thenReturn(mock(DriverSpec.class));
        when(spec.getExecutor()).thenReturn(mock(ExecutorSpec.class));
        when(spec.getDriver().getLabels()).thenReturn(labels);
        when(spec.getDriver().getMemory()).thenReturn("4G");
        when(spec.getDriver().getCores()).thenReturn(2);
        when(spec.getExecutor().getCores()).thenReturn(3);
        when(spec.getExecutor().getInstances()).thenReturn(5);
        when(spec.getExecutor().getMemory()).thenReturn("8G");
        // When
        submissionSummary.copyFrom(sparkApplicationResource);
        // Then
        assertEquals("testUser", submissionSummary.getUser());
        assertEquals("testQueue", submissionSummary.getQueue());
        assertEquals("testDag", submissionSummary.getDagName());
        assertEquals(2, submissionSummary.getDriverCores());
        assertEquals(Long.valueOf(4), submissionSummary.getDriverMemoryGB());
        assertEquals(5, submissionSummary.getExecutorInstances());
        assertEquals(3, submissionSummary.getExecutorCores());
        assertEquals(Long.valueOf(8), submissionSummary.getExecutorMemoryGB());
        assertEquals((5 * 8) + 4, submissionSummary.getTotalMemoryGB());
        assertEquals((5 * 3) + 2, submissionSummary.getTotalCores());
    }

    @Test
    void testCopyFrom_WithoutLabels() {
        // Given
        when(sparkApplicationResource.getMetadata().getLabels()).thenReturn(null);
        // When
        submissionSummary.copyFrom(sparkApplicationResource);
        // Then
        assertNull(submissionSummary.getUser());
        assertNull(submissionSummary.getQueue());
        assertNull(submissionSummary.getDagName());
        assertEquals(1, submissionSummary.getDriverCores());
        assertEquals(Long.valueOf(0), submissionSummary.getDriverMemoryGB());
        assertEquals(1, submissionSummary.getExecutorInstances());
        assertEquals(1, submissionSummary.getExecutorCores());
        assertEquals(Long.valueOf(0), submissionSummary.getExecutorMemoryGB());
        assertEquals(0L, submissionSummary.getTotalMemoryGB());
        assertEquals(3, submissionSummary.getTotalCores());
    }

    @Test
    void testCopyFrom_WithInvalidDriverMemory() {
        // Given
        SparkApplicationSpec spec = mock(SparkApplicationSpec.class);
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.PROXY_USER_LABEL, "testUser");
        labels.put(Constants.QUEUE_LABEL, "testQueue");
        when(sparkApplicationResource.getMetadata().getLabels()).thenReturn(labels);
        when(sparkApplicationResource.getSpec()).thenReturn(spec);
        when(spec.getDriver()).thenReturn(mock(DriverSpec.class));
        when(spec.getExecutor()).thenReturn(mock(ExecutorSpec.class));
        when(spec.getDriver().getMemory()).thenReturn("invalidMemoryString");
        // When
        submissionSummary.copyFrom(sparkApplicationResource);
        // Then
        assertEquals("testUser", submissionSummary.getUser());
        assertEquals("testQueue", submissionSummary.getQueue());
        assertNull(submissionSummary.getDagName());
        assertEquals(1, submissionSummary.getDriverCores());
        assertEquals(Long.valueOf(0), submissionSummary.getDriverMemoryGB());
        assertEquals(1, submissionSummary.getExecutorInstances());
        assertEquals(1, submissionSummary.getExecutorCores());
        assertEquals(Long.valueOf(0), submissionSummary.getExecutorMemoryGB());
        assertEquals(0L, submissionSummary.getTotalMemoryGB());
        assertEquals(3, submissionSummary.getTotalCores());
    }

    @Test
    void testCopyFrom_WithMissingSparkSpec() {
        // Given
        when(sparkApplicationResource.getSpec()).thenReturn(null);
        // When
        submissionSummary.copyFrom(sparkApplicationResource);
        // Then
        assertNull(submissionSummary.getSparkVersion());
        assertNull(submissionSummary.getApplicationArguments());
        assertNull(submissionSummary.getApplicationName());
    }

    @Test
    void testCopyFrom_WithMissingMetadata() {
        // Given
        when(sparkApplicationResource.getMetadata()).thenReturn(null);
        // When
        submissionSummary.copyFrom(sparkApplicationResource);
        // Then
        assertNull(submissionSummary.getSubmissionId());
    }

    @Test
    void testCopyFrom_WithInvalidExecutorMemory() {
        // Given
        SparkApplicationSpec spec = mock(SparkApplicationSpec.class);
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.PROXY_USER_LABEL, "testUser");
        labels.put(Constants.QUEUE_LABEL, "testQueue");
        when(sparkApplicationResource.getMetadata().getLabels()).thenReturn(labels);
        when(sparkApplicationResource.getSpec()).thenReturn(spec);
        when(spec.getDriver()).thenReturn(mock(DriverSpec.class));
        when(spec.getExecutor()).thenReturn(mock(ExecutorSpec.class));
        when(spec.getExecutor().getMemory()).thenReturn("invalidMemoryString");
        // When
        submissionSummary.copyFrom(sparkApplicationResource);
        // Then
        assertEquals("testUser", submissionSummary.getUser());
        assertEquals("testQueue", submissionSummary.getQueue());
        assertEquals(Long.valueOf(0), submissionSummary.getExecutorMemoryGB());
    }

    // Reflection-based test for private method
    @Test
    void testCopyFrom_privateReflection() throws Exception {
        Method method = SubmissionSummary.class.getDeclaredMethod("copyFrom", SparkApplication.class);
        method.setAccessible(true);
        // Invoke the method using reflection
        method.invoke(submissionSummary, sparkApplicationResource);
        // Further assertions after invoking using reflection as necessary
        assertNotNull(submissionSummary);
    }
}
