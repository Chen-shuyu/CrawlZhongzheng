# === 第一階段：編譯建造舞台 ===
# 加上 docker.io/library/ 明確指定來源倉庫
FROM docker.io/library/maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

# 先複製 pom.xml 進去，並下載相依套件 (利用容器分層快取)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 複製程式原始碼，並執行 Maven 打包
COPY src ./src
# 【關鍵修改 1】強制 Maven 在打包時，把相依套件獨立抽離到 target/lib 資料夾中
RUN mvn clean package dependency:copy-dependencies -DoutputDirectory=target/lib -DskipTests

# === 第二階段：正式運行舞台 ===
# 同樣加上 docker.io/library/ 明確指定來源倉庫
FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

# 從第一階段的編譯成果中，只把主程式與 lib 資料夾偷渡過來
COPY --from=builder /build/target/CrawlZhongzheng-1.0.jar ./app.jar
COPY --from=builder /build/target/lib ./lib

# 【關鍵修改 2】不依賴 JAR 檔內部的設定，直接用 -cp 參數強制指定套件位置與進入點
ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "com.taifex.App"]