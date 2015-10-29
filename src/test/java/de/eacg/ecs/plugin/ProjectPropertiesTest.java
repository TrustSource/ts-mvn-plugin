/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;


public class ProjectPropertiesTest {

   private ProjectProperties properties;


    @Before
    public void setUp() throws Exception {
        properties = new ProjectProperties();
    }


    @Test
    public void testGetName() throws Exception {
        assertEquals("ecs-mvn-plugin", properties.getName());

    }

    @Test
    public void testGetVersion() throws Exception {
        assertEquals(String.class, properties.getVersion().getClass());
    }

    @Test
    public void testGetBuildDate() throws Exception {
        assertEquals(Date.class, properties.getBuildDate().getClass());
    }
}
