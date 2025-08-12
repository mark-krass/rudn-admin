Param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$ConnectId
)

# Write the file to a sibling 'out' directory under the test resources scripts folder
$OutputDir = Join-Path -Path $PSScriptRoot -ChildPath "out"
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$FilePath = Join-Path -Path $OutputDir -ChildPath ("$ConnectId.ovpn")
"client`n# demo file for $ConnectId" | Out-File -FilePath $FilePath -Encoding UTF8 -Force


