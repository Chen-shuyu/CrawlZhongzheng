package com.taifex.utility;

import okhttp3.*;

import java.io.IOException;

public class LinePushMessage {

    // 替換成您的 Channel Access Token
    private static final String CHANNEL_ACCESS_TOKEN = "LINE-TOKEN";

    // Line Push Message API (發送給單一使用者)
    private static final String PUSH_API_URL = "https://api.line.me/v2/bot/message/push";

    // Line Broadcast API (發送給所有好友)
    private static final String BROADCAST_API_URL = "https://api.line.me/v2/bot/message/broadcast";

    /**
     * 發送訊息給指定使用者
     * @param userId 使用者的 Line User ID
     * @param message 要發送的訊息內容
     */
    public static void sendMessage(String userId, String message) throws IOException {

        OkHttpClient client = new OkHttpClient();

        // 轉義特殊字元
        String escapedMessage = escapeJson(message);

        // 建立 JSON 格式的訊息內容
        String json = "{"
                + "\"to\":\"" + userId + "\","
                + "\"messages\":["
                + "{"
                + "\"type\":\"text\","
                + "\"text\":\"" + escapedMessage + "\""
                + "}"
                + "]"
                + "}";

        // 建立請求
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json
        );

        Request request = new Request.Builder()
                .url(PUSH_API_URL)
                .addHeader("Authorization", "Bearer " + CHANNEL_ACCESS_TOKEN)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        Response response = null;
        try {
            // 發送請求
            response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                System.out.println("訊息發送成功！");
            } else {
                System.out.println("發送失敗：" + response.code() + " - " + response.message());
                System.out.println("錯誤訊息：" + response.body().string());
            }
        } finally {
            if (response != null) {
                response.close();
            }
            // 關閉 OkHttpClient 釋放資源
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }

    /**
     * 轉義 JSON 特殊字元
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 發送訊息給所有加入官方帳號的好友
     * @param message 要發送的訊息內容
     */
    public static void broadcastMessage(String message) throws IOException {

        OkHttpClient client = new OkHttpClient();

        // 轉義特殊字元
        String escapedMessage = escapeJson(message);

        // 建立 JSON 格式的訊息內容（不需要指定接收者）
        String json = "{"
                + "\"messages\":["
                + "{"
                + "\"type\":\"text\","
                + "\"text\":\"" + escapedMessage + "\""
                + "}"
                + "]"
                + "}";

        // 建立請求
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                json
        );

        Request request = new Request.Builder()
                .url(BROADCAST_API_URL)
                .addHeader("Authorization", "Bearer " + CHANNEL_ACCESS_TOKEN)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        Response response = null;
        try {
            // 發送請求
            response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                System.out.println("群發訊息成功！已發送給所有好友");
                System.out.println("發送的訊息內容：" + message);
            } else {
                System.out.println("發送失敗：" + response.code() + " - " + response.message());
                System.out.println("錯誤訊息：" + response.body().string());
                System.out.println("嘗試發送的 JSON：" + json);
            }
        } finally {
            if (response != null) {
                response.close();
            }
            // 關閉 OkHttpClient 釋放資源
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}