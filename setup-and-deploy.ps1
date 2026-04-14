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
    6. Deploys PostgreSQL and the example app to a dedicated Kubernetes namespace.
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
# Configuration â€” change the namespace to distinguish example deployments
# from the Fluffy framework itself.
# ---------------------------------------------------------------------------
$Namespace = "fluffy-example-1"

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

# Run a native command capturing stderr without triggering ErrorActionPreference=Stop.
# Returns combined stdout+stderr as strings; sets $LASTEXITCODE.
function Invoke-Native {
    $cmd = $args
    $output = $null
    $eap = $ErrorActionPreference
    try {
        $ErrorActionPreference = "SilentlyContinue"
        $rawOutput = & $cmd[0] $cmd[1..($cmd.Length-1)] 2>&1
        $exitCode = $LASTEXITCODE
        $output = $rawOutput | ForEach-Object { "$_" }
    } finally {
        $ErrorActionPreference = $eap
        $global:LASTEXITCODE = $exitCode
    }
    return $output
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
# 0. Auto-discover tools and add them to PATH if not already available
# ---------------------------------------------------------------------------

# Common install locations to probe when a tool is not on PATH
$toolSearchPaths = @(
    # Java / JDK
    "C:\Program Files\Eclipse Adoptium\*\bin"
    "C:\Program Files\Java\*\bin"
    "C:\Program Files\Microsoft\*\bin"
    "C:\Program Files\Zulu\*\bin"
    "C:\Program Files\Android\Android Studio\jbr\bin"
    # Maven
    "C:\Program Files\apache-maven-*\bin"
    "C:\ProgramData\chocolatey\lib\maven\apache-maven-*\bin"
    "C:\tools\apache-maven-*\bin"
    "$env:USERPROFILE\apache-maven-*\bin"
    "C:\Program Files\JetBrains\*\plugins\maven\lib\maven3\bin"
    # Docker
    "C:\Program Files\Docker\Docker\resources\bin"
)

function Find-AndAddToPath {
    param([string]$ExeName)
    if (Get-Command $ExeName -ErrorAction SilentlyContinue) { return }
    foreach ($pattern in $toolSearchPaths) {
        $dirs = Resolve-Path $pattern -ErrorAction SilentlyContinue
        foreach ($dir in $dirs) {
            # Check for .exe, .cmd, and .bat variants
            $found = (Test-Path (Join-Path $dir.Path "$ExeName.exe")) -or
                     (Test-Path (Join-Path $dir.Path "$ExeName.cmd")) -or
                     (Test-Path (Join-Path $dir.Path "$ExeName.bat"))
            if ($found) {
                Write-Host "  [AUTO] Found '$ExeName' at $($dir.Path)" -ForegroundColor DarkYellow
                $env:PATH = "$($dir.Path);$env:PATH"
                # Set JAVA_HOME if we just found java
                if ($ExeName -eq "java") {
                    $env:JAVA_HOME = Split-Path $dir.Path -Parent
                    Write-Host "  [AUTO] Set JAVA_HOME=$env:JAVA_HOME" -ForegroundColor DarkYellow
                }
                return
            }
        }
    }
}

# Refresh PATH from system/user environment first
Refresh-Path

# Try to locate tools that might not be on the terminal's PATH
Find-AndAddToPath "java"
Find-AndAddToPath "mvn"
Find-AndAddToPath "docker"

# ---------------------------------------------------------------------------
# 1. Check prerequisites
# ---------------------------------------------------------------------------

Write-Step "Checking prerequisites"

Assert-Command "java"   "Install JDK 21+: https://adoptium.net/"
Assert-Command "mvn"    "Install Maven 3.8+: https://maven.apache.org/download.cgi"
Assert-Command "docker" "Install Docker Desktop: https://www.docker.com/products/docker-desktop/"

# Verify Java 21+
$javaVersion = (Invoke-Native java -version) | Select-Object -First 1
Write-Host "  Java version: $javaVersion"
if ($javaVersion -notmatch '(21|22|23|24|25)') {
    Write-Host "  [WARN] Java 21+ is required. Current: $javaVersion" -ForegroundColor Yellow
}

# Ensure Docker daemon is running (start Docker Desktop if needed)
# Clear DOCKER_HOST in case a previous minikube docker-env left it pointing at a stale port
$savedDockerHost = $env:DOCKER_HOST
$env:DOCKER_HOST = $null
$dockerReady = Invoke-Native docker info
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Docker daemon is not running. Attempting to start Docker Desktop..." -ForegroundColor Yellow
    $dockerDesktopPath = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (Test-Path $dockerDesktopPath) {
        Start-Process $dockerDesktopPath
        Write-Host "  Waiting for Docker daemon to be ready (up to 120s)..." -ForegroundColor Yellow
        $waited = 0
        $maxWait = 120
        while ($waited -lt $maxWait) {
            Start-Sleep -Seconds 3
            $waited += 3
            Invoke-Native docker info | Out-Null
            if ($LASTEXITCODE -eq 0) { break }
            Write-Host "    Still waiting... ($waited`s)" -ForegroundColor DarkGray
        }
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [FAIL] Docker Desktop did not start within ${maxWait}s. Start it manually and re-run." -ForegroundColor Red
            exit 1
        }
        Write-OK "Docker Desktop is now running"
    } else {
        Write-Host "  [FAIL] Docker Desktop not found at expected path." -ForegroundColor Red
        Write-Host "  Install hint: Install Docker Desktop: https://www.docker.com/products/docker-desktop/" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-OK "Docker daemon is running"
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

$nsExists = Invoke-Native kubectl get namespace $Namespace --no-headers
if ($LASTEXITCODE -eq 0) {
    Write-Host "  Found existing '$Namespace' namespace - deleting..." -ForegroundColor Yellow
    Invoke-Native kubectl delete namespace $Namespace --timeout=120s | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARN] Could not fully delete namespace. Continuing anyway." -ForegroundColor Yellow
    } else {
        Write-OK "Existing deployment removed"
    }
} else {
    Write-Skip "No existing '$Namespace' namespace found"
}

