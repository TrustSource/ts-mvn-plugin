/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin.rest;


import de.eacg.ecs.plugin.ProjectProperties;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.ProxySelector;

public class RestApi {

    private final String baseUrl;
    private final String apiPath;
    private final String apiKey;
    private final String user;

    private final Client client;

    private int responseStatus = -1;



    public RestApi(String baseUrl, String apiPath, String apiKey, String user) {
        this.baseUrl = baseUrl;
        this.apiPath = apiPath;
        this.apiKey = apiKey;
        this.user = user;
        this.client = createClient();
    }

    public String transferScan(Scan scan) throws Exception {
        ProjectProperties props = new ProjectProperties();

        Response response =
            client.target(baseUrl).path(apiPath).path("scans").
                    request(MediaType.APPLICATION_JSON_TYPE).
                    header("User-Agent", props.getProperty("artifactId") + "/" + props.getProperty("version")).
                    header("X-ApiKey", this.apiKey).
                    header("X-User", this.user).
                    buildPost(Entity.json(scan)).invoke();

        responseStatus = response.getStatus();
        return response.readEntity(String.class);
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    private static Client createClient() {
        SystemDefaultRoutePlanner routePlanner = new SystemDefaultRoutePlanner(
                ProxySelector.getDefault());
        CloseableHttpClient httpClient = HttpClients.custom()
                .setRoutePlanner(routePlanner)
                .build();

        ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);

        return new ResteasyClientBuilder().httpEngine(engine).build();
    }
}
