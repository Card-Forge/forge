[CmdletBinding()]
param(
    [switch]$Execute
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$targets = @(Get-ChildItem -LiteralPath $repositoryRoot -Directory -Recurse -Force |
    Where-Object { $_.Name -eq 'target' -and $_.FullName.StartsWith($repositoryRoot, [StringComparison]::OrdinalIgnoreCase) })

$totalBytes = 0L
$totalFiles = 0L
foreach ($target in $targets) {
    $resolved = (Resolve-Path -LiteralPath $target.FullName).Path
    if (-not $resolved.StartsWith($repositoryRoot + [IO.Path]::DirectorySeparatorChar,
            [StringComparison]::OrdinalIgnoreCase) -or (Split-Path -Leaf $resolved) -ne 'target') {
        throw "Refusing to clean an unsafe path: $resolved"
    }
    $files = @(Get-ChildItem -LiteralPath $resolved -File -Recurse -Force -ErrorAction SilentlyContinue)
    $bytes = ($files | Measure-Object -Property Length -Sum).Sum
    if ($null -eq $bytes) { $bytes = 0L }
    $totalBytes += $bytes
    $totalFiles += $files.Count
    Write-Output ("{0} | {1:N1} MiB | {2:N0} files" -f $resolved, ($bytes / 1MB), $files.Count)
}

Write-Output ("Total: {0:N1} MiB in {1:N0} files across {2:N0} target directories." -f
    ($totalBytes / 1MB), $totalFiles, $targets.Count)
if (-not $Execute) {
    Write-Output 'Preview only. Re-run with -Execute to remove these generated target directories.'
    exit 0
}
foreach ($target in $targets) {
    Remove-Item -LiteralPath $target.FullName -Recurse -Force
}
Write-Output 'Generated target directories removed.'
