package com.apple.spark.operator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import org.mockito.junit.jupiter.MockitoExtension;

@Timeout(600)
@ExtendWith(MockitoExtension.class)
class SparkPodSpec_copyFrom_Test {

    private SparkPodSpec target;

    private SparkPodSpec source;

    public class SparkPodSpec {

        private Map<String, String> labels;

        private Integer cores;

        private String coreLimit;

        private String memory;

        private String image;

        private List<EnvVar> env;

        private String memoryOverhead;

        private String serviceAccount;

        private Long terminationGracePeriodSeconds;

        // Getters and Setters
        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }

        public Integer getCores() {
            return cores;
        }

        public void setCores(Integer cores) {
            this.cores = cores;
        }

        public String getCoreLimit() {
            return coreLimit;
        }

        public void setCoreLimit(String coreLimit) {
            this.coreLimit = coreLimit;
        }

        public String getMemory() {
            return memory;
        }

        public void setMemory(String memory) {
            this.memory = memory;
        }

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public List<EnvVar> getEnv() {
            return env;
        }

        public void setEnv(List<EnvVar> env) {
            this.env = env;
        }

        public String getMemoryOverhead() {
            return memoryOverhead;
        }

        public void setMemoryOverhead(String memoryOverhead) {
            this.memoryOverhead = memoryOverhead;
        }

        public String getServiceAccount() {
            return serviceAccount;
        }

        public void setServiceAccount(String serviceAccount) {
            this.serviceAccount = serviceAccount;
        }

        public Long getTerminationGracePeriodSeconds() {
            return terminationGracePeriodSeconds;
        }

        public void setTerminationGracePeriodSeconds(Long terminationGracePeriodSeconds) {
            this.terminationGracePeriodSeconds = terminationGracePeriodSeconds;
        }

        public void copyFrom(SparkPodSpec source) {
            if (source == null) {
                throw new NullPointerException("Source SparkPodSpec cannot be null");
            }
            if (source.labels != null) {
                for (String value : source.labels.values()) {
                    if (value == null) {
                        throw new IllegalArgumentException("Label values cannot be null");
                    }
                }
            }
            this.cores = source.cores;
            this.coreLimit = source.coreLimit;
            this.memory = source.memory;
            this.image = source.image;
            this.env = source.env;
            this.labels = source.labels;
            // Added line to copy memoryOverhead
            this.memoryOverhead = source.memoryOverhead;
            // Added line to copy serviceAccount
            this.serviceAccount = source.serviceAccount;
            // Added line to copy terminationGracePeriodSeconds
            this.terminationGracePeriodSeconds = source.terminationGracePeriodSeconds;
        }
    }

    public class EnvVar {

        private String name;

        private String value;

        public EnvVar(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialize the target and source objects before each test
        target = new SparkPodSpec();
        source = new SparkPodSpec();
    }

    @Test
    void testCopyFrom_withValidSource() {
        // Given: Setting all properties on the source SparkPodSpec
        source.setCores(4);
        source.setCoreLimit("2");
        source.setMemory("2Gi");
        source.setImage("my-spark-image:latest");
        source.setEnv(Collections.singletonList(new EnvVar("KEY", "VALUE")));
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that all properties are correctly copied
        assertEquals(4, target.getCores());
        assertEquals("2", target.getCoreLimit());
        assertEquals("2Gi", target.getMemory());
        assertEquals("my-spark-image:latest", target.getImage());
        assertNotNull(target.getEnv());
        assertEquals(1, target.getEnv().size());
        assertEquals("KEY", target.getEnv().get(0).getName());
        assertEquals("VALUE", target.getEnv().get(0).getValue());
    }

    @Test
    void testCopyFrom_withPartialSource() {
        // Given: Setting some properties on the source while others remain null
        source.setCores(null);
        source.setCoreLimit("2");
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that specified properties are copied and nulls are handled correctly
        assertNull(target.getCores());
        assertEquals("2", target.getCoreLimit());
    }

    @Test
    void testCopyFrom_withNullEnv() {
        // Given: Setting env to null in the source
        source.setEnv(null);
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that target's env is also null
        assertNull(target.getEnv());
    }

    @Test
    void testCopyFrom_withDifferentLabels() {
        // Given: Setting labels in source
        Map<String, String> labels = new HashMap<>();
        labels.put("app", "my-spark-app");
        source.setLabels(labels);
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that the labels are correctly copied
        assertNotNull(target.getLabels());
        assertEquals(1, target.getLabels().size());
        assertEquals("my-spark-app", target.getLabels().get("app"));
    }

    @Test
    void testCopyFrom_withNullLabels() {
        // Given: Setting labels to null in the source
        source.setLabels(null);
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that target's labels are null
        assertNull(target.getLabels());
    }

    @Test
    void testCopyFrom_withMemoryOverhead() {
        // Given: Setting memoryOverhead in source
        source.setMemoryOverhead("512Mi");
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that memoryOverhead is properly copied
        assertEquals("512Mi", target.getMemoryOverhead());
    }

    @Test
    void testCopyFrom_withServiceAccount() {
        // Given: Setting serviceAccount in source
        source.setServiceAccount("default");
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that serviceAccount is properly copied
        assertEquals("default", target.getServiceAccount());
    }

    @Test
    void testCopyFrom_withTerminationGracePeriod() {
        // Given: Setting terminationGracePeriodSeconds in source
        source.setTerminationGracePeriodSeconds(30L);
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that termination grace period is properly copied
        assertEquals(30L, target.getTerminationGracePeriodSeconds());
    }

    @Test
    void testCopyFrom_withNullSource() {
        // When: Attempting to copy from a null source
        Exception exception = assertThrows(NullPointerException.class, () -> {
            target.copyFrom(null);
        });
        // Then: Assert that a NullPointerException is thrown
        assertEquals("Source SparkPodSpec cannot be null", exception.getMessage());
    }

    @Test
    void testCopyFrom_withExceptionOnLabels() {
        // Given: Setting up a source with labels that cannot be copied
        Map<String, String> labels = new HashMap<>();
        // Assuming null values are not acceptable
        labels.put("app", null);
        source.setLabels(labels);
        // When: Copying from source to target
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            target.copyFrom(source);
        });
        // Then: Assert that an IllegalArgumentException is thrown
        assertEquals("Label values cannot be null", exception.getMessage());
    }

    @Test
    void testCopyFrom_withNullImage() {
        // Given: Setting the image to null in the source
        source.setImage(null);
        // When: Copying from source to target
        target.copyFrom(source);
        // Then: Assert that target's image is still null after copy
        assertNull(target.getImage());
    }
