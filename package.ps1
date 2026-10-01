<#
.SYNOPSIS
    Spark ERP uygulamasını Windows için paketler.

.DESCRIPTION
    Ad, sürüm ve açıklama yalnızca pom.xml'den okunur (tek kaynak). Proje
    derlenir, testler çalıştırılır ve jpackage ile Java'sı içinde gelen bir
    uygulama üretilir.

    Varsayılan:  dist\Spark ERP\Spark ERP.exe (kurulum gerektirmeyen klasör)
    -Installer:  dist\Spark ERP-<sürüm>.exe kurulum dosyası; kurulumda
                 LICENSE.txt gösterilir ve kabul edilmesi istenir.
                 WiX Toolset 3 gerekir (PATH'te ya da standart kurulum
                 klasöründe, ör. C:\Program Files (x86)\WiX Toolset v3.14).

    Lisans, uygulamanın ilk açılışında da ayrıca kabul ettirilir.

.EXAMPLE
    .\package.ps1
    .\package.ps1 -Installer
    .\package.ps1 -SkipTests
#>
param(
    [switch]$Installer,
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$Vendor      = 'Muhammet Akduman'
$Copyright   = '(c) 2026 Muhammet Akduman - Tüm hakları saklıdır'
$MainClass   = 'com.electrician.tracker.Launcher'
# Fixed so that a newer installer upgrades an older installation instead of
# installing beside it. NEVER change it: the old "Şantiye Takip" installs must
# be upgraded in place by Spark ERP, leaving one entry in Add/Remove Programs.
$UpgradeUuid = '6f2c1d9e-3b4a-4f7e-9a51-2d8c7b0e4a13'
$DistDir     = Join-Path $PSScriptRoot 'dist'
$InputDir    = Join-Path $PSScriptRoot 'target\jpackage-input'

function Find-Tool([string]$name) {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\$name.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    # Maven reports the JDK it runs on ("Java version: 21..., runtime: C:\...\jdk-21"); use the same one.
    $javaLine = (& mvn -v) | Where-Object { $_ -match 'runtime:\s*(.+)$' } | Select-Object -First 1
    if ($javaLine -and $javaLine -match 'runtime:\s*(.+)$') {
        $candidate = Join-Path $Matches[1].Trim() "bin\$name.exe"
        if (Test-Path $candidate) { return $candidate }
    }
    $onPath = Get-Command "$name.exe" -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    throw "$name bulunamadı. JAVA_HOME değişkenini JDK 21 klasörüne ayarlayın."
}

# 1. Ad, sürüm, açıklama: tek kaynak pom.xml
[xml]$pom = Get-Content (Join-Path $PSScriptRoot 'pom.xml') -Encoding UTF8
$AppName     = $pom.project.name.Trim()
$Description = $pom.project.description.Trim()
$Version     = $pom.project.version.Trim()
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    throw "pom.xml sürümü '$Version' jpackage için uygun değil (örnek: 1.0.0)."
}
Write-Host "Sürüm: $Version"

# The MSI database uses code page 1252, which has no ş, ı, ğ, İ; light.exe
# stops with LGHT0311 on them. Installer metadata is therefore folded to
# 1252-safe letters; texts inside the program stay fully Turkish.
function ConvertTo-InstallerText([string]$text) {
    # Pairs, not a hashtable: PowerShell hashtable keys ignore case, so 'ş' and 'Ş' would collide.
    $pairs = @(@('ş', 's'), @('Ş', 'S'), @('ı', 'i'), @('İ', 'I'), @('ğ', 'g'), @('Ğ', 'G'))
    foreach ($pair in $pairs) { $text = $text.Replace($pair[0], $pair[1]) }
    return $text
}

