package de.eacg.ecs.plugin;

import de.eacg.ecs.client.CheckResults;
import de.eacg.ecs.client.Dependency;
import de.eacg.ecs.client.RestClient;
import de.eacg.ecs.client.Scan;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.List;

@Mojo(name = "dependency-check", defaultPhase = LifecyclePhase.DEPLOY,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ScanAndCheckMojo extends ScanAndTransferMojo {

    /**
     * Allow to break a build<br/>
     * <p/>
     * Default: 'true'
     */
    @Parameter(property = "licenseCheck.allowBreakBuild", defaultValue = "true")
    private boolean allowBreakBuild;

    /**
     * Allow to break on legal issues<br/>
     * <p/>
     * Default: 'true'
     */
    @Parameter(property = "licenseCheck.breakOnLegalIssues", defaultValue = "true")
    private boolean breakOnLegalIssues;

    /**
     * Allow to break on vulnerabilities<br/>
     * <p/>
     * Default: 'true'
     */
    @Parameter(property = "licenseCheck.breakOnVulnerabilities", defaultValue = "true")
    private boolean breakOnVulnerabilities;

    /**
     * Allow to break on violations only<br/>
     * <p/>
     * Default: 'true'
     */
    @Parameter(property = "licenseCheck.breakOnViolationsOnly", defaultValue = "true")
    private boolean breakOnViolationsOnly;

    /**
     * Allow to break on violations and warnings<br/>
     * <p/>
     * Default: 'true'
     */
    @Parameter(property = "licenseCheck.breakOnViolationsAndWarnings", defaultValue = "false")
    private boolean breakOnViolationsAndWarnings;

    /**
     * By checking licenses assume that components are modified<br/>
     * <p/>
     * Default: 'false'
     */
    @Parameter(property = "licenseCheck.assumeComponentsModified", defaultValue = "false")
    private boolean assumeComponentsModified;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();

        Dependency dependency = createDependency();
        CheckResults results = null;


        Scan scan = createScan(dependency);
        RestClient restClient = createRestClient();

        try {
            results = restClient.checkScan(scan);
        } catch (RestClient.RestClientException e) {
            getLog().error(e.getMessage());
        }

        if (restClient.getResponseStatus() == 200) {
            assert results != null;
            evaluateResults(results);
        }
    }


    private void evaluateResults(CheckResults results) throws MojoFailureException {

        for (CheckResults.Warning w : results.getWarnings()) {
            String cStr = w.getComponent();
            String vStr = w.getVersion();

            String msg = String.format("Component \"%s %s\"", cStr != null ? cStr : "", vStr != null ? vStr : "");

            if (w.isComponentNotFound()) {
                getLog().warn(msg + " component not found");
            }

            if (w.isVersionNotFound()) {
                getLog().warn(msg + " version not found");
            }

            if (w.isLicenseNotFound()) {
                getLog().warn(msg + " license not found");
            }
        }

        if (!allowBreakBuild) {
            return;
        }

        if (breakOnLegalIssues) {
            int violations = 0;
            int warnings = 0;

            for (CheckResults.Result result : results.getData()) {
                String msg = result.getComponent().getName() + " " + result.getComponent().getVersion();
                List<CheckResults.Violation> legalViolations;
                if (assumeComponentsModified) {
                    legalViolations = result.getChanged().getViolations();
                } else {
                    legalViolations = result.getNot_changed().getViolations();
                }

                for (CheckResults.Violation v : legalViolations) {
                    if (v.isViolation()) {
                        getLog().error(msg + ": " + v.getMessage());
                        violations++;
                    } else if (v.isWarning()) {
                        getLog().warn(msg + ": " + v.getMessage());
                        warnings++;
                    }
                }
            }

            if (breakOnViolationsAndWarnings && (warnings > 0 || violations > 0)) {
                throw new MojoFailureException("Found legal violations");
            }

            if (breakOnViolationsOnly && (violations > 0)) {
                throw new MojoFailureException("Found legal violations");
            }

        }

        if (breakOnVulnerabilities) {
            int violations = 0;
            int warnings = 0;

            for (CheckResults.Result result : results.getData()) {
                String componentStr = result.getComponent().getName() + " " + result.getComponent().getVersion();
                for (CheckResults.Vulnerabilities v : result.getVulnerabilities()) {
                    String msg = componentStr + ": [" + v.getName() + "] " + v.getDescription();
                    if (v.isViolation()) {
                        getLog().error(msg);
                        violations++;
                    } else if (v.isWarning()) {
                        getLog().warn(msg);
                        warnings++;
                    }
                }
            }

            if (breakOnViolationsAndWarnings && (warnings > 0 || violations > 0)) {
                throw new MojoFailureException("Found vulnerabilities");
            }

            if (breakOnViolationsOnly && (violations > 0)) {
                throw new MojoFailureException("Found vulnerabilities");
            }

        }
    }

}
