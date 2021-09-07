package top.codecrab.upload.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import org.springframework.web.multipart.MultipartFile;
import top.codecrab.upload.config.AliyunConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author codecrab
 * @since 2021年08月21日 下午 04:56
 */

public class UploadUtil {

    /**
     * 多线程断点续传
     *
     * @param files 文件列表
     * @return 访问URL路径列表
     */
    public static List<String> multithreadedUpload(MultipartFile[] files) {
        List<String> result = new CopyOnWriteArrayList<>();
        try {
            int length = files.length;
            final BlockingQueue<Future<String>> queue = new LinkedBlockingDeque<>(length);
            //实例化CompletionService
            final CompletionService<String> completionService = new ExecutorCompletionService<>(ThreadPoolFactory.getInstance(), queue);

            for (MultipartFile file : files) {
                completionService.submit(new AliyunUpload(file));
            }
            for (int i = 0; i < length; i++) {
                Future<String> future = completionService.take();
                String url = future.get();
                result.add(url);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 单线程上传，如果文件是zip格式的，就会解压并上传，云端文件路径和压缩包路径一致，非zip文件的则直接上传
     */
    public static List<String> upload(MultipartFile file, String rootPath) {
        if ("zip".equals(FileUtil.getSuffix(file.getOriginalFilename()))) {
            try {
                return uploadZip(file.getInputStream(), CharsetUtil.CHARSET_GBK, rootPath);
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        } else {
            return Collections.singletonList(uploadSingle(file, rootPath));
        }
    }

    /**
     * 上传单个文件
     *
     * @param file     待上传文件
     * @param rootPath 根路径，为文件的父级路径
     */
    public static String uploadSingle(MultipartFile file, String rootPath) {
        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        String filePath = rootPath + "/" + RandomUtil.randomString(16) + "-" + RandomUtil.randomNumbers(16) + "." + suffix;
        try {
            OSSClientFactory.getInstance().putObject(AliyunConfig.bucketName, filePath, file.getInputStream());
            return AliyunConfig.bucketName + "." + AliyunConfig.endpoint + "/" + filePath;
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 上传zip压缩文件，UTF8编码，不常用
     *
     * @param zipPath  zip压缩包路径
     * @param rootPath 压缩包解压后的根路径，为解压后的文件夹的父级路径
     * @return 上传成功后的压缩包内的文件访问URL
     */
    public static List<String> uploadZip(String zipPath, String rootPath) {
        return uploadZip(zipPath, CharsetUtil.CHARSET_UTF_8, rootPath);
    }

    /**
     * 上传zip压缩文件，可指定编码，Windows下使用GBK
     *
     * @param zipPath  zip压缩包路径
     * @param charset  编码
     * @param rootPath 压缩包解压后的根路径，为解压后的文件夹的父级路径
     * @return 上传成功后的压缩包内的文件访问URL
     */
    public static List<String> uploadZip(String zipPath, Charset charset, String rootPath) {
        File unzip = ZipUtil.unzip(zipPath, charset);
        List<String> result = traverseFolder(unzip, rootPath, new ArrayList<>());
        deleteDir(unzip);
        return result;
    }


    /**
     * 上传zip压缩文件，UTF8编码，不常用
     *
     * @param zipStream zip压缩包流
     * @param rootPath  压缩包解压后的根路径，为解压后的文件夹的父级路径
     * @return 上传成功后的压缩包内的文件访问URL
     */
    public static List<String> uploadZip(InputStream zipStream, String rootPath) {
        return uploadZip(zipStream, CharsetUtil.CHARSET_UTF_8, rootPath);
    }

    /**
     * 上传zip压缩文件，可指定编码，Windows下使用GBK
     *
     * @param zipStream zip压缩包流
     * @param charset   编码
     * @param rootPath  压缩包解压后的根路径，为解压后的文件夹的父级路径
     * @return 上传成功后的压缩包内的文件访问URL
     */
    public static List<String> uploadZip(InputStream zipStream, Charset charset, String rootPath) {
        File temp = FileUtil.file(rootPath);
        if (!temp.exists() || temp.isFile()) {
            if (!temp.mkdirs()) {
                throw new RuntimeException("创建临时文件夹失败");
            }
        }
        File unzip = ZipUtil.unzip(zipStream, temp, charset);
        List<String> result = traverseFolder(unzip, "", new ArrayList<>());
        deleteDir(temp);
        return result;
    }

    /**
     * 递归上传文件夹内所有文件和子文件夹的文件到对象存储服务
     *
     * @param directory      需上传的文件夹
     * @param uploadRootPath 为上传的文件夹的父级路径
     * @param result         上传成功后的压缩包内的文件访问URL的存储集合
     * @return 上传成功后的压缩包内的文件访问URL
     */
    public static List<String> traverseFolder(File directory, String uploadRootPath, List<String> result) {
        if (directory.exists()) {
            if (directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (null == files || files.length == 0) {
                    return result;
                }

                StringBuilder sb = new StringBuilder(uploadRootPath);
                if (StrUtil.isNotBlank(uploadRootPath) && !uploadRootPath.endsWith("/")) {
                    sb.append("/");
                }
                String path = directory.getPath();
                if (path.contains("\\")) {
                    sb.append(path.substring(path.lastIndexOf("\\") + 1));
                }
                sb.append("/");
                for (File file : files) {
                    if (file.isDirectory()) {
                        traverseFolder(file, sb.toString(), result);
                    } else {
                        String suffix = FileUtil.getSuffix(file.getName());
                        String fileName = RandomUtil.randomString(16) + "-" + RandomUtil.randomNumbers(16) + "." + suffix;
                        traverseFolder(file, sb + fileName, result);
                    }
                }
            } else {
                OSSClientFactory.getInstance().putObject(AliyunConfig.bucketName, uploadRootPath, directory);
                result.add(AliyunConfig.bucketName + "." + AliyunConfig.endpoint + "/" + uploadRootPath);
            }
        } else {
            throw new RuntimeException("文件不存在！");
        }
        return result;
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param dir 将要删除的文件目录
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null && children.length != 0) {
                //递归删除目录中的子目录下
                for (String child : children) {
                    if (!deleteDir(new File(dir, child))) {
                        return false;
                    }
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
    }
}
