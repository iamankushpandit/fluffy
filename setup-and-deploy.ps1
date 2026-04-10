<#
.SYNOPSIS
    Builds and deploys the Fluffy Batch Example to a local Minikube Kubernetes cluster
    with a live PostgreSQL database.

.DESCRIPTION
    This script:
    1. Verifies required tools are installed (Java 21, Maven, Docker); installs
       Minikube and kubectl only if not already present.
    2. Tears down any existing Fluffy deployment before re-deploying.
    3. Starts Minikube if not already running.
    4. Builds the parent Maven project (fluffy-batch-starter + fluffy-batch-example).
    5. Builds the Docker image inside Minikube's Docker daemon.
    6. Deploys PostgreSQL and the example app to the "fluffy" Kubernetes namespace.
    7. Verifies every step before proceeding to the next.
    8. Prints the application URL including the dashboard link.

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

function Write-OK {
    param([string]$Message)
    Write-Host "  [OK] $Message" -ForegroundColor Green
}

function Write-Skip {
    param([string]$Message)
    Write-Host "  [SKIP] $Message" -ForegroundColor DarkGray
}

function Assert-Command {
    param([string]$Name, [string]$InstallHint)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Write-Host "  [FAIL] '$Name' is not installed or not on PATH." -ForegroundColor Red
        Write-Host "  Install hint: $InstallHint" -ForegroundColor Yellow
        exit 1
    }
    Write-OK "'$Name' found"
}

function Refresh-Path {
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
                [System.Environment]::GetEnvironmentVariable("Path", "User")
}

function Install-IfMissing {
    param(
        [string]$Name,
        [string]$WingetId,
        [string]$ChocoPackage,
        [string]$ManualUrl
    )
    if (Get-Command $Name -ErrorAction SilentlyContinue) {
        Write-Skip "'$Name' is already installed"
        return
    }
    Write-Host "  Installing $Name..." -ForegroundColor Yellow
    if (Get-Command "winget" -ErrorAction SilentlyContinue) {
        & winget install --id $WingetId --accept-package-agreements --accept-source-agreements
    } elseif (Get-Command "choco" -ErrorAction SilentlyContinue) {
        & choco install $ChocoPackage -y
    } else {
        Write-Host "  [FAIL] Cannot auto-install $Name. Install manually:" -ForegroundColor Red
        Write-Host "    $ManualUrl" -ForegroundColor Yellow
        exit 1
    }
    Refresh-Path
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Write-Host "  [FAIL] $Name installed but not found on PATH. Restart terminal and try again." -ForegroundColor Red
        exit 1
    }
    Write-OK "$Name installed successfully"
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
Write-Host "  Java version: $javaVersion"
if ($javaVersion -notmatch '(21|22|23|24|25)') {
    Write-Host "  [WARN] Java 21+ is required. Current: $javaVersion" -ForegroundColor Yellow
}

Install-IfMissing -Name "minikube" `
    -WingetId "Kubernetes.minikube" `
    -ChocoPackage "minikube" `
    -ManualUrl "https://minikube.sigs.k8s.io/docs/start/"

Install-IfMissing -Name "kubectl" `
    -WingetId "Kubernetes.kubectl" `
    -ChocoPackage "kubernetes-cli" `
    -ManualUrl "https://kubernetes.io/docs/tasks/tools/install-kubectl-windows/"

Write-OK "All prerequisites verified"

# ---------------------------------------------------------------------------
# 2. Tear down any existing deployment
# ---------------------------------------------------------------------------

Write-Step "Cleaning up existing deployment (if any)"

$nsExists = & kubectl get namespace fluffy --no-headers 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  Found existing 'fluffy' namespace — deleting..." -ForegroundColor Yellow
    & kubectl delete namespace fluffy --timeout=120s 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARN] Could not fully delete namespace. Continuing anyway." -ForegroundColor Yellow
    } else {
        Write-OK "Existing deployment removed"
    }
} else {
    Write-Skip "No existing 'fluffy' namespace found"
}

# ---------------------------------------------------------------------------
# 3. Start Minikube
# ---------------------------------------------------------------------------

Write-Step "Starting Minikube"

