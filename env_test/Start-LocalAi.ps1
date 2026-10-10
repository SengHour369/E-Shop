param([string]$Model = 'qwen2.5:1.5b')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$env:OLLAMA_MODEL = $Model
$compose = @('-f','compose.yaml','-f','compose.ai.yaml','-f','compose.local.yaml','-f','compose.ollama.yaml')
& docker compose @compose up -d ollama
if ($LASTEXITCODE -ne 0) { throw 'Ollama could not start.' }
& docker compose @compose exec -T ollama ollama pull $Model
if ($LASTEXITCODE -ne 0) { throw 'Model download failed. Check free disk space and network access.' }
& docker compose @compose up -d --build api-gateway catalog-service ai-service ai-inference
if ($LASTEXITCODE -ne 0) { throw 'The AI stack could not start.' }
Write-Host 'AI stack started. Wait for service registration before sending the first chat.'
