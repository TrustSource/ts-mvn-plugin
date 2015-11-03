/*
 * Copyright (c) 2015. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import de.eacg.ecs.plugin.rest.Dependency;
import de.eacg.ecs.plugin.rest.RestApi;
import de.eacg.ecs.plugin.rest.Scan;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.License;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.mojo.license.api.DefaultThirdPartyHelper;
import org.codehaus.mojo.license.api.DependenciesTool;
import org.codehaus.mojo.license.api.ThirdPartyHelper;
import org.codehaus.mojo.license.api.ThirdPartyTool;
import org.codehaus.mojo.license.model.LicenseMap;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;


@Mojo(name = "dependency-scan", defaultPhase = LifecyclePhase.DEPLOY,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ScanAndTransferMojo extends AbstractMojo {

    /** ----------------------------------------------------------------------
     * Mojo Parameters
     *
     * ----------------------------------------------------------------------*/

    /**
     * Activate verbose mode. If you use start mvn with -X, verbose is also on
     * <p/>
     * Default: 'false'
     */
    @Parameter( property = "licenseScan.verbose", defaultValue = "${maven.verbose}" )
    private boolean verbose;

    /**
     * The name of the project, configured in central evaluation server.
     */
    @Parameter(property = "licenseScan.projectName")
    private String projectName;

    /**
     * The name of the module as reported to central evaluation server.<br/>
     * <p/>
     * Default: '${project.name}'
     */
    @Parameter(property = "licenseScan.moduleName", defaultValue = "${project.name}")
    private String moduleName;

    /**
     * The id of the module as reported to central evaluation server.<br/>
     * <p/>
     * Default: '${project.groupId}:${project.artifactId}'
     */
    @Parameter(property = "licenseScan.moduleId", defaultValue = "${project.groupId}:${project.artifactId}")
    private String moduleId;

    /**
     * The Baseurl to access central evaluation server.<br/>
     * <p/>
     * Default: 'https://demo-ecs.eacg.de'
     */
    @Parameter(property = "licenseScan.baseUrl", defaultValue = "https://demo-ecs.eacg.de")
    private String baseUrl;

    /**
     * The API Path to access central evaluation server.<br/>
     * <p/>
     * Default: '/api/v1'
     */
    @Parameter(property = "licenseScan.apiPath", defaultValue = "/api/v1")
    private String apiPath;

    /**
     * To skip execution of this mojo.
     * <p/>
     * Default: 'false'
     */
    @Parameter(property = "licenseScan.skip", defaultValue = "false")
    private boolean skip;

    /**
     * To skip transfer of the result.
     * <p/>
     * Default: 'false'
     */
    @Parameter(property = "licenseScan.skipTransfer", defaultValue = "false")
    private boolean skipTransfer;

    /**
     * The scope to filter by when resolving the dependency tree, or <code>null</code> to include dependencies from
     * all scopes.
     * <p/>
     * Default: 'runtime'
     */
    @Parameter( property = "licenseScan.scope", defaultValue = "runtime")
    private String scope;

    /**
     * File in json format which may contain the security credentials
     * <p/>
     * Example:<br/>
     * {<code>
     *     "user": "willy",<br/>
     *     "apiKey": "12345678-12345678",<br/>
     *     "basicAuth": {<br/>
     *         "user": "optional",<br/>
     *         "password": "optional"<br/>
     *
     *     }
     * }</code>
     * <p/>
     * Required, if username or apiKey are not provided by plugin configuration
     */
    @Parameter( property = "licenseScan.credentials")
    private String credentials;
    /**
     * The username used to authorize the transfer of dependency information to central server.
     * <p/>
     * Required, if not specified in credentials file
     */
    @Parameter(property = "licenseScan.userName")
    private String userName;

    /**
     * The API key to used to authorize the transfer of dependency information to central server.
     * <p/>
     * Required, if not specified in credentials file
     */
    @Parameter(property = "licenseScan.apiKey")
    private String apiKey;

    /**
     * Username if server requires basic auth
     */
    @Parameter(property = "licenseScan.basicAuthUser")
    private String basicAuthUser;

    /**
     * Password if server requires basic auth
     */
    @Parameter(property = "licenseScan.basicAuthPasswd")
    private String basicAuthPasswd;

    /**
     * Specify as semicolon separated list, the groupId or groupId:artifactId of components you wish to mark as private.
     * This components are not visible by other parties on the central evaluation server.
     * The following example marks all artifacts with groupId "org.acme" and the artifact "org.foo:foo.bar" as private:
     * "org.acme;org.foo:foo.bar".
     * <p/>
     * Default: '${project.groupId}' groupId of the current project
     */
    @Parameter(property = "licenseScan.privateComponents", defaultValue = "${project.groupId}")
    private String privateComponents;


    /** ----------------------------------------------------------------------
     * Mojo injected components
     *
     * ----------------------------------------------------------------------*/

    @Component( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Component
    private MavenProject mavenProject;

    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}",required = true, readonly = true )
    private List<ArtifactRepository> remoteRepositories;

    @Parameter(defaultValue = "${project.build.sourceEncoding}", readonly = true)
    private String encoding;

    @Component
    private DependenciesTool dependenciesTool;

    @Component
    private ThirdPartyTool thirdPartyTool;

    /** ----------------------------------------------------------------------
     * Mojo private properties
     *
     * ----------------------------------------------------------------------*/
    private String[] privateComponentArr = null;


    /** ----------------------------------------------------------------------
     * Mojo implementation
     *
     * ----------------------------------------------------------------------*/
    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if(this.encoding == null) {
            this.encoding = "UTF-8"; // encoding is required by ThirdPartyHelper Todo: get rid of it
        }

        if ( this.skip ) {
            getLog().info( "skip flag is on, will skip goal." );
            return;
        }

        if ( getLog().isDebugEnabled() ) {
            this.verbose = true;
        }

        JsonCredentials credentials = readAndCheckCredentials();

        Dependency dependency = createDependency();

        if(skipTransfer) {
            getLog().info("Skipping rest transfer");
        } else {
            try {
                Scan scan = new Scan(projectName, moduleName, moduleId, dependency);
                RestApi restApi = new RestApi(baseUrl, apiPath,
                        credentials.getApiKey(this.apiKey),
                        credentials.getUser(this.userName),
                        credentials.getBasicAuthUser(basicAuthUser),
                        credentials.getBasicAuthPasswd(basicAuthPasswd));

                transferScan(restApi, scan);
            } catch (Exception e) {
                getLog().error("Calling Rest API failed", e);
                throw new MojoExecutionException("Exception while calling Rest API", e);
            }
        }
    }

    private Dependency createDependency() throws MojoExecutionException {

        try {
            LicenseMap licenseMap = createLicenseMap();
            ArtifactFilter artifactFilter = createResolvingArtifactFilter();

            DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph( mavenProject, artifactFilter );

            Set<Map.Entry<String, SortedSet<MavenProject>>> licenseAndProjectSet = licenseMap.entrySet();
            Set<Map.Entry<MavenProject, String[]>> projectsAndLicenseSet = licenseMap.toDependencyMap().entrySet();

            // add this project to list
            List<String> ownLicenses = new ArrayList<>();
            for(License lic : mavenProject.getModel().getLicenses()) {
                ownLicenses.add(lic.getName());
            }
            String[] ownLicensesArr = ownLicenses.toArray(new String[ownLicenses.size()]);

            Map<MavenProject, String[]> myOwnMap = new HashMap<>();
            myOwnMap.put(mavenProject, ownLicensesArr);
            for(Map.Entry<MavenProject, String[]>entry : projectsAndLicenseSet) {
                myOwnMap.put(entry.getKey(), entry.getValue());
            }

            printStats(licenseAndProjectSet, projectsAndLicenseSet);
            return  createDependencyTree(rootNode, myOwnMap.entrySet());
        } catch (Exception e) {
            getLog().error("License collection failed", e);
            throw new MojoExecutionException("Exception while collecting license information", e);
        }
    }

    private JsonCredentials readAndCheckCredentials() throws MojoExecutionException {
        try {
            JsonCredentials credentials = new JsonCredentials(this.credentials);
            checkMandatoryParameter("userName", credentials.getUser(this.userName));
            checkMandatoryParameter("apiKey", credentials.getApiKey(this.apiKey));
            return credentials;
        } catch (Exception e) {
            getLog().error("Evaluation of user credentials failed", e);
            throw new MojoExecutionException("Exception while evaluating user credentials", e);
        }
    }

    private void checkMandatoryParameter(String name, String p) throws MojoExecutionException {
        if (p == null || p.isEmpty()) {
            String err = String.format("The mandatory parameter '%s' for plugin %s is missing or invalid",
                    name, ComponentId.create(mavenProject));
            getLog().error(err);
            throw new MojoExecutionException("Exception: " + err);
        }
    }


    private LicenseMap createLicenseMap() {
        ThirdPartyHelper tpHelper = new DefaultThirdPartyHelper( this.mavenProject, this.encoding, this.verbose,
                this.dependenciesTool, this.thirdPartyTool, this.localRepository, this.remoteRepositories, getLog() );

        return tpHelper.createLicenseMap(tpHelper.loadDependencies(new MavenProjectDependenciesConfiguratorImpl()));
    }

    private String[] getPrivateComponents() {
        if(privateComponentArr == null) {
            privateComponentArr = privateComponents.split(";");
        }
        return privateComponentArr;
    }

    private boolean isPrivateComponent(MavenProject project) {
        for (String pc : getPrivateComponents()) {
            String compareString = project.getGroupId();
            if(pc.contains(":")) {
                compareString += ':' + project.getArtifactId();
            }
            boolean result = pc.equals(compareString);
            if(result) {
                return true;
            }

        }
        return false;
    }

    private Dependency mapDependency(DependencyNode node, Map<ComponentId, Map.Entry<MavenProject, String[]>> projectLookup) {
        Dependency.Builder builder = new Dependency.Builder();
        Artifact artifact = node.getArtifact();
        ComponentId artifactId = ComponentId.create(artifact);
        Map.Entry<MavenProject, String[]> projectLicensesPair = projectLookup.get(artifactId);

        if(projectLicensesPair == null) {
            getLog().error("Something weird happened: no Project found for artifact: " + artifactId);
            return null;
        }

        MavenProject project = projectLicensesPair.getKey();
        String[] licensesArr = projectLicensesPair.getValue();

        builder.setName(project.getName())
                .setDescription(project.getDescription())
                .setKey("mvn:" + project.getGroupId() + ':' + project.getArtifactId())
                .addVersion(project.getVersion())
                .setHomepageUrl(project.getUrl());
        if(isPrivateComponent(project)) {
            builder.setPrivate(true);
        }

        try {
            File file = artifact.getFile();
            if(file != null) {
                builder.setChecksum("sha-1:" + ChecksumCreator.createChecksum(file));
            } else {
                Artifact af = findProjectArtifact(artifact);
                if(af != null && af.getFile() != null) {
                    builder.setChecksum("sha-1:" + ChecksumCreator.createChecksum(af.getFile()));
                } else {
                    getLog().warn("Could not generate checksum - no file specified: " + ComponentId.create(artifact));
                }
            }
        } catch(NoSuchAlgorithmException | IOException e) {
            getLog().warn("Could not generate checksum: " + e.getMessage());
        }

        if (licensesArr != null && (licensesArr.length != 1 || !LicenseMap.UNKNOWN_LICENSE_MESSAGE.equals(licensesArr[0]))) {
            for(String license : licensesArr) {
                builder.addLicense(license);
            }
        }

        for(DependencyNode childNode : node.getChildren()) {
            Dependency dep = mapDependency(childNode, projectLookup);
            if(dep != null) {
                builder.addDependency(dep);
            }
        }

        return builder.getDependency();
    }

    private Artifact findProjectArtifact(Artifact other) {
        for(Object obj : mavenProject.getArtifacts()) {
            // unfortunately we can't use DefaultArtifact.equals(), because the classifier of both may differ (null vs "") even
            // if the Objects represent the same physical artifact.
            if ( obj == other ) {
                return (Artifact)obj;
            }

            if ( obj instanceof Artifact ) {
                Artifact self = (Artifact) obj;

                if (self.getGroupId().equals(other.getGroupId())
                        && self.getArtifactId().equals(other.getArtifactId())
                        && self.getVersion().equals(other.getVersion())
                        && self.getType().equals(other.getType())) {

                    String myClassifier = self.getClassifier() == null ? "" : self.getClassifier();
                    String otherClassifier = other.getClassifier() == null ? "" : other.getClassifier();

                    if (myClassifier.equals(otherClassifier)) {
                        return self;
                    }
                }
            }
        }
        return null;
    }

    private Dependency createDependencyTree(
            DependencyNode rootNode,
            Set<Map.Entry<MavenProject, String[]>> projectsAndLicenseSet) {

        Map<ComponentId, Map.Entry<MavenProject, String[]>> projectLookup = new HashMap<>();
        for(Map.Entry<MavenProject, String[]> entry : projectsAndLicenseSet) {
            projectLookup.put(ComponentId.create(entry.getKey()), entry);
        }

        return mapDependency(rootNode, projectLookup);
    }

    private void transferScan(RestApi api, Scan scan) throws MojoExecutionException {
        try {
            String body = api.transferScan(scan);
            getLog().info(String.format("API Response: code: %d, body: %n%s%n",
                    api.getResponseStatus(), body));

            if (api.getResponseStatus() != 201) {
                throw new MojoExecutionException("Failed : HTTP error code : " + api.getResponseStatus());
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Exception while transferring scan results to server", e);
        }
    }

    private void printStats(
            Set<Map.Entry<String, SortedSet<MavenProject>>> licenseAndProjectSet,
            Set<Map.Entry<MavenProject, String[]>> projectsAndLicenseSet) {

        Log log = getLog();
        if (log.isInfoEnabled() && this.verbose) {
            log.info("Dependencies found:");
            for (Map.Entry<MavenProject, String[]> entry : projectsAndLicenseSet) {
                MavenProject project = entry.getKey();
                String[] licenses = entry.getValue();
                log.info(String.format("%s %s, %s", project.getName(), project.getVersion(), licenses));
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Licenses found:");
            for (Map.Entry<String, SortedSet<MavenProject>> entry : licenseAndProjectSet) {
                log.info(String.format("%-75s %d", entry.getKey(), entry.getValue().size()));
            }
        }
    }

    // depends on configuration parameter scope
    private ArtifactFilter createResolvingArtifactFilter() {
        if ( scope != null ) {
            getLog().info(String.format("The selected scope is '%s'", scope));
            return  new ScopeArtifactFilter( scope );
        } else

        return null;
    }
}
