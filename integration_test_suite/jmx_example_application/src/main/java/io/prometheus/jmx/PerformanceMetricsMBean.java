package io.prometheus.jmx;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

public interface PerformanceMetricsMBean {

    CompositeData getPerformanceMetrics() throws OpenDataException;
}

class PerformanceMetrics implements PerformanceMetricsMBean {

    private final CompositeData compositeData;

    public PerformanceMetrics() throws OpenDataException {
        compositeData = build();
    }

    public CompositeData getPerformanceMetrics() {
        return compositeData;
    }

    private CompositeData build() throws OpenDataException {
        Map<String, Number> data = new LinkedHashMap<>();

        data.put("ActiveSessions", 2L);
        data.put("Bootstraps", 4L);
        data.put("BootstrapsDeferred", 6L);

        String[] names = {"ActiveSessions", "Bootstraps", "BootstrapsDeferred"};
        String[] descriptions = {"ActiveSessions", "Bootstraps", "BootstrapsDeferred"};
        OpenType[] types = {SimpleType.LONG, SimpleType.LONG, SimpleType.LONG};

        CompositeType compositeType =
                new CompositeType(
                        "PerformanceMetrics", "Example composite data", names, descriptions, types);
        return new CompositeDataSupport(compositeType, data);
    }
}
