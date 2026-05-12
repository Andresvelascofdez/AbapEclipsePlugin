param(
    [string]$Prompt = "Explain SELECT SINGLE in SAP ABAP using public SAP standard knowledge only."
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$buildRoot = Join-Path $root "build"
$classes = Join-Path $buildRoot "smoke-classes"
$sourcesFile = Join-Path $buildRoot "smoke-sources.txt"

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
$allSources = @($coreSources + $cliSources)

[System.IO.File]::WriteAllLines($sourcesFile, $allSources, [System.Text.UTF8Encoding]::new($false))

& javac -encoding UTF-8 --release 11 -d $classes "@$sourcesFile"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& java -cp $classes com.anvel.abapeclipseassistant.cli.AssistantCli $Prompt
if ($LASTEXITCODE -ne 0) {
    throw "OpenAI smoke test failed with exit code $LASTEXITCODE"
}
