param(
    [string]$EnvFile = ".env"
)

& "$PSScriptRoot/load-dotenv.ps1" -Path $EnvFile
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Push-Location "$PSScriptRoot/../service-b"
try {
    .\mvnw.cmd spring-boot:run
}
finally {
    Pop-Location
}
