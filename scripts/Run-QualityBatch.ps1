param(
    [Parameter(Mandatory=$true)][string]$Config,
    [string]$Cases='qa/dialogue-quality-v2.jsonl',
    [string]$Output='qa/private/quality-batch',
    [ValidateSet('check','run')][string]$Action='check',
    [switch]$Resume,
    [string]$DpapiKeyPath
)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path -Parent $PSScriptRoot
$taskJava=(Get-ChildItem -LiteralPath (Join-Path $env:LOCALAPPDATA 'CodexAndroidBuild/jdk17') -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe') } | Select-Object -First 1).FullName
if (-not $taskJava) { throw 'JDK17 not found.' }
$taskOriginalJava=$env:JAVA_HOME
$taskOriginalArgs=$env:XINDONG_QA_ARGS
$taskOriginalKey=$env:XINDONG_QA_KEY
$taskOriginalJavaOpts=$env:JAVA_OPTS
$taskOriginalEncoding=[Console]::OutputEncoding
$taskPointer=[IntPtr]::Zero
$taskSecret=$null
Push-Location $taskRoot
try {
    [Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)
    $env:JAVA_OPTS=($taskOriginalJavaOpts+' -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8').Trim()
    if ($DpapiKeyPath) {
        $taskProtected=Get-Content -LiteralPath $DpapiKeyPath -Raw
        $taskSecret=ConvertTo-SecureString $taskProtected.Trim()
        $taskPointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($taskSecret)
        $env:XINDONG_QA_KEY=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($taskPointer)
    }
    $taskParts=@('--config',(Resolve-Path -LiteralPath $Config).Path,'--cases',(Resolve-Path -LiteralPath $Cases).Path,
        '--out',[IO.Path]::GetFullPath((Join-Path $taskRoot $Output)),'--action',$Action,'--resume',$Resume.IsPresent.ToString().ToLowerInvariant())
    if ($taskParts | Where-Object { $_.Contains('|') }) { throw 'Paths cannot contain the argument separator.' }
    $env:JAVA_HOME=$taskJava
    $env:XINDONG_QA_ARGS=$taskParts -join '|'
    & './gradlew.bat' ':qa-runner:runQualityBatch' '--console=plain' '--no-daemon'
    if ($LASTEXITCODE -ne 0) { throw 'Batch stopped; inspect ledger and summary before resuming. Unknown costs block further calls.' }
} finally {
    $env:JAVA_HOME=$taskOriginalJava
    $env:XINDONG_QA_ARGS=$taskOriginalArgs
    $env:XINDONG_QA_KEY=$taskOriginalKey
    $env:JAVA_OPTS=$taskOriginalJavaOpts
    [Console]::OutputEncoding=$taskOriginalEncoding
    if ($taskPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($taskPointer) }
    if ($taskSecret) { $taskSecret.Dispose() }
    Pop-Location
}
