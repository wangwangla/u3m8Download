package kw.tony.u3m8download;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

public class ThreadDown implements Runnable {
    private ArrayList<String> arrayList;
    private boolean isSuccess;
    public ThreadDown(ArrayList<String> arrayList){
        this.arrayList = arrayList;
    }

    @Override
    public void run() {
        for (String s : arrayList) {
            downloadSegment(s,"tempts/");
        }
        isSuccess = true;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public static void downloadSegment(String segmentUrl, String outputDir) {
        File file = new File(outputDir);
        if (!file.exists()){
            file.mkdirs();
        }
        try (InputStream in = new BufferedInputStream(new URL(segmentUrl).openStream());
             FileOutputStream out = new FileOutputStream(outputDir + segmentUrl.substring(segmentUrl.lastIndexOf('/') + 1))) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, dataBuffer.length)) != -1) {
                out.write(dataBuffer, 0, bytesRead);
            }
            System.out.println("Downloaded: " + segmentUrl);
        } catch (IOException e) {
            System.err.println("Failed to download segment: " + segmentUrl);
            e.printStackTrace();
        }
    }
}
