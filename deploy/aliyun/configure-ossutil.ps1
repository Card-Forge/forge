$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ossutil = Join-Path $repositoryRoot '.tools\ossutil\ossutil-2.3.0-windows-amd64\ossutil.exe'

if (-not (Test-Path -LiteralPath $ossutil -PathType Leaf)) {
    throw "找不到 ossutil：$ossutil"
}

Write-Host '即将配置 Forge 专用 OSS 发布账号。' -ForegroundColor Cyan
Write-Host '按提示只输入值，不要输入 Region: 或 AccessKey: 等字段名称。'
Write-Host 'Region 请填写：cn-beijing' -ForegroundColor Yellow
Write-Host 'Security Token 没有则直接按回车。' -ForegroundColor Yellow
Write-Host ''

& $ossutil config
if ($LASTEXITCODE -ne 0) {
    throw "ossutil 配置失败，退出码：$LASTEXITCODE"
}

Write-Host ''
Write-Host '配置程序已经结束。请回到 Codex 告诉我“配置完成”。' -ForegroundColor Green
Read-Host '按回车关闭窗口'
