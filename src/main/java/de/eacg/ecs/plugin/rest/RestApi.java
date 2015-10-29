/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin.rest;


import de.eacg.ecs.plugin.ProjectProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class RestApi {

    private final String baseUrl;
    private final String apiPath;
    private final String apiKey;
    private final String user;

    private Client client;

    private int responseStatus = -1;


    public RestApi(String baseUrl, String apiPath, String apiKey, String user) {
        this(baseUrl, apiPath, apiKey, user, null, null);
    }

    public RestApi(String baseUrl, String apiPath, String apiKey, String user, String basicAuthUser, String basicAuthPwd) {
        this.baseUrl = baseUrl;
        this.apiPath = apiPath;
        this.apiKey = apiKey;
        this.user = user;
        this.client = ClientBuilder.newClient();
        if(basicAuthUser != null && basicAuthPwd != null) {
            this.client.register(new Authenticator(basicAuthUser, basicAuthPwd));
        }
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
}
