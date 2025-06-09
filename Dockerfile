# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 复制pom.xml
COPY pom.xml .
# 复制源代码
COPY src ./src
# 构建项目
RUN mvn clean package -DskipTests

# 运行阶段
FROM openjdk:21-jdk-slim
WORKDIR /app
# 从构建阶段复制构建好的jar包
COPY --from=build /build/target/*.jar app.jar

# 暴露应用端口
EXPOSE 8080

# 设置环境变量
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 