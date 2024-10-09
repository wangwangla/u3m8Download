package org.example;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.*;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class M3U8Downloader {

    public static void main(String[] args) {
        String m3u8Url = "https://vip.ffzy-video.com/20240930/3288_b98249b3/index.m3u8"; // 替换为你的.m3u8文件URL
        String outputDir = "output"; // 输出目录

        try {
            List<String> segmentUrls = fetchM3U8Segments(m3u8Url);
            List<String> strings = downloadSegments(segmentUrls, outputDir);
            downloadSegments1(strings,"outts");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> fetchM3U8Segments(String m3u8Url) throws IOException {
        List<String> segments = new ArrayList<>();
        URL url = new URL(m3u8Url);

        try (BufferedInputStream in = new BufferedInputStream(url.openStream())) {
            byte[] data = new byte[1024];
            StringBuilder sb = new StringBuilder();
            int bytesRead;
            while ((bytesRead = in.read(data, 0, data.length)) != -1) {
                sb.append(new String(data, 0, bytesRead, StandardCharsets.UTF_8));
            }

            String[] lines = sb.toString().split("\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.startsWith("#") && !line.isEmpty()) {
                    segments.add(line.startsWith("http") ? line : new URL(url, line).toString());
                }
            }
//            EXT_HEAD_TAG            = "#EXTM3U"                 # M3U8文件必须包含的标签，并且必须在文件的第一行，所有的M3U8文件中必须包含这个标签
//                    EXT_VERSION_TAG         = "#EXT-X-VERSION"          # M3U8文件的版本，常见的是3（目前最高版本应该是7）。
//            EXT_MEDIA_SEQUENCE_TAG  = "#EXT-X-MEDIA-SEQUENCE"   # 该标签指定了媒体文件持续时间的最大值，播放文件列表中的媒体文件在EXTINF标签中定义的持续时间必须小于或者等于该标签指定的持续时间。该标签在播放列表文件中必须出现一次。
//            EXT_TARGET_DURATION_TAG = "#EXT-X-TARGETDURATION"   # M3U8直播是的直播切换序列，当播放打开M3U8时，以这个标签的值作为参考，播放对应的序列号的切片。
//            EXT_INF_TAG             = "#EXTINF"                 # EXTINF为M3U8列表中每一个分片的duration.在EXTINF标签中，除了duration值，还可以包含可选的描述信息，主要为标注切片信息，使用逗号分隔开。
//            EXT_END_LIST_TAG        = "#EXT-X-ENDLIST"          # 若出现EXT-X-ENDLIST标签，则表明M3U8文件不会再产生更多的切片,这个M3U8即为点播M3U8
//            EXT_STREAM_INF_TAG      = "#EXT-X-STREAM-INF"       # 主要是出现在多级M3U8文件中时，例如M3U8中包含子M3U8列表，或者主M3U8中包含多码率M3U8时
        }
        return segments;
    }
//    https://vip.ffzy-video.com/20240930/3288_b98249b3/index.m3u8
//    https://vip.ffzy-video.com/20240930/3288_b98249b3/2000k/hls/mixed.m3u8
//    https://vip.ffzy-video.com/20240930/3288_b98249b3/2000k/hls/2d0796269ea227d2841f8e40387ee58f.ts

    private static List<String> downloadSegments(List<String> segmentUrls, String outputDir) throws IOException {
        List<String> outList = new ArrayList<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Files.createDirectories(Paths.get(outputDir)); // 创建输出目录
            StringBuilder allurl = new StringBuilder();
            for (int i = 0; i < segmentUrls.size(); i++) {
                String segmentUrl = segmentUrls.get(i);
                String substring = segmentUrl.substring(0, segmentUrl.lastIndexOf("/"));
                HttpGet httpGet = new HttpGet(segmentUrl);
                HttpResponse response = httpClient.execute(httpGet);
                try (InputStream inputStream = response.getEntity().getContent();
                     FileOutputStream fos = new FileOutputStream(outputDir + "/segment" + i + ".ts")) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                        allurl.append(new String(buffer));
                    }
                }
                String[] lines = allurl.toString().split("\n");
                for (String line : lines) {
                    if (line.endsWith(".ts")) {
                        System.out.println(substring+line);
                        outList.add(substring+"/"+line);
                    }
                }
                System.out.println("Downloaded: " + segmentUrl);
            }
        }
        return outList;
    }

    private static void downloadSegments1(List<String> segmentUrls, String outputDir) throws IOException {
        File file = new File(outputDir + "/test.mp4");
        if (!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        if (!file.exists()){
            file.createNewFile();
        }
        FileDownloadRandomAccessFile randomAccessFile = new FileDownloadRandomAccessFile(file);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Files.createDirectories(Paths.get(outputDir)); // 创建输出目录
            for (int i = 0; i < segmentUrls.size(); i++) {
                String segmentUrl = segmentUrls.get(i);
                System.out.println(segmentUrl);
                HttpGet httpGet = new HttpGet(segmentUrl);
                HttpResponse response = httpClient.execute(httpGet);
                try (InputStream inputStream = response.getEntity().getContent();) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        System.out.println(bytesRead);
                        randomAccessFile.write(buffer,0,bytesRead);
                        randomAccessFile.flushAndSync();
                    }
                }
                System.out.println("Downloaded: " + segmentUrl);
            }
        }
    }
}