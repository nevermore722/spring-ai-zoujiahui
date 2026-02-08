# 使用 JDK 8 或 17 作为基础镜像
FROM openjdk:17-jdk-alpine
# 将本地打好的 jar 包复制到容器中
COPY spring-ai-zoujiahui.jar app.jar
# 暴露 Spring Boot 默认端口
EXPOSE 8080
# 启动命令
ENTRYPOINT ["java", "-jar", "/app.jar"]