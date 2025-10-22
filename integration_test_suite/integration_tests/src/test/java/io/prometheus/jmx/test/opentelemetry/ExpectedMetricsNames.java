/*
 * Copyright (C) The Prometheus jmx_exporter Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.prometheus.jmx.test.opentelemetry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/** Class to implement ExpectedMetricsNames */
public class ExpectedMetricsNames {

    private static final Collection<String> METRIC_NAMES;

    static {
        Collection<String> metricNames = new ArrayList<>();

        metricNames.add("io_prometheus_jmx_autoIncrementing_Value");

        // These metric doesn't exist for Java 11+

        metricNames.add("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_avail");
        metricNames.add("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_pcent");
        metricNames.add("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_size");
        metricNames.add("io_prometheus_jmx_tabularData_Server_1_Disk_Usage_Table_used");
        metricNames.add("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_avail");
        metricNames.add("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_pcent");
        metricNames.add("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_size");
        metricNames.add("io_prometheus_jmx_tabularData_Server_2_Disk_Usage_Table_used");
        metricNames.add(
                "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_ActiveSessions");
        metricNames.add(
                "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_Bootstraps");
        metricNames.add(
                "io_prometheus_jmx_test_PerformanceMetricsMBean_PerformanceMetrics_BootstrapsDeferred");
        metricNames.add("java_lang_ClassLoading_LoadedClassCount");
        metricNames.add("java_lang_ClassLoading_TotalLoadedClassCount");
        metricNames.add("java_lang_ClassLoading_UnloadedClassCount");
        metricNames.add("java_lang_ClassLoading_Verbose");
        metricNames.add("java_lang_Compilation_CompilationTimeMonitoringSupported");
        metricNames.add("java_lang_Compilation_TotalCompilationTime");

        // These metrics don't exist for Java 11+

        /*
        metricNames.add("java_lang_GarbageCollector_CollectionCount");
        metricNames.add("java_lang_GarbageCollector_CollectionTime");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_GcThreadCount");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_duration");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_endTime");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_id");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_memoryUsageAfterGc_committed");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_memoryUsageAfterGc_init");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_memoryUsageAfterGc_max");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_memoryUsageAfterGc_used");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_memoryUsageBeforeGc_committed");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_memoryUsageBeforeGc_init");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_memoryUsageBeforeGc_max");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_memoryUsageBeforeGc_used");
        metricNames.add("java_lang_GarbageCollector_LastGcInfo_startTime");
        metricNames.add("java_lang_GarbageCollector_Valid");
        metricNames.add("java_lang_MemoryManager_Valid");
        metricNames.add("java_lang_MemoryPool_CollectionUsageThreshold");
        metricNames.add("java_lang_MemoryPool_CollectionUsageThresholdCount");
        metricNames.add("java_lang_MemoryPool_CollectionUsageThresholdExceeded");
        metricNames.add("java_lang_MemoryPool_CollectionUsageThresholdSupported");
        metricNames.add("java_lang_MemoryPool_CollectionUsage_committed");
        metricNames.add("java_lang_MemoryPool_CollectionUsage_init");
        metricNames.add("java_lang_MemoryPool_CollectionUsage_max");
        metricNames.add("java_lang_MemoryPool_CollectionUsage_used");
        metricNames.add("java_lang_MemoryPool_PeakUsage_committed");
        metricNames.add("java_lang_MemoryPool_PeakUsage_init");
        metricNames.add("java_lang_MemoryPool_PeakUsage_max");
        metricNames.add("java_lang_MemoryPool_PeakUsage_used");
        metricNames.add("java_lang_MemoryPool_UsageThreshold");
        metricNames.add("java_lang_MemoryPool_UsageThresholdCount");
        metricNames.add("java_lang_MemoryPool_UsageThresholdExceeded");
        metricNames.add("java_lang_MemoryPool_UsageThresholdSupported");
        metricNames.add("java_lang_MemoryPool_Usage_committed");
        metricNames.add("java_lang_MemoryPool_Usage_init");
        metricNames.add("java_lang_MemoryPool_Usage_max");
        metricNames.add("java_lang_MemoryPool_Usage_used");
        metricNames.add("java_lang_MemoryPool_Valid");
        metricNames.add("java_lang_Memory_HeapMemoryUsage_committed");
        metricNames.add("java_lang_Memory_HeapMemoryUsage_init");
        metricNames.add("java_lang_Memory_HeapMemoryUsage_max");
        metricNames.add("java_lang_Memory_HeapMemoryUsage_used");
        metricNames.add("java_lang_Memory_NonHeapMemoryUsage_committed");
        metricNames.add("java_lang_Memory_NonHeapMemoryUsage_init");
        metricNames.add("java_lang_Memory_NonHeapMemoryUsage_max");
        metricNames.add("java_lang_Memory_NonHeapMemoryUsage_used");
        metricNames.add("java_lang_Memory_ObjectPendingFinalizationCount");
        metricNames.add("java_lang_Memory_Verbose");
        */

        metricNames.add("java_lang_OperatingSystem_AvailableProcessors");
        metricNames.add("java_lang_OperatingSystem_CommittedVirtualMemorySize");
        metricNames.add("java_lang_OperatingSystem_FreePhysicalMemorySize");
        metricNames.add("java_lang_OperatingSystem_FreeSwapSpaceSize");
        metricNames.add("java_lang_OperatingSystem_MaxFileDescriptorCount");
        metricNames.add("java_lang_OperatingSystem_OpenFileDescriptorCount");
        metricNames.add("java_lang_OperatingSystem_ProcessCpuLoad");
        metricNames.add("java_lang_OperatingSystem_ProcessCpuTime");
        metricNames.add("java_lang_OperatingSystem_SystemCpuLoad");
        metricNames.add("java_lang_OperatingSystem_SystemLoadAverage");
        metricNames.add("java_lang_OperatingSystem_TotalPhysicalMemorySize");
        metricNames.add("java_lang_OperatingSystem_TotalSwapSpaceSize");
        metricNames.add("java_lang_Runtime_BootClassPathSupported");
        metricNames.add("java_lang_Runtime_StartTime");
        metricNames.add("java_lang_Runtime_Uptime");

        // These metrics don't exist for ibmjava

        /*metricNames.add("java_lang_Threading_CurrentThreadAllocatedBytes");
        metricNames.add("java_lang_Threading_CurrentThreadCpuTime");
        metricNames.add("java_lang_Threading_CurrentThreadCpuTimeSupported");
        metricNames.add("java_lang_Threading_CurrentThreadUserTime");
        metricNames.add("java_lang_Threading_DaemonThreadCount");
        metricNames.add("java_lang_Threading_ObjectMonitorUsageSupported");
        metricNames.add("java_lang_Threading_PeakThreadCount");
        metricNames.add("java_lang_Threading_SynchronizerUsageSupported");
        metricNames.add("java_lang_Threading_ThreadAllocatedMemoryEnabled");
        metricNames.add("java_lang_Threading_ThreadAllocatedMemorySupported");
        metricNames.add("java_lang_Threading_ThreadContentionMonitoringEnabled");
        metricNames.add("java_lang_Threading_ThreadContentionMonitoringSupported");
        metricNames.add("java_lang_Threading_ThreadCount");
        metricNames.add("java_lang_Threading_ThreadCpuTimeEnabled");
        metricNames.add("java_lang_Threading_ThreadCpuTimeSupported");
        metricNames.add("java_lang_Threading_TotalStartedThreadCount");
        */

        // These metrics don't exist for Java 11+

        /*
        metricNames.add("java_nio_BufferPool_Count");
        metricNames.add("java_nio_BufferPool_MemoryUsed");
        metricNames.add("java_nio_BufferPool_TotalCapacity");
        */

        metricNames.add("jmx_config_reload_failure_total");
        metricNames.add("jmx_config_reload_success_total");
        metricNames.add("jmx_exporter_build");
        metricNames.add("jmx_scrape_cached_beans");
        metricNames.add("jmx_scrape_duration_seconds");
        metricNames.add("jmx_scrape_error");
        metricNames.add("jvm_buffer_pool_capacity_bytes");
        metricNames.add("jvm_buffer_pool_used_buffers");
        metricNames.add("jvm_buffer_pool_used_bytes");
        metricNames.add("jvm_classes_currently_loaded");
        metricNames.add("jvm_classes_loaded_total");
        metricNames.add("jvm_classes_unloaded_total");
        metricNames.add("jvm_compilation_time_seconds_total");
        metricNames.add("jvm_gc_collection_seconds_count");
        metricNames.add("jvm_gc_collection_seconds_sum");
        metricNames.add("jvm_memory_committed_bytes");
        metricNames.add("jvm_memory_init_bytes");
        metricNames.add("jvm_memory_max_bytes");
        metricNames.add("jvm_memory_objects_pending_finalization");
        metricNames.add("jvm_memory_pool_allocated_bytes_total");
        metricNames.add("jvm_memory_pool_collection_committed_bytes");
        metricNames.add("jvm_memory_pool_collection_init_bytes");
        metricNames.add("jvm_memory_pool_collection_max_bytes");
        metricNames.add("jvm_memory_pool_collection_used_bytes");
        metricNames.add("jvm_memory_pool_committed_bytes");
        metricNames.add("jvm_memory_pool_init_bytes");
        metricNames.add("jvm_memory_pool_max_bytes");
        metricNames.add("jvm_memory_pool_used_bytes");
        metricNames.add("jvm_memory_used_bytes");
        metricNames.add("jvm_runtime");
        metricNames.add("jvm_threads_current");
        metricNames.add("jvm_threads_daemon");
        metricNames.add("jvm_threads_deadlocked");
        metricNames.add("jvm_threads_deadlocked_monitor");
        metricNames.add("jvm_threads_peak");
        metricNames.add("jvm_threads_started_total");
        metricNames.add("jvm_threads_state");
        metricNames.add("org_exist_management_exist_ProcessReport_RunningQueries_elapsedTime");
        metricNames.add("org_exist_management_exist_ProcessReport_RunningQueries_id");
        metricNames.add("org_exist_management_exist_ProcessReport_RunningQueries_startedAtTime");
        metricNames.add("process_cpu_seconds_total");
        metricNames.add("process_max_fds");
        metricNames.add("process_open_fds");
        metricNames.add("process_resident_memory_bytes");
        metricNames.add("process_start_time_seconds");
        metricNames.add("process_virtual_memory_bytes");

        METRIC_NAMES = Collections.unmodifiableCollection(metricNames);
    }

    /** Constructor */
    private ExpectedMetricsNames() {
        // INTENTIONALLY BLANK
    }

    /**
     * Method to get metrics names
     *
     * @return a List of metrics names
     */
    public static Collection<String> getMetricsNames() {
        return METRIC_NAMES;
    }
}
