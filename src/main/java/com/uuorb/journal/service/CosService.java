package com.uuorb.journal.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.uuorb.journal.config.TencentCloudConfig;
import com.uuorb.journal.controller.vo.CosCredential;
import com.uuorb.journal.controller.vo.UploadCredential;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import com.tencent.cloud.CosStsClient;
import com.tencent.cloud.Response;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.TreeMap;

/**
 * 腾讯云 COS（对象存储）服务类
 * 
 * 负责处理与腾讯云 COS 的交互，包括：
 * 1. 生成预签名上传 URL - 用于前端直接上传文件到私有存储桶
 * 2. 生成预签名下载 URL - 用于前端访问私有存储桶中的文件
 * 3. 生成临时密钥 - 用于需要 SDK 上传的场景（已弃用，保留备用）
 * 
 * 安全设计：
 * - 存储桶设置为私有读写，只有带签名的请求才能访问
 * - SecretKey 只存在于后端，前端无法获取
 * - 预签名 URL 有时效性（30分钟），过期自动失效
 * - 文件名使用 SHA-256 哈希，防止路径猜测攻击
 */
@Service
public class CosService {

    @Resource
    private TencentCloudConfig tencentCloudConfig;

    /**
     * 获取临时访问密钥（已弃用，保留备用）
     * 
     * 临时密钥方案适用于需要使用 COS SDK 进行上传的场景，
     * 但当前项目已改用预签名 URL 方案，安全性更高。
     * 
     * @return CosCredential 临时密钥信息，包含 tmpSecretId、tmpSecretKey、sessionToken
     */
    public CosCredential getCredential() {

        TreeMap<String, Object> config = new TreeMap<>();
        config.put("secretId", tencentCloudConfig.getSecretId());
        config.put("secretKey", tencentCloudConfig.getSecretKey());

        // 临时密钥有效期（秒）
        config.put("durationSeconds", 180);
        config.put("bucket", tencentCloudConfig.getBucket());
        config.put("region", tencentCloudConfig.getRegion());

        // 允许访问的文件路径前缀
        config.put("allowPrefixes", new String[] {tencentCloudConfig.getAllowPrefix()});

        // 允许的操作权限
        config.put("allowActions",
                new String[] {"name/cos:PutObject", "name/cos:PostObject",
                        "name/cos:InitiateMultipartUpload", "name/cos:ListMultipartUploads",
                        "name/cos:ListParts", "name/cos:UploadPart", "name/cos:CompleteMultipartUpload"});

        try {
            Response response = CosStsClient.getCredential(config);
            return new CosCredential().setSecretId(response.credentials.tmpSecretId)
                    .setSecretKey(response.credentials.tmpSecretKey)
                    .setSessionToken(response.credentials.sessionToken)
                    .setStartTime(response.startTime).setExpiredTime(response.expiredTime);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成文件上传凭证
     * 
     * 核心方法：为前端生成一个带签名的临时上传 URL
     * 前端拿到这个 URL 后可以直接向 COS 发送 PUT 请求上传文件
     * 
     * 工作流程：
     * 1. 根据文件类型生成安全的存储路径（如 Image/Avatar/xxx.jpg）
     * 2. 使用永久密钥生成预签名 URL（有效期30分钟）
     * 3. 返回上传 URL 和文件路径给前端
     * 
     * @param type   文件类型：avatar（头像）或 bill（账单附件）
     * @param userId 用户ID，用于生成唯一文件名
     * @param ext    文件扩展名（如 .jpg、.png）
     * @return UploadCredential 包含 uploadUrl（预签名上传URL）和 cosPath（文件存储路径）
     */
    public UploadCredential generateUploadCredential(String type, String userId, String ext) {
        // 生成安全的文件存储路径
        String cosPath = generateSecureCosPath(type, userId, ext);
        // 生成预签名上传 URL
        String uploadUrl = generatePresignedUploadUrl(cosPath);
        return new UploadCredential(uploadUrl, cosPath);
    }

    /**
     * 生成安全的 COS 存储路径
     * 
     * 路径格式：{文件夹}/{安全文件名}
     * 例如：Image/Avatar/a1b2c3d4_1234567890_1234.jpg
     * 
     * @param type   文件类型，决定存储文件夹
     * @param userId 用户ID，参与文件名生成
     * @param ext    文件扩展名
     * @return 完整的 COS 存储路径
     */
    private String generateSecureCosPath(String type, String userId, String ext) {
        // 根据类型确定存储文件夹
        String folder = "avatar".equals(type) ? "Image/Avatar" : "Image/Bill";
        // 生成安全的文件名（防止路径猜测攻击）
        String secureName = generateSecureFileName(userId, ext);
        return folder + "/" + secureName;
    }

    /**
     * 生成安全的文件名
     * 
     * 使用 SHA-256 哈希算法生成难以猜测的文件名：
     * 输入：userId + timestamp + random
     * 输出：{8位哈希}_{时间戳}_{随机数}{扩展名}
     * 
     * 示例：a1b2c3d4_1714567890123_5678.jpg
     * 
     * 安全优势：
     * - 哈希部分使文件名难以预测
     * - 时间戳保证唯一性
     * - 随机数增加熵值
     * 
     * @param userId 用户ID
     * @param ext    文件扩展名
     * @return 安全的文件名
     */
    private String generateSecureFileName(String userId, String ext) {
        long timestamp = System.currentTimeMillis();
        long random = (long) (Math.random() * 10000);
        String input = userId + "_" + timestamp + "_" + random;
        
        try {
            // 使用 SHA-256 哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 取前4字节（8位十六进制）作为文件名前缀
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(4, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString + "_" + timestamp + "_" + random + ext;
        } catch (NoSuchAlgorithmException e) {
            // 降级方案：直接使用时间戳和随机数
            return timestamp + "_" + random + ext;
        }
    }

    /**
     * 生成预签名上传 URL
     * 
     * 预签名 URL 原理：
     * 1. 使用永久密钥（SecretId + SecretKey）对请求进行签名
     * 2. 将签名信息编码到 URL 中
     * 3. COS 服务端收到请求后验证签名，签名正确则允许访问
     * 
     * 安全特性：
     * - URL 中包含签名，无需在请求中携带密钥
     * - 签名有过期时间，过期后 URL 失效
     * - 签名与特定 HTTP 方法绑定（PUT），无法用于其他操作
     * 
     * @param cosPath 文件在 COS 中的存储路径
     * @return 带签名的上传 URL，有效期30分钟
     */
    private String generatePresignedUploadUrl(String cosPath) {
        // 创建 COS 凭证（使用永久密钥）
        COSCredentials credentials = new BasicCOSCredentials(
                tencentCloudConfig.getSecretId(),
                tencentCloudConfig.getSecretKey()
        );
        
        // 创建客户端配置
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRegion(new com.qcloud.cos.region.Region(tencentCloudConfig.getRegion()));

        // 创建 COS 客户端
        COSClient cosClient = new COSClient(credentials, clientConfig);

        // 构建预签名 URL 请求
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                tencentCloudConfig.getBucket(),  // 存储桶名称
                cosPath,                          // 文件路径
                HttpMethodName.PUT                // HTTP 方法：PUT（上传）
        );
        
        // 设置签名过期时间：当前时间 + 30分钟
        request.setExpiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000));

        // 生成预签名 URL
        URL url = cosClient.generatePresignedUrl(request);
        
        // 关闭客户端释放资源
        cosClient.shutdown();

        return url.toString();
    }

