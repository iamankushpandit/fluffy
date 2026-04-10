<#
.SYNOPSIS
    Builds and deploys the Fluffy Batch Example to a local Minikube Kubernetes cluster
    with a live PostgreSQL database.

.DESCRIPTION
    This script:
    1. Verifies that required tools (Java 21, Maven, Docker, Minikube, kubectl) are installed.
    2. Starts Minikube if it is not already running.
    3. Builds the parent Maven project (fluffy-batch-starter + fluffy-batch-example).
    4. Builds the Docker image inside Minikube's Docker daemon.
    5. Deploys PostgreSQL and the example app to the "fluffy" Kubernetes namespace.
    6. Waits for all pods to be ready and prints the application URL.

.NOTES
    Run from the repository root directory in a PowerShell terminal.
    Requires Windows 10/11 with Docker Desktop or Hyper-V enabled.

.EXAMPLE
    .\setup-and-deploy.ps1
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------------------
# Helper functions
# ---------------------------------------------------------------------------

function Write-Step {
    param([string]$Message)
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
}

function Assert-Command {
    param([string]$Name, [string]$InstallHint)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Write-Host "ERROR: '$Name' is not installed or not on PATH." -ForegroundColor Red
        Write-Host "Install hint: $InstallHint" -ForegroundColor Yellow
        exit 1
    }
}

# ---------------------------------------------------------------------------
# 1. Check prerequisites
# ---------------------------------------------------------------------------

Write-Step "Checking prerequisites"

Assert-Command "java"   "Install JDK 21+: https://adoptium.net/"
Assert-Command "mvn"    "Install Maven 3.8+: https://maven.apache.org/download.cgi"
Assert-Command "docker" "Install Docker Desktop: https://www.docker.com/products/docker-desktop/"

# Verify Java 21+
$javaVersion = & java -version 2>&1 | Select-Object -First 1
Write-Host "Java version: $javaVersion"
if ($javaVersion -notmatch '(21|22|23|24|25)') {
    Write-Host "WARNING: Java 21+ is required. Current: $javaVersion" -ForegroundColor Yellow
}

# Install minikube if missing
if (-not (Get-Command "minikube" -ErrorAction SilentlyContinue)) {
    Write-Step "Installing Minikube"
    if (Get-Command "winget" -ErrorAction SilentlyContinue) {
        & winget install --id Kubernetes.minikube --accept-package-agreements --accept-source-agreements
    } elseif (Get-Command "choco" -ErrorAction SilentlyContinue) {
        & choco install minikube -y
    } else {
        Write-Host "ERROR: Cannot auto-install Minikube. Install manually:" -ForegroundColor Red
        Write-Host "  https://minikube.sigs.k8s.io/docs/start/" -ForegroundColor Yellow
        exit 1
    }
    # Refresh PATH
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
                [System.Environment]::GetEnvironmentVariable("Path", "User")
}

# Install kubectl if missing
if (-not (Get-Command "kubectl" -ErrorAction SilentlyContinue)) {
    Write-Step "Installing kubectl"
    if (Get-Command "winget" -ErrorAction SilentlyContinue) {
        & winget install --id Kubernetes.kubectl --accept-package-agreements --accept-source-agreements
    } elseif (Get-Command "choco" -ErrorAction SilentlyContinue) {
        & choco install kubernetes-cli -y
    } else {
        Write-Host "ERROR: Cannot auto-install kubectl. Install manually:" -ForegroundColor Red
        Write-Host "  https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/" -ForegroundColor Yellow
        exit 1
    }
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
                [System.Environment]::GetEnvironmentVariable("Path", "User")
}

Write-Host "All prerequisites verified." -ForegroundColor Green

# ---------------------------------------------------------------------------
# 2. Start Minikube
# ---------------------------------------------------------------------------

Write-Step "Starting Minikube"

$minikubeStatus = & minikube status --format "{{.Host}}" 2>&1
if ($minikubeStatus -ne "Running") {
    Write-Host "Starting Minikube cluster..."
    & minikube start --driver=docker --memory=4096 --cpus=2
} else {
    Write-Host "Minikube is already running."
}

# ---------------------------------------------------------------------------
# 3. Build the Maven project
# ---------------------------------------------------------------------------

Write-Step "Building Maven project (parent + starter + example)"

& mvn clean package -DskipTests -B
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Maven build failed." -ForegroundColor Red
    exit 1
}

Write-Host "Maven build succeeded." -ForegroundColor Green

# ---------------------------------------------------------------------------
# 4. Build Docker image inside Minikube
# ---------------------------------------------------------------------------

Write-Step "Building Docker image inside Minikube"

# Point Docker CLI to Minikube's Docker daemon
& minikube docker-env --shell powershell | Invoke-Expression

Push-Location fluffy-batch-example
try {
    & docker build -t fluffy-batch-example:latest .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Docker build failed." -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

Write-Host "Docker image built successfully." -ForegroundColor Green

# ---------------------------------------------------------------------------
# 5. Deploy to Kubernetes
# ---------------------------------------------------------------------------

Write-Step "Deploying to Kubernetes (namespace: fluffy)"

# Create namespace if it doesn't exist
$ns = & kubectl get namespace fluffy --no-headers 2>&1
if ($LASTEXITCODE -ne 0) {
    & kubectl create namespace fluffy
}

# Deploy PostgreSQL
Write-Host "Deploying PostgreSQL..."
& kubectl apply -f k8s/postgres.yaml -n fluffy

# Wait for PostgreSQL to be ready
Write-Host "Waiting for PostgreSQL to be ready..."
& kubectl rollout status deployment/postgres -n fluffy --timeout=120s
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: PostgreSQL deployment failed." -ForegroundColor Red
    exit 1
}

# Deploy the example app
Write-Host "Deploying Fluffy Batch Example..."
& kubectl apply -f k8s/app.yaml -n fluffy

# Wait for the app to be ready
Write-Host "Waiting for application to be ready..."
& kubectl rollout status deployment/fluffy-batch-example -n fluffy --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Application deployment failed." -ForegroundColor Red
    Write-Host "Check logs: kubectl logs -l app=fluffy-batch-example -n fluffy" -ForegroundColor Yellow
    exit 1
}

# ---------------------------------------------------------------------------
# 6. Print access information
# ---------------------------------------------------------------------------

Write-Step "Deployment complete!"

$serviceUrl = & minikube service fluffy-batch-example -n fluffy --url 2>&1 | Select-Object -First 1

Write-Host ""
Write-Host "Fluffy Batch Example is running!" -ForegroundColor Green
Write-Host ""
Write-Host "  Application URL : $serviceUrl" -ForegroundColor White
Write-Host "  Dashboard       : $serviceUrl/fluffy-dashboard/index.html" -ForegroundColor White
Write-Host "  API Base        : $serviceUrl/api/jobs" -ForegroundColor White
Write-Host "  Registered Jobs : $serviceUrl/api/jobs/registered" -ForegroundColor White
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Yellow
Write-Host "  kubectl get pods -n fluffy              # Check pod status"
Write-Host "  kubectl logs -l app=fluffy-batch-example -n fluffy  # View app logs"
Write-Host "  kubectl logs -l app=postgres -n fluffy  # View PostgreSQL logs"
Write-Host "  minikube dashboard                      # Open K8s dashboard"
Write-Host ""
Write-Host "To tear down:" -ForegroundColor Yellow
Write-Host "  kubectl delete namespace fluffy"
Write-Host "  minikube stop"
