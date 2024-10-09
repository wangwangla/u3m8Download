package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App3 {
    public static void main(String[] args) {
        String url = "https://ddys.one/vod/20232639/"; // 替换为你要检测的网址

        try {
            List<String> m3u8Links = findM3U8Links(url);
            if (m3u8Links.isEmpty()) {
                System.out.println("No M3U8 links found.");
            } else {
                System.out.println("Found M3U8 links:");
                for (String link : m3u8Links) {
                    System.out.println(link);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> findM3U8Links(String url) throws IOException {
        List<String> m3u8Links = new ArrayList<>();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String content = EntityUtils.toString(entity);
                System.out.println(content);
                String[] lines = content.split("\n");
                for (String line : lines) {
                    if (line.endsWith(".m3u8")) {
                        m3u8Links.add(line.startsWith("http") ? line : url + "/" + line);
                    }
                }
            }
        }

        return m3u8Links;
    }
}
