/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class JsonCredentialsTest {

    private JsonCredentials creds;

    @Before
    public void before() throws Exception {
        creds = new JsonCredentials(this.getClass().getResourceAsStream("/test.json"));
    }

    @Test
    public void testGetUser() throws Exception {
        assertEquals("willy", creds.getUser(""));
    }

    @Test
    public void testGetApiKey() throws Exception {
        assertEquals("12345678-12345678", creds.getApiKey(""));
    }

    @Test
    public void testGetWithDefault() throws Exception {
        String json = "{\"userName\": \"karl\"}";
        JsonCredentials newCreds = new JsonCredentials(
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

        assertEquals("karl", newCreds.getUser("unknown"));
        assertEquals("123", newCreds.getApiKey("123"));
    }
}
