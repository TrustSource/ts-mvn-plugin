/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;


public class ChecksumCreatorTest {

    private static final String EXPECTED_SHA1 = "396bc6e46abdf67e6e534050af0ea0a088fee058";

    @Test(expected = IOException.class)
    public void testCreateChecksumFileNotFound() throws Exception {
        ChecksumCreator.createChecksum("does_not_exist");
    }

    @Test
    public void testCreateChecksumByFilename() throws Exception {
        assertEquals(EXPECTED_SHA1, ChecksumCreator.createChecksum("./src/test/resources/test.file"));
    }

    @Test
    public void testCreateChecksumByFile() throws Exception {
        File f = new File(this.getClass().getClassLoader().getResource("test.file").getFile());
        assertEquals(EXPECTED_SHA1, ChecksumCreator.createChecksum(f));
    }
}