#!/usr/bin/env bash
# GitHub Actions ↔ GCP 인증을 Workload Identity Federation(WIF)으로 연결한다.
# 여러 번 실행해도 안전하다: 권한·신뢰 조건을 이 파일 기준으로 맞춘다 (보안 조건을 바꾼 뒤 다시 실행).
#
# WIF를 쓰는 이유:
#   서비스계정 JSON 키를 GitHub Secret에 넣는 방식은 "영구히 유효한 비밀번호"를 저장하는 것과 같다.
#   유출되면 폐기 전까지 계속 악용 가능하다. WIF는 키 파일이 없고, GitHub이 발급한 단기 OIDC 토큰을
#   GCP가 검증해 임시 자격증명으로 교환한다. 저장되는 비밀이 아예 없다.
#
# 사용법: bash gcp-setup-wif.sh

set -euo pipefail

# --- Git Bash 대응 ---
# gcloud 래퍼는 `python` 명령을 찾는데, Windows Cloud SDK는 python.exe를 번들로만 갖고 있어
# Git Bash(MINGW64)에서는 PATH에 잡히지 않는다 → "exec: python: not found".
# 번들 Python 경로를 CLOUDSDK_PYTHON 으로 명시해 해결한다.
if ! command -v python >/dev/null 2>&1 && [ -z "${CLOUDSDK_PYTHON:-}" ]; then
    for p in \
        "/c/Program Files (x86)/Google/Cloud SDK/google-cloud-sdk/platform/bundledpython/python.exe" \
        "/c/Program Files/Google/Cloud SDK/google-cloud-sdk/platform/bundledpython/python.exe" \
        "$HOME/AppData/Local/Google/Cloud SDK/google-cloud-sdk/platform/bundledpython/python.exe"
    do
        if [ -x "$p" ]; then
            export CLOUDSDK_PYTHON="$p"
            echo "  (CLOUDSDK_PYTHON 자동 설정: $p)"
            break
        fi
    done
    if [ -z "${CLOUDSDK_PYTHON:-}" ]; then
        echo "❌ gcloud용 Python을 찾지 못했습니다."
        echo "   PowerShell 또는 CMD에서 실행하거나, 아래처럼 직접 지정하세요:"
        echo '   export CLOUDSDK_PYTHON="/c/Program Files (x86)/Google/Cloud SDK/google-cloud-sdk/platform/bundledpython/python.exe"'
        exit 1
    fi
fi

PROJECT_ID="fitlog-505315"
PROJECT_NUMBER="49782628417"
GITHUB_REPO="pby1360/fitlog-v2-server"     # owner/repo
GITHUB_REPO_WEB="pby1360/fitlog-v2-web"     # 같은 공급자를 쓰는 프론트 레포 (gcp-setup-wif-web.sh 와 조건을 맞춘다)
DEPLOY_REF="refs/heads/main"                # 배포 인증은 main 브랜치의 워크플로에서만 허용

# 공급자 신뢰 조건: 두 레포의 main 브랜치에서 실행된 워크플로만 GCP 에 인증할 수 있다.
# (다른 브랜치/PR 에서 워크플로를 고쳐 배포 계정을 쓰는 것을 막는다)
PROVIDER_CONDITION="(assertion.repository=='${GITHUB_REPO}' || assertion.repository=='${GITHUB_REPO_WEB}') && assertion.ref=='${DEPLOY_REF}'"
PROVIDER_MAPPING="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner,attribute.ref=assertion.ref"

POOL="github-pool"
PROVIDER="github-provider"
DEPLOY_SA="fitlog-deployer"                 # CI 전용 계정
RUNTIME_SA="fitlog-run"                     # Cloud Run 실행 계정(기존)

DEPLOY_SA_EMAIL="${DEPLOY_SA}@${PROJECT_ID}.iam.gserviceaccount.com"
RUNTIME_SA_EMAIL="${RUNTIME_SA}@${PROJECT_ID}.iam.gserviceaccount.com"

echo ""
echo "=== 1/5 필요한 API 활성화 ==="
gcloud services enable iamcredentials.googleapis.com sts.googleapis.com \
    --project="$PROJECT_ID"

