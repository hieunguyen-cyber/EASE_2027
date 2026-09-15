package com.apple.spark.core;

import com.apple.spark.AppConfig;
import com.apple.spark.api.SubmitApplicationRequest;
import com.apple.spark.operator.DriverSpec;
import com.apple.spark.operator.ExecutorSpec;
import com.apple.spark.operator.SparkApplicationSpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import com.apple.spark.operator.EnvVar;
import java.util.List;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.lang.reflect.Method;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class ApplicationSubmissionHelper_populateEnv_Test {

    private SparkApplicationSpec sparkSpec;

    private SubmitApplicationRequest request;

    private AppConfig.SparkCluster sparkCluster;

    @BeforeEach
    void setupBeforeEach() {
        // Initialize mocks and test objects before each test
        sparkSpec = new SparkApplicationSpec();
        request = mock(SubmitApplicationRequest.class);
        sparkCluster = mock(AppConfig.SparkCluster.class);
        // Setup default behavior for mock objects if necessary
        when(request.getDriver()).thenReturn(new DriverSpec());
        when(sparkCluster.getDriver()).thenReturn(new DriverSpec());
        when(sparkCluster.getExecutor()).thenReturn(new ExecutorSpec());
    }

    // Reflection method to access the focal method
    private void accessFocalMethod(String methodName, Object... args) throws Exception {
        Method method = ApplicationSubmissionHelper.class.getDeclaredMethod(methodName, SparkApplicationSpec.class, SubmitApplicationRequest.class, AppConfig.SparkCluster.class);
        method.setAccessible(true);
        method.invoke(null, args);
    }

    @Test
    void testPopulateEnv_WithDriverEnvFromSparkCluster() {
        // Given: an environment variable in the Spark Cluster's driver
        DriverSpec driverSpec = new DriverSpec();
        List<EnvVar> envVars = new ArrayList<>();
        envVars.add(new EnvVar("VAR1", "value1"));
        driverSpec.setEnv(envVars);
        when(sparkCluster.getDriver().getEnv()).thenReturn(driverSpec.getEnv());
        // When: populating the environment
        ApplicationSubmissionHelper.populateEnv(sparkSpec, request, sparkCluster);
        // Then: ensure the environment variable is set in the sparkSpec's driver
        assertNotNull(sparkSpec.getDriver());
        assertEquals(1, sparkSpec.getDriver().getEnv().size());
        assertEquals("VAR1", sparkSpec.getDriver().getEnv().get(0).getName());
    }

    @Test
    void testPopulateEnv_WithDriverEnvFromRequest() {
        // Given: an environment variable in the request's driver
        DriverSpec driverSpec = new DriverSpec();
        List<EnvVar> envVars = new ArrayList<>();
        envVars.add(new EnvVar("VAR2", "value2"));
        driverSpec.setEnv(envVars);
        when(request.getDriver().getEnv()).thenReturn(driverSpec.getEnv());
        // When: populating the environment
        ApplicationSubmissionHelper.populateEnv(sparkSpec, request, sparkCluster);
        // Then: ensure the environment variable is set in the sparkSpec's driver
        assertNotNull(sparkSpec.getDriver());
        assertEquals(1, sparkSpec.getDriver().getEnv().size());
        assertEquals("VAR2", sparkSpec.getDriver().getEnv().get(0).getName());
    }

    @Test
    void testPopulateEnv_WithExecutorEnvFromSparkCluster() {
        // Given: an environment variable in the Spark Cluster's executor
        ExecutorSpec executorSpec = new ExecutorSpec();
        List<EnvVar> envVars = new ArrayList<>();
        envVars.add(new EnvVar("EXEC_VAR", "exec_value"));
        executorSpec.setEnv(envVars);
        when(sparkCluster.getExecutor().getEnv()).thenReturn(executorSpec.getEnv());
        // When: populating the environment
        ApplicationSubmissionHelper.populateEnv(sparkSpec, request, sparkCluster);
        // Then: ensure the environment variable is set in the sparkSpec's executor
        assertNotNull(sparkSpec.getExecutor());
        assertEquals(1, sparkSpec.getExecutor().getEnv().size());
        assertEquals("EXEC_VAR", sparkSpec.getExecutor().getEnv().get(0).getName());
    }

    @Test
    void testPopulateEnv_WithExecutorEnvFromRequest() {
        // Given: an environment variable in the request's executor
        ExecutorSpec executorSpec = new ExecutorSpec();
        List<EnvVar> envVars = new ArrayList<>();
        envVars.add(new EnvVar("REQ_EXEC_VAR", "req_exec_value"));
        executorSpec.setEnv(envVars);
        when(request.getExecutor()).thenReturn(executorSpec);
        // When: populating the environment
        ApplicationSubmissionHelper.populateEnv(sparkSpec, request, sparkCluster);
        // Then: ensure the environment variable is set in the sparkSpec's executor
        assertNotNull(sparkSpec.getExecutor());
        assertEquals(1, sparkSpec.getExecutor().getEnv().size());
        assertEquals("REQ_EXEC_VAR", sparkSpec.getExecutor().getEnv().get(0).getName());
    }

    @Test
    void testPopulateEnv_NoEnvInSparkClusterOrRequest() {
        // Given: no environment variables in both Spark Cluster and request
        when(sparkCluster.getDriver().getEnv()).thenReturn(null);
        when(request.getDriver()).thenReturn(null);
        when(sparkCluster.getExecutor().getEnv()).thenReturn(null);
        when(request.getExecutor()).thenReturn(null);
        // When: populating the environment
        ApplicationSubmissionHelper.populateEnv(sparkSpec, request, sparkCluster);
        // Then: ensure that sparkSpec has no driver or executor
        assertNull(sparkSpec.getDriver());
        assertNull(sparkSpec.getExecutor());
    }

    @Test
    void testPopulateEnv_WithNullSparkSpec() {
        // Test for a null Spark Application Spec
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ApplicationSubmissionHelper.populateEnv(null, request, sparkCluster);
        });
        assertEquals("SparkApplicationSpec cannot be null", exception.getMessage());
    }

    @Test
    void testPopulateEnv_WithNullRequest() {
        // Test for a null SubmitApplicationRequest
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ApplicationSubmissionHelper.populateEnv(sparkSpec, null, sparkCluster);
        });
        assertEquals("SubmitApplicationRequest cannot be null", exception.getMessage());
    }

    @Test
    void testPopulateEnv_WithNullSparkCluster() {
        // Test for a null SparkCluster
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ApplicationSubmissionHelper.populateEnv(sparkSpec, request, null);
        });
        assertEquals("SparkCluster cannot be null", exception.getMessage());
    }

    @Test
    void testPopulateEnv_WithJsonProcessingException() throws Exception {
        // Simulate throwing JsonProcessingException from a method called within populateEnv
        when(request.getDriver()).thenThrow(new JsonMappingException("JSON mapping error"));
        Exception exception = assertThrows(JsonMappingException.class, () -> {
            accessFocalMethod("populateEnv", sparkSpec, request, sparkCluster);
        });
        assertEquals("JSON mapping error", exception.getMessage());
    }
}
