/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumCreator {

    public static String createChecksum(String file) throws NoSuchAlgorithmException, IOException {
        try(FileInputStream fis = new FileInputStream(file)) {
            return createChecksum(fis);
        }
    }

    public static String createChecksum(File file) throws NoSuchAlgorithmException, IOException {
        try(FileInputStream fis = new FileInputStream(file)) {
            return createChecksum(fis);
        }
    }


    private static String createChecksum(FileInputStream fis) throws NoSuchAlgorithmException, IOException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");

        byte[] buffer = new byte[1024];
        int read;
        while ((read = fis.read(buffer)) != -1) {
            sha1.update(buffer, 0, read);
        }

        byte[] hash = sha1.digest();

        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(Integer.toString((b & 0x00ff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }
}
