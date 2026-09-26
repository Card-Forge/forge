@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -Command "$ErrorActionPreference = 'Stop'; $beta = Get-Content -Raw -LiteralPath './dist/latest-beta.json' | ConvertFrom-Json; Start-Process -FilePath (Join-Path $beta.directory $beta.executable)"
