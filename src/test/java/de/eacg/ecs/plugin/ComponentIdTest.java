/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ComponentIdTest {
    private ComponentId artifactId;
    private ComponentId groupId;

    @Before
    public void before() throws Exception {
        Model model = new Model();
        model.setArtifactId("artifact.id");
        model.setGroupId("group.id");
        model.setVersion("1.0.0");

        artifactId = ComponentId.create(new DefaultArtifact("group.id", "artifact.id", "1.0.0", "runtime", "type", "", null));
        groupId = ComponentId.create(model);
    }

    @Test
    public void testEquals() {
        assertEquals(artifactId, groupId);

        assertNotEquals(artifactId, "group.id:artifact.id:1.0.0");
    }

    @Test
    public void testGetArtifactId() {
        assertEquals("artifact.id", artifactId.getArtifactId());
        assertEquals("artifact.id", groupId.getArtifactId());
    }

    @Test
    public void testGroupId() {
        assertEquals("group.id", artifactId.getGroupId());
        assertEquals("group.id", groupId.getGroupId());
    }

    @Test
    public void testVersion() {
        assertEquals("1.0.0", artifactId.getVersion());
        assertEquals("1.0.0", groupId.getVersion());
    }

    /* Simulate a locked snapshot */
    @Test
    public void testFallback() throws Exception {
        Artifact artifact = new DefaultArtifact("group.id", "artifact.id",
                VersionRange.createFromVersionSpec("1.0-20151123.141732-1"),
                "runtime", "type", "", null);

        artifactId = ComponentId.createFallback(artifact);
        assertEquals("group.id", artifactId.getGroupId());
        assertEquals("artifact.id", artifactId.getArtifactId());
        assertEquals("1.0-SNAPSHOT", artifactId.getVersion());
    }
}
