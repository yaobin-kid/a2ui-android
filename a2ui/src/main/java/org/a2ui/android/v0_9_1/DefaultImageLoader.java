package org.a2ui.android.v0_9_1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class DefaultImageLoader implements A2uiImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(16 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            if (evicted && oldValue != null && !oldValue.isRecycled()) {
                oldValue.recycle();
            }
        }
    };

    DefaultImageLoader() {
        CACHE.trimToSize(8 * 1024);
    }

    @Override
    public void load(String url, ImageView target) {
        if (url == null || url.trim().isEmpty()) {
            target.setImageDrawable(null);
            return;
        }
        target.setTag(url);
        Bitmap cached = CACHE.get(url);
        if (cached != null && !cached.isRecycled()) {
            target.setImageBitmap(cached);
            return;
        }
        target.setImageDrawable(null);
        EXECUTOR.execute(() -> {
            Bitmap bitmap = null;
            try (InputStream input = open(url)) {
                bitmap = BitmapFactory.decodeStream(input);
                if (bitmap != null) {
                    CACHE.put(url, bitmap);
                }
            } catch (Exception ignored) {
                // 图片失败时保留空白，不影响其它组件。
            }
            Bitmap result = bitmap;
            MAIN.post(() -> {
                if (result != null && url.equals(target.getTag())) {
                    target.setImageBitmap(result);
                }
            });
        });
    }

    private InputStream open(String url) throws Exception {
        URL target = new URL(url);
        if ("http".equalsIgnoreCase(target.getProtocol())
                || "https".equalsIgnoreCase(target.getProtocol())) {
            HttpURLConnection connection = (HttpURLConnection) target.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            if (connection.getResponseCode() >= 400) {
                throw new IllegalStateException("Image HTTP " + connection.getResponseCode());
            }
            return connection.getInputStream();
        }
        return target.openStream();
    }
}
