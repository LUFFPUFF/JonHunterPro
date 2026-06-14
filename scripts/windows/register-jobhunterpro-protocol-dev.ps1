$ErrorActionPreference = "Stop"

$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$forwardScriptPath = Join-Path $scriptDirectory "forward-hh-oauth-callback.ps1"

if (-not (Test-Path $forwardScriptPath)) {
    throw "Forward script was not found: $forwardScriptPath"
}

$protocolName = "jobhunterpro"
$protocolDisplayName = "URL:JobHunterPro Protocol"

$command = "powershell.exe -NoProfile -ExecutionPolicy Bypass -File `"$forwardScriptPath`" `"%1`""

reg.exe add "HKCU\Software\Classes\$protocolName" /ve /d "$protocolDisplayName" /f | Out-Null
reg.exe add "HKCU\Software\Classes\$protocolName" /v "URL Protocol" /d "" /f | Out-Null
reg.exe add "HKCU\Software\Classes\$protocolName\shell" /f | Out-Null
reg.exe add "HKCU\Software\Classes\$protocolName\shell\open" /f | Out-Null
reg.exe add "HKCU\Software\Classes\$protocolName\shell\open\command" /ve /d "$command" /f | Out-Null

Write-Host "JobHunterPro protocol handler registered for current user."
Write-Host "Protocol: ${protocolName}://"
Write-Host "Command: $command"