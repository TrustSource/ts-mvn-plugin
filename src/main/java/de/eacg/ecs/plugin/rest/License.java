/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin.rest;


public class License {
    private final String name;
    private final String url;

    public License(String name, String url) {
        if(name == null) throw new NullPointerException("name must not be null");
        this.name = name;
        this.url = url;
    }

    public License(String name) {
        this(name, null);
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        License license = (License) o;

        if (!name.equals(license.name)) return false;
        if (url != null ? !url.equals(license.url) : license.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
