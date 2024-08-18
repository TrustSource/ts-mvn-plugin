/*
 * Copyright (c) 2016. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.project.MavenProject;

import java.util.HashMap;
import java.util.Map;

/**
 * This helper class is necessary in situations where the (faulty) pom of a maven project contains wrong coordinates.
 * e.g. found in org.milyn:flute:1.3 project, where the pom delivered by maven central contains the goupId "milyn"
 * instead of "org.milyn".
 */
public class ProjectFix {

    private static final Map<String, String> lookup = new HashMap<>();

    static {
        // groupId:artifactId:classifier:version
        lookup.put("milyn:flute:jar::1.3", "org.milyn:flute:jar::1.3");
    }


    public static void fixProject(MavenProject model) {
        Artifact a = model.getArtifact();
        String str = String.format("%s:%s:%s:%s:%s", a.getGroupId(), a.getArtifactId(),
                a.getType(), a.hasClassifier() ? a.getClassifier() : "", a.getVersion());

        String fixed = lookup.get(str);
        if(fixed != null) {
            String[] parts = fixed.split(":");
            if (parts.length == 5) {
                model.setGroupId(parts[0]);
                model.setArtifactId(parts[1]);
                model.setPackaging(parts[2]);
                model.setVersion(parts[4]);
                model.setArtifact(
                    new DefaultArtifact(parts[0], parts[1], parts[4], model.getArtifact().getScope(),
                        parts[2],
                        parts[3].isEmpty() ? null : parts[3],
                        model.getArtifact().getArtifactHandler())
                );
            }
        }
    }
}
