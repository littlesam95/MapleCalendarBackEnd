# EC2 배포 가이드

이 문서는 MapleCalendarBackEnd 애플리케이션을 AWS EC2에 배포하는 방법을 설명합니다.

## 사전 요구사항

1. AWS 계정 및 EC2 인스턴스
2. GitHub 저장소에 설정된 GitHub Actions Secrets
3. ECR 리포지토리 생성 완료

## 1. EC2 인스턴스 초기 설정

### 1.1 EC2 인스턴스 생성

1. AWS 콘솔에서 EC2 인스턴스를 생성합니다.
2. Ubuntu 22.04 LTS 이상을 권장합니다.
3. 보안 그룹에서 다음 포트를 열어주세요:
   - SSH (22): GitHub Actions에서 배포하기 위해 필요
   - HTTP (8080): 애플리케이션 포트
   - PostgreSQL (5432): 데이터베이스 포트 (선택사항, 같은 인스턴스에서 실행하는 경우)

### 1.2 IAM 역할 설정

EC2 인스턴스가 ECR에서 이미지를 pull할 수 있도록 IAM 역할을 설정합니다:

1. AWS 콘솔에서 IAM 역할 생성
2. 다음 정책을 연결:
   - `AmazonEC2ContainerRegistryReadOnly` (ECR 읽기 권한)
3. EC2 인스턴스에 이 역할을 할당

또는 인라인 정책을 사용하는 경우:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    }
  ]
}
```

### 1.3 EC2 인스턴스 접속 및 초기 설정

SSH로 EC2 인스턴스에 접속합니다:

```bash
ssh -i your-key.pem ubuntu@your-ec2-ip
```

#### Docker 설치

```bash
# 패키지 업데이트
sudo apt-get update

# Docker 설치
sudo apt-get install -y docker.io docker-compose-plugin

# Docker 서비스 시작 및 자동 시작 설정
sudo systemctl start docker
sudo systemctl enable docker

# 현재 사용자를 docker 그룹에 추가 (sudo 없이 docker 사용)
sudo usermod -aG docker ubuntu

# 변경사항 적용을 위해 로그아웃 후 재접속
exit
```

#### AWS CLI 설치 (EC2 IAM 역할을 사용하지 않는 경우)

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip
sudo ./aws/install
rm -rf aws awscliv2.zip
```

#### Git 설치

```bash
sudo apt-get install -y git
```

#### 프로젝트 클론

```bash
cd /home/ubuntu
git clone https://github.com/your-username/MapleCalendarBackEnd.git
cd MapleCalendarBackEnd

# deploy.sh 실행 권한 부여
chmod +x deploy.sh
```

### 1.4 환경 변수 설정

EC2 인스턴스에서 다음 환경 변수를 설정합니다:

```bash
# ~/.bashrc 또는 ~/.profile에 추가
export AWS_ACCOUNT_ID=your-aws-account-id
export AWS_REGION=ap-northeast-2
```

또는 `/etc/environment`에 추가 (시스템 전체 설정):

```bash
sudo nano /etc/environment
```

```bash
AWS_ACCOUNT_ID=your-aws-account-id
AWS_REGION=ap-northeast-2
```

### 1.5 애플리케이션 설정 파일 준비

`application-secret.properties` 파일을 EC2 인스턴스에 생성합니다:

```bash
cd /home/ubuntu/MapleCalendarBackEnd/src/main/resources
nano application-secret.properties
```

필요한 설정을 추가합니다 (데이터베이스 연결 정보, API 키 등).

### 1.6 Firebase 설정 파일 준비

Firebase Admin SDK 키 파일을 EC2 인스턴스에 업로드합니다:

```bash
# SCP를 사용하여 로컬에서 EC2로 파일 전송
scp -i your-key.pem maplecalendar-4add3-firebase-adminsdk-fbsvc-8bfd99e066.json ubuntu@your-ec2-ip:/home/ubuntu/MapleCalendarBackEnd/src/main/resources/
```

## 2. GitHub Secrets 설정

