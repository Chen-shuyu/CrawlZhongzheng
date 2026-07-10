package com.taifex.utility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static final Logger logger = LogManager.getLogger(ConfigManager.class);
    private static final Properties properties = new Properties();

    static {
        // 自動載入 application.properties
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("找不到 application.properties");
            }
            properties.load(input);
            logger.info("已載入 application.properties");
        } catch (IOException e) {
            throw new RuntimeException("設定檔載入失敗", e);
        }
    }

    // 取得字串
    public static String getString(String key) {
        return properties.getProperty(key);
    }

    // 取得整數
    public static int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    // 應用程式專用方法
    public static String getMainPath() {
        return getString("app.main.path");
    }

    public static String getCrawlerUrl() {
        return getString("crawler.url");
    }

    public static String getMailUsername() {
        return getString("mail.username");
    }

    public static String getMailPassword() {
        return getString("mail.password");
    }

    // ... 還有更多方法
}