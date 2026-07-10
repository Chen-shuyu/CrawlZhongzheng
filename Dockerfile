# === 第一階段：編譯建造舞台 ===
# 使用官方內建 Maven 與 JDK 21 的輕量鏡像作為編譯環境
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /build

# 先複製 pom.xml 進去，並下載相依套件 (利用容器分層快取)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 複製程式原始碼，並執行 Maven 打包 (採用您最喜歡的 thin jar + lib 分離模式)
COPY src ./src
RUN mvn clean package -DskipTests

# === 第二階段：正式運行舞台 ===
# 運行環境只需要最純粹、最小型的 JRE 即可，不需要龐大的編譯工具
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 從第一階段的編譯成果中，只把主程式與 lib 資料夾偷渡過來
COPY --from=builder /build/target/CrawlZhongzheng-1.0.jar ./app.jar
COPY --from=builder /build/target/lib ./lib

# 宣告容器啟動時的執行指令
ENTRYPOINT ["java", "-jar", "app.jar"]