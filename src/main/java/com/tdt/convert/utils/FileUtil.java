package com.tdt.convert.utils;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @program: trunk
 * @description: 操作文件工具类
 * @author: Mr.superbeyone
 * @create: 2018-10-23 14:04
 **/
public class FileUtil {


    public static String getFileMD5(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        // 16进制
        return bigInt.toString(16);
    }


    public static boolean delAllFile(File path) {
        boolean flag = false;
        if (!path.exists()) {
            System.out.println("[文件所在路径不存在]");
            return true;
        }
        if (!path.isDirectory()) {
            path.delete();
            System.out.println(path.getAbsolutePath() + "\t文件[删除成功]");
            return true;
        }
        File[] fileList = path.listFiles();
        for (int i = 0; i < fileList.length; i++) {
            if (fileList[i].isFile()) {
                fileList[i].delete();
            }
            if (fileList[i].isDirectory()) {
                delAllFile(fileList[i]);
                fileList[i].delete();
                flag = true;
            }
        }
        path.delete();
        if (flag == true) {
            System.out.println(path.getAbsolutePath() + "\t文件[删除成功]");
        }

        return flag;
    }


    public static String getFileExtName(File file) {
        String filename = file.getName();
        return filename.substring(filename.lastIndexOf("."), filename.length()).toLowerCase();
    }

    public static String getFileName(File file) {
        String filename = file.getName();
        return filename.substring(0, filename.lastIndexOf("."));
    }


    /*public static String getUserEmailInfo() {
        String email = RequestHolder.getCurrentUser().getLoginEmail();
        return getUserEmailInfo(email);
    }*/

    public static String getUserEmailInfo(String email) {
        return "tdt_" + email.replaceAll("\\@", "_").replaceAll("\\.", "_").replaceAll("\\#", "_").trim();
    }

    /**
     * 此方法将默认设置解压缩后文件的保存路径为zip文件所在路径
     * 即解压缩到当前文件夹下
     *
     * @param zip         zip文件位置
     * @param charsetName 字符编码
     */
//    public static void unpack(String zip, String charsetName) {
//        unpack(new File(zip), charsetName);
//    }

    /**
     * @param zip         zip文件位置
     * @param outputDir   解压缩后文件保存路径
     * @param charsetName 字符编码
     */
//    public static void unpack(String zip, String outputDir, String charsetName) {
//        unpack(new File(zip), outputDir);
//    }

    /**
     * 此方法将默认设置解压缩后文件的保存路径为zip文件所在路径
     * 即解压缩到当前文件夹
     *
     * @param zip         zip文件位置
     * @param charsetName 字符编码
     */
//    public static void unpack(File zip, String charsetName) {
//        unpack(zip, null, charsetName);
//    }

    /**
     * @param zip       zip文件位置
     * @param outputDir 解压缩后文件保存路径
     */
//    public static void unpack(File zip, File outputDir) {
//        unpack(zip, outputDir, "");
//    }

    /**
     * @param zip         zip文件位置
     * @param outputDir   解压缩后文件保存路径
     * @param charsetName 字符编码
     */
    public static void unpack2(File zip, File outputDir, String charsetName) {

        FileOutputStream out = null;
        InputStream in = null;
        //读出文件数据
        ZipFile zipFileData = null;

        ZipFile zipFile = null;
        try {
            //若目标保存文件位置不存在
            if (outputDir != null && !outputDir.exists()) {
                outputDir.mkdirs();
            }

            if (charsetName != null && charsetName != "") {
                zipFile = new ZipFile(zip.getPath(), Charset.forName(charsetName));
            } else {
                zipFile = new ZipFile(zip.getPath(), Charset.forName("utf8"));
            }
            //zipFile = new ZipFile(zip.getPath(), Charset.forName(charsetName));
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            //处理创建文件夹
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String filePath = "";

                if (outputDir == null) {
                    filePath = zip.getParentFile().getPath() + File.separator + entry.getName();
                } else {
                    filePath = outputDir.getPath() + File.separator + entry.getName();
                }
                File file = new File(filePath);
                File parentFile = file.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                if (parentFile.isDirectory()) {
                    continue;
                }
            }

            if (charsetName != null && charsetName != "") {
                zipFileData = new ZipFile(zip.getPath(), Charset.forName(charsetName));
            } else {
                zipFileData = new ZipFile(zip.getPath(), Charset.forName("utf8"));
            }
            Enumeration<? extends ZipEntry> entriesData = zipFileData.entries();
            while (entriesData.hasMoreElements()) {
                ZipEntry entry = entriesData.nextElement();
                in = zipFile.getInputStream(entry);
                String filePath = "";
                if (outputDir == null) {
                    filePath = zip.getParentFile().getPath() + File.separator + entry.getName();
                } else {
                    filePath = outputDir.getPath() + File.separator + entry.getName();
                }
                File file = new File(filePath);
                if (file.isDirectory()) {
                    continue;
                }
                out = new FileOutputStream(filePath);
                int len = -1;
                byte[] bytes = new byte[1024];
                while ((len = in.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
                zipFile.close();
                zipFileData.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void unpack(File zipFile, File descDir) {
        try (ZipArchiveInputStream inputStream = getZipFile(zipFile)) {
            if (!descDir.exists()) {
                descDir.mkdirs();
            }
            ZipArchiveEntry entry = null;

            while ((entry = inputStream.getNextZipEntry()) != null) {
                if (entry.isDirectory()) {
                    File directory = new File(descDir, entry.getName());
                    directory.mkdirs();
                } else {
                    OutputStream os = null;
                    try {
                        File file = new File(descDir, entry.getName());
                        File parentFile = file.getParentFile();
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        os = new BufferedOutputStream(new FileOutputStream(file));
                        //输出文件路径信息
                        System.out.println("解压文件的当前路径为:" + descDir + entry.getName());
                        IOUtils.copy(inputStream, os);
                    } finally {
                        IOUtils.closeQuietly(os);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ZipArchiveInputStream getZipFile(File zipFile) throws Exception {
        return new ZipArchiveInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
    }

}
