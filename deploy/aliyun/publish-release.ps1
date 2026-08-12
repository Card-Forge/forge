[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9A-Za-z._-]+$')]
    [string]$Version,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9-]+$')]
    [string]$Bucket,

    [Parameter(Mandatory = $true)]
    [string]$AndroidApk,

    [Parameter(Mandatory = $true)]
    [string]$DesktopPackage,

    [Parameter(Mandatory = $true)]
    [string]$AssetsZip,

    [string]$Ossutil = 'ossutil'
)

$ErrorActionPreference = 'Stop'

function Resolve-Artifact([string]$Path, [string]$Label) {
    $resolved = Resolve-Path -LiteralPath $Path -ErrorAction Stop
    $file = Get-Item -LiteralPath $resolved.Path
    if (-not $file.PSIsContainer -and $file.Length -gt 0) {
        return [PSCustomObject]@{
            Label = $Label
            File = $file
            Size = $file.Length
            Sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        }
    }
    throw "$Label 不是有效文件：$Path"
}

function Invoke-Ossutil([string[]]$Arguments) {
    & $Ossutil @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ossutil 执行失败，退出码：$LASTEXITCODE"
    }
}

$android = Resolve-Artifact $AndroidApk 'Android APK'
$desktop = Resolve-Artifact $DesktopPackage 'Desktop package'
$assets = Resolve-Artifact $AssetsZip 'Assets archive'

$androidObject = "forge/android/$Version/$($android.File.Name)"
$desktopObject = "forge/desktop/$Version/$($desktop.File.Name)"
$assetsObject = "forge/assets/$Version/$($assets.File.Name)"
$immutableCache = 'public,max-age=31536000,immutable'

Invoke-Ossutil @('cp', $android.File.FullName, "oss://$Bucket/$androidObject", '--cache-control', $immutableCache,
    '--content-type', 'application/vnd.android.package-archive')
Invoke-Ossutil @('cp', $desktop.File.FullName, "oss://$Bucket/$desktopObject", '--cache-control', $immutableCache,
    '--content-type', 'application/octet-stream')
Invoke-Ossutil @('cp', $assets.File.FullName, "oss://$Bucket/$assetsObject", '--cache-control', $immutableCache,
    '--content-type', 'application/zip')

$manifestPath = Join-Path ([IO.Path]::GetTempPath()) ("forge-manifest-" + [Guid]::NewGuid().ToString('N') + '.properties')
try {
    $manifest = @(
        'schema=1'
        "version=$Version"
        "publishedAt=$([DateTimeOffset]::Now.ToString('o'))"
        "android.version=$Version"
        "android.url=android/$Version/$($android.File.Name)"
        "android.size=$($android.Size)"
        "android.sha256=$($android.Sha256)"
        "desktop.version=$Version"
        "desktop.url=desktop/$Version/$($desktop.File.Name)"
        "desktop.size=$($desktop.Size)"
        "desktop.sha256=$($desktop.Sha256)"
        "assets.version=$Version"
        "assets.url=assets/$Version/$($assets.File.Name)"
        "assets.size=$($assets.Size)"
        "assets.sha256=$($assets.Sha256)"
    ) -join "`n"
    Set-Content -LiteralPath $manifestPath -Value $manifest -Encoding ASCII
    Invoke-Ossutil @('cp', $manifestPath, "oss://$Bucket/forge/manifest-v1.properties", '--cache-control',
        'no-cache,max-age=60', '--content-type', 'text/plain; charset=utf-8')
} finally {
    Remove-Item -LiteralPath $manifestPath -Force -ErrorAction SilentlyContinue
}

Write-Output "发布完成：$Version"
Write-Output "清单：oss://$Bucket/forge/manifest-v1.properties"
