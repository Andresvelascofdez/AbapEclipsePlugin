$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$buildRoot = Join-Path $root "build"
$classes = Join-Path $buildRoot "test-classes"
$sourcesFile = Join-Path $buildRoot "sources.txt"

if (Test-Path -LiteralPath $classes) {
    $resolvedClasses = Resolve-Path $classes
    if (-not $resolvedClasses.Path.StartsWith($root.Path, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside the project: $resolvedClasses"
    }
    Remove-Item -LiteralPath $classes -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $classes | Out-Null

$coreSources = Get-ChildItem -Path (Join-Path $root "src\com\anvel\abapeclipseassistant\core") -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$cliSources = Get-ChildItem -Path (Join-Path $root "src\com\anvel\abapeclipseassistant\cli") -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$testSources = Get-ChildItem -Path (Join-Path $root "test") -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$allSources = @($coreSources + $cliSources + $testSources)

if ($allSources.Count -eq 0) {
    throw "No Java sources found."
}

[System.IO.File]::WriteAllLines($sourcesFile, $allSources, [System.Text.UTF8Encoding]::new($false))

& javac -encoding UTF-8 --release 17 -d $classes "@$sourcesFile"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& java -cp $classes com.anvel.abapeclipseassistant.core.AssistantCoreTest
if ($LASTEXITCODE -ne 0) {
    throw "Core tests failed with exit code $LASTEXITCODE"
}

[xml]$pluginXml = Get-Content -Path (Join-Path $root "plugin.xml")
$view = $pluginXml.plugin.extension.view
if ($view.id -ne "com.anvel.abapeclipseassistant.views.assistant") {
    throw "plugin.xml does not expose the expected assistant view id."
}
if ($view.class -ne "com.anvel.abapeclipseassistant.ui.AssistantView") {
    throw "plugin.xml does not point to the expected AssistantView class."
}

$manifest = Get-Content -Path (Join-Path $root "META-INF\MANIFEST.MF") -Raw
if ($manifest -notmatch "Bundle-SymbolicName: com\.anvel\.abapeclipseassistant") {
    throw "Manifest does not contain the expected Bundle-SymbolicName."
}

$scannedFiles = Get-ChildItem -Path $root -Recurse -File |
    Where-Object {
        $_.FullName -notmatch "\\.git\\" -and
        $_.FullName -notmatch "\\build\\" -and
        $_.Name -ne ".env"
    }

foreach ($file in $scannedFiles) {
    $content = Get-Content -Path $file.FullName -Raw -ErrorAction SilentlyContinue
    if ($content -match "sk-(?:proj-)?[A-Za-z0-9_-]{20,}") {
        throw "Potential OpenAI API key found in $($file.FullName)"
    }
}

Write-Host "Validation completed successfully."
