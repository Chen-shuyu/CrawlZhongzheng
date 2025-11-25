package com.taifex;

import com.taifex.entity.AnnouncementDetail;
import com.taifex.entity.AnnouncementSummary;
import com.taifex.entity.Attachment;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class Crawler {
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
     */
    public static List<AnnouncementSummary> fetchSummaries(String targetDate, String url) {

        List<AnnouncementSummary> list = new ArrayList<AnnouncementSummary>();

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Elements rows = doc.select("tbody tr");

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
                }
            }

        } catch (Exception e) {
            System.err.println("fetchSummaries error: " + e.getMessage());
        }

        return list;
    }


    /**
     * Fetch detailed announcement content.
     */
    public static AnnouncementDetail fetchAnnouncementDetail(String link) {

        try {
            Document doc = Jsoup.connect(link)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            Element table = doc.select("table.single_news").first();
            if (table == null) {
                System.err.println("Cannot find detail table.");
                return null;
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
                contentList.add(p.text().trim());
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

            return new AnnouncementDetail(
                    subject, date, unit, category, level, views,
                    contentList, attachments
            );

        } catch (Exception e) {
            System.err.println("fetchAnnouncementDetail error: " + e.getMessage());
        }

        return null;
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

