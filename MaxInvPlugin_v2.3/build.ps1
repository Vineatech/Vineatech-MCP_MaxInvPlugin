
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) { Write-Error 'Maven not found in PATH.'; exit 1 }
mvn -version
mvn clean package
if ($LASTEXITCODE -ne 0) { Write-Error 'Build failed.'; exit 1 }
Write-Host "JAR built at: target\MaxInvPlugin-2.3.jar"
