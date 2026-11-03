package org.a2ui.android.v0_9_1.sample;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.a2ui.android.v0_9_1.A2uiView;
import org.a2ui.android.v0_9_1.core.A2uiAction;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String TAG = "A2uiSample";
    private static final String[] DEMO_NAMES = {
            "音乐播放器", "天气卡片", "签到卡片", "日程卡片", "运动卡片", "账户卡片", "综合表单",
            "媒体展示", "布局导航", "输入控件", "天气图片背景"
    };
    private static final String[] DEMO_FILES = {
            "music_player.json", "weather_card.json", "checkin_card.json",
            "schedule_card.json", "fitness_card.json", "payment_card.json", "sample.json",
            "media_showcase.json", "layout_showcase.json", "input_controls.json", "weather_image_card.json"
    };

    private static final String[] TRACK_TITLES = {"Night Drive", "Midnight City", "Ocean Eyes"};
    private static final String[] TRACK_ARTISTS = {
            "A2UI Sound Lab · Synthwave", "Neon Waves · Electronic", "Blue Coast · Chillout"
    };
    private static final int[] TRACK_DURATIONS = {225, 242, 198};

    private A2uiView a2uiView;
    private int currentDemo = -1;
    private int trackIndex;
    private int musicPosition = 72;
    private int refreshCount;
    private boolean musicPlaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245, 245, 247));
        int padding = dp(12);
        root.setPadding(padding, padding, padding, 0);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, DEMO_NAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        int initialDemo = Math.max(0, Math.min(DEMO_FILES.length - 1,
                getIntent().getIntExtra("demo", 0)));
        spinner.setSelection(initialDemo);
        root.addView(spinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        ScrollView scrollView = new ScrollView(this);
        a2uiView = new A2uiView(this);
        a2uiView.setPadding(0, dp(8), 0, dp(16));
        scrollView.addView(a2uiView, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        a2uiView.setActionListener(this::handleAction);
        a2uiView.setErrorListener((surfaceId, componentId, message) ->
                Log.e(TAG, surfaceId + "/" + componentId + ": " + message));
        registerStylers();

        showDemo(initialDemo);
        if (getIntent().getBooleanExtra("refreshDemo", false)) {
            refreshMusicData();
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showDemo(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void registerStylers() {
        a2uiView.setGlobalComponentStyler((view, component, context) -> {
            if (view instanceof TextView) {
                ((TextView) view).setIncludeFontPadding(false);
            }
        });
        a2uiView.registerComponentTypeStyler("Button",
                (view, component, context) -> view.setMinimumHeight(dp(44)));
        a2uiView.registerComponentStyler("demo", "weather-photo-card", (view, component, context) -> {
            FrameLayout card = (FrameLayout) view;
            ImageView background = new ImageView(this);
            background.setScaleType(ImageView.ScaleType.FIT_XY);
            background.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            card.setClipToPadding(false);
            card.addView(background, 0, params);
            card.addOnLayoutChangeListener((v, left, top, right, bottom,
                    oldLeft, oldTop, oldRight, oldBottom) ->
                    background.layout(0, 0, right - left, bottom - top));
            String url = component.getAsJsonObject("style")
                    .getAsJsonObject("background").get("imageUrl").getAsString();
            context.loadImage(url, background);
        });
        a2uiView.registerComponentStyler("demo", "music-cover", (view, component, context) -> {
            ImageView cover = (ImageView) view;
            cover.setImageResource(R.drawable.ic_play_arrow);
            cover.setColorFilter(Color.WHITE);
            cover.setScaleType(ImageView.ScaleType.CENTER);
        });
        a2uiView.registerComponentStyler("demo", "music-previous-icon", (view, component, context) -> {
            ImageView icon = (ImageView) view;
            icon.setImageResource(R.drawable.ic_skip_previous);
            icon.setColorFilter(Color.rgb(103, 80, 164));
            icon.setLayoutParams(new ViewGroup.LayoutParams(dp(28), dp(28)));
        });
        a2uiView.registerComponentStyler("demo", "music-next-icon", (view, component, context) -> {
            ImageView icon = (ImageView) view;
            icon.setImageResource(R.drawable.ic_skip_next);
            icon.setColorFilter(Color.rgb(103, 80, 164));
            icon.setLayoutParams(new ViewGroup.LayoutParams(dp(28), dp(28)));
        });
        a2uiView.registerComponentStyler("demo", "music-play-icon", (view, component, context) -> {
            ImageView icon = (ImageView) view;
            String name = context.getSurface().resolveString(component.get("name"), context.getScope());
            icon.setImageResource("pause".equals(name) ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
            icon.setColorFilter(Color.WHITE);
            icon.setLayoutParams(new ViewGroup.LayoutParams(dp(32), dp(32)));
        });
        a2uiView.registerComponentStyler("demo", "music-play", (view, component, context) -> {
            GradientDrawable background = new GradientDrawable();
            background.setColor(Color.rgb(103, 80, 164));
            background.setCornerRadius(dp(32));
            view.setBackground(background);
            view.setPadding(dp(16), dp(16), dp(16), dp(16));
            view.setMinimumHeight(dp(64));
        });
    }

    private void handleAction(A2uiAction action) {
        switch (action.getName()) {
            case "refreshData":
                refreshMusicData();
                break;
            case "togglePlay":
                musicPlaying = !musicPlaying;
                sendMusicUpdate(musicPlaying ? "正在播放 · 数据已更新" : "已暂停 · 数据已更新");
                break;
            case "previous":
                trackIndex = (trackIndex + TRACK_TITLES.length - 1) % TRACK_TITLES.length;
                musicPosition = 0;
                sendMusicUpdate("已切换到上一首");
                break;
            case "next":
                trackIndex = (trackIndex + 1) % TRACK_TITLES.length;
                musicPosition = 0;
                sendMusicUpdate("已切换到下一首");
                break;
            default:
                Toast.makeText(this, action.getName() + "\n" + action.getContext(),
                        Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void refreshMusicData() {
        trackIndex = (trackIndex + 1) % TRACK_TITLES.length;
        refreshCount++;
        musicPosition = 36 + refreshCount * 17;
        musicPlaying = true;
        sendMusicUpdate("正在播放 · 第 " + refreshCount + " 次刷新");
    }

    private void sendMusicUpdate(String status) {
        int duration = TRACK_DURATIONS[trackIndex];
        musicPosition = Math.min(musicPosition, duration);
        JsonObject track = new JsonObject();
        track.addProperty("title", TRACK_TITLES[trackIndex]);
        track.addProperty("artist", TRACK_ARTISTS[trackIndex]);

        JsonObject value = new JsonObject();
        value.add("track", track);
        value.addProperty("position", musicPosition);
        value.addProperty("duration", duration);
        value.addProperty("progressLabel", "播放进度 · " + formatTime(musicPosition)
                + " / " + formatTime(duration));
        value.addProperty("playingIcon", musicPlaying ? "pause" : "play");
        value.addProperty("status", status);

        JsonObject update = new JsonObject();
        update.addProperty("surfaceId", "demo");
        update.add("value", value);
        JsonObject message = new JsonObject();
        message.addProperty("version", "v0.9.1");
        message.add("updateDataModel", update);
        a2uiView.render(message);
        Log.d(TAG, "发送 updateDataModel: " + message);
        Toast.makeText(this, "updateDataModel 已发送", Toast.LENGTH_SHORT).show();
    }

    private static String formatTime(int seconds) {
        return String.format(java.util.Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void showDemo(int position) {
        if (currentDemo == position) {
            return;
        }
        int previousDemo = currentDemo;
        currentDemo = position;
        try {
            if (previousDemo >= 0) {
                String surfaceId = "sample.json".equals(DEMO_FILES[previousDemo]) ? "main" : "demo";
                if (a2uiView.getSurfaceView(surfaceId) != null) {
                    a2uiView.render("{\"version\":\"v0.9.1\",\"deleteSurface\":{\"surfaceId\":\""
                            + surfaceId + "\"}}");
                }
            }
            a2uiView.render(readAsset(DEMO_FILES[position]));
        } catch (Exception error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            Log.e(TAG, "Unable to render " + DEMO_FILES[position], error);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String readAsset(String name) throws Exception {
        try (InputStream input = getAssets().open(name)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
