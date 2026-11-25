# html 转 word 以及 pdf 方案

本文档介绍 `common-tools` 中将 html 模板转换为 Word / PDF 文档的实现方案、依赖要求与使用限制。

整体流程：**Thymeleaf 渲染 html 模板**（模板位于 classpath 的 `template` 目录）→ 各 DocumentCreator 按目标格式转换输出。统一入口：

```java
DocumentCreator creator = DocumentCreator.getInstance(DocumentType.WORD);
// 无水印
creator.create("模板路径", dataMap, outputStream);
// 带水印
creator.create("模板路径", dataMap, "水印文字", outputStream);
// 带水印 + baseUri（解析模板中相对路径的图片等资源）
creator.create("模板路径", dataMap, baseUri, "水印文字", outputStream);
```

---

## html 转 word

### 方式选择

html "变成 Word 文档"有三种技术路线，本质完全不同，按兼容性从高到低排列：

| 方式 | 实现 | 物理格式 | 适用场景 |
|---|---|---|---|
| 真 docx | `WordDocumentCreator`（docx4j） | Open XML（ZIP + XML） | 正式交付，推荐 |
| OLE2 伪装 | `FakeWordDocumentCreator`（POIFS 包壳） | OLE2 容器内嵌 html | 仅骗过文件类型检测，不推荐正式使用 |
| 纯 html 改后缀 | 直接输出 html，命名 `.doc` | 纯文本 html | 临时预览，最简单 |

**建议**：正式场景一律用真 docx；伪装方式仅在校验/演示"文件类型如何骗过系统"时了解即可。

### 伪装方式原理（FakeWordDocumentCreator 与纯 html 改后缀）

两者都没有生成真正的 Word 文档，区别在"外壳"：

1. **纯 html 改后缀**：文件物理上就是 UTF-8 html 文本。Word 打开时嗅探文件头（非 OLE2 魔数、非 ZIP 魔数），按纯文本读取后识别出 `<html>` 标签，走**内置 HTML 导入转换器**渲染——这是 Word 官方支持的路径，兼容性反而最好；但 `file` 命令直接显示 `HTML document`，非 Word 软件通常不认。
2. **OLE2 伪装**：`new POIFSFileSystem()` + `root.createDocument("WordDocument", htmlBytes)` 把 html 字节包进 OLE2 复合文档（老 .doc 的物理容器）。文件头是合法的 OLE2 魔数 `D0 CF 11 E0`，`file` 识别为 `CDFV2 Microsoft Word`，可骗过类型检测；但老 .doc 的 `WordDocument` 流有强约定的二进制结构（FIB 文件信息块），里面装 html 文本属于畸形文档——Word 新版本（2016+ / 365）常弹"文件已损坏"，第三方解析器按规范解析 OLE2 后找不到合法结构直接报错。

**共同结论**：`WordDocument` 流里没有合法的 doc 二进制结构（FIB），Word 的二进制解析必然失败，两种方式都依赖 Word 的降级/容错逻辑。真正的 doc 二进制格式没有开源库能从零生成（POI 的 HWPF 只能修改已有 doc），需要 .doc 时务实做法是生成 docx 后在 Word 中另存为 97-2003 格式。

#### 页面视图控制（Word HTML 元数据）

两种伪装方式最终都靠 Word/WPS 的 html 导入层渲染。默认情况下 html 没有视图指令，WPS 检测到内容是网页后会以 **Web 视图**打开（Word 则以页面视图导入）。解决方式是注入 **Word HTML 官方格式**的文档元数据——Word 自己"另存为网页"时就会写入：

```html
<html xmlns:w="urn:schemas-microsoft-com:office:word">
<head>
    <!--[if gte mso 9]>
    <xml>
        <w:WordDocument>
            <w:View>Print</w:View>
            <w:Zoom>100</w:Zoom>
        </w:WordDocument>
    </xml>
    <![endif]-->
```

- 条件注释形式：浏览器当普通注释忽略，Office 组件的 html 导入层解析 `<xml>` 块并遵从 `View` 设置，`w:View>Print</w:View>`含义 Print = 页面视图(Web = web 视图)
- **由模板声明**：视图属于文档的展示语义，直接写在模板中（html 根标签加 `xmlns:w` 命名空间、head 内加 `w:WordDocument` 块），`FakeWordDocumentCreator` 保持纯容器封装不做内容改写；同一模板对浏览器完全无害（就是注释），PDF/docx 链路亦不受影响（`xmlns:w` 是合法 XML 命名空间声明且无元素使用）
- 同一机制可扩展更多文档级设置（如默认字号、页边距），是 Word HTML 格式的标准组成部分，非 hack

