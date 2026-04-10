#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# setup-and-deploy.sh
#
# Builds and deploys the Fluffy Batch Example to a local Minikube Kubernetes
# cluster with a live PostgreSQL database.
#
# This script:
#   1. Verifies required tools are installed (Java 21, Maven, Docker); installs
#      Minikube and kubectl via Homebrew only if not already present.
#   2. Tears down any existing Fluffy deployment before re-deploying.
#   3. Starts Minikube if not already running.
#   4. Builds the parent Maven project (fluffy-batch-starter + fluffy-batch-example).
#   5. Builds the Docker image inside Minikube's Docker daemon.
#   6. Deploys PostgreSQL and the example app to the "fluffy" Kubernetes namespace.
#   7. Verifies every step before proceeding to the next.
#   8. Prints the application URL including the dashboard link.
#
# Usage:
#   chmod +x setup-and-deploy.sh
#   ./setup-and-deploy.sh
#
# Requirements:
#   macOS with Homebrew, Docker Desktop (or compatible Docker runtime).
# ---------------------------------------------------------------------------

set -euo pipefail

# ---------------------------------------------------------------------------
# Helper functions
# ---------------------------------------------------------------------------

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
GRAY='\033[0;90m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

write_step() {
    printf "\n${CYAN}========================================${NC}\n"
    printf "${CYAN}  %s${NC}\n" "$1"
    printf "${CYAN}========================================${NC}\n"
}

write_ok() {
    printf "  ${GREEN}[OK]${NC} %s\n" "$1"
}

write_skip() {
    printf "  ${GRAY}[SKIP]${NC} %s\n" "$1"
}

write_fail() {
    printf "  ${RED}[FAIL]${NC} %s\n" "$1"
}

write_warn() {
    printf "  ${YELLOW}[WARN]${NC} %s\n" "$1"
}

assert_command() {
    local name="$1"
    local install_hint="$2"
    if ! command -v "$name" &>/dev/null; then
        write_fail "'$name' is not installed or not on PATH."
        printf "  ${YELLOW}Install hint: %s${NC}\n" "$install_hint"
        exit 1
    fi
    write_ok "'$name' found"
}

install_if_missing() {
    local name="$1"
    local brew_package="$2"
    local manual_url="$3"
    if command -v "$name" &>/dev/null; then
        write_skip "'$name' is already installed"
        return
    fi
    printf "  ${YELLOW}Installing %s...${NC}\n" "$name"
    if command -v brew &>/dev/null; then
        brew install "$brew_package"
    else
        write_fail "Cannot auto-install $name. Homebrew is not installed."
        printf "    Install Homebrew: https://brew.sh/\n"
        printf "    Then install $name: $manual_url\n"
        exit 1
    fi
    if ! command -v "$name" &>/dev/null; then
        write_fail "$name installed but not found on PATH. Restart terminal and try again."
        exit 1
    fi
    write_ok "$name installed successfully"
}

# ---------------------------------------------------------------------------
# 1. Check prerequisites
# ---------------------------------------------------------------------------

write_step "Checking prerequisites"

assert_command "java"   "Install JDK 21+: https://adoptium.net/ or brew install --cask temurin@21"
assert_command "mvn"    "Install Maven 3.8+: brew install maven"
assert_command "docker" "Install Docker Desktop: https://www.docker.com/products/docker-desktop/"

# Verify Java 21+
java_version=$(java -version 2>&1 | head -1)
printf "  Java version: %s\n" "$java_version"
if ! echo "$java_version" | grep -qE '"(21|22|23|24|25)'; then
    write_warn "Java 21+ is required. Current: $java_version"
fi

install_if_missing "minikube" "minikube" "https://minikube.sigs.k8s.io/docs/start/"
install_if_missing "kubectl"  "kubernetes-cli" "https://kubernetes.io/docs/tasks/tools/install-kubectl-macos/"

write_ok "All prerequisites verified"

# ---------------------------------------------------------------------------
# 2. Tear down any existing deployment
# ---------------------------------------------------------------------------

write_step "Cleaning up existing deployment (if any)"

if kubectl get namespace fluffy --no-headers 2>/dev/null; then
    printf "  ${YELLOW}Found existing 'fluffy' namespace — deleting...${NC}\n"
    if kubectl delete namespace fluffy --timeout=120s 2>/dev/null; then
        write_ok "Existing deployment removed"
    else
        write_warn "Could not fully delete namespace. Continuing anyway."
    fi
else
    write_skip "No existing 'fluffy' namespace found"
fi

# ---------------------------------------------------------------------------
# 3. Start Minikube
# ---------------------------------------------------------------------------

write_step "Starting Minikube"

minikube_status=$(minikube status --format "{{.Host}}" 2>/dev/null || true)
if [ "$minikube_status" = "Running" ]; then
    write_skip "Minikube is already running"
