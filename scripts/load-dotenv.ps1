param(
    [string]$Path = ".env"
)

if (-not (Test-Path $Path)) {
    throw "Env file not found at '$Path'. Create it from the appropriate example file."
}

Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if ($line.StartsWith("#")) { return }

    $separatorIndex = $line.IndexOf("=")
    if ($separatorIndex -le 0) { return }

    $key = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim()

    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
}

Write-Host "Loaded env vars from $Path"
