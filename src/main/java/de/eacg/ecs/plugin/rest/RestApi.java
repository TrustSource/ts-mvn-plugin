/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin.rest;


import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class RestApi {

    private final String baseUrl;
    private final String apiPath;

    private Client client;

    private int responseStatus = -1;


    public RestApi(String baseUrl, String apiPath) {
        this(baseUrl, apiPath, null, null);
    }

    public RestApi(String baseUrl, String apiPath, String user, String password) {
        this.baseUrl = baseUrl;
        this.apiPath = apiPath;
        this.client = ClientBuilder.newClient();
        if(user != null && password != null) {
            this.client.register(new Authenticator(user, password));
        }
    }



    public String transferScan(Scan scan) throws Exception {
        Response response =
            client.target(baseUrl).path(apiPath).path("scans").
            request(MediaType.APPLICATION_JSON_TYPE).buildPost(Entity.json(scan)).invoke();

        responseStatus = response.getStatus();
        return response.readEntity(String.class);
    }

    public int getResponseStatus() {
        return responseStatus;
    }
}