echo ""
echo "=== 2/5 배포 전용 서비스계정 생성 ==="
if gcloud iam service-accounts describe "$DEPLOY_SA_EMAIL" --project="$PROJECT_ID" >/dev/null 2>&1; then
    echo "  이미 존재: $DEPLOY_SA_EMAIL"
else
    gcloud iam service-accounts create "$DEPLOY_SA" \
        --display-name="GitHub Actions Deployer" --project="$PROJECT_ID"
fi

echo ""
echo "=== 3/5 배포 계정 권한 부여 (최소 권한) ==="
# 이미지 푸시
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
    --role="roles/artifactregistry.writer" --condition=None >/dev/null
# Cloud Run 배포: 새 리비전 배포(--image 만 변경)에는 run.developer 로 충분하다.
# run.admin 은 서비스 IAM 정책(공개 여부 등)까지 바꿀 수 있어 CI 계정에는 과하다.
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
    --role="roles/run.developer" --condition=None >/dev/null
# 이전 버전 스크립트가 부여한 run.admin 회수 (없으면 무시)
gcloud projects remove-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
    --role="roles/run.admin" --condition=None >/dev/null 2>&1 || true
# 배포 시 런타임 SA를 지정하려면 그 SA에 대한 actAs 권한이 필요
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA_EMAIL" \
    --member="serviceAccount:${DEPLOY_SA_EMAIL}" \
    --role="roles/iam.serviceAccountUser" --project="$PROJECT_ID" >/dev/null
echo "  artifactregistry.writer / run.developer / serviceAccountUser(런타임SA) 부여 완료 (run.admin 회수)"

echo ""
echo "=== 4/5 Workload Identity 풀·공급자 생성 ==="
if gcloud iam workload-identity-pools describe "$POOL" \
        --location=global --project="$PROJECT_ID" >/dev/null 2>&1; then
    echo "  풀 이미 존재: $POOL"
else
    gcloud iam workload-identity-pools create "$POOL" \
        --location=global --display-name="GitHub Actions Pool" --project="$PROJECT_ID"
fi

if gcloud iam workload-identity-pools providers describe "$PROVIDER" \
        --workload-identity-pool="$POOL" --location=global --project="$PROJECT_ID" >/dev/null 2>&1; then
    # 이미 있으면 신뢰 조건을 이 파일 기준(브랜치 제한 포함)으로 갱신한다
    gcloud iam workload-identity-pools providers update-oidc "$PROVIDER" \
        --location=global --workload-identity-pool="$POOL" \
        --attribute-mapping="$PROVIDER_MAPPING" \
        --attribute-condition="$PROVIDER_CONDITION" \
        --project="$PROJECT_ID"
    echo "  공급자 조건 갱신: $PROVIDER_CONDITION"
else
    # attribute-condition 이 핵심 보안장치.
    # 없으면 "GitHub의 모든 레포"가 이 GCP 프로젝트에 인증할 수 있다.
    gcloud iam workload-identity-pools providers create-oidc "$PROVIDER" \
        --location=global \
        --workload-identity-pool="$POOL" \
        --display-name="GitHub OIDC" \
        --issuer-uri="https://token.actions.githubusercontent.com" \
        --attribute-mapping="$PROVIDER_MAPPING" \
        --attribute-condition="$PROVIDER_CONDITION" \
        --project="$PROJECT_ID"
fi

echo ""
echo "=== 5/5 해당 레포에만 배포 계정 사용 권한 위임 ==="
gcloud iam service-accounts add-iam-policy-binding "$DEPLOY_SA_EMAIL" \
    --role="roles/iam.workloadIdentityUser" \
    --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/attribute.repository/${GITHUB_REPO}" \
    --project="$PROJECT_ID" >/dev/null
echo "  ${GITHUB_REPO} 만 허용"

echo ""
echo "════════════════════════════════════════════════════════════"
echo " 완료. 아래 두 값을 GitHub 저장소 Variables 에 등록하세요."
echo " (Secret 아님 — 비밀값이 아니라 식별자다)"
echo ""
echo " Settings → Secrets and variables → Actions → Variables 탭"
echo ""
echo " WIF_PROVIDER ="
echo "   projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${POOL}/providers/${PROVIDER}"
echo ""
echo " DEPLOY_SA ="
echo "   ${DEPLOY_SA_EMAIL}"
echo "════════════════════════════════════════════════════════════"
echo ""
