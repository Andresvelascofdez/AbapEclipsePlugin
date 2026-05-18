param(
    [string]$EclipseHome = "C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse",
    [int]$TimeoutSeconds = 90,
    [string]$WorkspaceTemplate = "",
    [switch]$KeepPersistedState,
    [switch]$UseBundleEnv
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
$envProjectRoot = Join-Path $buildRoot "env-project"
$manifestContent = Get-Content -Path (Join-Path $root "META-INF\MANIFEST.MF") -Raw
$bundleVersionMatch = [regex]::Match($manifestContent, "(?m)^Bundle-Version:\s*([^\r\n]+)")
$bundleVersion = if ($bundleVersionMatch.Success) { $bundleVersionMatch.Groups[1].Value.Trim() -replace "\.qualifier$", "" } else { "0.0.0" }
$bundleVersionParts = $bundleVersion.Split(".")
$bundleVersionParts[$bundleVersionParts.Length - 1] = ([int]$bundleVersionParts[$bundleVersionParts.Length - 1] + 1).ToString()
$bundleVersionUpper = [string]::Join(".", $bundleVersionParts)
$productJar = Join-Path $dropins "com.abap.assistant_$bundleVersion.jar"
$smokeJar = Join-Path $dropins "com.abap.assistant.smoke_0.1.0.jar"
$expectedApiKey = if ($UseBundleEnv) { "smoke-bundle-key" } else { "smoke-test-key" }

if (Test-Path -LiteralPath $buildRoot) {
    $resolvedBuild = Resolve-Path $buildRoot
    if (-not $resolvedBuild.Path.StartsWith($root.Path, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the project: $resolvedBuild"
    }
    Remove-Item -LiteralPath $buildRoot -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $classes, $smokeClasses, $dropins, $workspace, $configuration, $envProjectRoot | Out-Null
$envFileRoot = if ($UseBundleEnv) { $workspace } else { $envProjectRoot }
[System.IO.File]::WriteAllText((Join-Path $envFileRoot ".env"), @"
OPENAI_API_KEY=$expectedApiKey
OPENAI_MODEL=gpt-5-mini
OPENAI_BASE_URL=https://api.openai.com/v1/responses
"@, [System.Text.UTF8Encoding]::new($false))

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
 org.eclipse.core.resources,
 org.eclipse.swt,
 org.eclipse.ui,
 com.abap.assistant;bundle-version="[$bundleVersion,$bundleVersionUpper)"
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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.abap.assistant.core.DotEnvLoader;
import com.abap.assistant.core.OpenAiSettings;
import com.abap.assistant.eclipse.EclipseDotEnvLocator;

import org.eclipse.swt.SWT;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public final class SmokeStartup implements IStartup {
    @Override
    public void earlyStartup() {
        String marker = System.getProperty("abap.assistant.smoke.marker");
        try {
            createSmokeProject();
            OpenAiSettings settings = OpenAiSettings.fromEnvironment(EclipseDotEnvLocator.candidateDotEnvFiles());
            String expectedApiKey = System.getProperty("abap.assistant.smoke.expectedApiKey");
            if (!expectedApiKey.equals(settings.apiKey())) {
                throw new IllegalStateException("Unexpected smoke API key source. " + describeDotEnvCandidates(expectedApiKey));
            }
        } catch (Throwable throwable) {
            writeFailure(marker, throwable);
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    PlatformUI.getWorkbench().close();
                }
            });
            return;
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
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
                    IViewPart view = page.showView("com.abap.assistant.ui.ChatView");
                    if (!"com.abap.assistant.ui.ChatView".equals(view.getClass().getName())) {
                        throw new IllegalStateException("Unexpected view class: " + view.getClass().getName());
                    }
                    if (!"com.abap.assistant.ui.ChatView".equals(view.getSite().getId())) {
                        throw new IllegalStateException("Unexpected view site id: " + view.getSite().getId());
                    }
                    assertConversationalUi(view);
                    Files.writeString(Path.of(marker), "PASS");
                } catch (Throwable throwable) {
                    writeFailure(marker, throwable);
                } finally {
                    PlatformUI.getWorkbench().close();
                }
            }
        });
    }

    private static void assertConversationalUi(IViewPart view) throws Exception {
        StyledText transcript = requiredField(view, "transcriptText", StyledText.class);
        org.eclipse.swt.widgets.Text composer = requiredField(view, "questionText", org.eclipse.swt.widgets.Text.class);
        Button askButton = requiredField(view, "askButton", Button.class);
        Button clearButton = requiredField(view, "clearButton", Button.class);
        Button copyResponseButton = requiredField(view, "copyResponseButton", Button.class);
        Button copyCodeButton = requiredField(view, "copyCodeButton", Button.class);
        Label statusLabel = requiredField(view, "statusLabel", Label.class);

        if (transcript.isDisposed()) {
            throw new IllegalStateException("Conversation transcript control is disposed.");
        }
        org.eclipse.swt.graphics.RGB foreground = transcript.getForeground().getRGB();
        org.eclipse.swt.graphics.RGB background = transcript.getBackground().getRGB();
        if (foreground.red < 180 || foreground.green < 180 || foreground.blue < 180) {
            throw new IllegalStateException("Conversation transcript foreground is not high contrast: " + foreground);
        }
        if (background.red > 90 || background.green > 90 || background.blue > 90) {
            throw new IllegalStateException("Conversation transcript background is not dark enough: " + background);
        }
        if (!transcript.getText().contains("Suggested code is copy-only")) {
            throw new IllegalStateException("Conversation transcript has no welcome/safety message.");
        }
        Method addUserMessage = view.getClass().getDeclaredMethod("addUserMessage", String.class, String.class);
        addUserMessage.setAccessible(true);
        addUserMessage.invoke(view, "smoke question", "Using 0 editor(s) | history 0");
        int userOffset = transcript.getText().lastIndexOf("smoke question");
        if (userOffset < 0) {
            throw new IllegalStateException("Smoke user message was not rendered in the transcript.");
        }
        int userLine = transcript.getLineAtOffset(userOffset);
        int userAlignment = transcript.getLineAlignment(userLine);
        if (userAlignment != SWT.RIGHT) {
            throw new IllegalStateException("User message line is not right-aligned.");
        }
        if (!"Ask about the open ABAP editors...".equals(composer.getMessage())) {
            throw new IllegalStateException("Bottom question composer placeholder was not found.");
        }
        if (!"Ask".equals(askButton.getText())) {
            throw new IllegalStateException("Ask button text is unexpected: " + askButton.getText());
        }
        if (!"Clear chat".equals(clearButton.getText())) {
            throw new IllegalStateException("Clear chat button text is unexpected: " + clearButton.getText());
        }
        if (!"Copy response".equals(copyResponseButton.getText())) {
            throw new IllegalStateException("Copy response button text is unexpected: " + copyResponseButton.getText());
        }
        if (!"Copy ABAP code".equals(copyCodeButton.getText())) {
            throw new IllegalStateException("Copy ABAP code button text is unexpected: " + copyCodeButton.getText());
        }
        if (statusLabel.getText() == null || statusLabel.getText().isBlank()) {
            throw new IllegalStateException("Status label is empty.");
        }
    }

    private static <T> T requiredField(IViewPart view, String name, Class<T> type) throws Exception {
        Field field = view.getClass().getDeclaredField(name);
        field.setAccessible(true);
        Object value = field.get(view);
        if (!type.isInstance(value)) {
            throw new IllegalStateException("Field " + name + " was not a " + type.getName() + ".");
        }
        return type.cast(value);
    }

    private static void createSmokeProject() throws Exception {
        String smokeProjectRoot = System.getProperty("abap.assistant.smoke.projectRoot");
        if (smokeProjectRoot == null || smokeProjectRoot.isBlank()) {
            return;
        }

        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("com.abap.assistant");
        if (!project.exists()) {
            IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription("com.abap.assistant");
            description.setLocation(new org.eclipse.core.runtime.Path(smokeProjectRoot));
            project.create(description, null);
        }
    }

    private static void writeFailure(String marker, Throwable throwable) {
        try {
            StringWriter writer = new StringWriter();
            throwable.printStackTrace(new PrintWriter(writer));
            Files.writeString(Path.of(marker), "FAIL" + System.lineSeparator() + writer);
        } catch (Exception ignored) {
            // Nothing useful can be done here; Eclipse log will contain the error.
        }
    }

    private static String describeDotEnvCandidates(String expectedApiKey) {
        StringBuilder builder = new StringBuilder("Candidates:");
        DotEnvLoader loader = new DotEnvLoader();
        for (Path candidate : EclipseDotEnvLocator.candidateDotEnvFiles()) {
            try {
                Map<String, String> values = loader.load(candidate);
                String value = values.get("OPENAI_API_KEY");
                builder.append(System.lineSeparator())
                    .append(candidate.toAbsolutePath())
                    .append(" exists=")
                    .append(Files.isRegularFile(candidate))
                    .append(" hasKey=")
                    .append(value != null && !value.isBlank())
                    .append(" matchesExpected=")
                    .append(expectedApiKey != null && expectedApiKey.equals(value));
            } catch (Exception exception) {
                builder.append(System.lineSeparator())
                    .append(candidate.toAbsolutePath())
                    .append(" unreadable");
            }
        }
        return builder.toString();
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
    "-Dabap.assistant.smoke.marker=$marker",
    "-Dabap.assistant.smoke.expectedApiKey=$expectedApiKey"
)
if ($UseBundleEnv) {
    $arguments += "-DABAP_ECLIPSE_ASSISTANT_ENV_DIR=$dropins"
}
if (-not $UseBundleEnv) {
    $arguments += "-Dabap.assistant.smoke.projectRoot=$envProjectRoot"
}
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
        "-Dabap.assistant.smoke.marker=$marker",
        "-Dabap.assistant.smoke.expectedApiKey=$expectedApiKey"
    )
    if ($UseBundleEnv) {
        $arguments += "-DABAP_ECLIPSE_ASSISTANT_ENV_DIR=$dropins"
    }
    if (-not $UseBundleEnv) {
        $arguments += "-Dabap.assistant.smoke.projectRoot=$envProjectRoot"
    }
}

$previousEnvDir = $env:ABAP_ECLIPSE_ASSISTANT_ENV_DIR
if ($UseBundleEnv) {
    $env:ABAP_ECLIPSE_ASSISTANT_ENV_DIR = $dropins
}
$process = Start-Process -FilePath $eclipseExe -ArgumentList $arguments -PassThru -WindowStyle Hidden -WorkingDirectory $eclipseHomePath
if ($UseBundleEnv) {
    if ($null -eq $previousEnvDir) {
        Remove-Item Env:\ABAP_ECLIPSE_ASSISTANT_ENV_DIR -ErrorAction SilentlyContinue
    } else {
        $env:ABAP_ECLIPSE_ASSISTANT_ENV_DIR = $previousEnvDir
    }
}
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
