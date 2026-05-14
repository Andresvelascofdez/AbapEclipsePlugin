package com.abap.assistant.eclipse;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public final class EclipseDotEnvLocator {
    private static final String PRIMARY_PROJECT_NAME = "com.abap.assistant";

    private EclipseDotEnvLocator() {
    }

    public static Path[] candidateDotEnvFiles() {
        Set<Path> candidates = new LinkedHashSet<>();
        addWorkspaceProjects(candidates, true);
        addWorkspaceProjects(candidates, false);
        addConfiguredEnvDirectory(candidates);
        addBundleLocation(candidates);
        addCodeSource(candidates);
        addDropinsDirectory(candidates);
        addWorkspaceRoot(candidates);
        addUserDir(candidates);
        return candidates.toArray(new Path[0]);
    }

    public static List<String> describeCandidates() {
        List<String> descriptions = new ArrayList<>();
        for (Path path : candidateDotEnvFiles()) {
            descriptions.add(path.toAbsolutePath().toString());
        }
        return descriptions;
    }

    private static void addWorkspaceProjects(Set<Path> candidates, boolean primaryOnly) {
        try {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            for (IProject project : root.getProjects()) {
                if (primaryOnly != PRIMARY_PROJECT_NAME.equals(project.getName())) {
                    continue;
                }
                addProjectLocation(candidates, project);
            }
        } catch (IllegalStateException exception) {
            // Workspace is not available yet.
        }
    }

    private static void addProjectLocation(Set<Path> candidates, IProject project) {
        if (project == null || !project.exists()) {
            return;
        }

        IPath location = project.getLocation();
        if (location != null) {
            candidates.add(Path.of(location.toOSString(), ".env"));
            return;
        }

        URI locationUri = project.getLocationURI();
        if (locationUri != null && "file".equalsIgnoreCase(locationUri.getScheme())) {
            candidates.add(Path.of(locationUri).resolve(".env"));
        }
    }

    private static void addWorkspaceRoot(Set<Path> candidates) {
        try {
            IPath location = ResourcesPlugin.getWorkspace().getRoot().getLocation();
            if (location != null) {
                candidates.add(Path.of(location.toOSString(), ".env"));
            }
        } catch (IllegalStateException exception) {
            // Workspace is not available yet.
        }
    }

    private static void addBundleLocation(Set<Path> candidates) {
        try {
            Bundle bundle = FrameworkUtil.getBundle(EclipseDotEnvLocator.class);
            if (bundle == null) {
                return;
            }

            URL entry = bundle.getEntry("/");
            if (entry != null) {
                addDotEnvNear(candidates, Path.of(FileLocator.toFileURL(entry).toURI()));
            }
            addBundleLocationString(candidates, bundle.getLocation());
        } catch (Exception exception) {
            // Bundle location discovery is a best-effort fallback.
        }
    }

    private static void addBundleLocationString(Set<Path> candidates, String location) {
        if (location == null || location.isBlank()) {
            return;
        }

        String normalized = location;
        int atIndex = normalized.indexOf('@');
        if (atIndex >= 0 && atIndex + 1 < normalized.length()) {
            normalized = normalized.substring(atIndex + 1);
        }
        if (normalized.startsWith("reference:")) {
            normalized = normalized.substring("reference:".length());
        }

        try {
            if (normalized.startsWith("file:")) {
                addDotEnvNear(candidates, Path.of(URI.create(normalized)));
            } else {
                addDotEnvNear(candidates, Path.of(normalized));
            }
        } catch (IllegalArgumentException exception) {
            // Some Equinox locations are not file-system paths.
        }
    }

    private static void addCodeSource(Set<Path> candidates) {
        try {
            CodeSource source = EclipseDotEnvLocator.class.getProtectionDomain().getCodeSource();
            if (source != null && source.getLocation() != null) {
                addDotEnvNear(candidates, Path.of(source.getLocation().toURI()));
            }
        } catch (Exception exception) {
            // Code source discovery is a best-effort fallback.
        }
    }

    private static void addConfiguredEnvDirectory(Set<Path> candidates) {
        String directory = firstPresent(System.getProperty("ABAP_ECLIPSE_ASSISTANT_ENV_DIR"), System.getenv("ABAP_ECLIPSE_ASSISTANT_ENV_DIR"));
        if (directory == null) {
            return;
        }

        try {
            candidates.add(Path.of(directory, ".env"));
        } catch (IllegalArgumentException exception) {
            // Ignore invalid directory override values.
        }
    }

    private static void addDropinsDirectory(Set<Path> candidates) {
        String dropins = System.getProperty("org.eclipse.equinox.p2.reconciler.dropins.directory");
        if (dropins == null || dropins.isBlank()) {
            return;
        }

        try {
            candidates.add(Path.of(dropins, ".env"));
        } catch (IllegalArgumentException exception) {
            // Ignore invalid dropins directory values.
        }
    }

    private static void addDotEnvNear(Set<Path> candidates, Path location) {
        if (location == null) {
            return;
        }

        Path base = location.toAbsolutePath().normalize();
        if (base.getFileName() != null && base.getFileName().toString().endsWith(".jar")) {
            base = base.getParent();
        }
        for (int depth = 0; base != null && depth < 3; depth++) {
            candidates.add(base.resolve(".env"));
            base = base.getParent();
        }
    }

    private static void addUserDir(Set<Path> candidates) {
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            candidates.add(Path.of(userDir, ".env"));
        }
    }

    private static String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.strip();
            }
        }
        return null;
    }
}
