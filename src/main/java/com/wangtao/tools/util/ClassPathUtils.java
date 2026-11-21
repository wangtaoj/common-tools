package com.wangtao.tools.util;

import com.wangtao.tools.exception.ToolRuntimeException;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 类路径资源工具类，列出 classpath 目录下的资源文件名，兼容文件系统与 jar 包部署
 *
 * @author wangtao
 * Created at 2026-11-21
 */
public final class ClassPathUtils {

    private ClassPathUtils() {

    }

    /**
     * 列出 classpath 指定目录下所有文件的资源名（不含目录前缀，如 "LXGWWenKai-Regular.ttf"），
     * 不递归子目录
     * @param directory classpath 目录路径（如 "fonts/"）
     * @return 目录下的文件资源名列表
     */
    public static List<String> listFileNames(String directory) {
        return listFileNames(directory, false);
    }

    /**
     * 列出 classpath 指定目录下所有文件的资源名（不含目录前缀，如 "LXGWWenKai-Regular.ttf"）
     * @param directory classpath 目录路径（如 "fonts/"），需以 "/" 结尾
     * @param recursive 是否递归读取子目录
     * @return 相对该目录的文件资源名列表（递归时含子目录路径，如 "sub/xxx.ttf"）
     */
    public static List<String> listFileNames(String directory, boolean recursive) {
        if (directory == null || directory.isEmpty()) {
            throw new IllegalArgumentException("directory 不能为空");
        }
        // 统一为以 "/" 结尾的形式
        String dir = directory.endsWith("/") ? directory : directory + "/";
        List<String> fileNames = new ArrayList<>();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> resources = classLoader.getResources(dir);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if ("file".equals(url.getProtocol())) {
                    // 本地文件系统（IDE 运行、解压部署）
                    listFromFileSystem(new File(url.toURI()), "", recursive, fileNames);
                } else if ("jar".equals(url.getProtocol())) {
                    // jar 包部署
                    listFromJar(url, dir, recursive, fileNames);
                }
            }
        } catch (Exception e) {
            throw new ToolRuntimeException(e);
        }
        return fileNames;
    }

    /**
     * 从本地文件系统目录列出文件
     */
    private static void listFromFileSystem(File dir, String prefix, boolean recursive,
                                           List<String> fileNames) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                if (recursive) {
                    listFromFileSystem(file, prefix + file.getName() + "/", recursive, fileNames);
                }
            } else {
                fileNames.add(prefix + file.getName());
            }
        }
    }

    /**
     * 从 jar 包列出文件
     */
    private static void listFromJar(URL url, String prefix, boolean recursive,
                                    List<String> fileNames) throws Exception {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith(prefix) || name.equals(prefix)) {
                    continue;
                }
                // 目录条目以 "/" 结尾
                if (name.endsWith("/")) {
                    continue;
                }
                String relative = name.substring(prefix.length());
                if (!recursive && relative.contains("/")) {
                    // 非递归时跳过子目录内容
                    continue;
                }
                fileNames.add(relative);
            }
        }
    }
}
