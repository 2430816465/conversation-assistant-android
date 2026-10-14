param([string]$Serial="emulator-5580",[ValidatePattern('^[0-9]{8}$')][string]$ReportDate=(Get-Date -Format 'yyyyMMdd'))
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$adb=Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if ($Serial -notmatch '^emulator-[0-9]+$' -or ((& $adb -s $Serial shell getprop ro.kernel.qemu) | Out-String).Trim() -ne '1') { throw 'Explicit running emulator required; never run on a real phone.' }
$runDir=Join-Path $projectRoot ('reports\ui-redesign\'+$ReportDate+'\matrix-'+(Get-Date -Format 'HHmmss'))
New-Item -ItemType Directory -Path $runDir -Force | Out-Null
$font=((& $adb -s $Serial shell settings get system font_scale) | Out-String).Trim()
$animations=@{}
foreach($key in @('window_animation_scale','transition_animation_scale','animator_duration_scale')) {
    $animations[$key]=((& $adb -s $Serial shell settings get global $key) | Out-String).Trim()
}
$cases=@(
    @{label='compact-light'; size='750x1624'; density='320'; font='1.0'; night='no'},
    @{label='compact-dark'; size='750x1624'; density='320'; font='1.0'; night='yes'},
    @{label='large-text-dark'; size='750x1624'; density='320'; font='2.0'; night='yes'},
    @{label='landscape-light'; size='1624x750'; density='320'; font='1.0'; night='no'},
    @{label='tablet-light'; size='1600x2560'; density='320'; font='1.0'; night='no'}
)
$results=@()
try {
    foreach($key in $animations.Keys) { & $adb -s $Serial shell settings put global $key 0 | Out-Null }
    foreach($case in $cases) {
        & $adb -s $Serial shell am force-stop cn.xindong.junshi | Out-Null
        & $adb -s $Serial shell wm size $case.size | Out-Null
        & $adb -s $Serial shell wm density $case.density | Out-Null
        & $adb -s $Serial shell settings put system font_scale $case.font | Out-Null
        & $adb -s $Serial shell cmd uimode night $case.night | Out-Null
        $output=& $adb -s $Serial shell am instrument -w -r -e class cn.xindong.junshi.VisualUiTest -e visualLabel $case.label cn.xindong.junshi.test/androidx.test.runner.AndroidJUnitRunner 2>&1
        $exitCode=$LASTEXITCODE; $text=$output | Out-String
        [IO.File]::WriteAllText((Join-Path $runDir ($case.label+'.txt')),$text,[Text.UTF8Encoding]::new($false))
        & 'C:\Program Files\Python38\python.exe' (Join-Path $PSScriptRoot 'export_ui_screens.py') --serial $Serial --label $case.label --output (Join-Path $runDir $case.label)
        if ($LASTEXITCODE -ne 0) { throw 'Synthetic screenshot export failed' }
        $passed=$exitCode -eq 0 -and $text -match 'OK \(1 test\)' -and $text -notmatch 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed'
        $results+=@{case=$case;passed=$passed}
        Write-Output ($case.label+': '+$passed)
        if (-not $passed) { throw ('Responsive UI test failed: '+$case.label+'; see '+$runDir) }
    }
} finally {
    & $adb -s $Serial shell am force-stop cn.xindong.junshi | Out-Null
    & $adb -s $Serial shell wm size reset | Out-Null
    & $adb -s $Serial shell wm density reset | Out-Null
    & $adb -s $Serial shell settings put system font_scale $font | Out-Null
    & $adb -s $Serial shell cmd uimode night no | Out-Null
    foreach($key in $animations.Keys) {
        if($animations[$key] -eq 'null') { & $adb -s $Serial shell settings delete global $key | Out-Null }
        else { & $adb -s $Serial shell settings put global $key $animations[$key] | Out-Null }
    }
    $summary=@{emulatorOnly=$true;synthetic=$true;newAiRequests=0;apkSha256=(Get-FileHash -LiteralPath (Join-Path $projectRoot 'deliverables\xindong-junshi-debug.apk') -Algorithm SHA256).Hash;cases=$results}
    [IO.File]::WriteAllText((Join-Path $runDir 'matrix.json'),($summary | ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
}
Write-Output $runDir