# ---------------------------------------------------------------------------
# 3. Start Minikube
# ---------------------------------------------------------------------------

Write-Step "Starting Minikube"

$minikubeStatus = Invoke-Native minikube status --format "{{.Host}}"
if ($minikubeStatus -eq "Running") {
    Write-Skip "Minikube is already running"
} else {
    Write-Host "  Starting Minikube cluster..."
    Invoke-Native minikube start --driver=docker --memory=4096 --cpus=2 | Out-Null
    # Verify by checking actual status (minikube start exits non-zero on benign warnings)
    $verify = Invoke-Native minikube status --format "{{.Host}}"
    if ($verify -ne "Running") {
        Write-Host "  [FAIL] Minikube failed to start. Run 'minikube delete' and try again." -ForegroundColor Red
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
Write-OK "Maven build succeeded - $($exampleJar.Name)"

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
$imageCheck = Invoke-Native docker images fluffy-batch-example:latest --format "{{.Repository}}"
if ($imageCheck -ne "fluffy-batch-example") {
    Write-Host "  [FAIL] Docker image not found after build." -ForegroundColor Red
    exit 1
}
Write-OK "Docker image built and verified"

# ---------------------------------------------------------------------------
# 6. Deploy to Kubernetes
# ---------------------------------------------------------------------------

Write-Step "Deploying to Kubernetes (namespace: $Namespace)"

# Create namespace (ignore if it already exists)
$nsCheck = Invoke-Native kubectl get namespace $Namespace --no-headers
if ($LASTEXITCODE -eq 0) {
    Write-OK "Namespace '$Namespace' already exists"
} else {
    Invoke-Native kubectl create namespace $Namespace | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [FAIL] Could not create namespace." -ForegroundColor Red
        exit 1
    }
    Write-OK "Namespace '$Namespace' created"
}

# Deploy PostgreSQL
Write-Host "  Deploying PostgreSQL..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/postgres.yaml -n $Namespace
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] PostgreSQL manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for PostgreSQL to be ready..."
& kubectl rollout status deployment/postgres -n $Namespace --timeout=120s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] PostgreSQL deployment did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl describe pods -l app=postgres -n $Namespace" -ForegroundColor Yellow
    exit 1
}
Write-OK "PostgreSQL is ready"