else
    printf "  Starting Minikube cluster...\n"
    minikube start --driver=docker --memory=4096 --cpus=2
    if [ $? -ne 0 ]; then
        write_fail "Could not start Minikube."
        exit 1
    fi
    # Verify
    verify=$(minikube status --format "{{.Host}}" 2>/dev/null || true)
    if [ "$verify" != "Running" ]; then
        write_fail "Minikube started but is not in Running state."
        exit 1
    fi
    write_ok "Minikube started"
fi

# ---------------------------------------------------------------------------
# 4. Build the Maven project
# ---------------------------------------------------------------------------

write_step "Building Maven project (parent + starter + example)"

mvn clean package -DskipTests -B
if [ $? -ne 0 ]; then
    write_fail "Maven build failed."
    exit 1
fi

# Verify the example jar was produced
example_jar=$(find fluffy-batch-starter/fluffy-batch-example/target -name "fluffy-batch-example-*.jar" -not -name "*-sources.jar" 2>/dev/null | head -1)
if [ -z "$example_jar" ]; then
    write_fail "Example JAR not found after build."
    exit 1
fi
write_ok "Maven build succeeded — $(basename "$example_jar")"

# ---------------------------------------------------------------------------
# 5. Build Docker image inside Minikube
# ---------------------------------------------------------------------------

write_step "Building Docker image inside Minikube"

# Point Docker CLI to Minikube's Docker daemon
eval "$(minikube docker-env)"

pushd fluffy-batch-starter/fluffy-batch-example >/dev/null
docker build -t fluffy-batch-example:latest .
if [ $? -ne 0 ]; then
    popd >/dev/null
    write_fail "Docker build failed."
    exit 1
fi
popd >/dev/null

# Verify image exists
image_check=$(docker images fluffy-batch-example:latest --format "{{.Repository}}" 2>/dev/null)
if [ "$image_check" != "fluffy-batch-example" ]; then
    write_fail "Docker image not found after build."
    exit 1
fi
write_ok "Docker image built and verified"

# ---------------------------------------------------------------------------
# 6. Deploy to Kubernetes
# ---------------------------------------------------------------------------

write_step "Deploying to Kubernetes (namespace: fluffy)"

# Create namespace
kubectl create namespace fluffy
if [ $? -ne 0 ]; then
    write_fail "Could not create namespace."
    exit 1
fi
write_ok "Namespace 'fluffy' created"

# Deploy PostgreSQL
printf "  Deploying PostgreSQL...\n"
kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/postgres.yaml -n fluffy
if [ $? -ne 0 ]; then
    write_fail "PostgreSQL manifest apply failed."
    exit 1
fi

printf "  Waiting for PostgreSQL to be ready...\n"
kubectl rollout status deployment/postgres -n fluffy --timeout=120s
if [ $? -ne 0 ]; then
    write_fail "PostgreSQL deployment did not become ready."
    printf "  ${YELLOW}Check: kubectl describe pods -l app=postgres -n fluffy${NC}\n"
    exit 1
fi
write_ok "PostgreSQL is ready"

# Deploy the example app
printf "  Deploying Fluffy Batch Example...\n"
kubectl apply -f fluffy-batch-starter/fluffy-batch-example/k8s/app.yaml -n fluffy
if [ $? -ne 0 ]; then
    write_fail "Application manifest apply failed."
    exit 1
fi

printf "  Waiting for application to be ready...\n"
kubectl rollout status deployment/fluffy-batch-example -n fluffy --timeout=180s
if [ $? -ne 0 ]; then
    write_fail "Application did not become ready."
    printf "  ${YELLOW}Check: kubectl logs -l app=fluffy-batch-example -n fluffy${NC}\n"
    exit 1
fi
write_ok "Application is ready"

# ---------------------------------------------------------------------------
# 7. Print access information
# ---------------------------------------------------------------------------

write_step "Deployment complete!"

service_url=$(minikube service fluffy-batch-example -n fluffy --url 2>/dev/null | head -1)

printf "\n"
printf "  ${GREEN}Fluffy Batch Example is running!${NC}\n"
printf "\n"
printf "  ${WHITE}Dashboard       : %s/fluffy-dashboard/index.html${NC}\n" "$service_url"
printf "  ${WHITE}Application URL : %s${NC}\n" "$service_url"
printf "  ${WHITE}API Base        : %s/api/jobs${NC}\n" "$service_url"
printf "  ${WHITE}Registered Jobs : %s/api/jobs/registered${NC}\n" "$service_url"
printf "\n"
printf "  ${YELLOW}Useful commands:${NC}\n"
printf "    kubectl get pods -n fluffy                                # Check pod status\n"
printf "    kubectl logs -l app=fluffy-batch-example -n fluffy        # View app logs\n"
printf "    kubectl logs -l app=postgres -n fluffy                    # View PostgreSQL logs\n"
printf "    minikube dashboard                                        # Open K8s dashboard\n"
printf "\n"
printf "  ${YELLOW}To tear down:${NC}\n"
printf "    kubectl delete namespace fluffy\n"
printf "    minikube stop\n"
