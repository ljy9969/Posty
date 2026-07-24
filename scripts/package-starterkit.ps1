# Builds the sale-ready starter kit zip in dist\.
# Uses `git archive`, so ONLY tracked files are included — keystore.properties,
# local.properties, *.apk / *.aab and all build output are excluded automatically.
# On top of that we exclude Korean-only / store-specific assets not meant for buyers.
$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)

New-Item -ItemType Directory -Force dist | Out-Null
$out = "dist\posty-starterkit.zip"

# NOTE: the Korean-named PDF is excluded by pattern — passing the literal
# filename through PowerShell breaks on encoding.
git archive HEAD -o $out `
    ":(exclude)*.pdf" `
    ":(exclude)play-assets" `
    ":(exclude)docs" `
    ":(exclude)scripts"
if ($LASTEXITCODE -ne 0) { throw "git archive failed" }

Write-Output "Created $out"
Write-Output "--- contents ---"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $out))
$zip.Entries | ForEach-Object { $_.FullName } | Sort-Object
$zip.Dispose()
