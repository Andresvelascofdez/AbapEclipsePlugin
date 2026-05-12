param(
    [string]$EclipseHome = "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse",
    [int]$TimeoutSeconds = 90,
    [string]$WorkspaceTemplate = "",
    [switch]$KeepPersistedState
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

$buildRoot = Join-Path $root "build\eclipse-smoke"
$classes = Join-Path $buildRoot "classes"
$smokeClasses = Join-Path $buildRoot "smoke-classes"
$dropins = Join-Path $buildRoot "dropins"
$workspace = Join-Path $buildRoot "workspace"
$configuration = Join-Path $buildRoot "configuration"
$sourcesFile = Join-Path $buildRoot "sources.txt"
$smokeSourcesFile = Join-Path $buildRoot "smoke-sources.txt"
$marker = Join-Path $buildRoot "marker.txt"
$productJar = Join-Path $dropins "com.abap.assistant_0.1.0.jar"
$smokeJar = Join-Path $dropins "com.abap.assistant.smoke_0.1.0.jar"

if (Test-Path -LiteralPath $buildRoot) {
    $resolvedBuild = Resolve-Path $buildRoot
    if (-not $resolvedBuild.Path.StartsWith($root.Path, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the project: $resolvedBuild"
    }
    Remove-Item -LiteralPath $buildRoot -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $classes, $smokeClasses, $dropins, $workspace, $configuration | Out-Null

if (-not [string]::IsNullOrWhiteSpace($WorkspaceTemplate)) {
    $templatePath = Resolve-Path $WorkspaceTemplate
    Copy-Item -Path (Join-Path $templatePath "*") -Destination $workspace -Recurse -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath (Join-Path $workspace ".metadata\.log") -Force -ErrorAction SilentlyContinue
    Get-ChildItem -Path (Join-Path $workspace ".metadata") -Filter ".bak_*.log" -File -ErrorAction SilentlyContinue |
        Remove-Item -Force -ErrorAction SilentlyContinue
}

$allSources = Get-ChildItem -Path (Join-Path $root "src") -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
[System.IO.File]::WriteAllLines($sourcesFile, $allSources, [System.Text.UTF8Encoding]::new($false))

& javac -encoding UTF-8 --release 11 -cp (Join-Path $plugins "*") -d $classes "@$sourcesFile"
if ($LASTEXITCODE -ne 0) {
    throw "Eclipse plug-in compilation failed with exit code $LASTEXITCODE"
}

& jar --create --file $productJar --manifest (Join-Path $root "META-INF\MANIFEST.MF") -C $classes . -C $root plugin.xml -C $root icons
if ($LASTEXITCODE -ne 0) {
    throw "Product plug-in jar packaging failed with exit code $LASTEXITCODE"
}

$smokeRoot = Join-Path $buildRoot "smoke-src"
$smokePackage = Join-Path $smokeRoot "com\abap\assistant\smoke"
$smokeMetaInf = Join-Path $smokeRoot "META-INF"
New-Item -ItemType Directory -Force -Path $smokePackage, $smokeMetaInf | Out-Null

[System.IO.File]::WriteAllText((Join-Path $smokeMetaInf "MANIFEST.MF"), @"
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: ABAP Assistant Smoke Test
Bundle-SymbolicName: com.abap.assistant.smoke;singleton:=true
Bundle-Version: 0.1.0
Bundle-RequiredExecutionEnvironment: JavaSE-11
Require-Bundle: org.eclipse.core.runtime,
 org.eclipse.ui,
 com.abap.assistant
Bundle-ActivationPolicy: lazy

"@, [System.Text.UTF8Encoding]::new($false))

[System.IO.File]::WriteAllText((Join-Path $smokeRoot "plugin.xml"), @"
<?xml version="1.0" encoding="UTF-8"?>
<?eclipse version="3.4"?>
<plugin>
    <extension point="org.eclipse.ui.startup">
        <startup class="com.abap.assistant.smoke.SmokeStartup"/>
    </extension>
</plugin>
"@, [System.Text.UTF8Encoding]::new($false))

[System.IO.File]::WriteAllText((Join-Path $smokePackage "SmokeStartup.java"), @"
package com.abap.assistant.smoke;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class SmokeStartup implements IStartup {
    @Override
    public void earlyStartup() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                String marker = System.getProperty("abap.assistant.smoke.marker");
                try {
                    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    if (window == null && PlatformUI.getWorkbench().getWorkbenchWindows().length > 0) {
                        window = PlatformUI.getWorkbench().getWorkbenchWindows()[0];
                    }
                    if (window == null) {
                        throw new IllegalStateException("No workbench window available.");
                    }
                    IWorkbenchPage page = window.getActivePage();
                    if (page == null) {
                        throw new IllegalStateException("No active workbench page available.");
                    }
                    page.showView("com.abap.assistant.ui.ChatView");
                    Files.writeString(Path.of(marker), "PASS");
                } catch (Throwable throwable) {
                    try {
                        StringWriter writer = new StringWriter();
                        throwable.printStackTrace(new PrintWriter(writer));
                        Files.writeString(Path.of(marker), "FAIL" + System.lineSeparator() + writer);
                    } catch (Exception ignored) {
                        // Nothing useful can be done here; Eclipse log will contain the error.
                    }
                } finally {
                    PlatformUI.getWorkbench().close();
                }
            }
        });
    }
}
"@, [System.Text.UTF8Encoding]::new($false))

$smokeSources = Get-ChildItem -Path $smokeRoot -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
[System.IO.File]::WriteAllLines($smokeSourcesFile, $smokeSources, [System.Text.UTF8Encoding]::new($false))
& javac -encoding UTF-8 --release 11 -cp ((Join-Path $plugins "*") + ";" + $productJar) -d $smokeClasses "@$smokeSourcesFile"
if ($LASTEXITCODE -ne 0) {
    throw "Smoke plug-in compilation failed with exit code $LASTEXITCODE"
}

& jar --create --file $smokeJar --manifest (Join-Path $smokeMetaInf "MANIFEST.MF") -C $smokeClasses . -C $smokeRoot plugin.xml
if ($LASTEXITCODE -ne 0) {
    throw "Smoke plug-in jar packaging failed with exit code $LASTEXITCODE"
}

$arguments = @(
    "-nosplash",
    "-consoleLog",
    "-clean",
    "-data", $workspace,
    "-configuration", $configuration,
    "-application", "org.eclipse.ui.ide.workbench",
    "-vmargs",
    "-Dorg.eclipse.equinox.p2.reconciler.dropins.directory=$dropins",
    "-Dabap.assistant.smoke.marker=$marker"
)
if (-not $KeepPersistedState) {
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
        "-Dabap.assistant.smoke.marker=$marker"
    )
}

$process = Start-Process -FilePath $eclipseExe -ArgumentList $arguments -PassThru -WindowStyle Hidden
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
    throw "Eclipse smoke test timed out after $TimeoutSeconds seconds."
}

if (-not (Test-Path -LiteralPath $marker)) {
    $logPath = Join-Path $workspace ".metadata\.log"
    if (Test-Path -LiteralPath $logPath) {
        Get-Content -Path $logPath -Tail 200
    }
    throw "Eclipse smoke marker was not created."
}

$markerContent = Get-Content -Path $marker -Raw
if (-not $markerContent.StartsWith("PASS")) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    throw "Eclipse smoke test failed: $markerContent"
}

$workspaceLog = Join-Path $workspace ".metadata\.log"
if (Test-Path -LiteralPath $workspaceLog) {
    $pluginLog = Select-String -Path $workspaceLog -Pattern "com\.abap\.assistant|ChatView|abap_icon|Could not create the view|Unable to resolve plug-in `"com.abap.assistant`"" -ErrorAction SilentlyContinue
    $errors = $pluginLog | Where-Object { $_.Line -match "Could not create the view|Unable to resolve plug-in|Invalid input url|Exception|ERROR" }
    if ($errors) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
        $errors | Select-Object -First 50
        throw "Eclipse smoke test found ABAP Assistant errors in the workspace log."
    }
}

if (-not $process.HasExited) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
}

Write-Host "Eclipse smoke test passed."
