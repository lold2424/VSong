# 1단계: 프론트엔드 애플리케이션 빌드
FROM node:20-alpine AS builder

WORKDIR /app

# package.json과 package-lock.json 복사
COPY front-next/package*.json ./

# 의존성 설치
RUN npm ci

# 소스 코드 복사
COPY front-next/ ./

# .env.production 파일 생성 (빌드 시 환경변수 주입)
# BASE_URL은 빌드 인자로 전달받음
ARG NEXT_PUBLIC_API_URL
RUN echo "NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}" > .env.production

# 애플리케이션 빌드
RUN npm run build

# 2단계: 실제 실행을 위한 최소한의 이미지 생성
FROM node:20-alpine AS runner

WORKDIR /app

# 빌드 단계에서 생성된 파일들 복사
COPY --from=builder /app/.next/standalone/ ./
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/static ./.next/static

# 80번 포트 노출 (Nginx가 이쪽으로 프록시 예정)
EXPOSE 80

# 서버 시작
CMD ["node", "server.js"]
