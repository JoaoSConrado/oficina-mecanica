# Gera docs/ddd/event-storming.pdf e docs/ddd/diagramas.pdf a partir dos HTML desta pasta,
# usando o Microsoft Edge (ou Chrome) em modo headless. Uso: .\docs\ddd\fontes\gerar-pdfs.ps1
$ErrorActionPreference = 'Stop'

$navegador = @(
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $navegador) { throw 'Edge ou Chrome não encontrado' }

$fontes = $PSScriptRoot
$destino = Split-Path $fontes -Parent

foreach ($nome in 'event-storming', 'diagramas') {
    $html = Join-Path $fontes "$nome.html"
    $pdf = Join-Path $destino "$nome.pdf"
    # O tempo virtual dá ao Mermaid tempo de desenhar os diagramas antes da impressão
    $argumentos = @('--headless=new', '--disable-gpu', '--no-pdf-header-footer',
        '--virtual-time-budget=15000', '--run-all-compositor-stages-before-draw',
        "--print-to-pdf=`"$pdf`"", "file:///$($html -replace '\\', '/')")
    Start-Process $navegador -ArgumentList $argumentos -Wait
    Write-Host "Gerado: $pdf"
}
