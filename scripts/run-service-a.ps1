param(
    [string]$EnvFile = ".env.service-a"
)

if (-not (Test-Path $EnvFile)) {
    $serviceLocalEnvFile = Join-Path "service-a" (Split-Path $EnvFile -Leaf)
    if (Test-Path $serviceLocalEnvFile) {
        Write-Host "Env file '$EnvFile' not found. Using '$serviceLocalEnvFile'."
        $EnvFile = $serviceLocalEnvFile
    }
    elseif (Test-Path ".env") {
        Write-Host "Env file '$EnvFile' not found. Falling back to '.env'."
        $EnvFile = ".env"
    }
    else {
        throw "Env file '$EnvFile' not found. Create it from .env.service-a.example."
    }
}

& "$PSScriptRoot/load-dotenv.ps1" -Path $EnvFile
if (-not $?) { exit 1 }

Push-Location "$PSScriptRoot/../service-a"
try {
    .\mvnw.cmd spring-boot:run
}
finally {
    Pop-Location
}
