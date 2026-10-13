param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# Use the locally installed JDK 21 without changing system settings.
$jdkCandidates = @(
    (Join-Path $env:USERPROFILE '.jdks\jbr-21.0.11'),
    (Join-Path $env:ProgramFiles 'Java\jdk-21.0.12.1')
)
$jdk21 = $jdkCandidates | Where-Object { Test-Path -LiteralPath (Join-Path $_ 'bin\java.exe') } | Select-Object -First 1
if (-not $jdk21) { throw 'JDK 21 was not found. Select JDK 21 in Android Studio and use Run.' }
$env:JAVA_HOME = $jdk21

$sdkDirectory = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$adb = Join-Path $sdkDirectory 'platform-tools\adb.exe'
$emulator = Join-Path $sdkDirectory 'emulator\emulator.exe'
if (-not (Test-Path -LiteralPath $adb)) { throw 'Android SDK was not found.' }
if (-not $SkipBuild) {
    Write-Host 'Building the Week7 preview app...'
    & (Join-Path $PSScriptRoot 'gradlew.bat') assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'The app build failed.' }
}

function Find-Emulator {
    $rows = & $adb devices
    foreach ($row in $rows) {
        if ($row -match '^(emulator-\d+)\s+device\b') { return $Matches[1] }
    }
}
$serial = Find-Emulator
if (-not $serial) {
    $avds = @(& $emulator -list-avds) | Where-Object { $_.Trim() }
    if (-not $avds) { throw 'Create an emulator in Android Studio Device Manager first.' }
    $avdName = if ($avds -contains 'Pixel_9') { 'Pixel_9' } else { $avds[0] }
    Write-Host "Opening $avdName. Waiting for startup..."
    # This launcher is invoked by the user to see and interact with the emulator.
    Start-Process -FilePath $emulator -ArgumentList @('-avd', $avdName) -WindowStyle Normal
}
$deadline = (Get-Date).AddMinutes(3)
do {
    if (-not $serial) { $serial = Find-Emulator }
    $booted = $false
    if ($serial) {
        $booted = ((& $adb -s $serial shell getprop sys.boot_completed) -join '').Trim() -eq '1'
    }
    if (-not $booted) { Start-Sleep -Seconds 2 }
} until ($booted -or (Get-Date) -gt $deadline)
if (-not $booted) { throw 'Emulator startup timed out. Open it from Android Studio and retry.' }

$apk = Join-Path $PSScriptRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $apk)) { throw 'Debug APK was not found. Run the build first.' }
& $adb -s $serial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'App installation failed.' }
& $adb -s $serial shell am start -n 'com.example.lifegame/.MainActivity'
if ($LASTEXITCODE -ne 0) { throw 'App launch failed.' }
Write-Host 'Open the design preview button on the start screen. Edit Week7DesignTokens.kt and run this launcher again to see changes.'
