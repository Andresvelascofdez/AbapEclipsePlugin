package com.abap.assistant.eclipse;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;

public final class EclipseDotEnvLocator {
    private static final String PRIMARY_PROJECT_NAME = "com.abap.assistant";

    private EclipseDotEnvLocator() {
    }

    public static Path[] candidateDotEnvFiles() {
        Set<Path> candidates = new LinkedHashSet<>();
        addWorkspaceProjects(candidates, true);
        addWorkspaceProjects(candidates, false);
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

    private static void addUserDir(Set<Path> candidates) {
        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            candidates.add(Path.of(userDir, ".env"));
        }
    }
}
