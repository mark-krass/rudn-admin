Param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$ConnectId
)

$OutputDir = Join-Path -Path $PSScriptRoot -ChildPath "out"
$FilePath = Join-Path -Path $OutputDir -ChildPath ("$ConnectId.ovpn")
if (Test-Path -Path $FilePath) {
    Remove-Item -Path $FilePath -Force
}


