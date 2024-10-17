package kw.tony.u3m8download;

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
import java.util.Arrays;
import java.util.List;

public class U3M8DownloadUtils {
    private static int threadNum = 4;
    private static ArrayList<ThreadDown> threadDowns;
    private static ArrayList<ArrayList<String>> arrayLists;
    public static void main(String[] args) throws IOException {
        String m3u8Url = "https://vip.ffzy-video.com/20240930/3288_b98249b3/index.m3u8";
//        String m3u8Url = "https://v11.tlkqc.com/wjv11/202409/03/7etU7gYbdX83/video/index.m3u8";
//        String m3u8Url = "https://ukzy.ukubf3.com/20240904/5fNs9gj6/index.m3u8";
        ArrayList<String> allTsUrl = new ArrayList<>();
        threadDowns = new ArrayList<>();
        //找到所有的
        downloadM3U8Recursive(m3u8Url,allTsUrl);
        System.out.println("all ts size :"+allTsUrl.size());
        if (allTsUrl.size()>0) {
            downLoadTs(allTsUrl);
            //检测是否下载完成
            ArrayList<ThreadDown> removeDown = new ArrayList<>();
            while (true) {
                try {
                    boolean flag = true;
                    removeDown.clear();
                    for (ThreadDown threadDown : threadDowns) {
                        if (!threadDown.isSuccess()) {
                            flag = false;
                            break;
                        } else {
                            removeDown.add(threadDown);
                        }
                    }
                    for (ThreadDown threadDown : removeDown) {
                        threadDowns.remove(threadDown);
                    }
                    if (flag) break;
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            mergeSegments(allTsUrl, "outFile.mp4");
        }
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

    public static void downLoadTs(ArrayList<String> allTsUrl){
        //分任务
        if (allTsUrl.size() > threadNum) {
            for (int i1 = 0; i1 < threadNum; i1++) {
                arrayLists = splitIntoEqualParts(allTsUrl, threadNum);
            }
            for (int i = 0; i < threadNum; i++) {
                ThreadDown threadDown = new ThreadDown(arrayLists.get(i));
                Thread thread = new Thread(threadDown);
                threadDowns.add(threadDown);
                thread.start();
            }
        }else {
            //不分
            ThreadDown threadDown = new ThreadDown(allTsUrl);
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
                    segmentUrl = segmentUrl.replace("\r", "");
                    System.out.println(segmentUrl);
                    if (segmentUrl.endsWith(".m3u8")) {
                        downloadM3U8Recursive(segmentUrl,allTsUrl);
                    }else {
                        allTsUrl.add(segmentUrl);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<String> parseM3U8Content(String content, String baseUrl) {
        List<String> segmentUrls = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.replace("\r", "");
            System.out.println(line);
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