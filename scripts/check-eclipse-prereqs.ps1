param(
    [Parameter(Mandatory = $true)]
    [string]$EclipseHome
)

$ErrorActionPreference = "Stop"

$resolvedHome = Resolve-Path $EclipseHome
$plugins = Join-Path $resolvedHome "plugins"

if (-not (Test-Path -LiteralPath $plugins)) {
    throw "The path does not look like an Eclipse installation because it has no plugins folder: $resolvedHome"
}

$requiredPatterns = @(
    "org.eclipse.core.runtime_*.jar",
    "org.eclipse.ui_*.jar",
    "org.eclipse.jface_*.jar",
    "org.eclipse.jface.text_*.jar",
    "org.eclipse.swt_*.jar",
    "org.eclipse.pde.core_*.jar"
)

$missing = @()
foreach ($pattern in $requiredPatterns) {
    $match = Get-ChildItem -Path $plugins -Filter $pattern -File -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $match) {
        $missing += $pattern
    }
}

if ($missing.Count -gt 0) {
    Write-Host "Missing Eclipse/PDE prerequisites:"
    $missing | ForEach-Object { Write-Host " - $_" }
    throw "Install Eclipse PDE or use an Eclipse package that includes Plug-in Development Environment."
}

Write-Host "Eclipse/PDE prerequisites found."
Write-Host "If the project still shows org.eclipse.* errors, set Window > Preferences > Plug-in Development > Target Platform to Running Platform, then clean/reimport the project."

