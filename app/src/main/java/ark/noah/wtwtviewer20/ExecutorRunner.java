package ark.noah.wtwtviewer20;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ExecutorRunner {
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onComplete(T result);
        void onError(Exception e);
    }

    public <T> void execute(Callable<T> callable, Callback<T> callback) {
        executor.execute(() -> {
            final T result;
            try {
                result = callable.call();
                handler.post(() -> callback.onComplete(result));
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(()-> callback.onError(e));
            }
        });
    }
}