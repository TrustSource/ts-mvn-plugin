/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin.rest;

import java.util.HashSet;
import java.util.Set;

public class Dependency {
    private final String name;
    private final String description;
    private final String homepageUrl;
    private final String repoUrl;
    private final String key;
    private final Set<String> versions;
    private final boolean priv;
    private final Set<License> licenses;
    private final Set<Dependency> dependencies;
    private final String checksum;

    private Dependency(String name, String description, String key, Set<String> versions, String homepageUrl,
                       String repoUrl, boolean priv, Set<License> licenses, Set<Dependency>dependencies,
                       String checksum) {
        this.name = name;
        this.description = description;
        this.homepageUrl = homepageUrl;
        this.repoUrl = repoUrl;
        this.key = key;
        this.versions = versions;
        this.priv = priv;
        this.licenses = licenses;
        this.dependencies = dependencies;
        this.checksum = checksum;
    }

    public String getName() {
        return name;
    }

    public String getHomepageUrl() {
        return homepageUrl;
    }
    public String getRepoUrl() {
        return repoUrl;
    }

    public String getKey() {
        return key;
    }

    public Set<String> getVersions() {
        return versions;
    }

    public Set<License> getLicenses() {
        return licenses;
    }

    public String getDescription() {
        return description;
    }

    public boolean getPrivate() {
        return priv;
    }

    public Set<Dependency> getDependencies() {
        return dependencies;
    }

    public void addDependency(Dependency dependency) {
        if(dependencies == null) throw new IllegalStateException("dependencies not initialized");
        dependencies.add(dependency);
    }

    public String getChecksum() {
        return checksum;
    }

    public static class Builder {
        private String name;
        private String description;
        private String homepageUrl;
        private String repoUrl;
        private String key;
        private Set<String> versions;
        private boolean priv = false;
        private Set<License> licenses;
        private Set<Dependency> dependencies;
        private String checksum;


        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setHomepageUrl(String url) {
            this.homepageUrl = url;
            return this;
        }

        public Builder setRepoUrl(String url) {
            this.repoUrl = url;
            return this;
        }

        public Builder setKey(String key) {
            this.key = key;
            return this;
        }

        public Builder setVersions(Set<String> versions) {
            this.versions = versions;
            return this;
        }

        public Builder addVersion(String version) {
            getVersions().add(version);
            return this;
        }

        public Builder setPrivate(boolean priv) {
            this.priv = priv;
            return this;
        }

        public Builder setChecksum(String checksum) {
            this.checksum = checksum;
            return this;
        }

        public Builder setLicenses(Set<License> licenses) {
            this.licenses = licenses;
            return this;
        }

        public Builder addLicense(String name) {
            getLicenses().add(new License(name));
            return this;
        }

        public Builder addLicense(String name, String url) {
            getLicenses().add(new License(name, url));
            return this;
        }

        public Builder addDependency(Dependency dependency) {
            getDependencies().add(dependency);
            return this;
        }

        public Dependency getDependency() {
            return new Dependency(name, description, key, versions, homepageUrl, repoUrl, priv, licenses, dependencies, checksum);
        }

        private Set<License> getLicenses() {
            if(this.licenses == null) {
                this.licenses = new HashSet<>();
            }
            return this.licenses;
        }

        private Set<Dependency> getDependencies() {
            if (this.dependencies == null) {
                this.dependencies = new HashSet<>();
            }
            return this.dependencies;
        }

        private Set<String> getVersions() {
            if(this.versions == null) {
                this.versions = new HashSet<>();
            }
            return this.versions;
        }

    }
}