# jpackage turns a .txt licence into RTF without Unicode, so the installer
# would show "?" for Turkish letters. The licence is converted here instead,
# from the single LICENSE.txt, writing every non-ASCII letter as \uN?.
function ConvertTo-RtfLicense([string]$source, [string]$target) {
    $text = [System.IO.File]::ReadAllText($source, [System.Text.Encoding]::UTF8)
    $builder = New-Object System.Text.StringBuilder
    [void]$builder.Append('{\rtf1\ansi\deff0{\fonttbl{\f0 Arial;}}\f0\fs18 ')
    foreach ($ch in $text.Replace("`r`n", "`n").ToCharArray()) {
        $code = [int]$ch
        if ($ch -eq "`n") { [void]$builder.Append("\par`r`n") }
        elseif ($ch -eq '\' -or $ch -eq '{' -or $ch -eq '}') { [void]$builder.Append('\' + $ch) }
        elseif ($code -gt 127) {
            # RTF \u takes a signed 16-bit number.
            $signed = if ($code -gt 32767) { $code - 65536 } else { $code }
            [void]$builder.Append("\u${signed}?")
        }
        else { [void]$builder.Append($ch) }
    }
    [void]$builder.Append('}')
    [System.IO.File]::WriteAllText($target, $builder.ToString(), [System.Text.Encoding]::ASCII)
}

function Add-WixToPath {
    if (Get-Command 'candle.exe' -ErrorAction SilentlyContinue) { return }
    $wixBin = Get-ChildItem "${env:ProgramFiles(x86)}\WiX Toolset v3*\bin\candle.exe" -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1
    if (-not $wixBin) {
        throw 'Kurulum dosyası için WiX Toolset 3 gerekli (candle.exe bulunamadı).'
    }
    $env:Path = "$($wixBin.DirectoryName);$env:Path"
}

if ($Installer) {
    Add-WixToPath
    $Copyright   = ConvertTo-InstallerText $Copyright
    $Description = ConvertTo-InstallerText $Description
}

# 2. Derleme (testlerle)
$mvnArgs = @('-B', 'clean', 'package')
if ($SkipTests) { $mvnArgs += '-DskipTests' }
& mvn @mvnArgs
if ($LASTEXITCODE -ne 0) { throw 'Maven derlemesi başarısız.' }

# 3. jpackage girdisi: uygulama jar'ı + çalışma zamanı bağımlılıkları (target\lib)
$MainJar = "electrician-tracker-$Version.jar"
if (Test-Path $InputDir) { Remove-Item $InputDir -Recurse -Force }
New-Item -ItemType Directory -Force $InputDir | Out-Null
Copy-Item (Join-Path $PSScriptRoot "target\$MainJar") $InputDir
Copy-Item (Join-Path $PSScriptRoot 'target\lib\*') $InputDir
# Shown by the launcher from the double click until the first window opens
# (Spring needs several seconds); the program closes it itself.
Copy-Item (Join-Path $PSScriptRoot 'packaging\splash.png') $InputDir

# 4. Paketleme
New-Item -ItemType Directory -Force $DistDir | Out-Null
$jpackage = Find-Tool 'jpackage'
$jpackageArgs = @(
    '--name', $AppName,
    '--app-version', $Version,
    '--vendor', $Vendor,
    '--copyright', $Copyright,
    '--description', $Description,
    '--input', $InputDir,
    '--main-jar', $MainJar,
    '--main-class', $MainClass,
    '--java-options', '-Dfile.encoding=UTF-8',
    '--java-options', '-splash:$APPDIR\splash.png',
    # Electric panel icon: desktop shortcut, Start menu, Add/Remove Programs and the exe itself.
    '--icon', (Join-Path $PSScriptRoot 'src\main\resources\ikon\ikon.ico'),
    '--dest', $DistDir
)
if ($Installer) {
    $licenseRtf = Join-Path $PSScriptRoot 'target\LICENSE.rtf'
    ConvertTo-RtfLicense (Join-Path $PSScriptRoot 'LICENSE.txt') $licenseRtf
    $jpackageArgs += @(
        '--type', 'exe',
        '--license-file', $licenseRtf,
        '--install-dir', $AppName,
        '--win-menu', '--win-menu-group', $AppName, '--win-shortcut', '--win-dir-chooser',
        '--win-upgrade-uuid', $UpgradeUuid
    )
} else {
    $appImage = Join-Path $DistDir $AppName
    if (Test-Path $appImage) { Remove-Item $appImage -Recurse -Force }
    $jpackageArgs += @('--type', 'app-image')
}
& $jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw 'jpackage başarısız.' }

if (-not $Installer) {
    # The installer shows the licence itself; a plain folder carries it as a file.
    Copy-Item (Join-Path $PSScriptRoot 'LICENSE.txt') (Join-Path $DistDir $AppName)
    Write-Host "Hazır: $(Join-Path $DistDir "$AppName\$AppName.exe")"
} else {
    Write-Host "Hazır: $DistDir"
}