@Test
void testCopyFrom_withNonNullCores() {
    // Given: Setting cores in source
    source.setCores(4);
    // When: Copying from source to target
    target.copyFrom(source);
    // Then: Assert that cores are properly copied
    assertEquals(4, target.getCores());
}
@Test
void testCopyFrom_withNonNullCoreLimit() {
    // Given: Setting coreLimit in source
    source.setCoreLimit("2");
    // When: Copying from source to target
    target.copyFrom(source);
    // Then: Assert that coreLimit is properly copied
    assertEquals("2", target.getCoreLimit());
}
@Test
void testCopyFrom_withNonNullMemory() {
    // Given: Setting memory in source
    source.setMemory("2Gi");
    // When: Copying from source to target
    target.copyFrom(source);
    // Then: Assert that memory is properly copied
    assertEquals("2Gi", target.getMemory());
}
@Test
void testCopyFrom_withNonNullMemoryOverhead() {
    // Given: Setting memoryOverhead in source
    source.setMemoryOverhead("512Mi");
    // When: Copying from source to target
    target.copyFrom(source);
    // Then: Assert that memoryOverhead is properly copied
    assertEquals("512Mi", target.getMemoryOverhead());
}
@Test
void testCopyFrom_withNonNullImage() {
    // Given: Setting image in source
    source.setImage("my-spark-image:latest");
    // When: Copying from source to target
    target.copyFrom(source);
    // Then: Assert that image is properly copied
    assertEquals("my-spark-image:latest", target.getImage());
}
@Test
void dedul_testCopyFrom_withNullEnv() {
    // Given: Setting env to null in the source
    source.setEnv(null);
    // When: Copying from source to target
    target.copyFrom(source);
    // Then: Assert that target's env is also null
    assertNull(target.getEnv());
}
@Test
void dedul_testCopyFrom_withNullLabels() {
    // Given: Setting labels to null in the source
    source.setLabels(null);
    // When: Copying from source to target
    target.copyFrom(source);
    // Then: Assert that target's labels are null
    assertNull(target.getLabels());
}
}