package top.codecrab.upload.config;

public final class AliyunConfig {
    //你的oss所在域，要加http://  不明白可以对照你的文件引用地址
    public static String endpoint = "oss-cn-shanghai.aliyuncs.com";
    //密匙keyId 可以在阿里云获取到
    public static String accessKeyId = "xxx";
    //密匙keySecret 可以在阿里云获取到
    public static String accessKeySecret = "xxx";
    //你的bucketName 名称  即是你的OSS对象名称 不明白查oss开发文档专业术语
    public static String bucketName = "xxx";
}