$minikubeStatus = & minikube status --format "{{.Host}}" 2>&1
if ($minikubeStatus -eq "Running") {
    Write-Skip "Minikube is already running"
} else {
    Write-Host "  Starting Minikube cluster..."
    & minikube start --driver=docker --memory=4096 --cpus=2
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Could not start Minikube." -ForegroundColor Red
        exit 1
    }
    # Verify
    $verify = & minikube status --format "{{.Host}}" 2>&1
    if ($verify -ne "Running") {
        Write-Host "  [FAIL] Minikube started but is not in Running state." -ForegroundColor Red
        exit 1
    }
    Write-OK "Minikube started"
}

# ---------------------------------------------------------------------------
# 4. Build the Maven project
# ---------------------------------------------------------------------------

Write-Step "Building Maven project (parent + starter + example)"

& mvn clean package -DskipTests -B
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Maven build failed." -ForegroundColor Red
    exit 1
}

# Verify the example jar was produced
$exampleJar = Get-ChildItem -Path "fluffy-batch-starter/fluffy-batch-example/target/fluffy-batch-example-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $exampleJar) {
    Write-Host "  [FAIL] Example JAR not found after build." -ForegroundColor Red
    exit 1
}
Write-OK "Maven build succeeded — $($exampleJar.Name)"

# ---------------------------------------------------------------------------
# 5. Build Docker image inside Minikube
# ---------------------------------------------------------------------------

Write-Step "Building Docker image inside Minikube"

# Point Docker CLI to Minikube's Docker daemon
& minikube docker-env --shell powershell | Invoke-Expression

Push-Location fluffy-batch-starter/fluffy-batch-example
try {
    & docker build -t fluffy-batch-example:latest .
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Docker build failed." -ForegroundColor Red
        exit 1
    }
} finally {
    Pop-Location
}

# Verify image exists
$imageCheck = & docker images fluffy-batch-example:latest --format "{{.Repository}}" 2>&1
if ($imageCheck -ne "fluffy-batch-example") {
    Write-Host "  [FAIL] Docker image not found after build." -ForegroundColor Red
    exit 1
}
Write-OK "Docker image built and verified"

# ---------------------------------------------------------------------------
# 6. Deploy to Kubernetes
# ---------------------------------------------------------------------------

Write-Step "Deploying to Kubernetes (namespace: fluffy)"

# Create namespace
& kubectl create namespace fluffy
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Could not create namespace." -ForegroundColor Red
    exit 1
}
Write-OK "Namespace 'fluffy' created"

# Deploy PostgreSQL
Write-Host "  Deploying PostgreSQL..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/postgres.yaml -n fluffy
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] PostgreSQL manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for PostgreSQL to be ready..."
& kubectl rollout status deployment/postgres -n fluffy --timeout=120s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] PostgreSQL deployment did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl describe pods -l app=postgres -n fluffy" -ForegroundColor Yellow
    exit 1
}
Write-OK "PostgreSQL is ready"

# Deploy the example app (original postgres-backed instance)
Write-Host "  Deploying Fluffy Batch Example..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app.yaml -n fluffy
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Application manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for application to be ready..."
& kubectl rollout status deployment/fluffy-batch-example -n fluffy --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Application did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl logs -l app=fluffy-batch-example -n fluffy" -ForegroundColor Yellow
    exit 1
}
Write-OK "Application is ready"

# Deploy Kafka
Write-Host "  Deploying Kafka..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/kafka.yaml -n fluffy
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Kafka manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for Kafka to be ready..."
& kubectl rollout status deployment/kafka -n fluffy --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Kafka deployment did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl describe pods -l app=kafka -n fluffy" -ForegroundColor Yellow
    exit 1
}
Write-OK "Kafka is ready"

# Deploy H2-backed instance
Write-Host "  Deploying Fluffy Batch H2 instance..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app-h2.yaml -n fluffy
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] H2 instance manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for H2 instance to be ready..."
& kubectl rollout status deployment/fluffy-batch-h2 -n fluffy --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] H2 instance did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl logs -l app=fluffy-batch-h2 -n fluffy" -ForegroundColor Yellow
    exit 1
}
Write-OK "H2 instance is ready"