### 依赖（真 docx 方式）

| 依赖 | 版本 | 说明 |
|---|---|---|
| `org.docx4j:docx4j-ImportXHTML` | 8.3.15 | html → docx 转换器 |
| `org.docx4j:docx4j-JAXB-ReferenceImpl` | 8.3.15 | docx4j core + JDK 内置 JAXB 实现 |

**适配 JDK 版本**：docx4j 8.3.x 支持 **JDK 8+**；11.x 系列要求 JDK 11+，12.x/13.x 要求更高，选型时已按项目 JDK 8 目标锁定 8.3.x。注意 8.3.x 的 JAXB 依赖 flavor 有 `docx4j-JAXB-Internal` / `docx4j-JAXB-ReferenceImpl` / `docx4j-JAXB-MOXy` 三种，本项目使用 ReferenceImpl（JDK 8 内置 JAXB 参考实现，无需额外引入 jaxb-api）。

生成的 docx 是 **Open XML 格式**（ZIP + XML，ECMA-376 标准），Word 2007+ 原生支持，WPS / LibreOffice / macOS textutil 等第三方工具均可正常解析。

### 字体

- docx **不内嵌字体**。模板 CSS `font-family` 中指定的字体名会写入 docx 的字体引用，**打开文档的机器必须安装该字体**，否则 Word 自动用后备字体替换显示（内容不丢、字形不同）
- docx4j 的字体映射机制：内置映射只有 `serif` → Times New Roman、`sans-serif` → Arial、`monospace` → Courier New，以及少量微软拉丁字体；**其它字体族名（尤其中文字体）默认视为"无映射"，会回退到 fallback 链的下一个（通常是 serif → Times New Roman）**
- `WordDocumentCreator` 中已经注册常用中文字体映射）：
  1. 把常用中文字体（Windows 宋体 / 微软雅黑 / 黑体 / 楷体 / 仿宋 / 等线、macOS 宋体、内置霞鹜文楷等）注册为"映射到自身"，避免回退成 Times New Roman
  2. 映射对象同时设置 `w:ascii` / `w:hAnsi`（拉丁字符）与 `w:eastAsia`（中文），docx4j 转换时直接复用该映射，无需转换后遍历补属性
  3. 模板使用列表之外的字体时不会自动映射（仍回退 serif → Times New Roman），需在 `COMMON_FONT_FAMILIES` 中追加
- 如需内嵌字体进 docx（`fontTable.xml` + `odttf` 混淆字体），docx4j 支持但配置繁琐，且部分字体许可证禁止内嵌，一般不建议
- 字体族名、字体格式、常用字体清单见文末「字体」章节

### 图片

- 支持 base64 data URI（`data:image/jpeg;base64,...`）、classpath 相对路径、http(s) URL 三种来源
- importer 解析 data URI 后嵌入 `word/media/`，尺寸由 html 的 `width`/CSS 控制
- 大图建议预压缩：图片以原始字节嵌入，一张数 MB 的图会直接撑大 docx 体积

### 其它转换能力与限制

- 支持：h1-h6、段落、行内格式（b/i/u/span）、表格（含 colspan/rowspan、边框底色）、ul/ol 列表（实现内已挂载默认 numbering 定义）、div 嵌套
- h1-h6 会映射到 Word 内置标题样式（Heading1/Heading2...，样式自带 outlineLvl 大纲级别），Word 的导航窗格 / 大纲视图 / 目录（TOC）可识别；该映射由 `docx4j-ImportXHTML.properties` 中的 `docx4j-ImportXHTML.Element.Heading.MapToStyle=true` 开启（默认关闭，关闭时 h1-h6 只保留字号加粗的直接格式，无大纲级别）
- CSS 支持**常用子集**：字体、颜色、对齐、边框、背景、间距生效；**float 布局、flex、position 等现代排版不支持**——这些在 Word 中没有自然对应物，模板中应避免
- 转换是**一次性语义映射**，不是像素级还原：html 的视觉效果与 docx 的最终排版不会完全一致（尤其复杂嵌套表格、混合对齐）
- 页码（"第 x 页 / 共 y 页"）：docx4j 无一步到位 API，需手工插入 PAGE / NUMPAGES 域代码的页脚，当前实现未包含
- 水印：Word 无原生水印 API，惯用做法是页眉内嵌艺术字（VML），实现繁琐，当前实现忽略水印参数
- 模板必须是**合法的 XHTML**（标签闭合、属性引号），Thymeleaf HTML 模式输出满足此要求
- 表格布局：`table-layout: auto`（默认）由 Word 自动适配页面宽度；`table-layout: fixed` 时 docx4j 按自身布局宽度计算列宽，生成的 `tblGrid` 可能超出 Word 页面内容宽（Word 对固定布局表格严格按列宽渲染，表格会溢出页面不可见）。`WordDocumentCreator` 已做修正：转换后将**超出内容宽的固定布局表格按比例缩放到页面内容宽**（保留列宽比例），自动布局表格不做改动
- 表格边框：docx4j 给每个单元格都写四边 `tcBorders`，相邻单元格边框位置重叠——Word 按边框冲突规则合并为一条线，WPS 则分别绘制，边框变粗后会出现单元格间隙。`WordDocumentCreator` 已做修正：**将出现次数最多的单元格边框组合上移到表级 `tblBorders`**（外框 + 内部横竖线），并移除与之一致的单元格边框；个别自定义边框的单元格保留自身 `tcBorders` 覆盖表级设置，同时移除零值的 `tblCellSpacing`

