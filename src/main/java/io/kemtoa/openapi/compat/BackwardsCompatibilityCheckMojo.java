package io.kemtoa.openapi.compat;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import io.kemtoa.openapi.compat.walker.OpenApiDiffWalker;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.kemtoa.openapi.compat.rules.AddedEnumValueInResponseRule;
import io.kemtoa.openapi.compat.rules.AddedRequiredRequestParameterRule;
import io.kemtoa.openapi.compat.rules.CompositeRule;
import io.kemtoa.openapi.compat.rules.ParameterLocationChangedRule;
import io.kemtoa.openapi.compat.rules.PropertyRemovedInResponseRule;
import io.kemtoa.openapi.compat.rules.PropertyTypeChangedRule;
import io.kemtoa.openapi.compat.rules.RemovedEnumValueInRequestRule;
import io.kemtoa.openapi.compat.rules.RemovedOperationRule;

/**
 * OpenAPI spec backwards compatibility check
 *
 * This Mojo enforces only backwards compatible change are made to the OpenAPI
 * specs in the {@link #openApiSourceDir} directory.
 * Only OpenAPI 3.0 specifications in the yaml format are supported.
 *
 * It works by keeping a copy of the last 'validated' specs document
 * in the {@link #openApiLockDir} directory. Each time the Mojo is executed,
 * the 'old' and the 'new' specs are compared for backwards incompatible
 * changes. If incompatible changes are found, the execution is failed.
 * Otherwise the execution succeeds and the 'new' spec is copied to
 * the {@link #openApiLockDir} directory and thus becomes the 'old' spec
 * for the next Mojo execution.
 */
@Mojo(
    name = "backwards-compatibility-check",
    defaultPhase = LifecyclePhase.VERIFY,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true
)
public class BackwardsCompatibilityCheckMojo extends AbstractMojo {

    @Parameter(defaultValue = "${basedir}/src/main/openapi")
    private String openApiSourceDir;

    @Parameter(defaultValue = "${basedir}/src/main/openapi")
    private String openApiLockDir;

    @Parameter(property = "skipOpenApiCheck")
    private boolean skipOpenApiCheck;

    private static class OpenApiGroup {
        private String name;
        private Path specPath;
        private Path lockPath;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Path openApiSourcePath = Paths.get(openApiSourceDir);
            Path openApiLockPath = Paths.get(openApiLockDir);

            if (skipOpenApiCheck) {
                getLog().info("The OpenAPI backwards compatibility check is skipped.");
                return;
            }

            if (!Files.exists(openApiSourcePath)) {
                getLog().info("The OpenAPI source directory does not exist '" + openApiSourceDir + "', skipping.");
                return;
            }

            Files.createDirectories(openApiLockPath);

            Map<String, OpenApiGroup> openApiGroups = loadOpenApiGroups(openApiSourcePath, openApiLockPath);

            for (OpenApiGroup group : openApiGroups.values()) {
                checkOpenApiGroupBackwardsCompatibility(group);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("An error occurred while running the OpenApi compatibility check", e);
        }
    }

    private Map<String, OpenApiGroup> loadOpenApiGroups(Path openApiSourcePath, Path openApiLockPath) throws IOException {
        Map<String, OpenApiGroup> openApiGroups = new HashMap<>();

        try (DirectoryStream<Path> ymlFilesStream = Files.newDirectoryStream(openApiSourcePath, "*.yml")) {
            for (Path specPath : ymlFilesStream) {
                OpenApiGroup group = new OpenApiGroup();
                group.name = FilenameUtils.removeExtension(specPath.getFileName().toString());
                group.specPath = specPath;

                openApiGroups.put(group.name, group);
            }
        }

        try (DirectoryStream<Path> lockFilesStream = Files.newDirectoryStream(openApiLockPath, "*.lock")) {
            for (Path lockPath : lockFilesStream) {
                String name = FilenameUtils.removeExtension(lockPath.getFileName().toString());

                openApiGroups.computeIfAbsent(name, (n) -> new OpenApiGroup());

                OpenApiGroup group = openApiGroups.get(name);
                group.name = name;
                group.lockPath = lockPath;
            }
        }

        return openApiGroups;
    }

    private void checkOpenApiGroupBackwardsCompatibility(OpenApiGroup group) throws IOException, MojoFailureException {
        if (group.specPath == null) {
            getLog().warn("Found a .lock file without a corresponding .yml file: " + group.lockPath.toString());

            throw new MojoFailureException("Backwards compatibility check failed for group " + group.name);
        }

        if (group.lockPath == null) {
            group.lockPath = Paths.get(openApiLockDir, group.name + ".lock");
            Files.createDirectories(Paths.get(openApiLockDir));
            Files.copy(group.specPath, group.lockPath);
            getLog().info("Initialized compatibility check for group '" + group.name + "'.");
            return;
        }

        OpenAPI openApiOld = new OpenAPIV3Parser().read(group.lockPath.toAbsolutePath().toString());
        if (openApiOld == null) {
            throw new MojoFailureException("Unable to parse OpenAPI lock file: " + group.lockPath);
        }

        OpenAPI openApiNew = new OpenAPIV3Parser().read(group.specPath.toAbsolutePath().toString());
        if (openApiNew == null) {
            throw new MojoFailureException("Unable to parse OpenAPI spec: " + group.specPath);
        }

        CompositeRule rules = new CompositeRule(
                new AddedEnumValueInResponseRule(),
                new AddedRequiredRequestParameterRule(),
                new ParameterLocationChangedRule(),
                new PropertyRemovedInResponseRule(),
                new PropertyTypeChangedRule(),
                new RemovedEnumValueInRequestRule(),
                new RemovedOperationRule()
        );

        OpenApiDiffWalker walker = new OpenApiDiffWalker();
        walker.walk(rules, openApiOld, openApiNew);

        if (!rules.getErrors().isEmpty()) {

            getLog().error("Backwards incompatible changes were found for group '" + group.name + "':");

            for (String error : rules.getErrors()) {
                getLog().error(error);
            }

            throw new MojoFailureException("Backwards compatibility check failed for group " + group.name);
        } else {
            Files.createDirectories(Paths.get(openApiLockDir));
            Files.copy(group.specPath, group.lockPath, StandardCopyOption.REPLACE_EXISTING);
            getLog().info("Backwards compatibility check passed for group '" + group.name + "'.");
        }
    }
}