# Deploy the example app (original postgres-backed instance)
Write-Host "  Deploying Fluffy Batch Example..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app.yaml -n $Namespace
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Application manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for application to be ready..."
& kubectl rollout status deployment/fluffy-batch-example -n $Namespace --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Application did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl logs -l app=fluffy-batch-example -n $Namespace" -ForegroundColor Yellow
    exit 1
}
Write-OK "Application is ready"

# Deploy Kafka
Write-Host "  Deploying Kafka..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/kafka.yaml -n $Namespace
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Kafka manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for Kafka to be ready..."
& kubectl rollout status deployment/kafka -n $Namespace --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Kafka deployment did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl describe pods -l app=kafka -n $Namespace" -ForegroundColor Yellow
    exit 1
}
Write-OK "Kafka is ready"

# Deploy H2-backed instance
Write-Host "  Deploying Fluffy Batch H2 instance..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app-h2.yaml -n $Namespace
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] H2 instance manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for H2 instance to be ready..."
& kubectl rollout status deployment/fluffy-batch-h2 -n $Namespace --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] H2 instance did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl logs -l app=fluffy-batch-h2 -n $Namespace" -ForegroundColor Yellow
    exit 1
}
Write-OK "H2 instance is ready"

# Deploy database-backed instance
Write-Host "  Deploying Fluffy Batch DB instance..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app-db.yaml -n $Namespace
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] DB instance manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for DB instance to be ready..."
& kubectl rollout status deployment/fluffy-batch-db -n $Namespace --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] DB instance did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl logs -l app=fluffy-batch-db -n $Namespace" -ForegroundColor Yellow
    exit 1
}
Write-OK "DB instance is ready"

# Deploy Kafka-backed instance
Write-Host "  Deploying Fluffy Batch Kafka instance..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app-kafka.yaml -n $Namespace
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Kafka instance manifest apply failed." -ForegroundColor Red
    exit 1
}

Write-Host "  Waiting for Kafka instance to be ready..."
& kubectl rollout status deployment/fluffy-batch-kafka -n $Namespace --timeout=180s
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [FAIL] Kafka instance did not become ready." -ForegroundColor Red
    Write-Host "  Check: kubectl logs -l app=fluffy-batch-kafka -n $Namespace" -ForegroundColor Yellow
    exit 1
}
Write-OK "Kafka instance is ready"

# Deploy aggregator node (React/MUI multi-node dashboard)
Write-Host ""
Write-Host "  [Aggregator] Deploying aggregator dashboard..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app-aggregator.yaml -n $Namespace
if ($LASTEXITCODE -ne 0) { Write-Host "  [ERROR] Aggregator deploy failed" -ForegroundColor Red; exit 1 }
Write-OK "Aggregator deployment applied"
Write-Host "  Waiting for aggregator rollout..."
Invoke-Native kubectl rollout status deployment/fluffy-aggregator -n $Namespace --timeout=120s
Write-OK "Aggregator is running"

# Optionally apply HorizontalPodAutoscaler
Write-Host "  Applying HorizontalPodAutoscaler for DB instance..."
& kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/hpa.yaml -n $Namespace
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [WARN] HPA apply failed (metrics-server may not be available). Skipping." -ForegroundColor Yellow
} else {
    Write-OK "HPA applied (fluffy-batch-db: 1-5 replicas, 70% CPU target)"
}

