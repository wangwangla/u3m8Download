package kw.tony.u3m8download;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class U3M8DownloadUtils {
    private static int threadNum = 4;
    private static ArrayList<ThreadDown> threadDowns;
    private static ArrayList<ArrayList<String>> arrayLists;
    public static void main(String[] args) throws IOException {
        String m3u8Url = "https://vip.ffzy-video.com/20240930/3288_b98249b3/index.m3u8";
        ArrayList<String> allTsUrl = new ArrayList<>();
        threadDowns = new ArrayList<>();
        downloadM3U8Recursive(m3u8Url,allTsUrl);
        System.out.println("all ts size :"+allTsUrl.size());
        downLoadTs(allTsUrl);
        while (true){
            try {
                boolean flag = true;
                for (ThreadDown threadDown : threadDowns) {
                    if (!threadDown.isSuccess()) {
                        flag = false;
                    }
                }
                if (flag)break;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void merge(){

    }

    public static void downLoadTs(ArrayList<String> allTsUrl){
        if (allTsUrl.size() > threadNum) {
            for (int i1 = 0; i1 < threadNum; i1++) {
                arrayLists = splitIntoEqualParts(allTsUrl, threadNum);
            }
        }
        for (int i = 0; i < threadNum; i++) {
            ThreadDown threadDown = new ThreadDown(arrayLists.get(i));
            Thread thread = new Thread(threadDown);
            threadDowns.add(threadDown);
            thread.start();
        }
    }

    public static ArrayList<ArrayList<String>> splitIntoEqualParts(ArrayList<String> list, int numberOfParts) {
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        int totalSize = list.size();
        int partSize = totalSize / numberOfParts;
        int remainder = totalSize % numberOfParts;

        int start = 0;
        for (int i = 0; i < numberOfParts; i++) {
            int end = start + partSize + (i < remainder ? 1 : 0); // 将余数分配到前几个部分
            ArrayList<String> part = new ArrayList<>(list.subList(start, Math.min(end, totalSize)));
            result.add(part);
            start = end;
        }
        return result;
    }

     private static void downloadM3U8Recursive(String m3u8Url,ArrayList<String> allTsUrl) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(m3u8Url);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String m3u8Content = EntityUtils.toString(entity);
                List<String> urls = parseM3U8Content(m3u8Content, m3u8Url);
                for (String segmentUrl : urls) {
                    if (segmentUrl.endsWith(".m3u8")) {
                        downloadM3U8Recursive(segmentUrl,allTsUrl);
                    }else {
                        allTsUrl.add(segmentUrl);
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

}