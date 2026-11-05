package lesson;

import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public final class SearchRepository {
    private final Map<String, String> cache = new HashMap<>();
    private final Executor worker;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final SearchApi api;

    public SearchRepository(Executor worker, SearchApi api) {
        this.worker = worker;
        this.api = api;
    }

    // All callers invoke find on the Android main thread.
    public void find(String query, ResultCallback<String> callback) {
        if (cache.containsKey(query)) {
            callback.success(cache.get(query));
            return;
        }
        worker.execute(() -> {
            final String result;
            try {
                result = api.fetch(query);
            } catch (Exception error) {
                main.post(() -> callback.failure(error));
                return;
            }
            main.post(() -> {
                cache.put(query, result);
                callback.success(result);
            });
        });
    }
}
