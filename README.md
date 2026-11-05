# A2UI Android v0.9.1

基于原生 Android View 的 A2UI 渲染库。传入 A2UI JSON、JSON 数组或 JSONL 消息，即可创建 Surface、渲染组件、处理数据绑定和交互 Action。

- 协议版本：**A2UI v0.9.1**
- Basic Catalog：18 类组件
- Java 包名：`org.a2ui.android.v0_9_1`
- 最低系统：Android 6.0（API 23）
- 编译版本：compileSdk 35
- 语言：Java 8
- JSON 解析：Gson 2.10.1

[![](https://jitpack.io/v/yaobin-kid/a2ui-android.svg)](https://jitpack.io/#yaobin-kid/a2ui-android)

## 1. 已支持能力

| 能力 | 当前支持 |
| --- | --- |
| Surface 消息 | `createSurface`、`updateComponents`、`updateDataModel`、`deleteSurface` |
| 输入格式 | 单条 JSON、JSON 消息数组、JSONL |
| 数据绑定 | RFC 6901 JSON Pointer、字面量、列表项相对路径 |
| 函数 | 内置 `formatString`，支持注册自定义函数 |
| 交互 | Event Action、FunctionCall Action、输入双向写回、checks 校验 |
| 布局 | Row、Column、List、Card、Tabs、Modal |
| 展示 | Text、Image、Icon、Video、AudioPlayer、Divider |
| 输入 | Button、TextField、CheckBox、ChoicePicker、Slider、DateTimeInput |
| 样式 | JSON 背景/渐变/圆角/描边/间距，以及全局、类型、组件 ID 原生样式设置器 |
| 扩展 | 自定义组件渲染器、自定义图片加载器 |

当前实现面向 v0.9.1 Basic Catalog。带 `svgPath` 的 Icon 需要注册自定义渲染器，默认 Icon 使用内置名称映射。

## 2. Gradle 导入

### 2.1 通过 JitPack

在 `settings.gradle` 的仓库配置中加入 JitPack：

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

如果项目仍在根 `build.gradle` 配置仓库：

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

然后在应用模块的 `build.gradle` 中添加：

```groovy
dependencies {
    implementation 'com.github.yaobin-kid:a2ui-android:v0.9.2'
}
```

Gson 已声明为 `api` 依赖，会随 AAR 的 Maven 元数据自动引入。JitPack 根据 GitHub 仓库和版本标签构建 AAR，不需要手动上传 AAR 文件。

### 2.2 直接使用本地 AAR

将构建出的 `a2ui-release.aar` 复制到应用的 `libs` 目录：

```groovy
dependencies {
    implementation files('libs/a2ui-release.aar')
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

本地 `files` 方式不会读取 AAR 的 Maven 依赖，因此需要显式添加 Gson。

## 3. 快速开始

### 3.1 创建容器并渲染

```java
import org.a2ui.android.v0_9_1.A2uiView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        A2uiView a2uiView = new A2uiView(this);
        setContentView(a2uiView);
        a2uiView.render(json);
    }
}
```

也可以把 `A2uiView` 放进已有原生布局。渲染、样式注册和自定义组件注册后，已有 Surface 会按当前状态重新绘制。

### 3.2 最小 JSON 示例

```json
[
  {
    "version": "v0.9.1",
    "createSurface": {
      "surfaceId": "main",
      "catalogId": "https://a2ui.org/specification/v0_9_1/catalogs/basic/catalog.json",
      "theme": { "primaryColor": "#6750A4" },
      "sendDataModel": true
    }
  },
  {
    "version": "v0.9.1",
    "updateComponents": {
      "surfaceId": "main",
      "components": [
        {
          "id": "root",
          "component": "Card",
          "child": "content",
          "style": {
            "background": { "color": "#FFFFFFFF", "cornerRadius": 20 },
            "padding": 20
          }
        },
        { "id": "content", "component": "Text", "text": { "path": "/message" }, "variant": "h3" }
      ]
    }
  },
  {
    "version": "v0.9.1",
    "updateDataModel": {
      "surfaceId": "main",
      "value": { "message": "Hello A2UI v0.9.1" }
    }
  }
]
```

根组件 ID 必须为 `root`。组件通过 ID 引用，例如 `Card.child = "content"`。

`createSurface.theme.primaryColor` 是 A2UI v0.9.1 的主题字段，使用 `#RRGGBB` 或 `#AARRGGBB` 颜色值。它应用于 Icon、Button、Tabs，以及 TextField 聚焦边框、CheckBox/ChoicePicker 选中状态、Slider 进度与滑块、DateTimeInput 边框；未设置时使用默认靛蓝色 `#3F51B5`。

### 3.3 更新数据

收到按钮事件或服务端数据后，可以继续向同一个 Surface 发送 `updateDataModel`：

