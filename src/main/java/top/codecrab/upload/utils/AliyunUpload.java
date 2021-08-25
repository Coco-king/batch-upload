package top.codecrab.upload.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadFileResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import top.codecrab.upload.config.AliyunConfig;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.*;

@Slf4j
public class AliyunUpload implements Callable<String> {

    //默认 本地存储路径
    private final String saveLocalPath = "D:/java_class/Company/WebRoot/";

    //默认 文件保存路径
    private String saveContextPath = "cnblogs-banner/";

    private final MultipartFile file;

    public AliyunUpload(MultipartFile file) {
        this.file = file;
    }

    /**
     * 文件具体上传
     */
    @Override
    public String call() throws Exception {
        //获取文件名称
        String fileName = file.getOriginalFilename();
        //生成存储路径
        String saveHandlerPath = saveLocalPath + saveContextPath;
        //获得文件后缀
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        //存储目录
        File parentDir = new File(saveHandlerPath);
        //存储目录是否存在
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        //生成文件存储名称
        String fileSaveName = RandomUtil.randomString(7) + System.currentTimeMillis() + prefix;
        try {
            File saveFile = new File(saveHandlerPath, fileSaveName);
            //移动临时文件
            file.transferTo(saveFile);
            //新增阿里云文件上传
            String fileUrl = this.uploadFile(saveHandlerPath + fileSaveName, saveContextPath + fileSaveName);
            saveFile.deleteOnExit();
            return fileUrl;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 断点续传
     */
    private String uploadFile(String uploadFile, String savePathName) {
        OSS ossClient = OSSClientFactory.getInstance();
        // 判断桶是否存在
        if (!ossClient.doesBucketExist(AliyunConfig.bucketName)) {
            // 不存在则创建桶
            ossClient.createBucket(AliyunConfig.bucketName);
            // 授权
            ossClient.setBucketAcl(AliyunConfig.bucketName, CannedAccessControlList.PublicRead);
        }

        try {
            UploadFileRequest uploadFileRequest = new UploadFileRequest(AliyunConfig.bucketName, savePathName);
            // 待上传的本地文件
            uploadFileRequest.setUploadFile(uploadFile);
            // 设置并发下载数，默认1
            uploadFileRequest.setTaskNum(5);
            // 设置分片大小，默认100KB
            uploadFileRequest.setPartSize(1024 * 1024);
            // 开启断点续传，默认关闭
            uploadFileRequest.setEnableCheckpoint(true);
            UploadFileResult uploadResult = ossClient.uploadFile(uploadFileRequest);
            CompleteMultipartUploadResult multipartUploadResult = uploadResult.getMultipartUploadResult();
            return multipartUploadResult.getLocation();
        } catch (OSSException oe) {
            log.error("*************************************************OSS upload file error create_date "
                + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss") + "*************************************");
            log.error("Caught an OSSException, which means your request made it to OSS, "
                + "but was rejected with an error response for some reason.");
            log.error("Error Message: " + oe.getErrorCode());
            log.error("Error Code:    " + oe.getErrorCode());
            log.error("Request ID:   " + oe.getRequestId());
            log.error("Host ID:      " + oe.getHostId());
            log.error("*************************************************OSS upload file error*************************************");
        } catch (ClientException ce) {
            log.error("Caught an ClientException, which means the client encountered "
                + "a serious internal problem while trying to communicate with OSS, "
                + "such as not being able to access the network.");
            log.error("Error Message: " + ce.getMessage());
        } catch (Throwable error) {
            error.printStackTrace();
        }
        return null;
    }

    public void setSaveContextPath(String saveContextPath) {
        this.saveContextPath = saveContextPath;
    }
}

