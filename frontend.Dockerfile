# 실행을 위한 최소한의 노드 이미지 사용 (ARM64 지원)
FROM node:20-slim AS runner

WORKDIR /app

# 호스트에서 빌드된 standalone 결과물 복사
COPY front-next/.next/standalone/ ./
COPY front-next/public ./public
COPY front-next/.next/static ./.next/static

# 80번 포트 노출
EXPOSE 80

# 서버 시작
CMD ["node", "server.js"]
