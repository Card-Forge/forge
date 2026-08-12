[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9A-Za-z._-]+$')]
    [string]$Version,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9-]+$')]
    [string]$Bucket,

    [Parameter(Mandatory = $true)]
    [string]$AssetsZip,

    [string]$Ossutil = 'ossutil'
)

$ErrorActionPreference = 'Stop'

function Invoke-Ossutil([string[]]$Arguments) {
    & $Ossutil @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ossutil failed with exit code $LASTEXITCODE"
    }
}

$resolved = Resolve-Path -LiteralPath $AssetsZip -ErrorAction Stop
$assets = Get-Item -LiteralPath $resolved.Path
if ($assets.PSIsContainer -or $assets.Length -le 0) {
    throw "Invalid assets archive: $AssetsZip"
}

$assetsObject = "forge/assets/$Version/$($assets.Name)"
$manifestObject = "oss://$Bucket/forge/manifest-v1.properties"
$assetsHash = (Get-FileHash -LiteralPath $assets.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
$tempDirectory = Join-Path ([IO.Path]::GetTempPath()) ("forge-assets-" + [Guid]::NewGuid().ToString('N'))
$manifestPath = Join-Path $tempDirectory 'manifest-v1.properties'

New-Item -ItemType Directory -Path $tempDirectory | Out-Null
try {
    $properties = [ordered]@{}
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & $Ossutil cp $manifestObject $manifestPath --force 2>$null
    $manifestDownloadExitCode = $LASTEXITCODE
    $ErrorActionPreference = $previousErrorActionPreference
    if ($manifestDownloadExitCode -eq 0 -and (Test-Path -LiteralPath $manifestPath)) {
        foreach ($line in Get-Content -LiteralPath $manifestPath -Encoding UTF8) {
            if ($line -match '^\s*([^#!][^=]*)=(.*)$') {
                $properties[$matches[1].Trim()] = $matches[2].Trim()
            }
        }
    }

    $properties['schema'] = '1'
    if (-not $properties.Contains('version')) {
        $properties['version'] = $Version
    }
    $properties['publishedAt'] = [DateTimeOffset]::Now.ToString('o')
    $properties['assets.version'] = $Version
    $properties['assets.url'] = "assets/$Version/$($assets.Name)"
    $properties['assets.size'] = [string]$assets.Length
    $properties['assets.sha256'] = $assetsHash

    $manifest = ($properties.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "`n"
    Set-Content -LiteralPath $manifestPath -Value $manifest -Encoding ASCII

    Invoke-Ossutil @('cp', $assets.FullName, "oss://$Bucket/$assetsObject", '--force',
        '--cache-control', 'public,max-age=31536000,immutable', '--content-type', 'application/zip')
    Invoke-Ossutil @('cp', $manifestPath, $manifestObject, '--force',
        '--cache-control', 'no-cache,max-age=60', '--content-type', 'text/plain; charset=utf-8')
} finally {
    Remove-Item -LiteralPath $tempDirectory -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Output "Assets version: $Version"
Write-Output "Assets object: oss://$Bucket/$assetsObject"
Write-Output "Size: $($assets.Length) bytes"
Write-Output "SHA-256: $assetsHash"
Write-Output "Manifest: $manifestObject"