```java
a2uiView.render("{\"version\":\"v0.9.1\",\"updateDataModel\":{"
        + "\"surfaceId\":\"main\",\"value\":{\"message\":\"数据已更新\"}}}");
```

Sample 的音乐播放器演示了刷新、切歌和播放暂停时同时更新曲名、歌手、进度与图标，见 `sample/src/main/assets/music_player.json`。

### 3.4 接收 Action

```java
a2uiView.setActionListener(action -> {
    String name = action.getName();
    JsonObject context = action.getContext();
    // 执行应用业务，然后按需发送 updateDataModel。
});
```

按钮 JSON：

```json
{
  "id": "refresh",
  "component": "Button",
  "child": "refreshText",
  "action": { "event": { "name": "refreshData" } }
}
```

## 4. registerComponentStyler 使用方法

`A2uiComponentStyler` 可以直接修改组件最外层的原生 View，适合设置文字颜色、背景、尺寸、间距和具体 Android 控件属性。

### 4.1 全局样式

没有更具体的设置器时使用：

```java
a2uiView.setGlobalComponentStyler((view, component, context) -> {
    if (view instanceof TextView) {
        ((TextView) view).setIncludeFontPadding(false);
    }
});
```

### 4.2 按组件类型注册

```java
a2uiView.registerComponentTypeStyler("Button", (view, component, context) -> {
    view.setMinimumHeight(dp(48));
});
```

### 4.3 为指定组件 ID 注册私有样式

只作用于 Surface `demo` 中的 `music-play`：

```java
a2uiView.registerComponentStyler("demo", "music-play", (view, component, context) -> {
    GradientDrawable background = new GradientDrawable();
    background.setColor(Color.rgb(103, 80, 164));
    background.setCornerRadius(dp(32));
    view.setBackground(background);
    view.setPadding(dp(16), dp(16), dp(16), dp(16));
});
```

省略 `surfaceId` 时，作用于所有 Surface 中同 ID 的组件：

```java
a2uiView.registerComponentStyler("music-play", styler);
```

### 4.4 优先级与执行规则

```text
指定 Surface + 组件 ID 设置器
    > 跨 Surface 的组件 ID 设置器
    > 组件类型设置器
    > 全局设置器
```

同一个组件只执行优先级最高的一个设置器，不会串联执行全部设置器。JSON `style` 总是先应用，所以原生设置器可以覆盖声明式样式。

- 类型或 ID 注册方法传入 `null`：移除对应设置器。
- `setGlobalComponentStyler(null)`：清除全局设置器。
- `view`：实际原生组件，可强转为 `TextView`、`ImageView`、`LinearLayout` 等。
- `component`：当前组件的原始 JSON。
- `context`：可获取 Surface、数据作用域、Android Context，或解析动态值。

### 4.4 在自定义 Styler 中加载网络图片

自定义渲染器或 Styler 可以通过 `context.loadImage(url, imageView)` 复用当前 `A2uiView` 的图片加载器，无需重复实现网络请求和缓存。

Sample 的 `weather_image_card.json` 演示了整卡网络图片背景：为 `weather-photo-card` 注册私有 Styler，将背景 `ImageView` 放在内容下层，并使用 `ImageView.ScaleType.FIT_XY` 按卡片宽高拉伸填满。完整实现见 `sample/src/main/java/org/a2ui/android/v0_9_1/sample/MainActivity.java`。

## 5. JSON 样式扩展

`style` 是本库为所有组件提供的扩展属性，尺寸单位为 dp：

```json
"style": {
  "background": {
    "gradientColors": ["#FF8E77C7", "#FF3D5A98"],
    "gradientOrientation": "TL_BR",
    "cornerRadius": 18,
    "strokeWidth": 1,
    "strokeColor": "#40FFFFFF",
    "clipToOutline": true
  },
  "padding": [16, 20, 16, 20],
  "elevation": 4,
  "alpha": 1
}
```

| 属性 | 说明 |
| --- | --- |
| `background` | 颜色字符串或背景对象 |
| `background.color` | 纯色，支持 `#RRGGBB`、`#AARRGGBB` |
| `background.gradientColors` | 至少两种渐变颜色 |
| `background.gradientOrientation` | 八种方向，如 `TOP_BOTTOM`、`TL_BR` |
| `background.cornerRadius` | 统一圆角 |
| `background.strokeWidth/strokeColor` | 描边宽度与颜色 |
| `background.clipToOutline` | 是否按背景轮廓裁剪子内容 |
| `padding` | 一个数或 `[left, top, right, bottom]` |
| `paddingHorizontal/paddingVertical` | 水平或垂直内边距 |
| `elevation` | 原生阴影高度 |
| `alpha` | 0～1 的透明度 |

## 6. 组件属性概览

