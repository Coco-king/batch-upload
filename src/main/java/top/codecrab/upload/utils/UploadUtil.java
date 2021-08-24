package top.codecrab.upload.utils;

import com.aliyun.oss.OSS;
import org.springframework.web.multipart.MultipartFile;

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
        OSS ossClient = OSSClientFactory.getInstance();
        try {
            int length = files.length;
            final BlockingQueue<Future<String>> queue = new LinkedBlockingDeque<>(length);
            //实例化CompletionService
            final CompletionService<String> completionService = new ExecutorCompletionService<>(ThreadPoolFactory.getInstance(), queue);

            for (MultipartFile file : files) {
                completionService.submit(new AliyunUpload(ossClient, file));
            }
            for (int i = 0; i < length; i++) {
                Future<String> future = completionService.take();
                String url = future.get();
                result.add(url);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }/* finally {
            //如果关闭连接，第二次批量上传就会失败，因为这里使用的OSSClient是单例的。也可以随时new，具体取舍自己定
            ossClient.shutdown();
        }*/
        return result;
    }

}