GitHub 저장소의 Settings > Secrets and variables > Actions에서 다음 Secrets를 설정합니다:

- `AWS_ROLE_TO_ASSUME`: GitHub Actions에서 사용할 IAM 역할 ARN
- `EC2_HOST`: EC2 인스턴스의 공용 IP 또는 도메인
- `EC2_USER`: EC2 사용자 이름 (일반적으로 `ubuntu`)
- `EC2_SSH_PRIVATE_KEY`: EC2 인스턴스 접속용 SSH 개인 키

### SSH 키 생성 및 설정

EC2 인스턴스에 GitHub Actions 전용 SSH 키를 설정합니다:

```bash
# EC2 인스턴스에서 실행
mkdir -p ~/.ssh
nano ~/.ssh/authorized_keys
# GitHub Actions에서 사용할 공개 키를 추가
```

또는 기존 키를 사용하는 경우, 개인 키를 GitHub Secrets에 추가합니다.

## 3. 배포 프로세스

### 자동 배포 (GitHub Actions)

`main` 브랜치에 push하면 자동으로 다음이 실행됩니다:

1. Docker 이미지 빌드 및 ECR에 푸시
2. EC2 인스턴스에 SSH 접속
3. `deploy.sh` 스크립트 실행
4. ECR에서 최신 이미지 pull
5. 컨테이너 재시작

### 수동 배포

EC2 인스턴스에 직접 접속하여 배포할 수 있습니다:

```bash
ssh -i your-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/MapleCalendarBackEnd
export AWS_ACCOUNT_ID=your-aws-account-id
export IMAGE_TAG=latest
./deploy.sh
```

## 4. 배포 확인

### 컨테이너 상태 확인

```bash
docker ps
docker logs -f maple-calendar-backend
```

### 애플리케이션 헬스 체크

```bash
curl http://localhost:8080/actuator/health
```

### 외부에서 접근 확인

EC2 인스턴스의 공용 IP를 사용하여 접근:

```bash
curl http://your-ec2-ip:8080/actuator/health
```

## 5. 문제 해결

### Docker 권한 오류

```bash
sudo usermod -aG docker ubuntu
# 로그아웃 후 재접속
```

### ECR 로그인 실패

IAM 역할이 올바르게 설정되었는지 확인:

```bash
aws sts get-caller-identity
aws ecr get-login-password --region ap-northeast-2
```

### 컨테이너가 시작되지 않는 경우

로그 확인:

```bash
docker logs maple-calendar-backend
```

### 데이터베이스 연결 오류

`compose.yaml`의 PostgreSQL 설정과 `application-secret.properties`의 데이터베이스 연결 정보가 일치하는지 확인합니다.

## 6. 보안 권장사항

1. **보안 그룹**: 필요한 포트만 열어두세요
2. **SSH 키**: 강력한 SSH 키를 사용하고 정기적으로 교체하세요
3. **환경 변수**: 민감한 정보는 환경 변수나 AWS Secrets Manager를 사용하세요
4. **Firebase 키**: Firebase Admin SDK 키 파일은 절대 Git에 커밋하지 마세요
5. **IAM 역할**: 최소 권한 원칙을 따르세요

## 7. 모니터링 및 로그

### 컨테이너 로그 확인

```bash
# 실시간 로그 확인
docker logs -f maple-calendar-backend

# 최근 100줄만 확인
docker logs --tail 100 maple-calendar-backend
```

### 시스템 리소스 모니터링

```bash
# 컨테이너 리소스 사용량
docker stats maple-calendar-backend

# 시스템 리소스
htop
```

## 8. 업데이트 및 롤백

### 업데이트

GitHub Actions를 통해 자동으로 업데이트되거나, 수동으로 `deploy.sh`를 실행합니다.

### 롤백

이전 이미지 태그를 사용하여 롤백:

```bash
export IMAGE_TAG=previous-sha-tag
./deploy.sh
```

또는 ECR에서 특정 태그를 확인하고 배포:

```bash
aws ecr describe-images --repository-name maple-calendar-api --region ap-northeast-2
```