所有组件都具有 `id`、`component`，并可使用本库扩展的 `style` 和 `accessibility`。完整可选值、默认值、校验和绑定说明见 [组件属性与样式详细文档](docs/components-and-styling.md)。

| 组件 | 主要属性 |
| --- | --- |
| `Text` | `text`、`variant` |
| `Image` | `url`、`description`、`fit`、`variant` |
| `Icon` | `name` |
| `Video` | `url` |
| `AudioPlayer` | `url`、`description` |
| `Row` | `children`、`justify`、`align` |
| `Column` | `children`、`justify`、`align` |
| `List` | `children`、`direction` |
| `Card` | `child` |
| `Tabs` | `tabs[{title, child}]` |
| `Modal` | `trigger`、`content` |
| `Divider` | `axis` |
| `Button` | `child`、`variant`、`action` |
| `TextField` | `label`、`value`、`variant`、`validationRegexp`、`checks` |
| `CheckBox` | `label`、`value`、`checks` |
| `ChoicePicker` | `label`、`options`、`value`、`variant`、`displayStyle`、`filterable`、`checks` |
| `Slider` | `label`、`min`、`max`、`value`、`checks` |
| `DateTimeInput` | `label`、`value`、`enableDate`、`enableTime`、`min`、`max`、`checks` |

支持动态值的属性可以使用 `{ "path": "/user/name" }`；动态列表使用 `{ "path": "/items", "componentId": "item" }`。

## 7. 业务 Demo 效果图

<table>
  <tr>
    <td align="center"><strong>音乐播放器</strong><br><img src="docs/images/demo-music.png" width="320"></td>
    <td align="center"><strong>天气卡片</strong><br><img src="docs/images/demo-weather.png" width="320"></td>
  </tr>
  <tr>
    <td align="center"><strong>签到卡片</strong><br><img src="docs/images/demo-checkin.png" width="320"></td>
    <td align="center"><strong>日程卡片</strong><br><img src="docs/images/demo-schedule.png" width="320"></td>
  </tr>
  <tr>
    <td align="center"><strong>运动卡片</strong><br><img src="docs/images/demo-fitness.png" width="320"></td>
    <td align="center"><strong>账户卡片</strong><br><img src="docs/images/demo-payment.png" width="320"></td>
  </tr>
  <tr>
    <td align="center"><strong>综合表单</strong><br><img src="docs/images/demo-form.png" width="320"></td>
    <td></td>
  </tr>
</table>

对应 JSON 位于 `sample/src/main/assets`：

| 文件 | 示例 |
| --- | --- |
| `music_player.json` | 播放器布局、矢量控制按钮、数据刷新 |
| `weather_card.json` | 天气指标、动态三日预报 |
| `checkin_card.json` | 每周签到状态、签到 Action |
| `schedule_card.json` | 日程、参与人、会议操作 |
| `fitness_card.json` | 步数、进度、运动指标 |
| `payment_card.json` | 余额、收支、快捷操作 |
| `sample.json` | 输入绑定、校验、动态商品列表 |
| `weather_image_card.json` | 天气卡片副本，通过私有 Styler 和 `FIT_XY` 使用整卡拉伸图片背景 |
| `media_showcase.json` | Image、Icon、Video、AudioPlayer 等媒体组件 |
| `layout_showcase.json` | Row、Column、List、Card、Tabs、Modal、Divider |
| `input_controls.json` | TextField 四种变体、ChoicePicker、Slider、CheckBox、DateTimeInput 和 Button |

Sample 共提供十一个可切换示例，新增三个组件示例合起来覆盖 Basic Catalog 全部 18 类组件。

## 8. 构建与发布

```bash
# 运行测试
./gradlew :a2ui:testDebugUnitTest

# 构建 AAR 与 Sample APK
./gradlew :a2ui:assembleRelease :sample:assembleDebug

# 验证 Maven 发布内容
./gradlew :a2ui:publishReleasePublicationToMavenLocal
```

产物：

- AAR：`a2ui/build/outputs/aar/a2ui-release.aar`
- Sample APK：`sample/build/outputs/apk/debug/sample-debug.apk`

发布 JitPack 时，将包含 `jitpack.yml` 和 `maven-publish` 配置的代码推送到 GitHub，再创建版本标签：

```bash
git tag -a v0.9.2 -m "Android renderer v0.9.2 (A2UI v0.9.1)"
git push origin v0.9.2
```

随后可在 [JitPack 构建页面](https://jitpack.io/#yaobin-kid/a2ui-android) 查看版本构建状态。

## 9. 源码结构

```text
a2ui/                 A2UI Android 库
  src/main/java/      协议、数据绑定、渲染器、样式 API
  src/test/java/      JVM 与 Robolectric 测试
sample/               十一个可切换示例
  src/main/assets/    A2UI JSON
  src/main/res/       Sample 矢量图标
docs/                 详细组件文档与真机效果图
```
