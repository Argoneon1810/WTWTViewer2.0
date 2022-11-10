package ark.noah.wtwtviewer20;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class LinkValidater {

    private boolean isDebug;

    public static LinkValidater Instance;

    static class Info {
        int toonID;
        int episodeID;
        String toonType;
    }

    public LinkValidater() {
        if(Instance != null) Instance = this;
        else return;

        isDebug = MainActivity.Instance.isDebug;
    }

    public boolean isLinkValidAll(String urlToParse) {
        Info info = extractInfo(urlToParse);
        if(info == null) return false;
        else return info.toonID != -1 && info.episodeID != -1 && info.toonType.length() > 1;
    }
    public boolean isLinkValidEpisodeList(String urlToParse) {
        Info info = extractInfo(urlToParse);
        if(info == null) return false;
        else return info.toonID != -1 && info.toonType.length() > 1;
    }

    public Info extractInfo(String url) {
        try {
            URL aURL = new URL(url);
            Info info = new Info();
            info.toonType = extractToonType(aURL);
            info.toonID = extractToonId(aURL);
            info.episodeID = extractEpisodeID(aURL);
            return info;
        } catch (MalformedURLException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractToonType(URL aURL)  {
        return aURL.getPath().substring(0, 2);
    }

    private int extractToonId(URL aURL)  {
        String queries = aURL.getQuery();
        if(isDebug) Log.i("DebugLog","queries: " + queries);
        if (queries == null) return -1;
        else if (queries.length() > 0) {
            int queryDividerIndex = queries.indexOf("&");
            if (queryDividerIndex != -1) queries = queries.substring(0, queryDividerIndex);
            return Integer.parseInt(queries.substring(queries.indexOf("=") + 1));
        }
        return -1;
    }

    private int extractEpisodeID(URL aURL) {
        String queries = aURL.getQuery();
        if (queries == null) return -1;
        else if(queries.length() > 0) {
            int queryDividerIndex = queries.indexOf("&");
            if (queryDividerIndex == -1) return -1;
            queries = queries.substring(queryDividerIndex + 1);
            return Integer.parseInt(queries.substring(queries.indexOf('=') + 1));
        }
        return -1;
    }
}
