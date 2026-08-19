# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

`com.wangtao:common-tools` 是一个 Java 工具库（非可运行应用），沉淀各类通用静态工具类，供其他项目以 Maven 依赖形式引入。编译目标 JDK 1.8，依赖版本与 Spring Boot 2.7.18 对齐（见 `pom.xml` 中的版本注释）。

## 构建与测试

直接使用系统 `mvn`。本机通过 sdkman 管理 JDK，Maven 默认在 JDK 8（8.0.502-librca）下运行。

- 编译：`mvn -q compile`
- 打包并安装到本地仓库：`mvn -q install`
- 跑全部测试：`mvn -q test`
- 跑单个测试类：`mvn -q test -Dtest=DecimalFormatUtilsTest`
- 跑单个测试方法：`mvn -q test -Dtest=DecimalFormatUtilsTest#testFormat`

如需显式切换 JDK：`sdk use java 8.0.502-librca`。项目语言级别为 1.8，更高版本 JDK 也能编译，但不要引入 JDK 8 之后的 API。

## 代码组织与约定

包根 `com.wangtao.tools`，按职责分子包：

- `util` — 静态工具类：`JsonUtils`、`XmlUtils`、`DateUtils`、`DecimalFormatUtils`、`GenericUtils`、`UuidUtils`、`Assert`、`DownloadUtils`、`JavaTimeModuleUtils`
- `jackson` — 自定义 Jackson 序列化器：`BigDecimalSerializer`
- `jackson.xml` — XML 元素名覆写机制：`@XmlElement` 注解 + `ObjectXmlSerializer` + `SdoObjectXmlSerializer`
- `exception` — `ToolRuntimeException`（包装检查型异常的运行时异常）

**统一约定**：
- 工具类一律 `final` + 私有构造器，只暴露 `static` 方法。
- 注释与 Javadoc 用中文；每个类带 `@author wangtao` / `Created at` 头注。
- 受检异常一律包装：序列化/反序列化失败抛 `IllegalArgumentException`，IO/下载类失败抛 `ToolRuntimeException`。
- 文件 UTF-8 / LF（IDEA 工程编码已在 `.idea/encodings.xml` 固定为 UTF-8）。
- 单元测试框架为junit5，编写单元测试时尽量使用断言而不是打印

