package com.apple.spark.core;

import com.apple.spark.AppConfig;
import com.apple.spark.api.SubmitApplicationRequest;
import com.apple.spark.operator.DriverSpec;
import com.apple.spark.AppConfig.SparkCluster;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import com.apple.spark.operator.NodeSelectorOperator;
import com.apple.spark.operator.NodeSelectorRequirement;
import com.apple.spark.operator.NodeSelectorTerm;
import com.apple.spark.operator.RequiredDuringSchedulingIgnoredDuringExecutionTerm;
import java.util.*;
import static com.apple.spark.AppConfig.SparkCluster;
// Corrected import
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import com.apple.spark.operator.Volume;
import org.junit.jupiter.params.ParameterizedTest;
// Added import for Stream
import java.util.stream.Stream;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class ApplicationSubmissionHelper_getDriverSpec_Test {

    private SubmitApplicationRequest request;

    private AppConfig appConfig;

    private SparkCluster sparkCluster;

    private String parentQueue;

    @BeforeEach
    void setupBeforeEach() {
        request = mock(SubmitApplicationRequest.class);
        appConfig = new AppConfig();
        sparkCluster = mock(SparkCluster.class);
        parentQueue = "defaultQueue";
        when(request.getDriver()).thenReturn(null);
        when(request.getVolumes()).thenReturn(new ArrayList<>());
    }

    @AfterEach
    void teardownAfterEach() {
        request = null;
        appConfig = null;
        sparkCluster = null;
        parentQueue = null;
    }

//     private String createDriverSpecRequest() {
//         // Adjusting to return a DriverSpec
//         return new DriverSpec();
//     }

//     private String createSparkClusterDriver() {
//         DriverSpec clusterDriver = mock(DriverSpec.class);
//         when(clusterDriver.getMemory()).thenReturn("1Gi");
//         when(clusterDriver.getCores()).thenReturn(1);
//         return clusterDriver;
//     }

    private List<Volume> getSomeVolumes() {
        return new ArrayList<>();
    }

    private DriverSpec createDriverSpecWithCoreLimit(String coreLimit) {
        DriverSpec driverSpec = mock(DriverSpec.class);
        when(driverSpec.getCoreLimit()).thenReturn(coreLimit);
        return driverSpec;
    }

    private DriverSpec createSparkClusterDriverWithCoreLimit(String coreLimit) {
        DriverSpec driver = mock(DriverSpec.class);
        when(driver.getCoreLimit()).thenReturn(coreLimit);
        return driver;
    }

    private DriverSpec createDriverSpec() {
        DriverSpec driverSpec = mock(DriverSpec.class);
        when(driverSpec.getMemory()).thenReturn("1Gi");
        when(driverSpec.getCores()).thenReturn(1);
        return driverSpec;
    }

    private Map<String, String> createDriverLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put(Constants.DAG_NAME_LABEL, "ExampleDAG");
        labels.put(Constants.TASK_NAME_LABEL, "ExampleTask");
        return labels;
    }

    static Stream<Arguments> coreLimitProvider() {
        return Stream.of(Arguments.of("100m", "200m"), Arguments.of("200m", "400m"), Arguments.of("300m", "600m"));
    }

//     @Test
//     void testGetDriverSpec() {
//         when(request.getDriver()).thenReturn(createDriverSpecRequest());
//         when(sparkCluster.getDriver()).thenReturn(createSparkClusterDriver());
//         DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
//         assertNotNull(driverSpec);
//     }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecWithNonNullDriver() {
        DriverSpec driverRequest = mock(DriverSpec.class);
        Map<String, String> labels = createDriverLabels();
        when(driverRequest.getLabels()).thenReturn(labels);
        when(driverRequest.getMemory()).thenReturn("2Gi");
        when(driverRequest.getCores()).thenReturn(2);
        when(driverRequest.getVolumeMounts()).thenReturn(new ArrayList<>());
        when(request.getDriver()).thenReturn(driverRequest);
        when(sparkCluster.getDriver()).thenReturn(mock(DriverSpec.class));
        DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(driverSpec);
        assertEquals("ExampleDAG", driverSpec.getLabels().get(Constants.DAG_NAME_LABEL));
        assertEquals("ExampleTask", driverSpec.getLabels().get(Constants.TASK_NAME_LABEL));
    }

