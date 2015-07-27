/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin.rest;

import java.util.Collections;
import java.util.List;

public class Scan {
    
    private final String user;
    private final String project;
    private final String module;
    private final String moduleId;
    private final String apiKey;
    private final List<Dependency> dependencies;

    public Scan(String apiKey, String user, String project, String module, String moduleId,
                List<Dependency> dependencies) {
        this.apiKey = apiKey;
        this.dependencies = dependencies;
        this.user = user;
        this.project = project;
        this.module = module;
        this.moduleId = moduleId;
    }

    public Scan(String apiKey, String user, String project, String module, String moduleId,
                Dependency dependency) {
        this(apiKey, user, project, module, moduleId, Collections.singletonList(dependency));
    }


    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public String getUser() {
        return user;
    }

    public String getProject() {
        return project;
    }

    public String getModule() {
        return module;
    }

    public String getModuleId() {
        return moduleId;
    }

    public String getApiKey() {
        return apiKey;
    }
}