# ---------------------------------------------------------------------------
# 7. Print access information
# ---------------------------------------------------------------------------

Write-Step "Deployment complete!"

# On Docker driver (Windows), the Minikube VM IP is not reachable from the host.
# Use kubectl port-forward to expose services on localhost.
$minikubeDriver = (Invoke-Native minikube profile list -o json) | Out-String
$usePortForward = $minikubeDriver -match '"Driver"\s*:\s*"docker"'

if ($usePortForward) {
    Write-Host "  Docker driver detected - setting up port-forwarding to localhost..." -ForegroundColor Yellow

    # Kill any leftover port-forward jobs
    Get-Job -Name "fluffy-pf-*" -ErrorAction SilentlyContinue | Stop-Job -PassThru | Remove-Job

    Start-Job -Name "fluffy-pf-example" -ScriptBlock { param($ns) kubectl port-forward svc/fluffy-batch-example 8080:8080 -n $ns } -ArgumentList $Namespace | Out-Null
    Start-Job -Name "fluffy-pf-h2"      -ScriptBlock { param($ns) kubectl port-forward svc/fluffy-batch-h2      8081:8080 -n $ns } -ArgumentList $Namespace | Out-Null
    Start-Job -Name "fluffy-pf-db"      -ScriptBlock { param($ns) kubectl port-forward svc/fluffy-batch-db      8082:8080 -n $ns } -ArgumentList $Namespace | Out-Null
    Start-Job -Name "fluffy-pf-kafka"   -ScriptBlock { param($ns) kubectl port-forward svc/fluffy-batch-kafka   8083:8080 -n $ns } -ArgumentList $Namespace | Out-Null
    Start-Job -Name "fluffy-pf-aggr"    -ScriptBlock { param($ns) kubectl port-forward svc/fluffy-aggregator    8084:8080 -n $ns } -ArgumentList $Namespace | Out-Null

    # Give port-forwards a moment to start
    Start-Sleep -Seconds 2

    $failedJobs = Get-Job -Name "fluffy-pf-*" | Where-Object { $_.State -eq 'Failed' }
    if ($failedJobs) {
        Write-Host "  [WARN] Some port-forwards failed to start. Check with: Get-Job | Receive-Job" -ForegroundColor Yellow
    } else {
        Write-OK "Port-forwarding active (background jobs)"
    }

    $exampleUrl  = "http://localhost:8080"
    $h2Url       = "http://localhost:8081"
    $dbUrl       = "http://localhost:8082"
    $kafkaAppUrl = "http://localhost:8083"
    $aggrUrl     = "http://localhost:8084"
} else {
    # Non-Docker driver: NodePort IPs are reachable directly
    $minikubeIp = (Invoke-Native minikube ip) | Select-Object -First 1
    if (-not $minikubeIp) { $minikubeIp = "localhost" }

    function Get-NodePort {
        param([string]$ServiceName)
        $port = (Invoke-Native kubectl get svc $ServiceName -n $Namespace -o "jsonpath={.spec.ports[0].nodePort}") | Select-Object -First 1
        return $port
    }

    $exampleUrl  = "http://${minikubeIp}:$(Get-NodePort 'fluffy-batch-example')"
    $h2Url       = "http://${minikubeIp}:$(Get-NodePort 'fluffy-batch-h2')"
    $dbUrl       = "http://${minikubeIp}:$(Get-NodePort 'fluffy-batch-db')"
    $kafkaAppUrl = "http://${minikubeIp}:$(Get-NodePort 'fluffy-batch-kafka')"
    $aggrUrl     = "http://${minikubeIp}:$(Get-NodePort 'fluffy-aggregator')"
}

