#!/bin/bash

set -e

# 색상 출력
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 설정 변수
PROJECT_DIR="/home/ubuntu/MapleCalendarBackEnd"
APP_NAME="maple-calendar-backend"
CONTAINER_NAME="maple-calendar-backend"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
ECR_REPOSITORY="maple-calendar-api"
IMAGE_TAG="${IMAGE_TAG:-latest}"

echo -e "${GREEN}=== MapleCalendarBackEnd Docker 배포 시작 ===${NC}"

# Docker 서비스 확인 및 시작
if ! systemctl is-active --quiet docker; then
    echo -e "${YELLOW}Docker 서비스가 실행되지 않았습니다. 시작 중...${NC}"
    sudo systemctl start docker.socket
    sudo systemctl start docker.service
    sleep 2
fi

# Docker 접근 권한 확인
if ! docker ps > /dev/null 2>&1; then
    echo -e "${RED}Docker에 접근할 수 없습니다. sudo 권한이 필요하거나 docker 그룹에 추가가 필요합니다.${NC}"
    echo -e "${YELLOW}다음 명령어로 docker 그룹에 추가하세요: sudo usermod -aG docker $USER${NC}"
    exit 1
fi

# AWS CLI 설치 확인
if ! command -v aws &> /dev/null; then
    echo -e "${YELLOW}AWS CLI가 설치되지 않았습니다. 설치 중...${NC}"
    curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
    unzip -q awscliv2.zip
    sudo ./aws/install
    rm -rf aws awscliv2.zip
fi

# AWS 계정 ID 가져오기
if [ -z "$AWS_ACCOUNT_ID" ]; then
    echo -e "${YELLOW}AWS 계정 ID를 자동으로 가져오는 중...${NC}"
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text 2>/dev/null)
    if [ -z "$AWS_ACCOUNT_ID" ]; then
        echo -e "${RED}AWS_ACCOUNT_ID를 가져올 수 없습니다.${NC}"
        echo -e "${YELLOW}다음 중 하나를 수행하세요:${NC}"
        echo -e "${YELLOW}1. EC2 인스턴스에 IAM 역할을 할당하세요${NC}"
        echo -e "${YELLOW}2. export AWS_ACCOUNT_ID=your-account-id 를 설정하세요${NC}"
        exit 1
    fi
fi
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

# ECR 로그인
echo -e "${YELLOW}[1/6] ECR에 로그인 중...${NC}"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$ECR_REGISTRY"

# 프로젝트 디렉토리로 이동
cd "$PROJECT_DIR"

# Git에서 최신 코드 가져오기 (compose.yaml 등이 필요할 수 있음)
echo -e "${YELLOW}[2/6] Git에서 최신 코드 가져오기...${NC}"
# 파일 권한 변경을 무시하고 pull (chmod +x로 인한 충돌 방지)
git config core.fileMode false
git pull origin main || echo "Git pull 실패 또는 이미 최신 상태"

# 기존 컨테이너 중지 및 제거
if docker ps -a | grep -q "$CONTAINER_NAME"; then
    echo -e "${YELLOW}[3/6] 기존 컨테이너 중지 및 제거 중...${NC}"
    docker stop "$CONTAINER_NAME" || true
    docker rm "$CONTAINER_NAME" || true
fi

# Docker Compose로 PostgreSQL 시작
echo -e "${YELLOW}[4/6] 인프라 컨테이너(Postgres, Redis) 시작 중...${NC}"
if command -v docker &> /dev/null && docker compose version &> /dev/null; then
    docker compose up -d postgres redis
elif command -v docker-compose &> /dev/null; then
    docker-compose up -d postgres redis
else
    echo -e "${RED}Docker Compose를 찾을 수 없습니다.${NC}"
    exit 1
fi

# 데이터베이스가 준비될 때까지 대기
echo -e "${YELLOW}데이터베이스 준비 대기 중...${NC}"
sleep 5

# ECR에서 이미지 Pull
echo -e "${YELLOW}[5/6] ECR에서 Docker 이미지 가져오는 중...${NC}"
FULL_IMAGE_NAME="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"
docker pull "$FULL_IMAGE_NAME"
docker tag "$FULL_IMAGE_NAME" "$APP_NAME:latest"

# 컨테이너 실행
echo -e "${YELLOW}[6/6] 컨테이너 시작 중...${NC}"
RESOURCES_DIR="$PROJECT_DIR/src/main/resources"
docker run -d \
    --name "$CONTAINER_NAME" \
    -e REDIS_HOST=localhost \
    -e REDIS_PORT=6379 \
    -e REDIS_PASSWORD=$REDIS_PASSWORD \
    -e SPRING_RABBITMQ_HOST=localhost \
    -e SPRING_RABBITMQ_PORT=5672 \
    -e SPRING_RABBITMQ_USERNAME=guest \
    -e SPRING_RABBITMQ_PASSWORD=guest \
    -e ENCRYPTION_KEY=$ENCRYPTION_KEY \
    -e GOOGLE_OAUTH_CLIENT_IDS_0=$GOOGLE_OAUTH_CLIENT_IDS_0 \
    -e GOOGLE_OAUTH_CLIENT_IDS_1=$GOOGLE_OAUTH_CLIENT_IDS_1 \
    -e JWT_SECRET=$JWT_SECRET \
    -e CLOUD_AWS_CREDENTIALS_ACCESS_KEY=$AWS_ACCESS_KEY \
    -e CLOUD_AWS_CREDENTIALS_SECRET_KEY=$AWS_SECRET_KEY \
    -e CLOUD_AWS_REGION_STATIC=$AWS_REGION \
    -e S3_BUCKET_NAME=$S3_BUCKET_NAME \
    -e TZ=Asia/Seoul \
    -v /etc/localtime:/etc/localtime:ro \
    --network host \
    -p 8080:8080 \
    --restart unless-stopped \
    -v "$RESOURCES_DIR:/app/resources:ro" \
    "$APP_NAME:latest"

# 컨테이너 상태 확인
sleep 5
if docker ps | grep -q "$CONTAINER_NAME"; then
    echo -e "${GREEN}✓ 배포 완료!${NC}"
    echo -e "${GREEN}컨테이너 이름: $CONTAINER_NAME${NC}"
    echo -e "${GREEN}이미지: $FULL_IMAGE_NAME${NC}"
    echo -e "${GREEN}로그 확인: docker logs -f $CONTAINER_NAME${NC}"
    echo -e "${GREEN}애플리케이션 상태 확인: curl http://localhost:8080/actuator/health${NC}"
else
    echo -e "${RED}✗ 배포 실패: 컨테이너가 시작되지 않았습니다.${NC}"
    echo -e "${RED}로그를 확인하세요: docker logs $CONTAINER_NAME${NC}"
    exit 1
fi
