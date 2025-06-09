# 二开推荐阅读[如何提高项目构建效率](https://developers.weixin.qq.com/miniprogram/dev/wxcloudrun/src/scene/build/speed.html)
# 选择构建用基础镜像。如需更换，请到[dockerhub官方仓库](https://hub.docker.com/_/java?tab=tags)自行选择后替换。
FROM openjdk:21-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制Maven构建的jar包到容器中
COPY target/*.jar app.jar

# 暴露应用端口（根据您的应用配置修改端口号）
EXPOSE 8080

# 设置环境变量
ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx512m"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
