param(
    [string]$EclipseHome = "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse",
    [int]$TimeoutSeconds = 120
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$eclipseHomePath = Resolve-Path $EclipseHome
$plugins = Join-Path $eclipseHomePath "plugins"
$eclipseExe = Join-Path $eclipseHomePath "eclipsec.exe"
if (-not (Test-Path -LiteralPath $eclipseExe)) {
    $eclipseExe = Join-Path $eclipseHomePath "eclipse.exe"
}

& (Join-Path $PSScriptRoot "check-eclipse-prereqs.ps1") -EclipseHome $eclipseHomePath

$buildRoot = Join-Path $root "build\eclipse-project-build"
$projectCopy = Join-Path $buildRoot "project"
$smokeRoot = Join-Path $buildRoot "smoke-src"
$smokeClasses = Join-Path $buildRoot "smoke-classes"
$smokePackage = Join-Path $smokeRoot "com\abap\assistant\projectbuild"
$smokeMetaInf = Join-Path $smokeRoot "META-INF"
$dropins = Join-Path $buildRoot "dropins"
$workspace = Join-Path $buildRoot "workspace"
$configuration = Join-Path $buildRoot "configuration"
$sourcesFile = Join-Path $buildRoot "smoke-sources.txt"
$marker = Join-Path $buildRoot "marker.txt"
$smokeJar = Join-Path $dropins "com.abap.assistant.projectbuild_0.1.0.jar"

if (Test-Path -LiteralPath $buildRoot) {
    $resolvedBuild = Resolve-Path $buildRoot
    if (-not $resolvedBuild.Path.StartsWith($root.Path, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the project: $resolvedBuild"
    }
    Remove-Item -LiteralPath $buildRoot -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $projectCopy, $smokeClasses, $smokePackage, $smokeMetaInf, $dropins, $workspace, $configuration | Out-Null
Copy-Item -LiteralPath (Join-Path $root ".classpath") -Destination $projectCopy
Copy-Item -LiteralPath (Join-Path $root ".project") -Destination $projectCopy
Copy-Item -LiteralPath (Join-Path $root "build.properties") -Destination $projectCopy
Copy-Item -LiteralPath (Join-Path $root "plugin.xml") -Destination $projectCopy
Copy-Item -LiteralPath (Join-Path $root "META-INF") -Destination $projectCopy -Recurse
Copy-Item -LiteralPath (Join-Path $root ".settings") -Destination $projectCopy -Recurse
Copy-Item -LiteralPath (Join-Path $root "icons") -Destination $projectCopy -Recurse
Copy-Item -LiteralPath (Join-Path $root "src") -Destination $projectCopy -Recurse

[System.IO.File]::WriteAllText((Join-Path $smokeMetaInf "MANIFEST.MF"), @"
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: ABAP Assistant Project Build Smoke Test
Bundle-SymbolicName: com.abap.assistant.projectbuild;singleton:=true
Bundle-Version: 0.1.0
Bundle-RequiredExecutionEnvironment: JavaSE-11
Require-Bundle: org.eclipse.core.runtime,
 org.eclipse.core.resources,
 org.eclipse.ui
Bundle-ActivationPolicy: lazy

"@, [System.Text.UTF8Encoding]::new($false))

[System.IO.File]::WriteAllText((Join-Path $smokeRoot "plugin.xml"), @"
<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>
    <extension point="org.eclipse.ui.startup">
        <startup class="com.abap.assistant.projectbuild.ProjectBuildStartup"/>
    </extension>
</plugin>
"@, [System.Text.UTF8Encoding]::new($false))

[System.IO.File]::WriteAllText((Join-Path $smokePackage "ProjectBuildStartup.java"), @"
package com.abap.assistant.projectbuild;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

public final class ProjectBuildStartup implements IStartup {
    @Override
    public void earlyStartup() {
        String marker = System.getProperty("abap.assistant.projectbuild.marker");
        try {
            String projectRoot = System.getProperty("abap.assistant.projectbuild.projectRoot");
            if (projectRoot == null || projectRoot.isBlank()) {
                throw new IllegalStateException("Missing project root.");
            }
            IProjectDescription description = ResourcesPlugin.getWorkspace()
                .loadProjectDescription(new org.eclipse.core.runtime.Path(projectRoot).append(".project"));
            description.setLocation(new org.eclipse.core.runtime.Path(projectRoot));
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
            if (!project.exists()) {
                project.create(description, null);
            }
            if (!project.isOpen()) {
                project.open(null);
            }
            project.build(IncrementalProjectBuilder.FULL_BUILD, null);

            IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            StringBuilder errors = new StringBuilder();
            for (IMarker markerItem : markers) {
                Object severity = markerItem.getAttribute(IMarker.SEVERITY);
                if (Integer.valueOf(IMarker.SEVERITY_ERROR).equals(severity)) {
                    errors.append(markerItem.getResource().getProjectRelativePath())
                        .append(":")
                        .append(markerItem.getAttribute(IMarker.LINE_NUMBER, -1))
                        .append(" ")
                        .append(markerItem.getAttribute(IMarker.MESSAGE, ""))
                        .append(System.lineSeparator());
                }
            }
            if (errors.length() > 0) {
                throw new IllegalStateException(errors.toString());
            }
            Files.writeString(Path.of(marker), "PASS");
        } catch (Throwable throwable) {
            writeFailure(marker, throwable);
        } finally {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    PlatformUI.getWorkbench().close();
                }
            });
        }
    }

    private static void writeFailure(String marker, Throwable throwable) {
        try {
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));
            Files.writeString(Path.of(marker), "FAIL" + System.lineSeparator() + writer);
        } catch (Exception ignored) {
            // Eclipse log will contain the error if the marker cannot be written.
        }
    }
}
"@, [System.Text.UTF8Encoding]::new($false))

$smokeSources = Get-ChildItem -Path $smokeRoot -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
[System.IO.File]::WriteAllLines($sourcesFile, $smokeSources, [System.Text.UTF8Encoding]::new($false))
& javac -encoding UTF-8 --release 11 -cp (Join-Path $plugins "*") -d $smokeClasses "@$sourcesFile"
if ($LASTEXITCODE -ne 0) {
    throw "Project build smoke plug-in compilation failed with exit code $LASTEXITCODE"
}

& jar --create --file $smokeJar --manifest (Join-Path $smokeMetaInf "MANIFEST.MF") -C $smokeClasses . -C $smokeRoot plugin.xml
if ($LASTEXITCODE -ne 0) {
    throw "Project build smoke plug-in packaging failed with exit code $LASTEXITCODE"
}

$arguments = @(
    "-nosplash",
    "-consoleLog",
    "-clean",
    "-clearPersistedState",
    "-data", $workspace,
    "-configuration", $configuration,
    "-application", "org.eclipse.ui.ide.workbench",
    "-vmargs",
    "-Dorg.eclipse.equinox.p2.reconciler.dropins.directory=$dropins",
    "-Dabap.assistant.projectbuild.marker=$marker",
    "-Dabap.assistant.projectbuild.projectRoot=$projectCopy"
)

$process = Start-Process -FilePath $eclipseExe -ArgumentList $arguments -PassThru -WindowStyle Hidden -WorkingDirectory $eclipseHomePath
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt $deadline) {
    if (Test-Path -LiteralPath $marker) {
        break
    }
    if ($process.HasExited) {
        break
    }
    Start-Sleep -Milliseconds 500
}

if (-not (Test-Path -LiteralPath $marker)) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    throw "Eclipse project build smoke test timed out after $TimeoutSeconds seconds."
}

$markerContent = Get-Content -Path $marker -Raw
if (-not $markerContent.StartsWith("PASS")) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    throw "Eclipse project build smoke test failed: $markerContent"
}

if (-not $process.HasExited) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
}

Write-Host "Eclipse project build smoke test passed."
