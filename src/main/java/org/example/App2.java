package org.example;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class App2 {
    public static void main(String[] args) {
        String m3u8Url = "https://vip.ffzy-video.com/20240930/3288_b98249b3/index.m3u8"; // 替换为你的.m3u8文件的URL
        String outputDir = "output/"; // 输出目录
        String outputFileName = "output/video.mp4"; // 合并后的文件名

        try {
            downloadM3U8(m3u8Url, outputDir, outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void downloadM3U8(String m3u8Url, String outputDir, String outputFileName) throws IOException {
        Set<String> downloadedSegments = new HashSet<>();
        List<String> segmentUrls = new ArrayList<>();
        downloadM3U8Recursive(m3u8Url, outputDir, downloadedSegments, segmentUrls);
        mergeSegments(segmentUrls, outputFileName);
    }

    private static void downloadM3U8Recursive(String m3u8Url, String outputDir, Set<String> downloadedSegments, List<String> segmentUrls) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(m3u8Url);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                String m3u8Content = EntityUtils.toString(entity);
                List<String> urls = parseM3U8Content(m3u8Content, m3u8Url);

                Files.createDirectories(Paths.get(outputDir));

                for (String segmentUrl : urls) {
                    if (!downloadedSegments.contains(segmentUrl)) {
                        downloadedSegments.add(segmentUrl);
                        if (segmentUrl.endsWith(".m3u8")) {
                            downloadM3U8Recursive(segmentUrl, outputDir, downloadedSegments, segmentUrls);
                        } else {
                            segmentUrls.add(segmentUrl); // 记录段 URL
                        }
                    }
                }
            }
        }
    }

    public static List<String> parseM3U8Content(String content, String baseUrl) {
        List<String> segmentUrls = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.endsWith(".ts") || line.endsWith(".m3u8")) {
                if (line.startsWith("http")) {
                    segmentUrls.add(line);
                } else {
                    segmentUrls.add(baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1) + line);
                }
            }
        }
        return segmentUrls;
    }

    public static void mergeSegments(List<String> segmentUrls, String outputFileName) {
        int totalSegments = segmentUrls.size(); // 总段数
        try (FileOutputStream fos = new FileOutputStream(outputFileName)) {
            for (int i = 0; i < totalSegments; i++) {
                String segmentUrl = segmentUrls.get(i);
                try (InputStream in = new BufferedInputStream(new URL(segmentUrl).openStream())) {
                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                        fos.write(dataBuffer, 0, bytesRead);
                    }
                    System.out.println("Merged segment " + (i + 1) + " of " + totalSegments + ": " + segmentUrl);
                } catch (IOException e) {
                    System.err.println("Failed to merge segment: " + segmentUrl);
                    e.printStackTrace();
                }
            }
            System.out.println("All segments merged into: " + outputFileName);
        } catch (IOException e) {
            System.err.println("Error writing to output file: " + outputFileName);
            e.printStackTrace();
        }
    }
}