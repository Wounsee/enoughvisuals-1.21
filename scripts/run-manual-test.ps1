param(
    [Parameter(Mandatory = $true)][string]$JarPath
)

$ErrorActionPreference = 'Stop'
$librariesRoot = 'C:\enoughvisuals\libraries'
$sourceJar = [IO.Path]::GetFullPath($JarPath)
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$testRoot = "D:\enoughvisuals-2.0\run-manual-punchy-$stamp"
$modsDir = Join-Path $testRoot 'mods'

if (-not (Test-Path -LiteralPath $sourceJar -PathType Leaf)) {
    throw "Manual-test JAR does not exist: $sourceJar"
}

New-Item -ItemType Directory -Force -Path $testRoot, $modsDir | Out-Null
Get-ChildItem -LiteralPath 'C:\enoughvisuals\mods' -File |
    Where-Object { $_.Name -notlike 'enoughvisuals-*' -and $_.Extension -eq '.jar' } |
    Copy-Item -Destination $modsDir -Force
Copy-Item -LiteralPath $sourceJar -Destination (Join-Path $modsDir 'enoughvisuals-2.6-manual.jar') -Force

$version = Get-Content 'C:\enoughvisuals\versions\1.21.4\1.21.4.json' -Raw | ConvertFrom-Json
$fabric = Invoke-RestMethod 'https://meta.fabricmc.net/v2/versions/loader/1.21.4/0.19.3/profile/json'
$fabricCoordinates = @{}
foreach ($library in $fabric.libraries) {
    $parts = $library.name.Split(':')
    $fabricCoordinates[$parts[0] + ':' + $parts[1]] = $true
}

$classpath = [Collections.Generic.List[string]]::new()
$classpath.Add('C:\enoughvisuals\versions\1.21.4\1.21.4.jar')
foreach ($library in $version.libraries) {
    $parts = $library.name.Split(':')
    if ($parts.Length -ge 2 -and $fabricCoordinates.ContainsKey($parts[0] + ':' + $parts[1])) { continue }
    $relative = $library.downloads.artifact.path
    if ($relative) {
        $path = Join-Path $librariesRoot ($relative -replace '/', '\')
        if (Test-Path -LiteralPath $path) { $classpath.Add($path) }
    }
}
foreach ($library in $fabric.libraries) {
    $parts = $library.name.Split(':')
    $relative = ($parts[0] -replace '\.', '/') + '/' + $parts[1] + '/' + $parts[2] + '/' + $parts[1] + '-' + $parts[2] + '.jar'
    $path = Join-Path $librariesRoot ($relative -replace '/', '\')
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing Fabric library: $path" }
    $classpath.Add($path)
}

$env:CLASSPATH = [string]::Join(';', $classpath)
$javaArgs = @(
    '-Xmx3G',
    '-Djava.library.path=C:\enoughvisuals\versions\1.21.4\natives',
    '-Dfabric.gameVersion=1.21.4',
    '-Dfabric.loaderVersion=0.19.3',
    'net.fabricmc.loader.impl.launch.knot.KnotClient',
    '--username', 'EnoughGuardTest',
    '--version', '1.21.4',
    '--gameDir', $testRoot,
    '--assetsDir', 'C:\enoughvisuals\assets',
    '--assetIndex', '19',
    '--uuid', '00000000-0000-0000-0000-000000000001',
    '--accessToken', '0',
    '--clientId', '0',
    '--xuid', '0',
    '--userType', 'legacy',
    '--versionType', 'release'
)

$process = Start-Process -FilePath 'C:\enoughvisuals\runtime\temurin-21.0.10+7\bin\java.exe' -ArgumentList $javaArgs -WorkingDirectory $testRoot -PassThru
Write-Output "PID=$($process.Id)"
Write-Output "GAME_DIR=$testRoot"
