package ark.noah.wtwtviewer20;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;

public class DeviceSizeGetter {
    public static DeviceSizeGetter Instance;

    private DisplayMetrics metrics;

    private int deviceWidth;
    private int deviceHeight;
    public int getDeviceWidth() { return deviceWidth; }
    public int getDeviceHeight() { return deviceHeight; }

    public DeviceSizeGetter(Activity activity) {
        if(Instance != null) return;
        Instance = this;

        WindowManager windowManager = activity.getWindowManager();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = windowManager.getCurrentWindowMetrics();
            WindowInsets windowInsets = windowMetrics.getWindowInsets();
            Insets insets = windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout());
            Rect bounds = windowMetrics.getBounds();
            deviceWidth = bounds.width() - (insets.left + insets.right);
            deviceHeight = bounds.height() - (insets.top + insets.bottom);
        }
        else {
            Display display = windowManager.getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            deviceWidth = point.x;
            deviceHeight = point.y;
        }

        Resources resources = activity.getResources();
        metrics = resources.getDisplayMetrics();
    }

    public float px2dp(float px){
        return px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    public float dp2px(float dp){
        return dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }
}
