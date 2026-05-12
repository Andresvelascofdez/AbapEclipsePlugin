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

$coreSources = Get-ChildItem -Path (Join-Path $root "src\com\abap\assistant\core") -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$cliSources = Get-ChildItem -Path (Join-Path $root "src\com\abap\assistant\cli") -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$testSources = Get-ChildItem -Path (Join-Path $root "test") -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
$allSources = @($coreSources + $cliSources + $testSources)

if ($allSources.Count -eq 0) {
    throw "No Java sources found."
}

[System.IO.File]::WriteAllLines($sourcesFile, $allSources, [System.Text.UTF8Encoding]::new($false))

& javac -encoding UTF-8 --release 11 -d $classes "@$sourcesFile"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& java -cp $classes com.abap.assistant.core.AssistantCoreTest
if ($LASTEXITCODE -ne 0) {
    throw "Core tests failed with exit code $LASTEXITCODE"
}

[xml]$pluginXml = Get-Content -Path (Join-Path $root "plugin.xml")
$view = $pluginXml.plugin.extension.view
if ($view.id -ne "com.abap.assistant.ui.ChatView") {
    throw "plugin.xml does not expose the expected assistant view id."
}
if ($view.class -ne "com.abap.assistant.ui.ChatView") {
    throw "plugin.xml does not point to the expected ChatView class."
}
if ($view.icon -ne "icons/abap_icon.png") {
    throw "plugin.xml does not point to the expected ABAP Chat icon."
}

$iconPath = Join-Path $root "icons\abap_icon.png"
if (-not (Test-Path -LiteralPath $iconPath)) {
    throw "ABAP Chat icon is missing: $iconPath"
}
$iconBytes = [System.IO.File]::ReadAllBytes($iconPath)
if ($iconBytes.Length -lt 8 -or $iconBytes[0] -ne 0x89 -or $iconBytes[1] -ne 0x50 -or $iconBytes[2] -ne 0x4E -or $iconBytes[3] -ne 0x47) {
    throw "ABAP Chat icon is not a valid PNG file header: $iconPath"
}

$manifest = Get-Content -Path (Join-Path $root "META-INF\MANIFEST.MF") -Raw
if ($manifest -notmatch "Bundle-SymbolicName: com\.abap\.assistant") {
    throw "Manifest does not contain the expected Bundle-SymbolicName."
}
if ($manifest -notmatch "Bundle-RequiredExecutionEnvironment: JavaSE-11") {
    throw "Manifest must keep the JavaSE-11 execution environment for Eclipse compatibility."
}

$javaSources = Get-ChildItem -Path (Join-Path $root "src"), (Join-Path $root "test") -Filter "*.java" -Recurse
foreach ($source in $javaSources) {
    $bytes = [System.IO.File]::ReadAllBytes($source.FullName)
    if ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF) {
        throw "Java source contains a UTF-8 BOM that breaks javac: $($source.FullName)"
    }
    $content = Get-Content -Path $source.FullName -Raw
    if ($content -match '"""') {
        throw "Java text blocks are not allowed while the project targets Java 11: $($source.FullName)"
    }
    if ($content -match '(?m)^\s*(public\s+|private\s+|protected\s+)?(static\s+)?record\s+') {
        throw "Java records are not allowed while the project targets Java 11: $($source.FullName)"
    }
    if ($content -match 'instanceof\s+[A-Za-z0-9_<>, ?]+\s+[a-zA-Z_][A-Za-z0-9_]*\s*(?:&&|\)|\{)') {
        throw "Pattern matching instanceof is not allowed while the project targets Java 11: $($source.FullName)"
    }
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
