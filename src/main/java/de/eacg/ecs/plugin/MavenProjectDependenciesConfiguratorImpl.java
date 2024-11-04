/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.codehaus.mojo.license.api.ArtifactFilters;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;


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
    public boolean isExcludeTransitiveDependencies() {
        return false;
    }

    @Override
    public ArtifactFilters getArtifactFilters() {
        return ArtifactFilters.buidler().build();
    }

    @Override
    public boolean isVerbose() {
        return this.isVerbose;
    }
}
