# html 转 word 以及 pdf 方案

本文档介绍 `common-tools` 中将 html 模板转换为 Word / PDF 文档的实现方案、依赖要求与使用限制。

整体流程：**Thymeleaf 渲染 html 模板**（模板位于 classpath 的 `template` 目录）→ 各 DocumentCreator 按目标格式转换输出。统一入口：

```java
DocumentCreator creator = DocumentCreator.getInstance(DocumentType.WORD);
creator.create("模板路径", dataMap, outputStream);
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

### 依赖（真 docx 方式）

| 依赖 | 版本 | 说明 |
|---|---|---|
| `org.docx4j:docx4j-ImportXHTML` | 8.3.15 | html → docx 转换器 |
| `org.docx4j:docx4j-JAXB-ReferenceImpl` | 8.3.15 | docx4j core + JDK 内置 JAXB 实现 |

**适配 JDK 版本**：docx4j 8.3.x 支持 **JDK 8+**；11.x 系列要求 JDK 11+，12.x/13.x 要求更高，选型时已按项目 JDK 8 目标锁定 8.3.x。注意 8.3.x 的 JAXB 依赖 flavor 有 `docx4j-JAXB-Internal` / `docx4j-JAXB-ReferenceImpl` / `docx4j-JAXB-MOXy` 三种，本项目使用 ReferenceImpl（JDK 8 内置 JAXB 参考实现，无需额外引入 jaxb-api）。

生成的 docx 是 **Open XML 格式**（ZIP + XML，ECMA-376 标准），Word 2007+ 原生支持，WPS / LibreOffice / macOS textutil 等第三方工具均可正常解析。

### 字体

- docx **不内嵌字体**。模板 CSS `font-family` 中指定的字体名会写入 docx 的字体引用，**打开文档的机器必须安装该字体**，否则 Word 自动用后备字体替换显示（内容不丢、字形不同）
- 与 PDF 方案的本质区别：PDF 由 flying-saucer 在生成时内嵌字形，跨设备显示一致；docx 的显示效果依赖阅读方环境
- 如需内嵌字体进 docx（`fontTable.xml` + `odttf` 混淆字体），docx4j 支持但配置繁琐，且部分字体许可证禁止内嵌，一般不建议

#### Windows 常用字体族名对照

同一字体在字体文件内部同时注册英文族名与中文族名（name 表 ID 1），CSS `font-family` 两者都能命中，推荐写英文族名（不依赖文档编码环境）：

| 中文习惯叫法 | 英文族名（推荐） | 中文族名 | 字体文件 | 备注 |
|---|---|---|---|---|
| 宋体 | `SimSun` | 宋体 | `simsun.ttc` | Win 系统内置，最常用的正文字体 |
| 新宋体 | `NSimSun` | 新宋体 | `simsun.ttc` | 宋体的等宽变体，同文件内 |
| 仿宋 | `FangSong` | 仿宋 | `simfang.ttf` | 公文正文常用 |
| 楷体 | `KaiTi` | 楷体 | `simkai.ttf` | 公文标题/引文常用 |
| 黑体 | `SimHei` | 黑体 | `simhei.ttf` | Win 内置，标题常用 |
| 微软雅黑 | `Microsoft YaHei` | 微软雅黑 | `msyh.ttc` | Vista 起内置，现代 UI 风格 |
| 隶书 | `LiSu` | 隶书 | `SIMLI.TTF` | Office 安装附带 |
| 幼圆 | `YouYuan` | 幼圆 | `SIMYOU.TTF` | Office 安装附带 |
| 华文宋体 | `STSong` | 华文宋体 | Office 安装附带 | macOS 也有（`Songti.ttc` 内含） |
| 华文楷体 | `STKaiti` | 华文楷体 | Office 安装附带 | |
| 华文黑体 | `STHeiti` | 华文黑体 | Office 安装附带 | macOS 也有 |
| 等线 | `DengXian` | 等线 | `Deng.ttc` | Office 2016 起的默认正文字体 |

macOS 注意：系统**没有** `SimSun`/`SimHei` 等字体，对应物是 `Songti SC`（宋体-简，`Songti.ttc` 内含多个变体）、`Heiti SC`、`PingFang SC`（苹方）。跨平台文档引用 `SimSun` 时 macOS 会自动替换显示，内容不丢但字形有差异；要求跨平台一致请走 PDF 方案（字体内嵌）。

### 图片

- 支持 base64 data URI（`data:image/jpeg;base64,...`）、classpath 相对路径、http(s) URL 三种来源
- importer 解析 data URI 后嵌入 `word/media/`，尺寸由 html 的 `width`/CSS 控制
- 大图建议预压缩：图片以原始字节嵌入，一张数 MB 的图会直接撑大 docx 体积

### 其它转换能力与限制

- 支持：h1-h6、段落、行内格式（b/i/u/span）、表格（含 colspan/rowspan、边框底色）、ul/ol 列表（实现内已挂载默认 numbering 定义）、div 嵌套
- CSS 支持**常用子集**：字体、颜色、对齐、边框、背景、间距生效；**float 布局、flex、position 等现代排版不支持**——这些在 Word 中没有自然对应物，模板中应避免
- 转换是**一次性语义映射**，不是像素级还原：html 的视觉效果与 docx 的最终排版不会完全一致（尤其复杂嵌套表格、混合对齐）
- 页码（"第 x 页 / 共 y 页"）：docx4j 无一步到位 API，需手工插入 PAGE / NUMPAGES 域代码的页脚，当前实现未包含
- 水印：Word 无原生水印 API，惯用做法是页眉内嵌艺术字（VML），实现繁琐，当前实现忽略水印参数
- 模板必须是**合法的 XHTML**（标签闭合、属性引号），Thymeleaf HTML 模式输出满足此要求

---

## html 转 pdf

### 依赖

| 依赖 | 版本 | 说明 |
|---|---|---|
| `org.xhtmlrenderer:flying-saucer-pdf` | 9.4.1 | html 布局引擎 + PDF 输出（openpdf 后端） |
| `com.github.librepdf:openpdf`（传递依赖） | 1.3.35 | openpdf（iText 2.x 分支） |

**适配 JDK 版本**：9.4.x 支持 **JDK 8+**。注意 flying-saucer 9.5.0 起要求 JDK 11、9.13.0 起要求 JDK 17——升级版本时需同步评估 JDK。

生成的 PDF 为标准 PDF 1.4+，任意阅读器可打开。

### 字体

- flying-saucer-pdf默认字体不支持中文，通过 `FontRegistrar` 自动注册 classpath `fonts/` 目录下的字体文件（`.ttf/.otf/.ttc`），jar 包部署与本地文件系统均兼容（openpdf 对相对路径自动回退 classpath 加载）
- 注册后的字体以**内嵌(EMBEDDED + Identity-H)方式**写入 PDF，子集化只打包用到的字形——**任何设备上显示一致，无需阅读方安装字体**（与 docx 方案的本质区别）
- **只支持 TTF（TrueType outlines）格式的字体文件内嵌**。OTF（CFF outlines）在 openpdf 1.3.35 的 Identity-H 子集嵌入路径上存在缺陷（实测文本层正确但字形渲染空白/错乱），推荐使用 TTF；TTC 集合字体可用 `字体名,索引` 语法引用
- 模板 CSS `font-family` 写**字体真实族名**（从字体文件内部 name 表读取，如 `"LXGW WenKai"`，而非文件名）
- 水印与页码文字复用同一字体机制，中文正常显示

### 图片

- 支持 base64 data URI 内嵌（推荐，单文件交付无外部依赖）、相对路径（相对 baseUrl）、http(s) URL
- data URI 图片由 flying-saucer 解码后嵌入 PDF，尺寸跟随 html `width`/CSS 声明

### 其它转换能力与限制

- flying-saucer 使用 **XML 解析器**（非容错 html parser）解析模板，**html 必须是合法 XHTML**：`<meta/>`、`<img/>`、`<br/>` 等必须自闭合，属性必须有引号，Thymeleaf HTML 模式的输出满足要求
- 页面尺寸与边距由模板 CSS `@page { size: A4; margin: 40pt; }` 控制
- 页码（"第 x 页 / 共 y 页"）：已实现，每页底部居中；总页数用 PdfTemplate 占位、文档关闭时回填（openpdf 经典手法）
- 文字水印：已实现，平铺 + 旋转 + 透明度（`PdfGState`），绘制在正文内容层之上
- 与 docx 的对比优势：**所见即所得**（html 布局引擎真实排版）、字体内嵌跨设备一致、水印/页码内建支持
- 局限：CSS 支持以 CSS 2.1 为主（flying-saucer 的强项），flex/grid 等现代布局同样不支持；JavaScript 不执行，动态内容需在模板渲染阶段生成
