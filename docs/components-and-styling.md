# A2UI Android 组件属性、播放器布局与样式指南

本文对应当前 Android 实现。除特别标注为“本库扩展”的属性外，组件名和消息结构遵循 A2UI v0.9.1 Basic Catalog。

## 1. 最小使用方式

```java
A2uiView a2uiView = new A2uiView(context);
setContentView(a2uiView);
a2uiView.render(json);
```

`render` 接受一条 JSON 消息、JSON 消息数组或 JSONL。一个可显示的 Surface 至少需要：

1. `createSurface`：创建 Surface，可通过 `theme.primaryColor` 设置主题色；该颜色应用于图标、按钮、Tabs、输入框聚焦边框、选择控件选中状态和 Slider 高亮。
2. `updateComponents`：提供组件树，根组件 ID 必须为 `root`。
3. `updateDataModel`：可选，提供绑定数据。

组件之间不直接嵌套 JSON 对象，而是通过 ID 引用。例如 `Card.child = "content"` 表示渲染 ID 为 `content` 的组件。

## 2. 所有组件的通用属性

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 当前 Surface 内唯一的组件 ID，根节点固定为 `root`。 |
| `component` | string | 组件类型，如 `Text`、`Row`、`Card`。 |
| `accessibility.label` | string/动态值 | 无障碍标签。 |
| `accessibility.description` | string/动态值 | 无障碍描述，并在 Android 8.0 及以上作为 tooltip。 |

支持动态值的属性可写为字面量，也可写为：

```json
{ "path": "/user/name" }
```

或函数调用：

```json
{ "call": "formatPrice", "args": { "value": { "path": "/price" } } }
```

动态列表的 `children` 使用 `{ "path": "/items", "componentId": "item" }`。列表项内部的相对路径会绑定到当前项。

## 3. Basic Catalog 组件属性

### 3.1 展示组件

#### Text

| 属性 | 类型/可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `text` | string/动态值 | `""` | 文本；支持 `**粗体**`、`*斜体*`、`` `等宽` `` 三个 Markdown 子集。 |
| `variant` | `h1`、`h2`、`h3`、`h4`、`h5`、`body`、`caption` | `body` | 字号、字重及 caption 颜色。 |

#### Image

| 属性 | 类型/可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `url` | string/动态值 | `""` | HTTP、HTTPS 或其它可由 `URL.openStream` 打开的地址；也可用 `A2uiImageLoader` 自定义加载。 |
| `description` | string/动态值 | `url` | 图片内容描述。 |
| `fit` | `fill`、`contain`、`scaleDown`、`cover`、`none` | `fill` | 分别映射到原生 `ImageView.ScaleType`。 |
| `variant` | `icon`、`avatar`、`smallFeature`、`mediumFeature`、`largeFeature`、`header` | `mediumFeature` | 控制默认尺寸；avatar 会裁剪为圆形。 |

默认高度：icon 24dp、avatar 48dp、smallFeature 90dp、mediumFeature 160dp、largeFeature 240dp、header 180dp。

#### Icon

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `name` | string/动态值 | `help` | 内置 Material 名称到 Android 系统图标的映射。 |

内置名称包括：`add`、`arrowBack`、`arrowForward`、`call`、`phone`、`camera`、`check`、`close`、`delete`、`download`、`upload`、`edit`、`event`、`calendarToday`、`error`、`warning`、`fastForward`、`favorite`、`star`、`favoriteOff`、`starOff`、`starHalf`、`help`、`home`、`info`、`locationOn`、`lock`、`lockOpen`、`mail`、`menu`、`moreVert`、`moreHoriz`、`notifications`、`notificationsOff`、`pause`、`payment`、`person`、`accountCircle`、`photo`、`play`、`refresh`、`rewind`、`search`、`send`、`settings`、`share`、`shoppingCart`、`skipNext`、`skipPrevious`、`stop`、`visibility`、`visibilityOff`、`volumeDown`、`volumeMute`、`volumeOff`、`volumeUp`。

