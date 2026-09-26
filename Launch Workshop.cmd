@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -Command "$beta = Get-Content -Raw -LiteralPath './dist/latest-beta.json' | ConvertFrom-Json; Start-Process -FilePath (Join-Path $beta.directory 'Forge Workshop.exe')"
