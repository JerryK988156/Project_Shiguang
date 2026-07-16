param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$MavenArgs
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $PSScriptRoot
$propertiesPath = Join-Path $PSScriptRoot 'maven-wrapper.properties'

if (-not (Test-Path $propertiesPath)) {
    throw "缺少 $propertiesPath"
}

$properties = @{}
Get-Content $propertiesPath | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) {
        return
    }

    $pair = $line.Split('=', 2)
    if ($pair.Length -eq 2) {
        $properties[$pair[0].Trim()] = $pair[1].Trim()
    }
}

$distributionUrl = $properties['distributionUrl']
if (-not $distributionUrl) {
    throw 'maven-wrapper.properties 未配置 distributionUrl'
}

$zipName = [System.IO.Path]::GetFileName($distributionUrl)
$distName = [System.IO.Path]::GetFileNameWithoutExtension($zipName)
$expectedDir = $distName -replace '-bin$', ''
$cacheRoot = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
$downloadDir = Join-Path $cacheRoot $distName
$zipPath = Join-Path $downloadDir $zipName
$mavenHome = Join-Path $downloadDir $expectedDir
$mvnCmd = Join-Path $mavenHome 'bin\mvn.cmd'

if (-not (Test-Path $mvnCmd)) {
    New-Item -ItemType Directory -Force -Path $downloadDir | Out-Null

    if (-not (Test-Path $zipPath)) {
        Invoke-WebRequest -Uri $distributionUrl -OutFile $zipPath
    }

    Expand-Archive -LiteralPath $zipPath -DestinationPath $downloadDir -Force

    if (-not (Test-Path $mvnCmd)) {
        $resolved = Get-ChildItem -Path $downloadDir -Filter mvn.cmd -Recurse | Select-Object -First 1
        if (-not $resolved) {
            throw 'Maven 解压完成，但未找到 mvn.cmd'
        }
        $mvnCmd = $resolved.FullName
    }
}

& $mvnCmd @MavenArgs
exit $LASTEXITCODE
