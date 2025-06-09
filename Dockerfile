# 构建阶段
FROM maven:3.9.6-eclipse-temurin-17 as build
# 指定构建过程中的工作目录
WORKDIR /aipBackend
# 将src目录下所有文件，拷贝到工作目录中src目录下（.gitignore/.dockerignore中文件除外）
COPY src /aipBackend/src
# 将pom.xml文件，拷贝到工作目录下
COPY settings.xml pom.xml /aipBackend/
# 执行代码编译命令
# 自定义settings.xml, 选用国内镜像源以提高下载速度
RUN mvn -s /aipBackend/settings.xml -f /aipBackend/pom.xml clean package

# 运行阶段
FROM eclipse-temurin:17-jre-jammy
# 指定运行时的工作目录
WORKDIR /app
# 将构建产物jar包拷贝到运行时目录中
COPY --from=build /aipBackend/target/*.jar app.jar

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"] 