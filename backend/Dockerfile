# syntax=docker/dockerfile:1

# ---------- 构建阶段 ----------
FROM gradle:9.5.1-jdk21 AS build
WORKDIR /workspace

# 先只拷贝构建配置（不含源码），让「依赖下载」这一层独立缓存
COPY settings.gradle build.gradle ./
COPY common/build.gradle ./common/
COPY gateway/build.gradle ./gateway/
COPY auth-service/build.gradle ./auth-service/
COPY medical-service/build.gradle ./medical-service/
COPY clinic-service/build.gradle ./clinic-service/
COPY prescription-service/build.gradle ./prescription-service/

# 预下载依赖（结果写入本层，Docker 层缓存会自动复用）
RUN gradle dependencies --no-daemon || true

# 再拷贝全部源码
COPY common ./common
COPY gateway ./gateway
COPY auth-service ./auth-service
COPY medical-service ./medical-service
COPY clinic-service ./clinic-service
COPY prescription-service ./prescription-service

# 构建指定服务（SERVICE_NAME 由 docker compose 传入）
ARG SERVICE_NAME
RUN gradle :${SERVICE_NAME}:bootJar --no-daemon -x test

# ---------- 运行阶段 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
ARG SERVICE_NAME
COPY --from=build /workspace/${SERVICE_NAME}/build/libs/${SERVICE_NAME}-1.0.0.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
