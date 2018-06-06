package io.prometheus.jmx;

import io.prometheus.client.CollectorRegistry;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BuildInfoCollectorTest {

  private CollectorRegistry registry = new CollectorRegistry();

  @Before
  public void setUp() {
    new BuildInfoCollector().register(registry);
  }

  @Test
  public void testBuildInfo() {
    String version = this.getClass().getPackage().getImplementationVersion();

    assertEquals(
            1L,
            registry.getSampleValue(
                    "jmx_exporter_build_info", new String[]{"version"}, new String[]{
                            version != null ? version : "unknown"
                    }),
            .0000001);
  }
}
