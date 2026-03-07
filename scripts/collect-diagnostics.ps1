param(
    [string]$OutFile = "",
    [int]$TailLines = 200
)

if ([string]::IsNullOrWhiteSpace($OutFile)) {
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $OutFile = "diagnostics-$timestamp.log"
}

function Add-Section {
    param(
        [string]$Title
    )
    Add-Content -Path $OutFile -Value ""
    Add-Content -Path $OutFile -Value ("=" * 80)
    Add-Content -Path $OutFile -Value $Title
    Add-Content -Path $OutFile -Value ("=" * 80)
}

function Add-Block {
    param(
        [string]$Text
    )
    Add-Content -Path $OutFile -Value $Text
}

Set-Content -Path $OutFile -Value "Generated: $(Get-Date -Format o)"
Add-Block "Working Directory: $(Get-Location)"

Add-Section "Service Mode Snapshot"
try {
    $envFiles = @(".env.service-a", ".env.service-b", ".env")
    foreach ($file in $envFiles) {
        if (-not (Test-Path $file)) {
            continue
        }
        Add-Block "-- $file --"
        $lines = Get-Content $file | Where-Object {
            $_ -match "SERVICE_A_TOKEN_MODE|SERVICE_B_TOKEN_MODE|SERVICE_A_JWE_SECRET|SERVICE_B_JWE_SECRET|SERVICE_A_JWK_SET_URI|SERVICE_A_ISSUER|SERVICE_A_KEY_ID"
        }
        if ($lines.Count -eq 0) {
            Add-Block "No matching key lines found."
        }
        else {
            foreach ($line in $lines) {
                if ($line -match "SECRET=") {
                    $parts = $line.Split("=", 2)
                    Add-Block ("{0}=***masked***" -f $parts[0])
                }
                else {
                    Add-Block $line
                }
            }
        }
    }
}
catch {
    Add-Block ("Failed to read env files: {0}" -f $_.Exception.Message)
}

Add-Section "Process Snapshot (java/mvn/cmd/powershell)"
try {
    $processLines = Get-Process | Where-Object {
        $_.ProcessName -match "java|mvn|cmd|powershell"
    } | Sort-Object StartTime -Descending | Select-Object -First 50 ProcessName, Id, StartTime
    if ($processLines) {
        Add-Block ($processLines | Format-Table -AutoSize | Out-String)
    }
    else {
        Add-Block "No matching processes found."
    }
}
catch {
    Add-Block ("Failed to collect process snapshot: {0}" -f $_.Exception.Message)
}

Add-Section "Port Snapshot (9000/9001)"
try {
    $netstat = cmd /c "netstat -ano | findstr :9000 & netstat -ano | findstr :9001"
    if ($netstat) {
        Add-Block ($netstat | Out-String)
    }
    else {
        Add-Block "No bindings found for ports 9000/9001."
    }
}
catch {
    Add-Block ("Failed to collect netstat output: {0}" -f $_.Exception.Message)
}

Add-Section "Health Endpoints"
foreach ($url in @("http://localhost:9000/actuator/health", "http://localhost:9001/actuator/health")) {
    try {
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 5
        Add-Block ("{0} -> {1}" -f $url, ($response | ConvertTo-Json -Depth 5 -Compress))
    }
    catch {
        Add-Block ("{0} -> ERROR: {1}" -f $url, $_.Exception.Message)
    }
}

Add-Section "Log Tail: service-a-run.log"
if (Test-Path "service-a-run.log") {
    Add-Block ((Get-Content "service-a-run.log" -Tail $TailLines) -join [Environment]::NewLine)
}
else {
    Add-Block "service-a-run.log not found."
}

Add-Section "Log Tail: service-b-run.log"
if (Test-Path "service-b-run.log") {
    Add-Block ((Get-Content "service-b-run.log" -Tail $TailLines) -join [Environment]::NewLine)
}
else {
    Add-Block "service-b-run.log not found."
}

Write-Host "Diagnostics written to: $OutFile"
