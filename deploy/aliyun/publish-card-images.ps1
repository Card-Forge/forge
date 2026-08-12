[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9-]+$')]
    [string]$Bucket,

    [Parameter(Mandatory = $true)]
    [string]$CardImageDirectory,

    [string]$Ossutil = 'ossutil',

    [switch]$SkipImageSync
)

$ErrorActionPreference = 'Stop'
$source = (Resolve-Path -LiteralPath $CardImageDirectory -ErrorAction Stop).Path
if (-not (Test-Path -LiteralPath $source -PathType Container)) {
    throw "Card image directory does not exist: $CardImageDirectory"
}

# Incremental and non-destructive: existing cloud objects are never deleted.
if (-not $SkipImageSync) {
    & $Ossutil sync ($source.TrimEnd('\') + '\') "oss://$Bucket/cards/" --force --checksum `
        --exclude '_*' --exclude '*.csv' --exclude '*.json' --exclude '*.log'
    if ($LASTEXITCODE -ne 0) {
        throw "Card image sync failed with exit code $LASTEXITCODE"
    }
}

# OSS does not expose directory listings through CDN. Publish a compact set
# index so Forge's bulk image downloader can avoid requesting absent sets.
$setIndex = [System.IO.Path]::GetTempFileName()
try {
    Get-ChildItem -LiteralPath $source -Directory |
        ForEach-Object Name |
        Sort-Object -Unique |
        Set-Content -LiteralPath $setIndex -Encoding utf8

    & $Ossutil cp $setIndex "oss://$Bucket/cards/sets.txt" --force `
        --content-type 'text/plain; charset=utf-8' --cache-control 'public, max-age=300'
    if ($LASTEXITCODE -ne 0) {
        throw "Set index upload failed with exit code $LASTEXITCODE"
    }
} finally {
    Remove-Item -LiteralPath $setIndex -Force -ErrorAction SilentlyContinue
}

if (-not $SkipImageSync) {
    Write-Output "Card images synchronized to oss://$Bucket/cards/"
}
Write-Output "Set index uploaded to oss://$Bucket/cards/sets.txt"
Write-Output 'Existing cloud objects were not deleted.'
