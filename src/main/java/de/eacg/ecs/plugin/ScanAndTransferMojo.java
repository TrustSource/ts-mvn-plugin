/*
 * Copyright (c) 2016. Enterprise Architecture Group, EACG
 *
 * SPDX-License-Identifier:	MIT
 *
 */

package de.eacg.ecs.plugin;

import de.eacg.ecs.client.Dependency;
import de.eacg.ecs.client.JsonProperties;
import de.eacg.ecs.client.RestClient;
import de.eacg.ecs.client.Scan;
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
import org.codehaus.mojo.license.api.*;
import org.codehaus.mojo.license.model.LicenseMap;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;


@Mojo(name = "dependency-scan", defaultPhase = LifecyclePhase.DEPLOY,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ScanAndTransferMojo extends AbstractMojo {

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
     * The VCS branch of the module as reported to central evaluation server.<br/>
     * <p/>
     * Default: ''
     */
    @Parameter(property = "licenseScan.branch")
    private String branch;

    /**
     * The VCS tag of the module as reported to central evaluation server.<br/>
     * <p/>
     * Default: ''
     */
    @Parameter(property = "licenseScan.tag")
    private String tag;

    /**
     * The base url to access central evaluation server.<br/>
     * <p/>
     * Default: '<a href="https://api.trustsource.io">...</a>'
     */
    @Parameter(property = "licenseScan.baseUrl", defaultValue = "https://api.trustsource.io")
    private String baseUrl;

    /**
     * The proxy server url.<br/>
     * <p/>
     * Default: ''
     */
    @Parameter(property = "licenseScan.proxyUrl")
    private String proxyUrl;

    /**
     * The proxy server port.<br/>
     * <p/>
     * Default: '8080'
     */
    @Parameter(property = "licenseScan.proxyPort", defaultValue = "8080")
    private String proxyPort;

    /**
     * The proxy server username.<br/>
     * <p/>
     * Default: ''
     */
    @Parameter(property = "licenseScan.proxyUser")
    private String proxyUser;

    /**
     * The proxy server password.<br/>
     * <p/>
     * Default: ''
     */
    @Parameter(property = "licenseScan.proxyPass")
    private String proxyPass;

    /**
     * The API Path to access central evaluation server.<br/>
     * <p/>
     * Default: '/api/v2'
     */
    @Parameter(property = "licenseScan.apiPath", defaultValue = "/v2")
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
     *     "user": "willy@company.com",<br/>
     *     "apiKey": "12345678-12345678",<br/>
     * }</code>
     * <p/>
     * Required, if username or apiKey are not provided by plugin configuration
     */
    @Parameter( property = "licenseScan.credentials")
    private String credentials;
    /**
     * The API key to used to authorize the transfer of dependency information to central server.
     * <p/>
     * Required, if not specified in credentials file
     */
    @Parameter(property = "licenseScan.apiKey")
    private String apiKey;

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

    /**
     * Output file path for the server response
     * <p/>
     * Default: '${project.groupId}' groupId of the current project
     */
    @Parameter(property = "licenseScan.privateComponents", defaultValue = "${project.groupId}")
    private String output;

    /** ----------------------------------------------------------------------
     * Mojo injected components
     * <p>
     * ----------------------------------------------------------------------*/

    @Component( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Component
    private MavenProject mavenProject;

    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true )
    private ArtifactRepository localRepository;

    @Parameter(defaultValue = "${project.remoteArtifactRepositories}",required = true, readonly = true )
    private List<RemoteRepository> remoteRepositories;

    @Parameter(defaultValue = "${project.build.sourceEncoding}", readonly = true)
    private String encoding;

    @Component
    private DependenciesTool dependenciesTool;

    @Component
    private ThirdPartyTool thirdPartyTool;

    /** ----------------------------------------------------------------------
     * Mojo private properties
     * <p>
     * ----------------------------------------------------------------------*/
    private String[] privateComponentArr = null;


    /** ----------------------------------------------------------------------
     * Mojo properties accessors
     * <p>
     * ----------------------------------------------------------------------*/

    public String getProjectName() {
        return projectName;
    }

    public String getModuleName() {
        return moduleName;
    }


    /** ----------------------------------------------------------------------
     * Mojo implementation
     * <p>
     * ----------------------------------------------------------------------*/
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();

        Dependency dependency = createDependency();

        if(skipTransfer) {
            getLog().info("Skipping rest transfer");
        } else {
            try {
                Scan scan = createScan(dependency);
                RestClient restClient = createRestClient();

                transferScan(restClient, scan);
            } catch (Exception e) {
                getLog().error("Calling Rest API failed", e);
                throw new MojoExecutionException("Exception while calling Rest API", e);
            }
        }
    }

    protected void init() {
        if(this.encoding == null) {
            this.encoding = "UTF-8"; // encoding is required by ThirdPartyHelper Todo: get rid of it
        }

        if ( this.skip ) {
            getLog().info( "skip flag is set, will skip goal." );
            return;
        }

        if ( getLog().isDebugEnabled() ) {
            this.verbose = true;
        }
    }

    protected Scan createScan(Dependency dependency) {
        return new Scan(projectName, moduleName, moduleId, branch, tag, dependency);
    }

    protected RestClient createRestClient() throws MojoExecutionException {
        return new RestClient(readAndCheckCredentials(), getUserAgent());
    }

    protected Dependency createDependency() throws MojoExecutionException {

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
            String[] ownLicensesArr = ownLicenses.toArray(new String[0]);

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

    private JsonProperties readAndCheckCredentials() throws MojoExecutionException {
        JsonProperties credentials;
        try {
            credentials = new JsonProperties(this.credentials);
        } catch (Exception e) {
            getLog().error("Evaluation of user credentials failed", e);
            throw new MojoExecutionException("Exception while evaluating user credentials", e);
        }
        credentials.setApiKey(this.apiKey);
        credentials.setBaseUrl(this.baseUrl);
        credentials.setApiPath(this.apiPath);

        credentials.setProxyUrl(this.proxyUrl);
        credentials.setProxyPort(this.proxyPort);
        credentials.setProxyUser(this.proxyUser);
        credentials.setProxyPass(this.proxyPass);

        List<String> missingKeys = credentials.validate();
        if(!missingKeys.isEmpty()) {
            String err = String.format("The mandatory parameter(s) '%s' for plugin %s is missing or invalid",
                    missingKeys.toString(), ComponentId.create(mavenProject));
            getLog().error(err);
            throw new MojoExecutionException("Exception: " + err);
        }

        return credentials;
    }

    private LicenseMap createLicenseMap() {
        ThirdPartyHelper tpHelper = new DefaultThirdPartyHelper( this.mavenProject, this.encoding, this.verbose,
                this.dependenciesTool, this.thirdPartyTool,
                Collections.singletonList(this.localRepository), this.remoteRepositories);

        return tpHelper.createLicenseMap(tpHelper.loadDependencies(
                new MavenProjectDependenciesConfiguratorImpl(),
                new ResolvedProjectDependencies(
                        mavenProject.getArtifacts(),
                        mavenProject.getDependencyArtifacts())));
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

        // try fallback to artifact baseVersion, (for example because a snapshot is locked )
        if(projectLicensesPair == null) {
            projectLicensesPair =  projectLookup.get(ComponentId.createFallback(artifact));
        }

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

        return builder.buildDependency();
    }

    private Artifact findProjectArtifact(Artifact other) {
        for(Artifact obj : mavenProject.getArtifacts()) {
            if ( obj == other ) {
                return obj;
            }

            if (obj.getGroupId().equals(other.getGroupId())
                        && obj.getArtifactId().equals(other.getArtifactId())
                        && obj.getVersion().equals(other.getVersion())
                        && obj.getType().equals(other.getType())) {

                String myClassifier = obj.getClassifier() == null ? "" : obj.getClassifier();
                String otherClassifier = other.getClassifier() == null ? "" : other.getClassifier();

                if (myClassifier.equals(otherClassifier)) {
                    return obj;
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
            MavenProject project = entry.getKey();
            ProjectFix.fixProject(project);
            projectLookup.put(ComponentId.create(project), entry);
        }

        return mapDependency(rootNode, projectLookup);
    }

    private void transferScan(RestClient api, Scan scan) throws MojoExecutionException {
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
                log.info(String.format("%s %s, %s", project.getId(), project.getName(),
                        Arrays.toString(licenses)));
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
        }
        return null;
    }

    private String getUserAgent() {
        String userAgent = "ecs-mvn-plugin/0.0";
        try {
            ProjectProperties properties = new ProjectProperties();
            userAgent = properties.getProperty("artifactId") + "/" + properties.getProperty("version");
        } catch(IOException ignored) { }

        return userAgent;
    }
}