Write-Host ""
Write-Host "  Fluffy Batch Example is running!" -ForegroundColor Green
Write-Host ""
Write-Host "  --- Original instance (postgres profile) ---" -ForegroundColor White
Write-Host "  Dashboard       : $exampleUrl/fluffy-dashboard/index.html" -ForegroundColor White
Write-Host "  Application URL : $exampleUrl" -ForegroundColor White
Write-Host "  API Base        : $exampleUrl/api/jobs" -ForegroundColor White
Write-Host "  Registered Jobs : $exampleUrl/api/jobs/registered" -ForegroundColor White
Write-Host ""
Write-Host "  --- H2 instance (in-memory, no external DB) ---" -ForegroundColor White
Write-Host "  Dashboard       : $h2Url/fluffy-dashboard/index.html" -ForegroundColor White
Write-Host "  Application URL : $h2Url" -ForegroundColor White
Write-Host "  H2 Console      : $h2Url/h2-console" -ForegroundColor White
Write-Host "  API Base        : $h2Url/api/jobs" -ForegroundColor White
Write-Host ""
Write-Host "  --- Database instance (database profile) ---" -ForegroundColor White
Write-Host "  Dashboard       : $dbUrl/fluffy-dashboard/index.html" -ForegroundColor White
Write-Host "  Application URL : $dbUrl" -ForegroundColor White
Write-Host "  API Base        : $dbUrl/api/jobs" -ForegroundColor White
Write-Host ""
Write-Host "  --- Kafka instance (kafka profile) ---" -ForegroundColor White
Write-Host "  Dashboard       : $kafkaAppUrl/fluffy-dashboard/index.html" -ForegroundColor White
Write-Host "  Application URL : $kafkaAppUrl" -ForegroundColor White
Write-Host "  API Base        : $kafkaAppUrl/api/jobs" -ForegroundColor White
Write-Host ""
Write-Host "  --- Aggregator Dashboard (multi-node React/MUI) ---" -ForegroundColor Cyan
Write-Host "  Dashboard       : $aggrUrl/fluffy-aggregator" -ForegroundColor Cyan
Write-Host "  API Summary     : $aggrUrl/api/aggregator/summary" -ForegroundColor Cyan
Write-Host "  API Nodes       : $aggrUrl/api/aggregator/nodes" -ForegroundColor Cyan
Write-Host ""
Write-Host "  --- Kubernetes Dashboard ---" -ForegroundColor White
Write-Host "  Run: minikube dashboard" -ForegroundColor White
Write-Host ""
if ($usePortForward) {
    Write-Host "  NOTE: Port-forwarding is running as background jobs in this terminal." -ForegroundColor Yellow
    Write-Host "  Keep this terminal open for the URLs to remain accessible." -ForegroundColor Yellow
    Write-Host "  To stop: Get-Job -Name 'fluffy-pf-*' | Stop-Job | Remove-Job" -ForegroundColor Yellow
    Write-Host ""
}
Write-Host "  Useful commands:" -ForegroundColor Yellow
Write-Host "    kubectl get pods -n $Namespace                                # Check pod status"
Write-Host "    kubectl logs -l app=fluffy-batch-example -n $Namespace        # View original app logs"
Write-Host "    kubectl logs -l app=fluffy-batch-h2 -n $Namespace             # View H2 instance logs"
Write-Host "    kubectl logs -l app=fluffy-batch-db -n $Namespace             # View DB instance logs"
Write-Host "    kubectl logs -l app=fluffy-batch-kafka -n $Namespace          # View Kafka instance logs"
Write-Host "    kubectl logs -l app=fluffy-aggregator -n $Namespace           # View Aggregator logs"
Write-Host "    kubectl logs -l app=postgres -n $Namespace                    # View PostgreSQL logs"
Write-Host "    kubectl logs -l app=kafka -n $Namespace                       # View Kafka logs"
Write-Host "    minikube dashboard                                        # Open K8s dashboard"
Write-Host ""
Write-Host "  To tear down:" -ForegroundColor Yellow
Write-Host "    kubectl delete namespace $Namespace"
Write-Host "    minikube stop"

