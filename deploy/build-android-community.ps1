[CmdletBinding()]
param(
    [string]$JavaHome,
    [string]$AndroidSdk,
    [string]$BuildToolsVersion,
    [string]$Maven,
    [string]$Keystore,
    [string]$KeyAlias = 'androiddebugkey',
    [string]$StorePassword,
    [string]$KeyPassword,
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Resolve-RequiredPath {
    param([string]$Path, [string]$Description, [switch]$Container)
    if ([string]::IsNullOrWhiteSpace($Path)) { throw "$Description is not configured." }
    $pathType = if ($Container) { 'Container' } else { 'Leaf' }
    if (-not (Test-Path -LiteralPath $Path -PathType $pathType)) { throw "$Description does not exist: $Path" }
    return (Resolve-Path -LiteralPath $Path).Path
}

function Invoke-Checked {
    param([string]$FilePath, [string[]]$Arguments, [string]$Description)
    Write-Host "[$Description] $FilePath"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Description failed with exit code $LASTEXITCODE" }
}

function Convert-ToShortRepoPath {
    param([string]$Path, [string]$RepositoryRoot, [string]$MappedDrive)
    $fullPath = [System.IO.Path]::GetFullPath($Path)
    if ($fullPath.StartsWith($RepositoryRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        $relative = $fullPath.Substring($RepositoryRoot.Length).TrimStart('\')
        return "$MappedDrive\$relative"
    }
    return $fullPath
}

function New-ShortDriveMapping {
    param([string]$RepositoryRoot)
    foreach ($drive in @('W:', 'V:', 'U:', 'T:')) {
        $existing = (& subst.exe $drive 2>$null | Out-String).Trim()
        if ($LASTEXITCODE -eq 0 -and $existing) {
            if ($existing -match '=>\s*(.+)$') {
                $mappedPath = $Matches[1].Trim().TrimEnd('\')
                if ($mappedPath.Equals($RepositoryRoot.TrimEnd('\'), [System.StringComparison]::OrdinalIgnoreCase)) {
                    return [pscustomobject]@{ Drive = $drive; Created = $false }
                }
            }
            continue
        }
        & subst.exe $drive $RepositoryRoot
        if ($LASTEXITCODE -eq 0) { return [pscustomobject]@{ Drive = $drive; Created = $true } }
    }
    throw 'Cannot create a temporary drive mapping. Free W:, V:, U:, or T: and retry.'
}

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $scriptDirectory '..')).Path
$androidDirectory = Join-Path $repositoryRoot 'forge-gui-android'
$targetDirectory = Join-Path $androidDirectory 'target'
[xml]$rootProject = Get-Content -LiteralPath (Join-Path $repositoryRoot 'pom.xml') -Raw -Encoding UTF8
[xml]$androidProject = Get-Content -LiteralPath (Join-Path $androidDirectory 'pom.xml') -Raw -Encoding UTF8
$versionCode = [string]$rootProject.project.properties.versionCode
$snapshotName = [string]$rootProject.project.properties.snapshotName
$revision = "$versionCode$snapshotName"
$displayVersion = [string]$rootProject.project.properties.displayVersion
$androidVersionCode = [string]$rootProject.project.properties.androidVersionCode
if ([string]::IsNullOrWhiteSpace($BuildToolsVersion)) { $BuildToolsVersion = [string]$androidProject.project.properties.androidBuildTools }
$androidPlatform = [string]$androidProject.project.properties.androidPlatform

if ([string]::IsNullOrWhiteSpace($JavaHome)) {
    $JavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Java\jdk-21.0.10' }
}
if ([string]::IsNullOrWhiteSpace($AndroidSdk)) {
    if ($env:ANDROID_SDK_ROOT) { $AndroidSdk = $env:ANDROID_SDK_ROOT }
    elseif ($env:ANDROID_HOME) { $AndroidSdk = $env:ANDROID_HOME }
    else { $AndroidSdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
}
if ([string]::IsNullOrWhiteSpace($Maven)) { $Maven = Join-Path $repositoryRoot '.tools\apache-maven-3.9.12\bin\mvn.cmd' }
if ([string]::IsNullOrWhiteSpace($Keystore)) { $Keystore = Join-Path $env:USERPROFILE '.android\debug.keystore' }
if ([string]::IsNullOrWhiteSpace($StorePassword)) {
    $StorePassword = if ($env:FORGE_ANDROID_STORE_PASSWORD) { $env:FORGE_ANDROID_STORE_PASSWORD } else { 'android' }
}
if ([string]::IsNullOrWhiteSpace($KeyPassword)) {
    $KeyPassword = if ($env:FORGE_ANDROID_KEY_PASSWORD) { $env:FORGE_ANDROID_KEY_PASSWORD } else { $StorePassword }
}
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) { $OutputDirectory = Join-Path $repositoryRoot 'dist\android' }

$JavaHome = Resolve-RequiredPath $JavaHome 'JDK directory' -Container
$AndroidSdk = Resolve-RequiredPath $AndroidSdk 'Android SDK directory' -Container
$Maven = Resolve-RequiredPath $Maven 'Maven launcher'
$Keystore = Resolve-RequiredPath $Keystore 'Android signing keystore'
$buildToolsDirectory = Resolve-RequiredPath (Join-Path $AndroidSdk "build-tools\$BuildToolsVersion") 'Android Build Tools directory' -Container
$androidJar = Resolve-RequiredPath (Join-Path $AndroidSdk "platforms\android-$androidPlatform\android.jar") 'Android platform android.jar'
$jar = Resolve-RequiredPath (Join-Path $JavaHome 'bin\jar.exe') 'jar'
$aapt = Resolve-RequiredPath (Join-Path $buildToolsDirectory 'aapt.exe') 'aapt'
$d8 = Resolve-RequiredPath (Join-Path $buildToolsDirectory 'd8.bat') 'D8'
$zipalign = Resolve-RequiredPath (Join-Path $buildToolsDirectory 'zipalign.exe') 'zipalign'
$apksigner = Resolve-RequiredPath (Join-Path $buildToolsDirectory 'apksigner.bat') 'apksigner'

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidSdk
$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:ANDROID_PREFS_ROOT = Join-Path $env:USERPROFILE '.android'
$env:MAVEN_OPTS = '-Xmx4g -Dfile.encoding=UTF-8'

Write-Host 'Forge Android community build'
Write-Host "Revision: $revision (display: $displayVersion, versionCode: $androidVersionCode)"
Write-Host "Output: $OutputDirectory"

$mapping = $null
try {
    Write-Step 'Create an ASCII-only short path for D8'
    $mapping = New-ShortDriveMapping $repositoryRoot
    Write-Host "Short path: $($mapping.Drive)\ -> $repositoryRoot"

    Write-Step 'Incrementally compile, generate Android resources, and run ProGuard'
    Write-Host '[Android Maven intermediates] The legacy plugin may fail at D8; this script validates and resumes from its outputs.'
    $androidMavenArguments = @('-pl', 'forge-gui-android', '-am', '-Pandroid-debug', '-DskipTests', '-Dcheckstyle.skip=true', 'package')
    & $Maven @androidMavenArguments
    $androidMavenExitCode = $LASTEXITCODE
    $manifest = Join-Path $targetDirectory 'AndroidManifest.xml'
    $obfuscatedJar = Join-Path $targetDirectory "forge-android-$revision`_obfuscated.jar"
    $classesDirectory = Join-Path $targetDirectory 'classes'
    foreach ($required in @($manifest, $obfuscatedJar, $classesDirectory)) {
        if (-not (Test-Path -LiteralPath $required)) {
            throw "Android Maven did not generate required intermediate: $required (exit code: $androidMavenExitCode)"
        }
    }
    if ($androidMavenExitCode -ne 0) { Write-Host 'Required intermediates are present; continuing with short-path D8.' -ForegroundColor Yellow }

    Write-Step 'Generate the D8 runtime classpath'
    $classpathFile = Join-Path $targetDirectory 'community-runtime-classpath.txt'
    Invoke-Checked $Maven @('-pl', 'forge-gui-android', '-am', '-DincludeScope=runtime', '-Dmdep.outputFile=target/community-runtime-classpath.txt', 'dependency:build-classpath') 'Maven classpath'
    if (-not (Test-Path -LiteralPath $classpathFile)) { throw "Maven did not generate the classpath file: $classpathFile" }
    $classpathEntries = [System.Collections.Generic.List[string]]::new()
    $rawClasspath = Get-Content -LiteralPath $classpathFile -Raw -Encoding UTF8
    foreach ($entry in ($rawClasspath -split [System.IO.Path]::PathSeparator)) {
        $candidate = $entry.Trim()
        if ($candidate -and (Test-Path -LiteralPath $candidate) -and ([System.IO.Path]::GetExtension($candidate) -eq '.jar')) {
            $classpathEntries.Add((Resolve-Path -LiteralPath $candidate).Path)
        }
    }
    Get-ChildItem -LiteralPath (Join-Path $androidDirectory 'libs') -Filter '*.jar' -File | ForEach-Object { $classpathEntries.Add($_.FullName) }
    $unpackedLibraries = Join-Path $targetDirectory 'unpacked-libs'
    if (Test-Path -LiteralPath $unpackedLibraries) {
        Get-ChildItem -LiteralPath $unpackedLibraries -Recurse -Filter 'classes.jar' -File | ForEach-Object { $classpathEntries.Add($_.FullName) }
    }
    $classpathEntries = @($classpathEntries | Sort-Object -Unique)
    if ($classpathEntries.Count -lt 5) { throw "Invalid D8 classpath: only $($classpathEntries.Count) JAR files found." }

    Write-Step "Run D8 with an argument file ($($classpathEntries.Count) classpath JARs)"
    $stageDirectory = Join-Path $targetDirectory 'community-build'
    if (Test-Path -LiteralPath $stageDirectory) { Remove-Item -LiteralPath $stageDirectory -Recurse -Force }
    $dexDirectory = Join-Path $stageDirectory 'dex'
    $nativeDirectory = Join-Path $stageDirectory 'native'
    New-Item -ItemType Directory -Path $dexDirectory, $nativeDirectory -Force | Out-Null
    $d8ArgumentsFile = Join-Path $stageDirectory 'd8.args'
    $d8Lines = [System.Collections.Generic.List[string]]::new()
    foreach ($line in @('--release', '--min-api', '26', '--output', (Convert-ToShortRepoPath $dexDirectory $repositoryRoot $mapping.Drive), '--lib', $androidJar)) { $d8Lines.Add($line) }
    foreach ($entry in $classpathEntries) {
        $d8Lines.Add('--classpath')
        $d8Lines.Add((Convert-ToShortRepoPath $entry $repositoryRoot $mapping.Drive))
    }
    $d8Lines.Add((Convert-ToShortRepoPath $obfuscatedJar $repositoryRoot $mapping.Drive))
    [System.IO.File]::WriteAllLines($d8ArgumentsFile, $d8Lines, [System.Text.UTF8Encoding]::new($false))
    Invoke-Checked $d8 @("@$(Convert-ToShortRepoPath $d8ArgumentsFile $repositoryRoot $mapping.Drive)") 'D8'
    $dexFiles = @(Get-ChildItem -LiteralPath $dexDirectory -Filter 'classes*.dex' -File)
    if ($dexFiles.Count -eq 0) { throw 'D8 did not generate classes.dex.' }

    Write-Step 'Rebuild Android resources and assemble the complete APK'
    $resourcePackage = Join-Path $stageDirectory "forge-android-$revision.ap_"
    Invoke-Checked $aapt @('package', '-f', '-M', (Convert-ToShortRepoPath $manifest $repositoryRoot $mapping.Drive), '-S', (Convert-ToShortRepoPath (Join-Path $androidDirectory 'res') $repositoryRoot $mapping.Drive), '-A', (Convert-ToShortRepoPath (Join-Path $classesDirectory 'assets') $repositoryRoot $mapping.Drive), '-I', $androidJar, '-F', (Convert-ToShortRepoPath $resourcePackage $repositoryRoot $mapping.Drive), '--auto-add-overlay') 'aapt resource packaging'
    $unsignedApk = Join-Path $stageDirectory "Forge-$revision-unsigned.apk"
    Copy-Item -LiteralPath $resourcePackage -Destination $unsignedApk -Force
    Invoke-Checked $jar @('uf', $unsignedApk, '-C', $dexDirectory, '.') 'Add DEX files'

    $nativeLibRoot = Join-Path $nativeDirectory 'lib'
    New-Item -ItemType Directory -Path $nativeLibRoot -Force | Out-Null
    foreach ($abi in @('arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64')) {
        $sourceAbi = Join-Path (Join-Path $androidDirectory 'libs') $abi
        $targetAbi = Join-Path $nativeLibRoot $abi
        if (-not (Test-Path -LiteralPath $sourceAbi)) { throw "Missing Android native library directory: $sourceAbi" }
        New-Item -ItemType Directory -Path $targetAbi -Force | Out-Null
        Copy-Item -Path (Join-Path $sourceAbi '*') -Destination $targetAbi -Force
    }
    Invoke-Checked $jar @('uf', $unsignedApk, '-C', $nativeDirectory, 'lib') 'Add native libraries'
    Invoke-Checked $jar @('uf', $unsignedApk, '-C', $androidDirectory, 'assets') 'Add bundled fonts and startup assets'

    $gdxFontDirectory = Join-Path $classesDirectory 'com\badlogic\gdx\utils'
    foreach ($fontFile in @('lsans-15.fnt', 'lsans-15.png')) {
        if (-not (Test-Path -LiteralPath (Join-Path $gdxFontDirectory $fontFile))) { throw "Missing bundled libGDX font: $fontFile" }
    }
    Invoke-Checked $jar @('uf', $unsignedApk, '-C', $classesDirectory, 'com/badlogic/gdx/utils/lsans-15.fnt', '-C', $classesDirectory, 'com/badlogic/gdx/utils/lsans-15.png') 'Add libGDX fonts'
    if (-not (Test-Path -LiteralPath (Join-Path $classesDirectory 'META-INF\services'))) { throw 'Missing META-INF/services; tinylog cannot initialize on Android.' }
    Invoke-Checked $jar @('uf', $unsignedApk, '-C', $classesDirectory, 'META-INF/services') 'Add service descriptors'

    Write-Step 'Align, sign, and verify the APK'
    New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
    $alignedApk = Join-Path $stageDirectory "Forge-$revision-aligned.apk"
    $finalApk = Join-Path $OutputDirectory "Forge-$revision-Android.apk"
    Invoke-Checked $zipalign @('-f', '-p', '4', $unsignedApk, $alignedApk) 'zipalign'
    Write-Host '[apksigner] Signing (passwords are not printed)'
    & $apksigner sign --ks $Keystore --ks-key-alias $KeyAlias --ks-pass "pass:$StorePassword" --key-pass "pass:$KeyPassword" --out $finalApk $alignedApk
    if ($LASTEXITCODE -ne 0) { throw "APK signing failed with exit code $LASTEXITCODE" }
    Invoke-Checked $apksigner @('verify', '--verbose', '--print-certs', $finalApk) 'APK signature verification'
    Invoke-Checked $zipalign @('-c', '-p', '4', $finalApk) 'APK alignment verification'

    $badging = & $aapt dump badging (Convert-ToShortRepoPath $finalApk $repositoryRoot $mapping.Drive)
    if ($LASTEXITCODE -ne 0) { throw 'aapt cannot read the final APK.' }
    $packageLine = $badging | Select-String '^package:' | Select-Object -First 1
    if (-not $packageLine -or $packageLine.Line -notmatch "versionName='$([regex]::Escape($revision))'") { throw "APK versionName does not match Maven revision: $($packageLine.Line)" }
    if ($packageLine.Line -notmatch "versionCode='$([regex]::Escape($androidVersionCode))'") { throw "APK versionCode does not match pom.xml: $($packageLine.Line)" }

    $requiredEntries = @('classes.dex', 'assets/localization/cardnames-zh-CN.txt', 'assets/update-mirror/forge-update.properties', 'assets/update-mirror/forge-community-release-notes-zh-CN.txt', 'assets/bundled-font/SourceHanSansCN.ttf', 'assets/fallback_skin/bg_splash.png', 'com/badlogic/gdx/utils/lsans-15.fnt', 'META-INF/services/org.tinylog.provider.LoggingProvider', 'lib/arm64-v8a/libgdx-freetype.so', 'lib/armeabi-v7a/libgdx-freetype.so', 'lib/x86/libgdx-freetype.so', 'lib/x86_64/libgdx-freetype.so')
    $apkEntries = @(& $jar tf $finalApk)
    foreach ($entry in $requiredEntries) { if ($apkEntries -notcontains $entry) { throw "Final APK is missing required entry: $entry" } }

    $apkInfo = Get-Item -LiteralPath $finalApk
    $sha256 = (Get-FileHash -LiteralPath $finalApk -Algorithm SHA256).Hash.ToLowerInvariant()
    [System.IO.File]::WriteAllText("$finalApk.sha256", "$sha256  $($apkInfo.Name)`n", [System.Text.UTF8Encoding]::new($false))
    [System.IO.File]::WriteAllLines((Join-Path $OutputDirectory 'latest-build.properties'), @("revision=$revision", "displayVersion=$displayVersion", "androidVersionCode=$androidVersionCode", "apk=$($apkInfo.Name)", "size=$($apkInfo.Length)", "sha256=$sha256", "generatedAt=$([DateTimeOffset]::Now.ToString('o'))"), [System.Text.UTF8Encoding]::new($false))

    Write-Host "`nBuild succeeded" -ForegroundColor Green
    Write-Host "APK: $finalApk"
    Write-Host "Size: $([Math]::Round($apkInfo.Length / 1MB, 2)) MiB"
    Write-Host "SHA-256: $sha256"
    Write-Host "Badging: $($packageLine.Line)"
}
finally {
    if ($mapping -and $mapping.Created) { & subst.exe $mapping.Drive /D | Out-Null }
}
