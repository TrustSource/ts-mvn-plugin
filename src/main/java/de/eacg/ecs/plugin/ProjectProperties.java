/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class ProjectProperties extends Properties {

    public ProjectProperties() throws IOException {
        super.load(this.getClass().getResourceAsStream("/project.properties"));
    }

    public String getName() {
        return this.getProperty("artifactId");
    }

    public String getVersion() {
        return this.getProperty("version");
    }

    public Date getBuildDate() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return df.parse(this.getProperty("buildDate"));
        }catch(ParseException e) {
            return null;
        }
    }
}
