# 공식 Nginx 이미지 사용
FROM nginx:alpine

# 사용자 정의 Nginx 설정 파일을 컨테이너 내의 기본 설정 파일로 복사
COPY ./nginx/nginx.conf /etc/nginx/conf.d/default.conf

# 80번 포트 노출
EXPOSE 80

# Nginx 시작 (기본 CMD 사용)
CMD ["nginx", "-g", "daemon off;"]
