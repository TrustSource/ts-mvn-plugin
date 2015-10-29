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
    
    private final String project;
    private final String module;
    private final String moduleId;
    private final List<Dependency> dependencies;

    public Scan(String project, String module, String moduleId, List<Dependency> dependencies) {
        this.dependencies = dependencies;
        this.project = project;
        this.module = module;
        this.moduleId = moduleId;
    }

    public Scan(String project, String module, String moduleId, Dependency dependency) {
        this(project, module, moduleId, Collections.singletonList(dependency));
    }


    public List<Dependency> getDependencies() {
        return dependencies;
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


}