---

## html 转 pdf

### 实现选择

项目提供两种 html→PDF 实现，通过工厂方法切换：

```java
// flying-saucer 实现
DocumentCreator.getInstance(DocumentType.PDF).create("模板", data, out);
// openhtmltopdf 实现
DocumentCreator.getInstance(DocumentType.OPENHTMLTOPDF).create("模板", data, out);
```

| | flying-saucer（`PdfDocumentCreator`） | openhtmltopdf（`OpenHtmlToPdfDocumentCreator`） |
|---|---|---|
| 后端 | openpdf（iText 2.x 分支） | PDFBox 2.0.x |
| CSS 支持 | CSS 2.1 为主，表格/浮动较成熟 | 更完整（@page 边距盒、CSS 计数器等），布局更规范 |
| 成熟度 | 老牌稳定，文档多 | 较新，bug 修复活跃 |
| 水印/页码 | openpdf 页面事件绘制 | PDFBox 后处理绘制 |
| 字体格式支持 | TTF（推荐）、TTC（多字面展开）；OTF 内嵌有缺陷 | **仅 TTF**（OTF/TTC 实际不可用，详见「字体」章节） |
| 适用 | 存量模板以 CSS 2.1 为主 | 需要更好 CSS 兼容性的新模板 |

两种实现的水印（平铺 + 旋转 + 半透明）与页码（底部居中"第 x 页 / 共 y 页"）效果一致，字体均内嵌。

### 依赖

| 实现 | 依赖 | 版本 | 说明 |
|---|---|---|---|
| flying-saucer | `org.xhtmlrenderer:flying-saucer-pdf` | 9.4.1 | html 布局引擎 + PDF 输出（openpdf 后端） |
| flying-saucer | `com.github.librepdf:openpdf`（传递依赖） | 1.3.35 | openpdf（iText 2.x 分支） |
| openhtmltopdf | `com.openhtmltopdf:openhtmltopdf-pdfbox` | 1.0.10 | html 布局 + PDF 输出（PDFBox 后端，传递引入 PDFBox 2.0.24） |

**适配 JDK 版本**：

- flying-saucer 9.4.x 支持 **JDK 8+**。注意 9.5.0 起要求 JDK 11、9.13.0 起要求 JDK 17——升级版本时需同步评估 JDK
- openhtmltopdf 1.0.x 支持 **JDK 8+**（1.0.10 为最后一个支持 JDK 8 的正式版本，其内置 PDFBox 2.0.24 同样兼容 JDK 8）

生成的 PDF 为标准格式，任意阅读器可打开。

### 字体

- 两种实现默认字体均不含中文，均通过 `FontRegistrar` 自动注册 classpath `fonts/` 目录下的字体文件，jar 包部署与本地文件系统均兼容
  - flying-saucer：`FontRegistrar.registerTo(ITextFontResolver)`，openpdf 对相对路径自动回退 classpath 加载
  - openhtmltopdf：`FontRegistrar.getFontFiles()` + `getFontFamilyName()` 取字体族名，逐个 `builder.useFont(supplier, 族名)` 注册（supplier 从 classpath 读流，运行期自动关闭）
- 注册后的字体以**内嵌 + 子集化**方式写入 PDF（flying-saucer 走 openpdf Identity-H；openhtmltopdf 走 PDFBox `PDType0Font` 子集嵌入）——**任何设备上显示一致，无需阅读方安装字体**（与 docx 方案的本质区别）
- 模板 CSS `font-family` 写**字体真实族名**（从字体文件内部 name 表读取，如 `"LXGW WenKai"`，而非文件名）
- **字体格式存在重要差异：两种实现均只推荐 TTF**，OTF/TTC 的支持情况各不相同，详见文末「字体」章节
- 水印与页码文字复用同一字体机制，中文正常显示

