#!/usr/bin/env python2.7
"""
Integration test of the jmmix webserver.

Runs a sample Java app and jmmix webserver then tests some sample beans
in the resulting JSON.
"""

import json
import os
import signal
import subprocess
import time
import unittest
import urllib


def setUpModule():
    dtry = os.path.abspath(os.path.dirname(__file__) + '/../')
    global MBEAN_SERVER
    MBEAN_SERVER = subprocess.Popen(
            ['bash', dtry + '/run_sample_server.sh'], preexec_fn=os.setsid)
    global JMMIX_SERVER
    JMMIX_SERVER = subprocess.Popen(
            ['bash', dtry + '/run_jmmix_server.sh'], preexec_fn=os.setsid)
    time.sleep(1)


def tearDownModule():
    os.killpg(MBEAN_SERVER.pid, signal.SIGTERM)
    os.killpg(JMMIX_SERVER.pid, signal.SIGTERM)
    MBEAN_SERVER.wait()
    JMMIX_SERVER.wait()


class JimmxE2ETest(unittest.TestCase):

    def test_basic_json(self):
        self.assertTrue(self.make_json_request())

    def test_jvm_bean(self):
        beans = self.make_json_request()

        self.assertIn('java_lang_MemoryPool_PeakUsage_init', beans.keys())
        bean = beans['java_lang_MemoryPool_PeakUsage_init']
        self.assertBeanEquals(bean,
                        'java_lang_MemoryPool_PeakUsage_init',
                        'java.lang.management.MemoryUsage',
                        {'nom': 'PS Survivor Space'})
        self.assertTrue(self.beanValue(bean) > 0, bean)

    def test_cassandra_bean(self):
        beans = self.make_json_request()
        self.assertIn('org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount', beans.keys())
        bean = beans['org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount']
        self.assertBeanEquals(bean,
                        'org_apache_cassandra_concurrent_CONSISTENCY_MANAGER_ActiveCount',
                        'Attribute exposed for management'
                        )
        self.assertEquals(100, self.beanValue(bean))

    def test_cassandra_metrics(self):
        beans = self.make_json_request()
        self.assertIn('org_apache_cassandra_metrics_Compaction_Value', beans.keys())
        bean = beans['org_apache_cassandra_metrics_Compaction_Value']
        self.assertBeanEquals(bean,
                        'org_apache_cassandra_metrics_Compaction_Value',
                        'Attribute exposed for management',
                        {u'nom': u'CompletedTasks'}
                        )
        self.assertEquals(0.2, self.beanValue(bean))

    def test_hadoop_bean(self):
        beans = self.make_json_request()
        bean = beans['hadoop_DataNode_replaceBlockOpMinTime']
        self.assertBeanEquals(bean,
                        'hadoop_DataNode_replaceBlockOpMinTime',
                        'Attribute exposed for management',
                        {'nom': 'DataNodeActivity-ams-hdd001-50010', 'service': 'DataNode'}
                        )
        self.assertEquals(200, self.beanValue(bean))

    def test_tabular_bean(self):
        beans = self.make_json_request()
        self.assertIn('java_lang_GarbageCollector_LastGcInfo_memoryUsageAfterGc_committed', beans.keys())
        bean = beans['java_lang_GarbageCollector_LastGcInfo_memoryUsageAfterGc_committed']
        bean = beans['java_lang_GarbageCollector_LastGcInfo_memoryUsageAfterGc_committed']
        self.assertBeanEquals(bean,
                        'java_lang_GarbageCollector_LastGcInfo_memoryUsageAfterGc_committed',
                        'java.lang.management.MemoryUsage',
                        {'key': 'PS Survivor Space', 'nom': 'PS Scavenge'}
                        )
        self.assertTrue(self.beanValue(bean) > 0)

    def test_error_message(self):
        f = urllib.urlopen('http://localhost:5556?target=localhost:10101')
        t = f.read()
        self.assertTrue(len(t) > 0)
        o = json.loads(t)
        self.assertTrue(len(o) > 0)
        self.assertIn('error', o[0])

    def make_json_request(self):
        f = urllib.urlopen('http://localhost:5556')
        t = f.read()
        self.assertTrue(len(t) > 0)

        o = json.loads(t)
        self.assertTrue(len(o) > 0)
        # we can acutally have duplicate names, but that's ok
        # for the test if we sort
        o.sort()
        beans = dict(self.convertBean(bean) for bean in o)
        return beans

    def assertBeanEquals(self, bean, name, docstring, optLabels=None):
        self.assertEqual(len(bean.keys()), 3)
        self.assertEqual(bean['docstring'], docstring)
        self.assertEqual(len(bean['baseLabels']), 1)
        self.assertEqual(bean['baseLabels']['name'], name)
        self.assertEqual(len(bean['metric']), 2)
        self.assertEqual(bean['metric']['type'], 'gauge')
        self.assertEqual(len(bean['metric']['value']), 1)
        if optLabels:
            self.assertEqual(bean['metric']['value'][0]['labels'], optLabels)
            self.assertEqual(len(bean['metric']['value'][0]), 2)
        else:
            self.assertEqual(len(bean['metric']['value'][0]), 1, bean)

    def beanValue(self, bean):
        return bean['metric']['value'][0]['value']

    def convertBean(self, bean):
        return bean['baseLabels']['name'], bean



if __name__ == '__main__':
    try:
        unittest.main()
    except KeyboardInterrupt:
        # I feel like this should be handled by unittest library.
        tearDownModule()
        raise
