package kw.tony.u3m8download;

import java.util.HashMap;

public class DownloadManager {
//    private HashMap<U3m8Request> request;
    private static DownloadManager manager;

    public static DownloadManager getManager() {
        if (manager == null) {
            manager = new DownloadManager();
        }
        return manager;
    }


}
