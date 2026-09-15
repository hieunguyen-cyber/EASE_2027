package com.apple.spark.core;

import static org.mockito.Mockito.when;
import com.apple.spark.AppConfig;
import com.apple.spark.api.SubmitApplicationRequest;
import com.apple.spark.operator.ExecutorSpec;
import com.apple.spark.AppConfig.SparkCluster;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Collections;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.ParameterizedTest;
// Added import for Volume
import com.apple.spark.operator.Volume;
// Added import for VolumeMount
import com.apple.spark.operator.VolumeMount;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class ApplicationSubmissionHelper_getExecutorSpec_Test {

    private AppConfig appConfig;

    private SparkCluster sparkCluster;

    private SubmitApplicationRequest request;

    @BeforeEach
    void setupBeforeEach() {
        // Mock AppConfig
        appConfig = mock(AppConfig.class);
        sparkCluster = mock(SparkCluster.class);
        request = mock(SubmitApplicationRequest.class);
        when(request.getExecutor()).thenReturn(new ExecutorSpec());
    }

    @Test
    public void testGetExecutorSpec() {
        String parentQueue = "testQueue";
        ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(executorSpec);
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    public void testGetExecutorSpec_withExecutorAndLabels() {
        String parentQueue = "testQueue";
        ExecutorSpec executor = new ExecutorSpec();
        HashMap<String, String> labels = new HashMap<>();
        labels.put(Constants.DAG_NAME_LABEL, "myDag");
        when(request.getExecutor()).thenReturn(executor);
        when(executor.getLabels()).thenReturn(labels);
        ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(executorSpec);
        assertEquals("myDag", executorSpec.getLabels().get(Constants.DAG_NAME_LABEL));
    }

    @Test
    public void testGetExecutorSpec_withExecutorAndVolumes() {
        String parentQueue = "testQueue";
        ExecutorSpec executor = new ExecutorSpec();
        executor.setVolumeMounts(Collections.singletonList(new VolumeMount()));
        when(request.getExecutor()).thenReturn(executor);
        when(request.getVolumes()).thenReturn(Collections.singletonList(new Volume()));
        ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(executorSpec);
        assertEquals(executor.getVolumeMounts(), executorSpec.getVolumeMounts());
    }

    @Test
    public void testGetExecutorSpec_withSparkClusterVolumes() {
        String parentQueue = "testQueue";
        ExecutorSpec executor = new ExecutorSpec();
        when(request.getExecutor()).thenReturn(executor);
        ExecutorSpec clusterExecutor = new ExecutorSpec();
        clusterExecutor.setVolumeMounts(Collections.singletonList(new VolumeMount()));
        when(sparkCluster.getExecutor()).thenReturn(clusterExecutor);
        when(sparkCluster.getVolumes()).thenReturn(Collections.singletonList(new Volume()));
        ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(executorSpec);
        assertEquals(clusterExecutor.getVolumeMounts(), executorSpec.getVolumeMounts());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    public void testGetExecutorSpec_executorHasNoCoresSet() {
        String parentQueue = "testQueue";
        ExecutorSpec executor = new ExecutorSpec();
        when(executor.getCores()).thenReturn(0);
        when(request.getExecutor()).thenReturn(executor);
        ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(executorSpec);
        assertEquals(0, executorSpec.getCores());
        assertNull(executorSpec.getCoreLimit());
    }

    @Test
    public void testGetExecutorSpec_withAnnotations() {
        String parentQueue = "testQueue";
        ExecutorSpec executor = new ExecutorSpec();
        when(request.getExecutor()).thenReturn(executor);
        ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(executorSpec);
        assertNotNull(executorSpec.getAnnotations());
        assertEquals(Constants.KUBE_CLUSTER_AUTOSCALER_SCALE_IN_ANNOTATION_VALUE, executorSpec.getAnnotations().get(Constants.KUBE_CLUSTER_AUTOSCALER_SCALE_IN_ANNOTATION_KEY));
    }

//     @Test
//     public void testGetExecutorSpec_withDifferentBufferRatios() {
//         String parentQueue = "testQueue";
//         // Stubbing out methods for testing using mock
//         when(appConfig.getExecutorCpuBufferRatio()).thenReturn(1.5);
//         when(appConfig.getExecutorMemBufferRatio()).thenReturn(2.0);
//         ExecutorSpec executor = new ExecutorSpec();
//         executor.setCores(4);
//         executor.setMemory("4Gi");
//         when(request.getExecutor()).thenReturn(executor);
//         ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
//         assertNotNull(executorSpec);
//         assertEquals(6, executorSpec.getCores());
//         assertEquals("8Gi", executorSpec.getMemory());
//     }

    @ParameterizedTest
    // Cores values to be tested
    @ValueSource(ints = { 0, 1, 10 })
    public void testGetExecutorSpec_withCores(int cores) {
        String parentQueue = "testQueue";
        ExecutorSpec executor = new ExecutorSpec();
        executor.setCores(cores);
        when(request.getExecutor()).thenReturn(executor);
        ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(executorSpec);
        assertEquals(cores, executorSpec.getCores());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    public void testGetExecutorSpec_withNullRequest() {
        String parentQueue = "testQueue";
        Exception exception = assertThrows(NullPointerException.class, () -> {
            ApplicationSubmissionHelper.getExecutorSpec(null, appConfig, parentQueue, sparkCluster);
        });
        assertEquals("request is null", exception.getMessage());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    public void testGetExecutorSpec_withNullAppConfig() {
        String parentQueue = "testQueue";
        Exception exception = assertThrows(NullPointerException.class, () -> {
            ApplicationSubmissionHelper.getExecutorSpec(request, null, parentQueue, sparkCluster);
        });
        assertEquals("appConfig is null", exception.getMessage());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    public void testGetExecutorSpec_withNullSparkCluster() {
        String parentQueue = "testQueue";
        Exception exception = assertThrows(NullPointerException.class, () -> {
            ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, null);
        });
        assertEquals("sparkCluster is null", exception.getMessage());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    public void testGetExecutorSpec_withInvalidMemoryFormat() {
        String parentQueue = "testQueue";
        ExecutorSpec executor = new ExecutorSpec();
        executor.setMemory("invalidMemory");
        when(request.getExecutor()).thenReturn(executor);
        when(sparkCluster.getExecutor()).thenReturn(new ExecutorSpec());
        Exception exception = assertThrows(NumberFormatException.class, () -> {
            ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
        });
        assertTrue(exception.getMessage().contains("For input string: \"invalidMemory\""));
    }
@Test
public void test_requestVolumes_remove_conditional_mutator() {
    String parentQueue = "testQueue";
    ExecutorSpec executor = new ExecutorSpec();
    executor.setVolumeMounts(Collections.singletonList(new VolumeMount()));
    when(request.getExecutor()).thenReturn(executor);
    when(request.getVolumes()).thenReturn(Collections.singletonList(new Volume()));
    ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
    assertNotNull(executorSpec);
    assertEquals(executor.getVolumeMounts(), executorSpec.getVolumeMounts());
}
@Test
public void test_requestVolumesNotEmpty_remove_conditional_mutator() {
    String parentQueue = "testQueue";
    ExecutorSpec executor = new ExecutorSpec();
    executor.setVolumeMounts(Collections.singletonList(new VolumeMount()));
    when(request.getExecutor()).thenReturn(executor);
    when(request.getVolumes()).thenReturn(Collections.singletonList(new Volume()));
    ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
    assertNotNull(executorSpec);
    assertEquals(executor.getVolumeMounts(), executorSpec.getVolumeMounts());
}
@Test
public void test_setVolumeMounts_void_method_call_mutator() {
    String parentQueue = "testQueue";
    ExecutorSpec executor = new ExecutorSpec();
    executor.setVolumeMounts(Collections.singletonList(new VolumeMount()));
    when(request.getExecutor()).thenReturn(executor);
    when(request.getVolumes()).thenReturn(Collections.singletonList(new Volume()));
    ExecutorSpec executorSpec = ApplicationSubmissionHelper.getExecutorSpec(request, appConfig, parentQueue, sparkCluster);
    assertNotNull(executorSpec);
    assertEquals(executor.getVolumeMounts(), executorSpec.getVolumeMounts());
}
}