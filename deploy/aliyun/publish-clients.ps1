[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ManifestVersion,

    [Parameter(Mandatory = $false)]
    [string]$DesktopVersion = '',

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9A-Za-z._-]+$')]
    [string]$ObjectVersion,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9-]+$')]
    [string]$Bucket,

    [Parameter(Mandatory = $true)]
    [string]$AndroidApk,

    [Parameter(Mandatory = $true)]
    [string]$DesktopPackage,

    [Parameter(Mandatory = $true)]
    [string]$Ossutil
)

$ErrorActionPreference = 'Stop'

# Android's startup updater compares the installed APK versionName with the
# manifest identity. ManifestVersion must therefore be the APK's exact ASCII
# versionName (for example, 2.0.15-cn0813r2). The desktop build can retain its
# localized Implementation-Version via DesktopVersion.
if ([string]::IsNullOrWhiteSpace($DesktopVersion)) {
    $DesktopVersion = $ManifestVersion
}

function Resolve-Artifact([string]$Path, [string]$Label) {
    $file = Get-Item -LiteralPath (Resolve-Path -LiteralPath $Path -ErrorAction Stop).Path
    if ($file.PSIsContainer -or $file.Length -le 0) {
        throw "$Label is not a valid file: $Path"
    }
    [PSCustomObject]@{
        File = $file
        Size = $file.Length
        Sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    }
}

function Invoke-Ossutil([string[]]$Arguments) {
    & $Ossutil @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ossutil failed with exit code $LASTEXITCODE"
    }
}

function Read-Properties([string]$Path) {
    $values = [ordered]@{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        if ($line -match '^\s*([^#!][^=]*)=(.*)$') {
            $values[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
    return $values
}

$android = Resolve-Artifact $AndroidApk 'Android APK'
$desktop = Resolve-Artifact $DesktopPackage 'Desktop package'
$androidName = "forge-android-$ObjectVersion.apk"
$desktopName = "forge-windows-$ObjectVersion.zip"
$androidObject = "forge/android/$ObjectVersion/$androidName"
$desktopObject = "forge/desktop/$ObjectVersion/$desktopName"
$manifestObject = "oss://$Bucket/forge/manifest-v1.properties"
$immutableCache = 'public,max-age=31536000,immutable'
$tempDirectory = Join-Path ([IO.Path]::GetTempPath()) ('forge-release-' + [Guid]::NewGuid().ToString('N'))
$oldManifest = Join-Path $tempDirectory 'old-manifest.properties'
$newManifest = Join-Path $tempDirectory 'manifest-v1.properties'

New-Item -ItemType Directory -Path $tempDirectory | Out-Null
try {
    Invoke-Ossutil @('cp', $manifestObject, $oldManifest, '--force')
    $old = Read-Properties $oldManifest
    foreach ($required in @('assets.version', 'assets.url', 'assets.size', 'assets.sha256')) {
        if (-not $old.Contains($required) -or [string]::IsNullOrWhiteSpace($old[$required])) {
            throw "Existing manifest is missing $required; refusing to publish an incomplete release."
        }
    }

    # Immutable artifacts are uploaded first. The mutable manifest is the final
    # operation so clients can never observe paths that have not finished uploading.
    Invoke-Ossutil @('cp', $android.File.FullName, "oss://$Bucket/$androidObject", '--force',
        '--cache-control', $immutableCache, '--content-type', 'application/vnd.android.package-archive')
    Invoke-Ossutil @('cp', $desktop.File.FullName, "oss://$Bucket/$desktopObject", '--force',
        '--cache-control', $immutableCache, '--content-type', 'application/zip')

    $manifest = @(
        'schema=1'
        "version=$ManifestVersion"
        "publishedAt=$([DateTimeOffset]::Now.ToString('o'))"
        "android.version=$ManifestVersion"
        "android.url=android/$ObjectVersion/$androidName"
        "android.size=$($android.Size)"
        "android.sha256=$($android.Sha256)"
        "desktop.version=$DesktopVersion"
        "desktop.url=desktop/$ObjectVersion/$desktopName"
        "desktop.size=$($desktop.Size)"
        "desktop.sha256=$($desktop.Sha256)"
        "assets.version=$($old['assets.version'])"
        "assets.url=$($old['assets.url'])"
        "assets.size=$($old['assets.size'])"
        "assets.sha256=$($old['assets.sha256'])"
    ) -join "`n"
    [IO.File]::WriteAllText($newManifest, $manifest + "`n", [Text.UTF8Encoding]::new($false))
    Invoke-Ossutil @('cp', $newManifest, $manifestObject, '--force', '--cache-control',
        'no-cache,max-age=60', '--content-type', 'text/plain; charset=utf-8')

    Write-Output "Published version: $ManifestVersion"
    Write-Output "Android object: oss://$Bucket/$androidObject"
    Write-Output "Desktop object: oss://$Bucket/$desktopObject"
    Write-Output "Manifest: $manifestObject"
} finally {
    Remove-Item -LiteralPath $tempDirectory -Recurse -Force -ErrorAction SilentlyContinue
}
