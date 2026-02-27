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

package org.logstash.config.source;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ConfigFetchResultTest {

    @Test
    public void testSuccessFactory() {
        PipelineConfigParts parts = createSampleParts("test-pipeline");
        ConfigFetchResult result = ConfigFetchResult.success(Collections.singletonList(parts));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getConfigs().size());
        assertEquals("test-pipeline", result.getConfigs().get(0).getPipelineId());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testSuccessWithEmptyList() {
        ConfigFetchResult result = ConfigFetchResult.success(Collections.emptyList());

        assertTrue(result.isSuccess());
        assertTrue(result.getConfigs().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    public void testSuccessWithMultipleConfigs() {
        List<PipelineConfigParts> configs = Arrays.asList(
                createSampleParts("pipeline-a"),
                createSampleParts("pipeline-b"),
                createSampleParts("pipeline-c")
        );
        ConfigFetchResult result = ConfigFetchResult.success(configs);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getConfigs().size());
    }

    @Test
    public void testFailureFactory() {
        List<String> errors = Arrays.asList("Error 1", "Error 2");
        ConfigFetchResult result = ConfigFetchResult.failure(errors);

        assertFalse(result.isSuccess());
        assertTrue(result.getConfigs().isEmpty());
        assertEquals(2, result.getErrors().size());
        assertEquals("Error 1", result.getErrors().get(0));
        assertEquals("Error 2", result.getErrors().get(1));
    }

    @Test
    public void testFailureWithSingleError() {
        ConfigFetchResult result = ConfigFetchResult.failure(
                Collections.singletonList("Something went wrong"));

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertEquals("Something went wrong", result.getErrors().get(0));
    }

    @Test
    public void testConfigsListIsImmutable() {
        List<PipelineConfigParts> mutableList = new ArrayList<>();
        mutableList.add(createSampleParts("pipeline-1"));
        ConfigFetchResult result = ConfigFetchResult.success(mutableList);

        // Modify the original list
        mutableList.add(createSampleParts("pipeline-2"));

        // Result should not be affected
        assertEquals(1, result.getConfigs().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testConfigsListCannotBeModifiedDirectly() {
        ConfigFetchResult result = ConfigFetchResult.success(
                Collections.singletonList(createSampleParts("p")));
        result.getConfigs().add(createSampleParts("another"));
    }

    @Test
    public void testErrorsListIsImmutable() {
        List<String> mutableErrors = new ArrayList<>();
        mutableErrors.add("error-1");
        ConfigFetchResult result = ConfigFetchResult.failure(mutableErrors);

        // Modify the original list
        mutableErrors.add("error-2");

        // Result should not be affected
        assertEquals(1, result.getErrors().size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testErrorsListCannotBeModifiedDirectly() {
        ConfigFetchResult result = ConfigFetchResult.failure(
                Collections.singletonList("err"));
        result.getErrors().add("another");
    }

    @Test
    public void testToStringSuccess() {
        ConfigFetchResult result = ConfigFetchResult.success(
                Collections.singletonList(createSampleParts("p")));
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("success=true"));
        assertTrue(str.contains("configs=1"));
    }

    @Test
    public void testToStringFailure() {
        ConfigFetchResult result = ConfigFetchResult.failure(
                Collections.singletonList("bad config"));
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("success=false"));
        assertTrue(str.contains("bad config"));
    }

    private PipelineConfigParts createSampleParts(String pipelineId) {
        PipelineConfigParts.ConfigPart part = new PipelineConfigParts.ConfigPart(
                "string", pipelineId, "input { stdin {} } output { stdout {} }");
        return new PipelineConfigParts(pipelineId, Collections.singletonList(part),
                Collections.emptyMap());
    }
}