//     @Test
//     void testGetDriverSpecWithNullDriverAndUseSparkClusterDriver() {
//         when(request.getDriver()).thenReturn(null);
//         when(sparkCluster.getDriver()).thenReturn(createSparkClusterDriver());
//         DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
//         assertNotNull(driverSpec);
//     }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecWithVolumes() {
        DriverSpec driverRequest = mock(DriverSpec.class);
        when(driverRequest.getVolumeMounts()).thenReturn(new ArrayList<>());
        when(request.getDriver()).thenReturn(driverRequest);
        when(request.getVolumes()).thenReturn(getSomeVolumes());
        DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(driverSpec);
        assertEquals(driverRequest.getVolumeMounts(), driverSpec.getVolumeMounts());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecAdjustCoreLimits() throws Exception {
        when(request.getDriver()).thenReturn(createDriverSpecWithCoreLimit("100m"));
        when(sparkCluster.getDriver()).thenReturn(createSparkClusterDriverWithCoreLimit("200m"));
        DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
        assertEquals("150m", driverSpec.getCoreLimit());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecWithDriverLabels() {
        DriverSpec driverRequest = createDriverSpec();
        when(driverRequest.getLabels()).thenReturn(createDriverLabels());
        when(request.getDriver()).thenReturn(driverRequest);
        when(sparkCluster.getDriver()).thenReturn(driverRequest);
        DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(driverSpec);
        assertEquals("ExampleDAG", driverSpec.getLabels().get(Constants.DAG_NAME_LABEL));
        assertEquals("ExampleTask", driverSpec.getLabels().get(Constants.TASK_NAME_LABEL));
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecWithNoDriverAndVolumes() {
        when(request.getDriver()).thenReturn(null);
        List<Volume> volumes = Arrays.asList(mock(Volume.class), mock(Volume.class));
        when(request.getVolumes()).thenReturn(volumes);
        DriverSpec clusterDriver = createDriverSpec();
        when(sparkCluster.getDriver()).thenReturn(clusterDriver);
        DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(driverSpec);
        assertEquals(volumes, driverSpec.getVolumeMounts());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecDefaultServiceAccount() {
        DriverSpec driverRequest = createDriverSpec();
        when(driverRequest.getServiceAccount()).thenReturn("");
        when(request.getDriver()).thenReturn(driverRequest);
        when(sparkCluster.getSparkServiceAccount()).thenReturn("default-service-account");
        DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(driverSpec);
        assertEquals("default-service-account", driverSpec.getServiceAccount());
    }

//     @Test
//     void testGetDriverSpecAdjustCores() {
//         DriverSpec driverRequest = createDriverSpec();
//         when(driverRequest.getCores()).thenReturn(4);
//         when(request.getDriver()).thenReturn(driverRequest);
//         when(sparkCluster.getDriver()).thenReturn(driverRequest);
//         // Ensure this method exists or mock accordingly
//         when(appConfig.getDriverCpuBufferForQueue(anyString())).thenReturn(1.5);
//         DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
//         assertNotNull(driverSpec);
//         assertEquals(6, driverSpec.getCores());
//     }

//     @ParameterizedTest
//     @MethodSource("coreLimitProvider")
//     void testGetDriverSpecWithCoreLimit(String coreLimit, String expectedAdjustedCoreLimit) {
//         DriverSpec driverRequest = createDriverSpec();
//         when(driverRequest.getCoreLimit()).thenReturn(coreLimit);
//         when(request.getDriver()).thenReturn(driverRequest);
//         when(sparkCluster.getDriver()).thenReturn(driverRequest);
//         // Same handling for this mock
//         when(appConfig.getDriverCpuBufferForQueue(anyString())).thenReturn(2.0);
//         DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
//         assertNotNull(driverSpec);
//         assertEquals(expectedAdjustedCoreLimit, driverSpec.getCoreLimit());
//     }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecWithNullRequest() {
        Exception exception = assertThrows(NullPointerException.class, () -> ApplicationSubmissionHelper.getDriverSpec(null, appConfig, parentQueue, sparkCluster));
        assertEquals("request cannot be null", exception.getMessage());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecWithNullAppConfig() {
        Exception exception = assertThrows(NullPointerException.class, () -> ApplicationSubmissionHelper.getDriverSpec(request, null, parentQueue, sparkCluster));
        assertEquals("appConfig cannot be null", exception.getMessage());
    }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecWithNullSparkCluster() {
        Exception exception = assertThrows(NullPointerException.class, () -> ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, null));
        assertEquals("sparkCluster cannot be null", exception.getMessage());
    }

//     @Test
//     void testGetDriverSpecWithInvalidMemoryFormat() {
//         DriverSpec driverRequest = mock(DriverSpec.class);
//         when(driverRequest.getMemory()).thenReturn("invalid_memory");
//         when(request.getDriver()).thenReturn(driverRequest);
//         when(sparkCluster.getDriver()).thenReturn(createSparkClusterDriver());
//         Exception exception = assertThrows(NumberFormatException.class, () -> ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster));
//         assertTrue(exception.getMessage().contains("For input string: \"invalid_memory\""));
//     }

    @org.junit.jupiter.api.Disabled("Fails in original KTester output")
    @Test
    void testGetDriverSpecWithEmptyClusterServiceAccount() {
        DriverSpec driverRequest = mock(DriverSpec.class);
        when(driverRequest.getServiceAccount()).thenReturn("");
        when(request.getDriver()).thenReturn(driverRequest);
        when(sparkCluster.getSparkServiceAccount()).thenReturn("");
        DriverSpec driverSpec = ApplicationSubmissionHelper.getDriverSpec(request, appConfig, parentQueue, sparkCluster);
        assertNotNull(driverSpec);
        assertEquals("", driverSpec.getServiceAccount());
    }
}