`name` 解析为带 `svgPath` 的对象时，需要注册自定义 `Icon` 渲染器；默认渲染器不解析 SVG Path。

#### Video

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `url` | string/动态值 | 视频地址，使用原生 `VideoView + MediaController` 播放。 |

默认播放区域高度为 220dp。

#### AudioPlayer

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `url` | string/动态值 | 无 | 音频地址，当前同样由 `VideoView + MediaController` 解码播放。 |
| `description` | string/动态值 | `Audio` | 播放器上方显示的说明。 |

默认控制区域高度为 72dp。如果需要自研音乐播放器 UI，请用 `Image + Text + Slider + Button`，由应用控制播放器，见第 4 节。

### 3.2 布局与容器组件

#### Row / Column

| 属性 | 类型/可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `children` | ID 数组或动态列表模板 | 无 | 子组件；Row 横向排列，Column 纵向排列。 |
| `justify` | `start`、`center`、`end`、`stretch`、`spaceBetween`、`spaceAround`、`spaceEvenly` | `start` | 主轴布局；`stretch` 为每个子项分配相同 weight。 |
| `align` | `start`、`center`、`end`、`stretch` | `stretch` | 交叉轴对齐；Column 的子项默认 MATCH_PARENT 宽度。 |

动态列表模板：

```json
{
  "id": "root",
  "component": "Column",
  "children": { "path": "/items", "componentId": "item" }
}
```

#### List

| 属性 | 类型/可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `children` | ID 数组或动态列表模板 | 无 | 同 Row/Column。 |
| `direction` | `vertical`、`horizontal` | `vertical` | 分别使用 `ScrollView` 或 `HorizontalScrollView`。 |

List 自身已经可滚动，不建议再把它放进同方向的外部滚动容器。

#### Card

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `child` | 组件 ID | 卡片内容。 |

默认为白底圆角容器；具体视觉由应用通过 `A2uiComponentStyler` 设置，见第 5 节。

#### Tabs

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `tabs` | array | 标签数组，每项包含 `title` 和 `child`。 |
| `tabs[].title` | string/动态值 | 标签标题。 |
| `tabs[].child` | 组件 ID | 当前标签显示的内容。 |

#### Modal

| 属性 | 类型 | 说明 |
| --- | --- | --- |
| `trigger` | 组件 ID | 点击后打开对话框的组件。 |
| `content` | 组件 ID | `AlertDialog` 中显示的内容。 |

如果 trigger 本身是带 Action 的 Button，点击时会先派发 Action，再打开对话框。

#### Divider

| 属性 | 类型/可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `axis` | `horizontal`、`vertical` | `horizontal` | 分割线方向。 |

### 3.3 操作与输入组件

#### Button

| 属性 | 类型/可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `child` | 组件 ID | 无 | 按钮内容，常用 Text 或 Icon，也可以是 Column。 |
| `variant` | `default`、`primary`、`borderless` | `default` | 描边按钮、主题色实心按钮、无边框按钮。 |
| `action` | object | 无 | 点击时执行 Event Action 或 FunctionCall Action。 |

Event Action：

```json
{
  "event": {
    "name": "submit",
    "context": {
      "name": { "path": "/form/name" },
      "source": "android"
    }
  }
}
```

FunctionCall Action：

```json
{ "functionCall": { "call": "openUrl", "args": { "url": "https://a2ui.org" } } }
```

#### TextField

| 属性 | 类型/可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `label` | string/动态值 | `""` | 输入框标签。 |
| `value` | string/动态值 | `""` | 当前值；使用 `{ "path": ... }` 时输入会写回数据模型。 |
| `variant` | `shortText`、`longText`、`number`、`obscured` | `shortText` | 单行、多行、数字、密码输入。 |
| `validationRegexp` | string | `""` | Java 正则；空值或不匹配时显示输入错误。 |
| `checks` | array | 无 | 通用校验规则，见下文。 |

