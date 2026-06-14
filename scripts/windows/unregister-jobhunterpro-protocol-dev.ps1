$ErrorActionPreference = "Stop"

$protocolName = "jobhunterpro"

reg.exe delete "HKCU\Software\Classes\$protocolName" /f | Out-Null

Write-Host "JobHunterPro protocol handler removed for current user."