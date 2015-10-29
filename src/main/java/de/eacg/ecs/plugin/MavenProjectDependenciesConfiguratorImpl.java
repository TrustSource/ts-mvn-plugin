/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;

import java.util.Collections;
import java.util.List;

public class MavenProjectDependenciesConfiguratorImpl implements MavenProjectDependenciesConfigurator{

    private boolean isVerbose = false;


    public MavenProjectDependenciesConfiguratorImpl() {
        this(false);
    }

    public MavenProjectDependenciesConfiguratorImpl(boolean isVerbose) {
        this.isVerbose = isVerbose;
    }

    @Override
    public boolean isIncludeTransitiveDependencies() {
        return true;
    }

    @Override
    public List<String> getIncludedScopes() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getExcludedScopes() {
        return Collections.emptyList();
    }

    @Override
    public String getIncludedArtifacts() {
        return "";
    }

    @Override
    public String getExcludedArtifacts() {
        return "";
    }

    @Override
    public String getIncludedGroups() {
        return "";
    }

    @Override
    public String getExcludedGroups() {
        return "";
    }

    @Override
    public boolean isVerbose() {
        return this.isVerbose;
    }
}