#### CheckBox

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `label` | string/动态值 | `""` | 选项文本。 |
| `value` | boolean/动态值 | `false` | 是否选中；路径绑定支持双向写回。 |
| `checks` | array | 无 | 通用校验规则。 |

#### ChoicePicker

| 属性 | 类型/可选值 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `label` | string/动态值 | `""` | 选择器标签。 |
| `options` | array | 无 | 选项数组，每项包含 `label` 和 `value`。 |
| `value` | string 数组/动态值 | 空数组 | 已选值；路径绑定支持双向写回。 |
| `variant` | `mutuallyExclusive`、`multipleSelection` | `mutuallyExclusive` | 单选或多选。 |
| `displayStyle` | `checkbox`、`chips` | `checkbox` | 普通选项或 chips 外观。 |
| `filterable` | boolean/动态值 | `false` | 是否显示按 label 过滤的输入框。 |
| `checks` | array | 无 | 通用校验规则。 |

#### Slider

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `label` | string/动态值 | `""` | 滑块标签。 |
| `min` | number/动态值 | `0` | 最小值。 |
| `max` | number/动态值 | `100` | 最大值。 |
| `value` | number/动态值 | `min` | 当前值；拖动后写回路径，并显示当前数值。 |
| `checks` | array | 无 | 通用校验规则。 |

原生 SeekBar 使用 0～1000 的进度映射到 min～max。

#### DateTimeInput

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `label` | string/动态值 | `""` | 输入标签。 |
| `value` | string/动态值 | `""` | ISO 日期/时间；路径绑定支持双向写回。 |
| `enableDate` | boolean/动态值 | `false` | 是否选择日期。 |
| `enableTime` | boolean/动态值 | `false` | 是否选择时间。 |
| `min` | string/动态值 | `""` | 最小日期，支持 ISO 日期格式。 |
| `max` | string/动态值 | `""` | 最大日期，支持 ISO 日期格式。 |
| `checks` | array | 无 | 通用校验规则。 |

输出格式：日期 `yyyy-MM-dd`，时间 `HH:mm:ss`，日期时间 `yyyy-MM-dd'T'HH:mm:ss`。

#### checks 通用校验项

```json
{
  "checks": [
    {
      "condition": { "path": "/form/agreed" },
      "message": "请先同意用户协议"
    }
  ]
}
```

`condition` 解析为 true 表示通过；第一条失败规则的 `message` 会显示为原生错误提示。

## 4. 如何布局一个音乐播放器

推荐组件树：

```text
Card
└── Column
    ├── Image             封面
    ├── Text              曲名
    ├── Text              歌手/专辑
    ├── Slider            播放进度
    └── Row               控制区
        ├── Button        上一首
        ├── Button        播放/暂停
        └── Button        下一首
```

关键 JSON 片段：

```json
{
  "id": "root",
  "component": "Card",
  "child": "player"
}
```

```json
{
  "id": "controls",
  "component": "Row",
  "justify": "center",
  "align": "center",
  "children": ["previous", "play", "next"]
}
```

```json
{
  "id": "play",
  "component": "Button",
  "variant": "borderless",
  "child": "playIcon",
  "action": { "event": { "name": "togglePlay" } }
}
```

完整示例在 `sample/src/main/assets/music_player.json`。

A2UI 负责界面和事件，不强制应用的播放引擎。典型交互流程：

1. 用户点击播放按钮，`A2uiActionListener` 收到 `togglePlay`。
2. 应用使用 MediaPlayer、ExoPlayer 或现有播放服务执行播放/暂停。
3. 应用通过 `updateDataModel` 更新 `/position`、曲名或播放状态。
4. A2UI 根据新数据重新渲染绑定属性。

```java
a2uiView.setActionListener(action -> {
    if ("togglePlay".equals(action.getName())) {
        playerController.toggle();
    }
});
```