### 图片

- 支持 base64 data URI 内嵌（推荐，单文件交付无外部依赖）、相对路径、http(s) URL
- 相对路径图片通过 `create(templatePath, data, baseUri, watermarkText, out)` 的 `baseUri` 参数解析（如 `new File("模板目录").toURI().toString()`；两种 PDF 实现与 word 实现均支持）
- data URI 图片由布局引擎解码后嵌入 PDF，尺寸跟随 html `width`/CSS 声明

### 其它转换能力与限制

- 两种实现均使用 **XML 解析器**（非容错 html parser）解析模板，**html 必须是合法 XHTML**：`<meta/>`、`<img/>`、`<br/>` 等必须自闭合，属性必须有引号，Thymeleaf HTML 模式的输出满足要求；模板中的 Word HTML 元数据条件注释（前文"页面视图控制"）对两者均无害
- 页面尺寸与边距由模板 CSS `@page { size: A4; margin: 40pt; }` 控制
- 页码（"第 x 页 / 共 y 页"）：
  - flying-saucer：已实现，每页底部居中；总页数用 PdfTemplate 占位、文档关闭时回填（openpdf 经典手法）
  - openhtmltopdf：已实现，先由 openhtmltopdf 渲染出 PDF 字节，再用 PDFBox 逐页追加页码（总页数直接取 `getNumberOfPages()`，无需占位回填）
- 文字水印：
  - flying-saucer：openpdf 页面事件（`PdfPageEventHelper`）绘制，平铺 + 旋转 + 透明度（`PdfGState`）
  - openhtmltopdf：PDFBox 后处理绘制（`PDPageContentStream` + `PDExtendedGraphicsState`），平铺 + 旋转 + 0.1 透明度，参数与 flying-saucer 实现一致
  - 说明：openhtmltopdf 的 CSS 也支持固定定位元素做水印，但 1.0.10 对 `opacity` / `rgba()` 透明度实测不生效，实色水印会覆盖正文文字，故采用 PDFBox 后处理以保证半透明效果与正文可读性
- openhtmltopdf 的水印/页码在渲染完成后由 PDFBox 统一绘制，模板本身保持干净，无需注入任何样式或标记
- 与 docx 的对比优势：**所见即所得**（html 布局引擎真实排版）、字体内嵌跨设备一致、水印/页码内建支持
- 局限：flex/grid 等现代布局支持有限（openhtmltopdf 略好于 flying-saucer，但仍非浏览器级）；JavaScript 均不执行，动态内容需在模板渲染阶段生成
- openhtmltopdf 基于 Java2D 渲染文本/图片，与 flying-saucer 一样在桌面环境下会激活 AWT（macOS 上 Dock 会短暂显示 java 图标）；服务器/Linux 无图形环境时自动 headless，无影响

## 字体

### 字体族名约定

- 模板 CSS `font-family` 写**字体真实族名**（字体文件 name 表 ID 1 的值，如 `"LXGW WenKai"`），不是文件名
- 同一字体常同时注册英文族名与中文族名（如 `SimSun` / `宋体`），两者都能命中；**推荐写英文族名**，不依赖文档编码环境
- docx4j（word）与 flying-saucer / openhtmltopdf（pdf）都是按族名匹配的

### 各方案的字体处理方式

| 方案 | 是否内嵌字体 | 说明 |
|---|---|---|
| html→docx（docx4j） | 不内嵌 | 仅写入字体名引用，打开文档的机器需安装对应字体，否则后备字体替换（内容不丢、字形不同） |
| html→pdf（flying-saucer / openhtmltopdf） | 内嵌 + 子集化 | 只打包用到的字形，任何设备显示一致 |

### 字体格式支持（实测）

| 格式 | flying-saucer（openpdf） | openhtmltopdf（PDFBox） |
|---|---|---|
| **TTF**（TrueType outlines） | ✅ 推荐，子集内嵌正常 | ✅ 推荐，子集内嵌正常 |
| **OTF**（CFF outlines） | ⚠️ 有缺陷：openpdf 1.3.35 子集嵌入后文本层正确但字形渲染空白/错乱 | ❌ 不支持：注册不报错但**静默回退**默认字体（中文显示为方块）；File API 注册甚至抛 NPE |
| **TTC**（字体集合） | ✅ 支持：内部通过 `enumerateTTCNames` 展开各字面；受字体嵌入许可限制（`fsType=2` 时报 "cannot be embedded due to licensing restrictions"） | ⚠️ 仅 `useFont(File, family)` 可用（内部走 `TrueTypeCollection`）；本库经 classpath `InputStream` 注册，**TTC 会静默回退**，即对本库实际不可用 |

