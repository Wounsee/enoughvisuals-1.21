$ErrorActionPreference = 'Stop'

$testRoot = 'D:\enoughvisuals-2.0\run-protected'
$librariesRoot = 'C:\enoughvisuals\libraries'
$protectedJar = 'D:\enoughvisuals-2.0\build\libs\enoughvisuals-2.6-protected.jar'
$testJar = Join-Path $testRoot 'mods\enoughvisuals-2.6-protected.jar'

New-Item -ItemType Directory -Force -Path $testRoot, (Join-Path $testRoot 'mods') | Out-Null
Get-ChildItem 'C:\enoughvisuals\mods' -File | Copy-Item -Destination (Join-Path $testRoot 'mods') -Force
Copy-Item -LiteralPath $protectedJar -Destination $testJar -Force

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
    if ($parts.Length -ge 2 -and $fabricCoordinates.ContainsKey($parts[0] + ':' + $parts[1])) {
        continue
    }
    $relative = $library.downloads.artifact.path
    if ($relative) {
        $path = Join-Path $librariesRoot ($relative -replace '/', '\')
        if (Test-Path -LiteralPath $path) {
            $classpath.Add($path)
        }
    }
}
foreach ($library in $fabric.libraries) {
    $parts = $library.name.Split(':')
    $relative = ($parts[0] -replace '\.', '/') + '/' + $parts[1] + '/' + $parts[2] + '/' + $parts[1] + '-' + $parts[2] + '.jar'
    $classpath.Add((Join-Path $librariesRoot ($relative -replace '/', '\')))
}

$env:CLASSPATH = [string]::Join(';', $classpath)
& 'C:\enoughvisuals\runtime\temurin-21.0.10+7\bin\java.exe' `
    '-Xmx3G' `
    '-Djava.library.path=C:\enoughvisuals\versions\1.21.4\natives' `
    '-Dfabric.gameVersion=1.21.4' `
    '-Dfabric.loaderVersion=0.19.3' `
    'net.fabricmc.loader.impl.launch.knot.KnotClient' `
    '--username' 'EnoughGuardTest' '--version' '1.21.4' `
    '--gameDir' $testRoot '--assetsDir' 'C:\enoughvisuals\assets' '--assetIndex' '19' `
    '--uuid' '00000000-0000-0000-0000-000000000001' '--accessToken' '0' `
    '--clientId' '0' '--xuid' '0' '--userType' 'legacy' '--versionType' 'release'
