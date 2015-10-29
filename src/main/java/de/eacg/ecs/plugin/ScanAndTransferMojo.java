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
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.codehaus.mojo.license.AbstractAddThirdPartyMojo;
import org.codehaus.mojo.license.api.MavenProjectDependenciesConfigurator;
import org.codehaus.mojo.license.model.LicenseMap;
import org.codehaus.mojo.license.utils.SortedProperties;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Initializes (if not skipped) 'ecs-maven-plugin' with goal
 * 'aggregate-add-third-party'. The file, created by this plugin is then scanned and the results a transferred to server
 */
@Mojo(name = "dependency-scan", defaultPhase = LifecyclePhase.DEPLOY,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ScanAndTransferMojo extends AbstractAddThirdPartyMojo
        implements MavenProjectDependenciesConfigurator {

    // ----------------------------------------------------------------------
    // Mojo Parameters
    // ----------------------------------------------------------------------
    /**
     * The name of the project, configured in central server.
     */
    @Parameter(property = "licenseScan.projectName", required = true)
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
     * Default: '${project.groupId}:${project.artifactId}:${project.version}'
     */
    @Parameter(property = "licenseScan.moduleId", defaultValue = "${project.groupId}:${project.artifactId}")
    private String moduleId;

    /**
     * The username used to authorize the transfer of dependency information to central server.
     */
    @Parameter(property = "licenseScan.userName", required = true)
    private String userName;

    /**
     * The API key to used to authorize the transfer of dependency information to central server.
     */
    @Parameter(property = "licenseScan.apiKey", required = true)
    private String apiKey;

    /**
     * The Baseurl to access central evaluation server.<br/>
     * <p/>
     * Default: 'http://localhost:3000'
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
     */
    @Parameter(property = "licenseScan.skip", defaultValue = "false")
    private boolean skip;

    /**
     * To skip transfer of the result.
     */
    @Parameter(property = "licenseScan.skipTransfer", defaultValue = "false")
    private boolean skipTransfer;

    /**
     * The scope to filter by when resolving the dependency tree, or <code>null</code> to include dependencies from
     * all scopes.
     *
     */
    @Parameter( property = "scope" )
    private String scope;

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
     * "org.acme;org.foo:foo.bar"
     *
     * default: groupId of the current project
     */
    @Parameter(property = "licenseScan.privateComponents", defaultValue = "${project.groupId}")
    private String privateComponents;

    /**
     * The dependency graph builder to use.
     */
    @Component( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Component
    private MavenProject mavenProject;

    @Override
    public boolean isSkip() {
        return skip;
    }

    @Override
    protected void doAction() throws Exception {

        LicenseMap licenseMap = getLicenseMap();


        Dependency dependency = null;
        try {
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
            dependency = createDependencyTree(rootNode, myOwnMap.entrySet());
        } catch (Exception e) {
            getLog().error("License collection failed", e);
            throw new MojoExecutionException("Exception while parsing license file", e);
        }
        try {
            Scan scan = new Scan(projectName, moduleName, moduleId, dependency);

            RestApi restApi = new RestApi(baseUrl, apiPath, apiKey, userName, basicAuthUser, basicAuthPasswd);
            if(skipTransfer) {
                getLog().info("Skipping rest transfer");
            } else {
                transferScan(restApi, scan);
            }
        } catch (Exception e) {
            getLog().error("Calling Rest API failed", e);
            throw new MojoExecutionException("Exception while calling Rest API", e);
        }
    }

    @Override
    protected SortedProperties createUnsafeMapping() {
        return new SortedProperties(getEncoding());
    }

    @Override
    protected SortedMap<String, MavenProject> loadDependencies() {
        return getHelper().loadDependencies(this);
    }

    // ----------------------------------------------------------------------
    // MavenProjectDependenciesConfigurator Implementaton
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExcludedGroups() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIncludedGroups() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getExcludedScopes() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getIncludedScopes() {
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExcludedArtifacts() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIncludedArtifacts() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIncludeTransitiveDependencies() {
        return true;
    }

    private String[] privateComponentArr = null;
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


    private Dependency mapDependency(DependencyNode node, Map<String, Map.Entry<MavenProject, String[]>> projectLookup) {
        Dependency.Builder builder = new Dependency.Builder();
        Artifact artifact = node.getArtifact();
        Map.Entry<MavenProject, String[]> projectLicensesPair = projectLookup.get(createId(artifact));

        if(projectLicensesPair == null) {
            getLog().error("Something weird happend: no Project found for artifact: " + createId(artifact));
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
        try{
            File file = artifact.getFile();
            if(file != null) {
                builder.setChecksum("sha-1:" + ChecksumCreator.createChecksum(file));
            } else {
                Artifact af = findProjectArtifact(artifact);
                if(af != null && af.getFile() != null) {
                    builder.setChecksum("sha-1:" + ChecksumCreator.createChecksum(af.getFile()));
                } else {
                    getLog().warn("Could not generate checksum - no file specified: " + createId(artifact));
                }
            }
        }catch(NoSuchAlgorithmException | IOException e) {
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

    Artifact findProjectArtifact(Artifact other) {
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
                    String otherClasifier = other.getClassifier() == null ? "" : other.getClassifier();

                    if (myClassifier.equals(otherClasifier)) {
                        return self;
                    }
                }
            }
        }
        return null;
    }

    private Dependency createDependencyTree(
            DependencyNode rootNode,
            Set<Map.Entry<MavenProject, String[]>> projectsAndLicenseSet) throws IOException {

        Map<String, Map.Entry<MavenProject, String[]>> projectLookup = new HashMap<>();
        for(Map.Entry<MavenProject, String[]> entry : projectsAndLicenseSet) {
            projectLookup.put(createId(entry.getKey()), entry);
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
        if (log.isInfoEnabled()) {
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

    /**
     * Gets the artifact filter to use when resolving the dependency tree.
     *
     * @return the artifact filter
     */
    private ArtifactFilter createResolvingArtifactFilter() {
        ArtifactFilter filter;

        // filter scope
        if ( scope != null )
        {
            getLog().debug( "+ Resolving dependency tree for scope '" + scope + "'" );

            filter = new ScopeArtifactFilter( scope );
        }
        else
        {
            filter = null;
        }

        return filter;
    }

    private static String createId(Artifact artifact) {
        StringBuilder id = new StringBuilder( 64 );

        id.append((artifact.getGroupId() == null) ? "[inherited]" : artifact.getGroupId());
        id.append(":");
        id.append(artifact.getArtifactId());
        id.append(":");
        id.append((artifact.getVersion() == null ) ? "[inherited]" : artifact.getVersion());

        return id.toString();
    }

    private static String createId(MavenProject project) {
        StringBuilder id = new StringBuilder( 64 );
        Model model = project.getModel();



        id.append((model.getGroupId() == null) ? "[inherited]" : model.getGroupId());
        id.append(":");
        id.append(model.getArtifactId());
        id.append(":");
        id.append((model.getVersion() == null ) ? "[inherited]" : model.getVersion());

        return id.toString();
    }
}
