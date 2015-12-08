/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class JsonCredentials {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Object> properties;

    public JsonCredentials(String filename) throws IOException {
        if(filename != null) {
            File f = new File(filename.replaceAll("^~", System.getProperty("user.home")));
            this.properties = mapper.readValue(f, Map.class);
        } else {
            properties = null;
        }
    }

    public JsonCredentials(File f) throws IOException {
        this.properties = mapper.readValue(f, Map.class);
    }

    public JsonCredentials(InputStream is) throws IOException {
        this.properties = mapper.readValue(is, Map.class);
    }

    public String getUser(String deflt) {
        String v = getString("userName");
        return v == null ? deflt : v;
    }

    public String getApiKey(String deflt) {
        String v = getString("apiKey");
        return v == null ? deflt : v;
    }

    private String getString(String key){
        return getType(this.properties, key, String.class);
    }

    private static String getString(Map<String, Object> map, String key){
        return getType(map, key, String.class);
    }

    private <T> T getType(String key, Class<T>type){
        return getType(this.properties, key, type);
    }

    private static <T> T getType(Map<String, Object> map, String key, Class<T>type){
        try {
            return map == null ? null : type.cast(map.get(key));
        } catch (ClassCastException e) {
            return null;
        }
    }
}
