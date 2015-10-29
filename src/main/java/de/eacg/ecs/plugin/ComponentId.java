/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;

/**
 * provide a consistent view to maven things which have the typical "groupId:artifactId:version" representation
 * e.g. instances of org.apache.maven.model.Model and org.apache.maven.artifact.Artifact;
 */
public abstract class ComponentId {

    private String id;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o instanceof ComponentId) == false) {
            return false;
        }
        return toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        if(this.id == null) {
            StringBuilder sb = new StringBuilder(64);

            sb.append((this.getGroupId() == null) ? "[inherited]" : this.getGroupId());
            sb.append(":");
            sb.append(this.getArtifactId());
            sb.append(":");
            sb.append((this.getVersion() == null) ? "[inherited]" : this.getVersion());
            this.id = sb.toString();
        }
        return this.id;
    }


    public abstract String getGroupId();
    public abstract String getArtifactId();
    public abstract String getVersion();

    public static ComponentId create(MavenProject project) {
        return create(project.getModel());
    }

    public static ComponentId create(final Model model) {
        return new ComponentId() {
            @Override
            public String getGroupId() {
                return model.getGroupId();
            }

            @Override
            public String getArtifactId() {
                return model.getArtifactId();
            }

            @Override
            public String getVersion() {
                return model.getVersion();
            }
        };
    }

    public static ComponentId create(final Artifact artifact) {
        return new ComponentId() {
            @Override
            public String getGroupId() {
                return artifact.getGroupId();
            }

            @Override
            public String getArtifactId() {
                return artifact.getArtifactId();
            }

            @Override
            public String getVersion() {
                return artifact.getVersion();
            }
        };
    }
}
