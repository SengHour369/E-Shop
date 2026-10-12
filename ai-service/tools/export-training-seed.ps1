param(
    [Parameter(Mandatory=$true)][string]$OutputPath,
    [ValidateRange(2,10000000)][int]$Count = 10000000
)
$ErrorActionPreference = 'Stop'
if ($Count % 2 -ne 0) { throw 'Count must be even for balanced English/Khmer pairs.' }
$moduleRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $moduleRoot 'src\main\java\com\example\eshop\ai\seed'
$classRoot = Join-Path $moduleRoot 'target\training-export'
New-Item -ItemType Directory -Force $classRoot | Out-Null
$javaBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { Split-Path -Parent (Get-Command javac.exe).Source }
& (Join-Path $javaBin 'javac.exe') --release 17 -encoding UTF-8 -d $classRoot (Join-Path $sourceRoot 'AiTrainingSeedData.java') (Join-Path $sourceRoot 'AiTrainingDataExporter.java')
if ($LASTEXITCODE -ne 0) { throw 'Seed exporter compilation failed.' }
& (Join-Path $javaBin 'java.exe') -Xmx128m -cp $classRoot com.example.eshop.ai.seed.AiTrainingDataExporter $OutputPath $Count
if ($LASTEXITCODE -ne 0) { throw 'Export failed. An unfinished .partial file may remain; it is not a complete dataset.' }
