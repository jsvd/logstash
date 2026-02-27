/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.logstash.api;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class NodeInfoTest {

    // --- OsInfo tests ---

    @Test
    public void testOsInfoConstruction() {
        final NodeInfo.OsInfo os = new NodeInfo.OsInfo("Linux", "amd64", "5.4.0", 8);
        assertEquals("Linux", os.getName());
        assertEquals("amd64", os.getArch());
        assertEquals("5.4.0", os.getVersion());
        assertEquals(8, os.getAvailableProcessors());
    }

    @Test
    public void testOsInfoFrom() {
        final NodeInfo.OsInfo os = NodeInfo.OsInfo.from("Mac OS X", "aarch64", "14.0", 10);
        assertEquals("Mac OS X", os.getName());
        assertEquals("aarch64", os.getArch());
        assertEquals("14.0", os.getVersion());
        assertEquals(10, os.getAvailableProcessors());
    }

    @Test
    public void testOsInfoToMap() {
        final NodeInfo.OsInfo os = new NodeInfo.OsInfo("Linux", "amd64", "5.4.0", 8);
        final Map<String, Object> map = os.toMap();
        assertEquals("Linux", map.get("name"));
        assertEquals("amd64", map.get("arch"));
        assertEquals("5.4.0", map.get("version"));
        assertEquals(8, map.get("available_processors"));
        assertEquals(4, map.size());
    }

    @Test
    public void testOsInfoEquality() {
        final NodeInfo.OsInfo os1 = new NodeInfo.OsInfo("Linux", "amd64", "5.4.0", 8);
        final NodeInfo.OsInfo os2 = new NodeInfo.OsInfo("Linux", "amd64", "5.4.0", 8);
        assertEquals(os1, os2);
        assertEquals(os1.hashCode(), os2.hashCode());
    }

    @Test
    public void testOsInfoInequality() {
        final NodeInfo.OsInfo os1 = new NodeInfo.OsInfo("Linux", "amd64", "5.4.0", 8);
        final NodeInfo.OsInfo os2 = new NodeInfo.OsInfo("Mac OS X", "aarch64", "14.0", 10);
        assertNotEquals(os1, os2);
    }

    // --- JvmMemInfo tests ---

    @Test
    public void testJvmMemInfoConstruction() {
        final NodeInfo.JvmMemInfo mem = new NodeInfo.JvmMemInfo(268435456L, 4294967296L, 2555904L, -1L);
        assertEquals(268435456L, mem.getHeapInitBytes());
        assertEquals(4294967296L, mem.getHeapMaxBytes());
        assertEquals(2555904L, mem.getNonHeapInitBytes());
        assertEquals(-1L, mem.getNonHeapMaxBytes());
    }

    @Test
    public void testJvmMemInfoFrom() {
        final NodeInfo.JvmMemInfo mem = NodeInfo.JvmMemInfo.from(256L, 1024L, 64L, 128L);
        assertEquals(256L, mem.getHeapInitBytes());
        assertEquals(1024L, mem.getHeapMaxBytes());
        assertEquals(64L, mem.getNonHeapInitBytes());
        assertEquals(128L, mem.getNonHeapMaxBytes());
    }

    @Test
    public void testJvmMemInfoToMap() {
        final NodeInfo.JvmMemInfo mem = new NodeInfo.JvmMemInfo(256L, 1024L, 64L, 128L);
        final Map<String, Object> map = mem.toMap();
        assertEquals(256L, map.get("heap_init_in_bytes"));
        assertEquals(1024L, map.get("heap_max_in_bytes"));
        assertEquals(64L, map.get("non_heap_init_in_bytes"));
        assertEquals(128L, map.get("non_heap_max_in_bytes"));
        assertEquals(4, map.size());
    }

    @Test
    public void testJvmMemInfoEquality() {
        final NodeInfo.JvmMemInfo m1 = new NodeInfo.JvmMemInfo(256L, 1024L, 64L, 128L);
        final NodeInfo.JvmMemInfo m2 = new NodeInfo.JvmMemInfo(256L, 1024L, 64L, 128L);
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    // --- JvmInfo tests ---

    @Test
    public void testJvmInfoBuilder() {
        final NodeInfo.JvmMemInfo mem = new NodeInfo.JvmMemInfo(256L, 1024L, 64L, 128L);
        final List<String> gcCollectors = Arrays.asList("G1 Young Generation", "G1 Old Generation");

        final NodeInfo.JvmInfo jvm = NodeInfo.JvmInfo.builder()
                .pid(12345L)
                .version("17.0.1")
                .vmVersion("17.0.1+12")
                .vmVendor("Eclipse Adoptium")
                .vmName("OpenJDK 64-Bit Server VM")
                .startTimeMillis(1700000000000L)
                .memInfo(mem)
                .gcCollectors(gcCollectors)
                .build();

        assertEquals(12345L, jvm.getPid());
        assertEquals("17.0.1", jvm.getVersion());
        assertEquals("17.0.1+12", jvm.getVmVersion());
        assertEquals("Eclipse Adoptium", jvm.getVmVendor());
        assertEquals("OpenJDK 64-Bit Server VM", jvm.getVmName());
        assertEquals(1700000000000L, jvm.getStartTimeMillis());
        assertEquals(mem, jvm.getMemInfo());
        assertEquals(gcCollectors, jvm.getGcCollectors());
    }

    @Test
    public void testJvmInfoFrom() {
        final NodeInfo.JvmMemInfo mem = new NodeInfo.JvmMemInfo(256L, 1024L, 64L, 128L);
        final List<String> gc = Arrays.asList("G1");

        final NodeInfo.JvmInfo jvm = NodeInfo.JvmInfo.from(
                42L, "11.0.2", "11.0.2+9", "AdoptOpenJDK", "HotSpot", 9999L, mem, gc
        );

        assertEquals(42L, jvm.getPid());
        assertEquals("11.0.2", jvm.getVersion());
    }

    @Test
    public void testJvmInfoToMap() {
        final NodeInfo.JvmMemInfo mem = new NodeInfo.JvmMemInfo(256L, 1024L, 64L, 128L);
        final List<String> gc = Arrays.asList("G1 Young", "G1 Old");

        final NodeInfo.JvmInfo jvm = NodeInfo.JvmInfo.builder()
                .pid(100L).version("17").vmVersion("17+35")
                .vmVendor("Oracle").vmName("HotSpot")
                .startTimeMillis(5000L).memInfo(mem).gcCollectors(gc)
                .build();

        final Map<String, Object> map = jvm.toMap();
        assertEquals(100L, map.get("pid"));
        assertEquals("17", map.get("version"));
        assertEquals("17+35", map.get("vm_version"));
        assertEquals("Oracle", map.get("vm_vendor"));
        assertEquals("HotSpot", map.get("vm_name"));
        assertEquals(5000L, map.get("start_time_in_millis"));
        assertNotNull(map.get("mem"));
        assertEquals(gc, map.get("gc_collectors"));
    }

    @Test
    public void testJvmInfoGcCollectorsDefaultsToEmpty() {
        final NodeInfo.JvmInfo jvm = NodeInfo.JvmInfo.builder().build();
        assertNotNull(jvm.getGcCollectors());
        assertTrue(jvm.getGcCollectors().isEmpty());
    }

    @Test
    public void testJvmInfoGcCollectorsImmutable() {
        final List<String> gc = Arrays.asList("G1");
        final NodeInfo.JvmInfo jvm = NodeInfo.JvmInfo.builder().gcCollectors(gc).build();
        try {
            jvm.getGcCollectors().add("ZGC");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testJvmInfoEquality() {
        final NodeInfo.JvmMemInfo mem = new NodeInfo.JvmMemInfo(256L, 1024L, 64L, 128L);
        final List<String> gc = Arrays.asList("G1");

        final NodeInfo.JvmInfo j1 = NodeInfo.JvmInfo.builder()
                .pid(1L).version("17").vmVersion("17+35").vmVendor("V").vmName("N")
                .startTimeMillis(100L).memInfo(mem).gcCollectors(gc).build();
        final NodeInfo.JvmInfo j2 = NodeInfo.JvmInfo.builder()
                .pid(1L).version("17").vmVersion("17+35").vmVendor("V").vmName("N")
                .startTimeMillis(100L).memInfo(mem).gcCollectors(gc).build();

        assertEquals(j1, j2);
        assertEquals(j1.hashCode(), j2.hashCode());
    }

    // --- NodeInfo tests ---

    @Test
    public void testNodeInfoBuilder() {
        final NodeInfo node = NodeInfo.builder()
                .id("abc123")
                .name("my-node")
                .ephemeralId("eph-456")
                .version("8.11.0")
                .httpAddress("127.0.0.1:9600")
                .host("localhost")
                .status("green")
                .build();

        assertEquals("abc123", node.getId());
        assertEquals("my-node", node.getName());
        assertEquals("eph-456", node.getEphemeralId());
        assertEquals("8.11.0", node.getVersion());
        assertEquals("127.0.0.1:9600", node.getHttpAddress());
        assertEquals("localhost", node.getHost());
        assertEquals("green", node.getStatus());
        assertNull(node.getOsInfo());
        assertNull(node.getJvmInfo());
    }

    @Test
    public void testNodeInfoFrom() {
        final NodeInfo.OsInfo os = NodeInfo.OsInfo.from("Linux", "amd64", "5.4.0", 4);
        final NodeInfo.JvmInfo jvm = NodeInfo.JvmInfo.builder().pid(1L).version("17").build();

        final NodeInfo node = NodeInfo.from(
                "id1", "name1", "eph1", "9.0.0", "0.0.0.0:9600", "host1", "yellow", os, jvm
        );

        assertEquals("id1", node.getId());
        assertEquals("name1", node.getName());
        assertEquals(os, node.getOsInfo());
        assertEquals(jvm, node.getJvmInfo());
    }

    @Test
    public void testNodeInfoToMap() {
        final NodeInfo.OsInfo os = new NodeInfo.OsInfo("Linux", "amd64", "5.4.0", 8);
        final NodeInfo.JvmInfo jvm = NodeInfo.JvmInfo.builder()
                .pid(1L).version("17").vmVersion("17+35")
                .vmVendor("V").vmName("N").startTimeMillis(100L)
                .build();

        final NodeInfo node = NodeInfo.builder()
                .id("id1").name("node1").ephemeralId("eph1")
                .version("8.11.0").httpAddress("127.0.0.1:9600")
                .host("localhost").status("green")
                .osInfo(os).jvmInfo(jvm)
                .build();

        final Map<String, Object> map = node.toMap();
        assertEquals("id1", map.get("id"));
        assertEquals("node1", map.get("name"));
        assertEquals("eph1", map.get("ephemeral_id"));
        assertEquals("8.11.0", map.get("version"));
        assertEquals("127.0.0.1:9600", map.get("http_address"));
        assertEquals("localhost", map.get("host"));
        assertEquals("green", map.get("status"));
        assertNotNull(map.get("os"));
        assertNotNull(map.get("jvm"));
    }

    @Test
    public void testNodeInfoToMapWithoutOptionalFields() {
        final NodeInfo node = NodeInfo.builder()
                .id("id1").name("node1")
                .build();

        final Map<String, Object> map = node.toMap();
        assertEquals("id1", map.get("id"));
        assertFalse(map.containsKey("os"));
        assertFalse(map.containsKey("jvm"));
    }

    @Test
    public void testNodeInfoToMapIsImmutable() {
        final NodeInfo node = NodeInfo.builder().id("id1").build();
        final Map<String, Object> map = node.toMap();
        try {
            map.put("extra", "value");
            fail("Expected UnsupportedOperationException");
        } catch (final UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testNodeInfoEquality() {
        final NodeInfo n1 = NodeInfo.builder().id("id1").name("name1").version("1.0").build();
        final NodeInfo n2 = NodeInfo.builder().id("id1").name("name1").version("1.0").build();
        assertEquals(n1, n2);
        assertEquals(n1.hashCode(), n2.hashCode());
    }

    @Test
    public void testNodeInfoInequality() {
        final NodeInfo n1 = NodeInfo.builder().id("id1").build();
        final NodeInfo n2 = NodeInfo.builder().id("id2").build();
        assertNotEquals(n1, n2);
    }

    @Test
    public void testNodeInfoToString() {
        final NodeInfo node = NodeInfo.builder()
                .id("abc").name("my-node").version("8.11.0")
                .build();
        final String str = node.toString();
        assertTrue(str.contains("abc"));
        assertTrue(str.contains("my-node"));
        assertTrue(str.contains("8.11.0"));
    }
}