**结论：需要内嵌中文时统一使用 TTF。** 下载的开源字体如果只有 OTF/TTC，先用工具（如 fontTools / FontForge）转换为 TTF。

### Windows 常用字体族名对照

| 中文习惯叫法 | 英文族名（推荐） | 中文族名 | 字体文件 | 备注 |
|---|---|---|---|---|
| 宋体 | `SimSun` | 宋体 | `simsun.ttc` | Win 系统内置，最常用的正文字体 |
| 新宋体 | `NSimSun` | 新宋体 | `simsun.ttc` | 宋体的等宽变体，同文件内 |
| 仿宋 | `FangSong` | 仿宋 | `simfang.ttf` | 公文正文常用 |
| 楷体 | `KaiTi` | 楷体 | `simkai.ttf` | 公文标题/引文常用 |
| 黑体 | `SimHei` | 黑体 | `simhei.ttf` | Win 内置，标题常用 |
| 微软雅黑 | `Microsoft YaHei` | 微软雅黑 | `msyh.ttc` | Vista 起内置，现代 UI 风格 |
| 等线 | `DengXian` | 等线 | `Deng.ttc` | Office 2016 起的默认正文字体 |
| 隶书 | `LiSu` | 隶书 | `SIMLI.TTF` | Office 安装附带 |
| 幼圆 | `YouYuan` | 幼圆 | `SIMYOU.TTF` | Office 安装附带 |
| 华文宋体 | `STSong` | 华文宋体 | Office 安装附带 | macOS 也有（`Songti.ttc` 内含） |
| 华文楷体 | `STKaiti` | 华文楷体 | Office 安装附带 | |
| 华文黑体 | `STHeiti` | 华文黑体 | Office 安装附带 | macOS 也有 |

macOS 注意：系统**没有** `SimSun`/`SimHei` 等字体，对应物是 `Songti SC`（宋体-简）、`Heiti SC`、`PingFang SC`（苹方）。跨平台文档引用 `SimSun` 时 macOS 会自动替换显示，内容不丢但字形有差异；要求跨平台一致请走 PDF 方案（字体内嵌）。

### 流行开源中文字体（可免费商用 / 开源许可）

| 字体 | 风格 | 格式 | 许可 | 说明 |
|---|---|---|---|---|
| **霞鹜文楷**（LXGW WenKai） | 楷体 | TTF | SIL OFL 1.1 | 本项目 `fonts/` 默认字体，族名 `LXGW WenKai`；同系列有 Light / Bold 等字重 |
| **思源宋体**（Source Han Serif SC / Noto Serif CJK SC） | 宋体 | OTF / TTC / TTF（可变字体） | SIL OFL 1.1 | Adobe + Google 合作出品，七种字重，字符覆盖完整；**注意官方发布多为 OTF**，PDF 内嵌需转换 TTF |
| **思源黑体**（Source Han Sans SC / Noto Sans CJK SC） | 黑体 | OTF / TTC / TTF | SIL OFL 1.1 | 同上，现代无衬线风格 |
| **文泉驿微米黑**（WenQuanYi Micro Hei） | 黑体 | TTC / TTF | Apache 2.0（另有 GPLv3 + 字体嵌入例外） | 体积小（约 5MB），适合嵌入式场景；实测 TTC 在 flying-saucer 下可正常内嵌 |
| **得意黑**（Smiley Sans） | 标题黑体 | TTF | SIL OFL 1.1 | 倾斜紧凑风格，适合报告标题，不适合长正文 |
| **阿里巴巴普惠体**（Alibaba PuHuiTi） | 黑体 | TTF / OTF | 阿里巴巴免费商用授权（非开源） | 商用免费但需遵守授权条款，分为多个字重 |

实践建议：

- 生成 PDF（需内嵌）：优先选 **TTF 格式**的开源字体；本项目已内置霞鹜文楷，开箱即用
- 生成 docx（不内嵌）：选 Windows 自带字体（`SimSun`、`Microsoft YaHei` 等）兼容性最好；若使用开源字体，需在目标机器安装
- 避免使用商业字体（方正、汉仪等）的文件再分发，注意授权范围
