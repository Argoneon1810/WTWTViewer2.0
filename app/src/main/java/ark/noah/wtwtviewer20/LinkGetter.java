package ark.noah.wtwtviewer20;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.PreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class LinkGetter {
    public static LinkGetter Instance;

    private SharedPreferences sharedPreferences;
    private Resources resources;
    private ArrayList<String> entrypointArrayList;

    private boolean isReady = false;
    private String pref_key_entrypoint;
    private String validEntryPoint;

    private Callback callback;

    public LinkGetter(Context context, Callback callback) {
        if(Instance != null) return;
        Instance = this;

        this.callback = callback;

        Thread entryLinkGetterThread = new Thread(this::run);
        entrypointArrayList = new ArrayList<>();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        resources = context.getResources();
        pref_key_entrypoint = context.getString(R.string.pref_key_entrypoint);

        entryLinkGetterThread.start();
    }

    public boolean isReady() {
        return isReady;
    }

    public String getEntryPoint() {
        return validEntryPoint;
    }

    private void run() {
        Document doc;

        try {
            doc = Jsoup.connect(
                    sharedPreferences.getString(
                            pref_key_entrypoint,                                //selected value
                            resources.getStringArray(R.array.entry_points)[5]   //if non, use default value
                    )
            ).get();

            //get links
            Elements element = doc.select("ul.list");
            for (Element value : element.select("a"))
                entrypointArrayList.add(value.attr("href"));

            validEntryPoint = entrypointArrayList.get(1);       //0 = wolf, 1 = wtwt, etc.
            isReady = true;

            new Handler(Looper.getMainLooper()).post(() -> {
                if(callback != null)
                    callback.onEntryPointReady(validEntryPoint);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface Callback {
        void onEntryPointReady(String url);
    }
}