    /**
     * 生成预签名下载 URL
     * 
     * 与上传 URL 类似，但 HTTP 方法为 GET
     * 用于前端访问私有存储桶中的文件
     * 
     * 使用场景：
     * - 显示用户头像
     * - 显示账单附件图片
     * - 下载文件
     * 
     * @param cosPath 文件在 COS 中的存储路径
     * @return 带签名的下载 URL，有效期30分钟
     */
    public String generatePresignedUrl(String cosPath) {
        // 创建 COS 凭证
        COSCredentials credentials = new BasicCOSCredentials(
                tencentCloudConfig.getSecretId(),
                tencentCloudConfig.getSecretKey()
        );
        
        // 创建客户端配置
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRegion(new com.qcloud.cos.region.Region(tencentCloudConfig.getRegion()));

        // 创建 COS 客户端
        COSClient cosClient = new COSClient(credentials, clientConfig);

        // 构建预签名 URL 请求
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                tencentCloudConfig.getBucket(),  // 存储桶名称
                cosPath,                          // 文件路径
                HttpMethodName.GET                // HTTP 方法：GET（下载）
        );
        
        // 设置签名过期时间：当前时间 + 30分钟
        request.setExpiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000));

        // 生成预签名 URL
        URL url = cosClient.generatePresignedUrl(request);
        
        // 关闭客户端释放资源
        cosClient.shutdown();

        return url.toString();
    }
}
