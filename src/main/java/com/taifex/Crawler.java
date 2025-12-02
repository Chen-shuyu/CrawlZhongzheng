package com.taifex;

import com.taifex.entity.AnnouncementDetail;
import com.taifex.entity.AnnouncementSummary;
import com.taifex.entity.Attachment;
import com.taifex.utility.CrawlerException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Crawler {

    // ========================================
    // 重試設定
    // ========================================
    private static final int MAX_RETRIES = 5;              // 最大重試次數
    private static final int BASE_TIMEOUT = 30000;         // 基礎超時：30秒
    private static final long MIN_RETRY_DELAY = 2000;      // 最小重試延遲：2秒
    private static final long MAX_RETRY_DELAY = 10000;     // 最大重試延遲：10秒

    // --- Strict keywords ---
    private static final String[] KEYWORDS_STRICT = {
            "場地開放",
            "球場",
            "羽球",
            "羽",
            "籃球",
            "籃",
            "空球場",
            "租借",
            "預約",
            "登記",
            "申請",
            "意願"
    };

    // --- Soft keywords ---
    private static final String[] KEYWORDS_EXT = {
            "開放",
            "校園場地",
            "中正國中",
            "未繳費",
            "季",
            "表單"
    };

    /**
     * Fetch announcement summaries from listing page.
     * 改進：加入重試機制和更好的錯誤處理
     */
    public static List<AnnouncementSummary> fetchSummaries(String targetDate, String url) throws CrawlerException {
        System.out.println("===== 開始抓取公告列表 =====");
        System.out.println("目標日期: " + targetDate);
        System.out.println("目標網址: " + url);

        List<AnnouncementSummary> list = new ArrayList<AnnouncementSummary>();
        int attempt = 0;
        long retryDelay = MIN_RETRY_DELAY;

        while (attempt < MAX_RETRIES) {

            try {
                System.out.println("\n[嘗試 " + (attempt + 1) + "/" + MAX_RETRIES + "] 正在連線...");

                // 每次重試增加超時時間
                int timeout = BASE_TIMEOUT + (attempt * 10000);
                System.out.println("超時設定: " + (timeout / 1000) + " 秒");

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(timeout)
                        .get();

                System.out.println("✓ 連線成功！開始解析...");

                Elements rows = doc.select("tbody tr");
                System.out.println("找到 " + rows.size() + " 筆公告");

                int matchCount = 0;
                for (Element row : rows) {
                    String date = row.select(".nt_date").text().trim();

                    if (date.equals(targetDate)) {

                        String title = row.select(".nt_subject a").text().trim();
                        if (!isTargetAnnouncement(title)) {
                            System.out.println(title + " >> 不符合關鍵字!!! ");
                            continue; // skip unrelated titles
                        }

                        String link = row.select(".nt_subject a").attr("href");
                        String category = row.select(".nt_category").text().trim();
                        String unit = row.select(".nt_unit").text().trim();

                        AnnouncementSummary summary = new AnnouncementSummary(
                                date, title, category, unit, link
                        );

                        list.add(summary);
                        matchCount++;
                        System.out.println("  [符合] " + title);
                    }
                }

                System.out.println("\n===== 抓取完成 =====");
                System.out.println("符合條件: " + matchCount + " 筆");
                System.out.println("總共: " + rows.size() + " 筆");

                return list;  // 成功，返回結果

            } catch (IOException e) {
                attempt++;
                System.err.println("✗ 連線失敗: " + e.getMessage());

                if (attempt >= MAX_RETRIES) {
                    // 達到最大重試次數，拋出例外
                    String errorMsg = String.format(
                            "抓取公告列表失敗，已重試 %d 次。最後錯誤: %s",
                            MAX_RETRIES,
                            e.getMessage()
                    );
                    System.err.println("\n" + errorMsg);
                    throw new CrawlerException(errorMsg, e);
                }

                // 計算延遲時間（指數退避 + 隨機抖動）
                long jitter = new Random().nextInt(2000);
                long waitTime = Math.min(retryDelay + jitter, MAX_RETRY_DELAY);

                System.out.println("等待 " + (waitTime / 1000.0) + " 秒後重試...");
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CrawlerException("重試過程被中斷", ie);
                }

                retryDelay *= 2;  // 指數增長

            } catch (Exception e) {
                // 其他未預期的錯誤
                String errorMsg = "抓取公告列表時發生未預期錯誤: " + e.getMessage();
                System.err.println("✗ " + errorMsg);
                e.printStackTrace();
                throw new CrawlerException(errorMsg, e);
            }
        }

        // 理論上不會到這裡
        throw new CrawlerException("未知錯誤");
    }

    /**
     * Fetch detailed announcement content.
     * 改進：加入重試機制和更好的錯誤處理
     */
    public static AnnouncementDetail fetchAnnouncementDetail(String link) throws CrawlerException {
        System.out.println("\n----- 抓取公告詳細內容 -----");
        System.out.println("網址: " + link);

        int attempt = 0;
        long retryDelay = MIN_RETRY_DELAY;

        while (attempt < MAX_RETRIES) {

            try {
                System.out.println("[嘗試 " + (attempt + 1) + "/" + MAX_RETRIES + "] 正在連線...");

                int timeout = BASE_TIMEOUT + (attempt * 10000);

                Document doc = Jsoup.connect(link)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .timeout(timeout)
                        .get();

                System.out.println("✓ 連線成功！開始解析詳細內容...");

                Element table = doc.select("table.single_news").first();
                if (table == null) {
                    System.err.println("✗ 找不到公告內容表格");
                    throw new CrawlerException("找不到公告內容表格，頁面結構可能已改變");
                }

                String subject = table.select(".news_title .newstd").text().trim();
                String date = table.select(".news_date .newstd").text().trim();
                String unit = table.select(".news_unit .newstd").text().trim();
                String category = table.select(".news_cat .newstd").text().trim();
                String level = table.select(".news_type .newstd").text().trim();
                String views = table.select(".news_view .newstd").text().trim();

                // Announcement content paragraphs
                Elements paras = table.select(".news_content .content p");
                List<String> contentList = new ArrayList<String>();
                for (Element p : paras) {
                    String text = p.text().trim();
                    if (!text.isEmpty()) {
                        contentList.add(text);
                    }
                }

                //  附件
                Elements attachEls = table.select(".news_attach a");
                List<Attachment> attachments = new ArrayList<Attachment>();

                for (Element a : attachEls) {
                    String name = a.text().trim();
                    String href = a.attr("href");

                    if (href.startsWith("/")) {
                        href = "https://www.ccjhs.tp.edu.tw" + href;
                    }

                    attachments.add(new Attachment(name, href));
                }

                System.out.println("✓ 解析完成");
                System.out.println("  主旨: " + subject);
                System.out.println("  附件數量: " + attachments.size());

                return new AnnouncementDetail(
                        subject, date, unit, category, level, views,
                        contentList, attachments
                );

            } catch (IOException e) {
                attempt++;
                System.err.println("✗ 連線失敗: " + e.getMessage());

                if (attempt >= MAX_RETRIES) {
                    String errorMsg = String.format(
                            "抓取公告詳細內容失敗，已重試 %d 次。網址: %s，錯誤: %s",
                            MAX_RETRIES,
                            link,
                            e.getMessage()
                    );
                    System.err.println("\n" + errorMsg);
                    throw new CrawlerException(errorMsg, e);
                }

                long jitter = new Random().nextInt(2000);
                long waitTime = Math.min(retryDelay + jitter, MAX_RETRY_DELAY);

                System.out.println("等待 " + (waitTime / 1000.0) + " 秒後重試...");
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CrawlerException("重試過程被中斷", ie);
                }

                retryDelay *= 2;

            } catch (Exception e) {
                String errorMsg = "抓取公告詳細內容時發生未預期錯誤: " + e.getMessage();
                System.err.println("✗ " + errorMsg);
                e.printStackTrace();
                throw new CrawlerException(errorMsg, e);
            }
        }

        throw new CrawlerException("未知錯誤");
    }

    // -----------------------------------------------
    // keyword filter
    // -----------------------------------------------
    private static boolean isTargetAnnouncement(String title) {
        String t = title.toLowerCase();

        // strict match first
        for (String k : KEYWORDS_STRICT) {
            if (t.contains(k.toLowerCase())) return true;
        }

        // extended keywords: match >= 2
        int hit = 0;
        for (String k : KEYWORDS_EXT) {
            if (t.contains(k.toLowerCase())) hit++;
        }

        return hit >= 2;
    }
}