# Deploy database-backed instance
Write-Host "  Deploying Fluffy Batch DB instance..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app-db.yaml -n fluffy
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] DB instance manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for DB instance to be ready..."
& kubectl rollout status deployment/fluffy-batch-db -n fluffy --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] DB instance did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl logs -l app=fluffy-batch-db -n fluffy" -ForegroundColor Yellow
    exit 1
}
Write-OK "DB instance is ready"

# Deploy Kafka-backed instance
Write-Host "  Deploying Fluffy Batch Kafka instance..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app-kafka.yaml -n fluffy
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Kafka instance manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for Kafka instance to be ready..."
& kubectl rollout status deployment/fluffy-batch-kafka -n fluffy --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Kafka instance did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl logs -l app=fluffy-batch-kafka -n fluffy" -ForegroundColor Yellow
    exit 1
}
Write-OK "Kafka instance is ready"

# Optionally apply HorizontalPodAutoscaler
Write-Host "  Applying HorizontalPodAutoscaler for DB instance..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/hpa.yaml -n fluffy
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [WARN] HPA apply failed (metrics-server may not be available). Skipping." -ForegroundColor Yellow
} else {
    Write-OK "HPA applied (fluffy-batch-db: 1-5 replicas, 70% CPU target)"
}

# ---------------------------------------------------------------------------
# 7. Print access information
# ---------------------------------------------------------------------------

Write-Step "Deployment complete!"

$serviceUrl = & minikube service fluffy-batch-example -n fluffy --url 2>&1 | Select-Object -First 1
$minikubeIp = & minikube ip 2>&1

Write-Host ""
Write-Host "  Fluffy Batch Example is running!" -ForegroundColor Green
Write-Host ""
Write-Host "  --- Original instance (postgres profile) ---" -ForegroundColor White
Write-Host "  Dashboard       : $serviceUrl/fluffy-dashboard/index.html" -ForegroundColor White
Write-Host "  Application URL : $serviceUrl" -ForegroundColor White
Write-Host "  API Base        : $serviceUrl/api/jobs" -ForegroundColor White
Write-Host "  Registered Jobs : $serviceUrl/api/jobs/registered" -ForegroundColor White
Write-Host ""
Write-Host "  --- H2 instance (in-memory, no external DB) ---" -ForegroundColor White
Write-Host "  Application URL : http://${minikubeIp}:30081" -ForegroundColor White
Write-Host "  H2 Console      : http://${minikubeIp}:30081/h2-console" -ForegroundColor White
Write-Host "  API Base        : http://${minikubeIp}:30081/api/jobs" -ForegroundColor White
Write-Host ""
Write-Host "  --- Database instance (database profile) ---" -ForegroundColor White
Write-Host "  Application URL : http://${minikubeIp}:30082" -ForegroundColor White
Write-Host "  API Base        : http://${minikubeIp}:30082/api/jobs" -ForegroundColor White
Write-Host ""
Write-Host "  --- Kafka instance (kafka profile) ---" -ForegroundColor White
Write-Host "  Application URL : http://${minikubeIp}:30083" -ForegroundColor White
Write-Host "  API Base        : http://${minikubeIp}:30083/api/jobs" -ForegroundColor White
Write-Host ""
Write-Host "  Useful commands:" -ForegroundColor Yellow
Write-Host "    kubectl get pods -n fluffy                                # Check pod status"
Write-Host "    kubectl logs -l app=fluffy-batch-example -n fluffy        # View original app logs"
Write-Host "    kubectl logs -l app=fluffy-batch-h2 -n fluffy             # View H2 instance logs"
Write-Host "    kubectl logs -l app=fluffy-batch-db -n fluffy             # View DB instance logs"
Write-Host "    kubectl logs -l app=fluffy-batch-kafka -n fluffy          # View Kafka instance logs"
Write-Host "    kubectl logs -l app=postgres -n fluffy                    # View PostgreSQL logs"
Write-Host "    kubectl logs -l app=kafka -n fluffy                       # View Kafka logs"
Write-Host "    minikube dashboard                                        # Open K8s dashboard"
Write-Host ""
Write-Host "  To tear down:" -ForegroundColor Yellow
Write-Host "    kubectl delete namespace fluffy"
Write-Host "    minikube stop"
