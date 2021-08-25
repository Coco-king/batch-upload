package top.codecrab.upload.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import top.codecrab.upload.config.AliyunConfig;

/**
 * @author codecrab
 * @since 2021年08月21日 下午 04:14
 */
public class OSSClientFactory {

    public static OSS getInstance() {
        return INSTANCE;
    }

    private static final OSS INSTANCE;

    static {
        INSTANCE = new OSSClientBuilder().build(
            AliyunConfig.endpoint, AliyunConfig.accessKeyId, AliyunConfig.accessKeySecret
        );
    }

}
