param([ValidateSet('Run','Test','UiTest')][string]$Mode='Run')
$ErrorActionPreference='Stop'
$project=Split-Path -Parent $PSScriptRoot
Set-Location $project
$jdkCandidates=@()
if ($env:JAVA_HOME) { $jdkCandidates+=$env:JAVA_HOME }
$javacCommand=Get-Command javac -ErrorAction SilentlyContinue
if ($javacCommand) { $jdkCandidates+=Split-Path -Parent (Split-Path -Parent $javacCommand.Source) }
foreach ($jdkRoot in @((Join-Path $env:USERPROFILE '.jdks'), (Join-Path $env:ProgramFiles 'Java'), (Join-Path $env:ProgramFiles 'Eclipse Adoptium'))) {
    if (Test-Path $jdkRoot) {
        $jdkCandidates+=@(Get-ChildItem $jdkRoot -Directory | Sort-Object Name -Descending | ForEach-Object { $_.FullName })
    }
}
$java=$null
foreach ($jdk in $jdkCandidates) {
    $candidateCompiler=Join-Path $jdk 'bin\javac.exe'
    $candidateJava=Join-Path $jdk 'bin\java.exe'
    if ((Test-Path $candidateCompiler) -and (Test-Path $candidateJava)) {
        $compilerVersion=(& $candidateCompiler -version 2>&1 | Out-String)
        if ($LASTEXITCODE -eq 0 -and $compilerVersion -match 'javac\s+(\d+)' -and [int]$Matches[1] -ge 17) {
            $java=$candidateJava
            $javac=$candidateCompiler
            break
        }
    }
}
if (-not $java) { throw 'JDK 17+ is required. Set JAVA_HOME to a JDK or install a JDK in your .jdks folder.' }
Write-Host "Using JDK: $java"
$fx=Join-Path $project 'runtime\javafx'
foreach ($module in @('base','graphics','controls','swing')) {
    if (-not (Test-Path (Join-Path $fx "javafx-$module-17.0.10-win.jar"))) { throw 'JavaFX libraries are missing. Extract the entire ZIP, including runtime/javafx.' }
}
$out=Join-Path $project 'out'
New-Item -ItemType Directory -Force $out | Out-Null
$sources=@(Get-ChildItem (Join-Path $project 'src\main\java\*.java') | ForEach-Object {$_.FullName})
if ($Mode -ne 'Run') { $sources+=@(Get-ChildItem (Join-Path $project 'src\test\java\*.java') | ForEach-Object {$_.FullName}) }
Write-Host 'Compiling Campus Exchange...'
& $javac --release 17 --module-path $fx --add-modules 'javafx.controls,javafx.swing' -encoding UTF-8 -d $out @sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Check the JDK version and read the error above.' }
Copy-Item (Join-Path $project 'src\main\resources\app.css') $out -Force
if ($Mode -eq 'Test') { & $java -cp $out IntegrationTest }
elseif ($Mode -eq 'UiTest') { & $java --module-path $fx --add-modules 'javafx.controls,javafx.swing' -cp $out FxSmokeTest }
else { & $java --module-path $fx --add-modules 'javafx.controls,javafx.swing' -cp $out Launcher }
if ($LASTEXITCODE -ne 0) { throw "Application failed with exit code $LASTEXITCODE. Read the error above." }