如果不需要自定义控制区，直接使用 `AudioPlayer` 即可得到系统媒体控制器。

## 5. 协议合规性说明

A2UI v0.9.1 Basic Catalog 的 schema 对每个组件均声明 `"unevaluatedProperties": false`，这意味着 schema 未定义的字段（如曾经的 `style`）是协议外扩展，客户端应当**静默忽略**而不是解析或报错。

本库严格遵循协议：JSON 消息只解析协议定义的字段；视觉样式（背景、圆角、padding、阴影、渐变等）完全不通过 JSON 传递，而是通过 Android 原生 `A2uiComponentStyler` 回调在应用层完成。

## 6. 全局、类型和组件私有样式设置器

A2UI v0.9.1 协议不包含 `style` 字段。所有视觉定制（背景、圆角、padding、阴影、渐变、图片等）必须通过 Android 原生 `A2uiComponentStyler` 实现。

```java
// 1. 全局：没有更具体的设置器时使用。
a2uiView.setGlobalComponentStyler((view, component, context) -> {
    if (view instanceof TextView) {
        ((TextView) view).setIncludeFontPadding(false);
    }
});

// 2. 类型：所有 Button 使用这个设置器。
a2uiView.registerComponentTypeStyler("Button", (view, component, context) -> {
    view.setMinimumHeight(dp(48));
});

// 3. 私有：仅指定 Surface 中 ID 为 music-play 的组件使用。
a2uiView.registerComponentStyler("demo", "music-play", (view, component, context) -> {
    GradientDrawable background = new GradientDrawable();
    background.setColor(Color.WHITE);
    background.setCornerRadius(dp(28));
    view.setBackground(background);
});
```

另有两参数重载，作用于所有 Surface 中同 ID 的组件：

```java
a2uiView.registerComponentStyler("music-play", styler);
```

### 选择规则

```text
指定 Surface + 组件 ID 的设置器
    > 跨 Surface 的组件 ID 设置器
    > 组件类型设置器
    > 全局设置器
```

同一个组件只调用命中的最高优先级设置器，不会把全局、类型和私有设置器全部串联执行。JSON `style` 总是先应用，因此原生设置器可以覆盖声明式背景或间距。

注册后已有 Surface 会立即刷新。类型或 ID 注册方法传入 `null` 可移除设置器，`setGlobalComponentStyler(null)` 可清除全局设置器。

回调参数：

- `view`：可强转为 `TextView`、`ImageView`、`LinearLayout` 等实际原生类型。
- `component`：原始组件 JSON，可读取 `variant` 等属性。
- `context`：可获取 `Context`、Surface、数据作用域，或调用 `renderChild`、`dispatchAction`。

## 7. 示例索引

Sample 顶部可切换以下示例：

| 文件 | 内容 |
| --- | --- |
| `music_player.json` | 音乐播放器、封面渐变、进度、播放控制 Action。 |
| `weather_card.json` | 天气概览、指标、动态三日预报。 |
| `checkin_card.json` | 连续签到、每周状态、签到 Action。 |
| `schedule_card.json` | 日程时间、地点、参与人和操作按钮。 |
| `fitness_card.json` | 步数、运动进度和健康指标。 |
| `payment_card.json` | 账户余额、收支和快捷操作。 |
| `sample.json` | 表单、校验、输入绑定和动态商品卡片。 |
| `weather_image_card.json` | 天气卡片副本，通过私有 Styler 和 `context.loadImage` 实现整卡网络图片背景。 |
| `media_showcase.json` | Image、Icon、Video、AudioPlayer、Text 和 Divider。 |
| `layout_showcase.json` | Row、Column、List、Card、Tabs、Modal 和 Divider。 |
| `input_controls.json` | TextField、CheckBox、ChoicePicker、Slider、DateTimeInput 和 Button 的主要变体。 |

媒体展示、布局导航和输入控件三个组件示例合起来覆盖 Basic Catalog 全部 18 类组件。
