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
        if (!(o instanceof ComponentId)) {
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
            this.id = ((this.getGroupId() == null) ? "[inherited]" : this.getGroupId()) +
                    ":" +
                    this.getArtifactId() +
                    ":" +
                    ((this.getVersion() == null) ? "[inherited]" : this.getVersion());
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


    public static ComponentId createFallback(final Artifact artifact) {
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
                return artifact.getBaseVersion();
            }
        };
    }
}
