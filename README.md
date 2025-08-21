# we: up

대학생들을 대상으로 한 팀 프로젝트 관리 웹 서비스, we:up 백엔드 서버입니다.

본 문서는 해당 프로젝트를 로컬에서 실행하는 방법을 설명합니다.

## 실행 환경

```
- JDK 21
- Gradle
- Docker
- IntelliJ IDEA (권장)
```

## 실행 방법

### 1. 저장소 클론

```
git clone https://github.com/2025IR/WeUp_BE.git
cd WeUp_BE
```

### 2. 환경 변수 설정

프로젝트 실행에 필요한 환경 변수를 .env 파일을 통해 설정합니다.

필요 시, 함께 제공한 .env.example 파일을 .env로 변환하여 사용하실 수 있습니다.

```
cp .env.example .env
```

### 3. Mysql 실행 (Docker)

.env.example을 그대로 사용한다고 가정한 명령어입니다.

.env 파일을 직접 설정하실 경우 명령어도 그에 맞게 수정해야 할 수 있습니다.

```
docker run -d \
  --name weup-mysql \
  -e MYSQL_ROOT_PASSWORD=123 \
  -e MYSQL_DATABASE=db \
  -e MYSQL_USER=123 \
  -e MYSQL_PASSWORD=123 \
  -p 8082:3306 \
  mysql:8.0
```

확인 방법:

```
docker ps
docker logs weup-mysql
```

### 4. Redis 실행 (Docker)

.env.example을 그대로 사용한다고 가정한 명령어입니다.

.env 파일을 직접 설정하실 경우 명령어도 그에 맞게 수정해야 할 수 있습니다.

```
docker run -d \
  --name weup-redis \
  -e REDIS_PASSWORD=1223 \
  -p 6379:6379 \
  redis:7 redis-server --requirepass 1223
```

확인 방법:

```
docker ps
docker logs weup-redis
```

### 5. Livekit 실행 (Docker)

livekit.yaml 설정 파일을 기반으로 컨테이너를 실행합니다.

필요 시 .env 파일처럼 수정하여 사용하실 수 있으며, 이 경우 명령어 역시 그에 맞게 수정해야 할 수 있습니다.

livekit.yaml 파일은 아래와 같이 위치시킬 수 있습니다:

```
WeUp_BE/
├─ livekit/
│  └─ livekit.yaml
├─ coturn/
│  └─ turnserver.conf
├─ src/
├─ build.gradle
└─ ...
```

명령어:

```
docker run -d \
  --name weup-livekit \
  -v $(pwd)/livekit/livekit.yaml:/etc/livekit.yaml \
  -p 7880:7880 \
  -p 7881:7881 \
  -p 7882-7999:7882-7999/udp \
  livekit/livekit-server:latest \
  --config /etc/livekit.yaml
```

livekit.yaml:

```
port: 7880
log_level: info

rtc:
  use_external_ip: true
  stun_servers:
    - "coturn:3478"
  port_range: 7882-7999

keys:
  weup-livekit-key: a7lpgym54w2kdq3uhfsvbj9iecxr60mztno8akvgby14w5dqsf

turn:
  enabled: true
  udp_port: 3478
  domain: livekit.io

```

확인 방법:

```
docker ps
docker logs weup-livekit
```

### 6. Coturn 실행 (Docker)

turnserver.conf 설정 파일을 기반으로 컨테이너를 실행합니다.

필요 시 .env 파일처럼 수정하여 사용하실 수 있으며, 이 경우 명령어 역시 그에 맞게 수정해야 할 수 있습니다.

turnserver.conf 파일 역시 아래와 같이 위치시킬 수 있습니다:

```
WeUp_BE/
├─ livekit/
│  └─ livekit.yaml
├─ coturn/
│  └─ turnserver.conf
├─ src/
├─ build.gradle
└─ ...
```

명령어:

```
docker run -d \
  --name weup-coturn \
  -v $(pwd)/coturn/turnserver.conf:/etc/turnserver.conf \
  -p 3478:3478 \
  -p 3478:3478/udp \
  -p 49160-49200:49160-49200/udp \
  coturn/coturn:latest -c /etc/turnserver.conf
```

turnserver.conf:

```
fingerprint
use-auth-secret
static-auth-secret=a7lpgym54w2kdq3uhfsvbj9iecxr60mztno8akvgby14w5dqsf
realm=livekit.io
server-name=coturn
listening-port=3478
external-ip=210.119.104.149/172.17.0.1
min-port=49160
max-port=49200
```

확인 방법:

```
docker ps
docker logs weup-coturn
```

### 7. Spring Boot 실행

두 가지 방식 중 편한 방식을 선택하세요:

**개발 모드 실행** (Gradle 필요, 빠르게 실행):

```
./gradlew bootRun --args='--spring.profiles.active=prod'
```

**빌드 후 실행** (운영/배포와 동일한 방식):

```
./gradlew clean bootJar
java -jar build/libs/*.jar --spring.profiles.active=prod
```

### 8. 실행 확인

서버 실행 후 접속:

```
http://localhost:8080/welcome
```

실행이 정상적으로 완료되었을 시, 다음과 같은 문구가 출력됩니다:

```
🎉 팀 프로젝트 관리 웹, we:up에 오신 걸 환영합니다!
```