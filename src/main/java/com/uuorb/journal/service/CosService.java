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

@Service
public class CosService {
    @Resource
    private TencentCloudConfig tencentCloudConfig;

    public CosCredential getCredential() {

        TreeMap<String, Object> config = new TreeMap<>();
        config.put("secretId", tencentCloudConfig.getSecretId());
        config.put("secretKey", tencentCloudConfig.getSecretKey());

        config.put("durationSeconds", 180);
        config.put("bucket", tencentCloudConfig.getBucket());
        config.put("region", tencentCloudConfig.getRegion());

        config.put("allowPrefixes", new String[] {tencentCloudConfig.getAllowPrefix()});

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

    public UploadCredential generateUploadCredential(String type, String userId, String ext) {
        String cosPath = generateSecureCosPath(type, userId, ext);
        String uploadUrl = generatePresignedUploadUrl(cosPath);
        return new UploadCredential(uploadUrl, cosPath);
    }

    private String generateSecureCosPath(String type, String userId, String ext) {
        String folder = "avatar".equals(type) ? "Image/Avatar" : "Image/Bill";
        String secureName = generateSecureFileName(userId, ext);
        return folder + "/" + secureName;
    }

    private String generateSecureFileName(String userId, String ext) {
        long timestamp = System.currentTimeMillis();
        long random = (long) (Math.random() * 10000);
        String input = userId + "_" + timestamp + "_" + random;
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(4, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString + "_" + timestamp + "_" + random + ext;
        } catch (NoSuchAlgorithmException e) {
            return timestamp + "_" + random + ext;
        }
    }

    private String generatePresignedUploadUrl(String cosPath) {
        COSCredentials credentials = new BasicCOSCredentials(
                tencentCloudConfig.getSecretId(),
                tencentCloudConfig.getSecretKey()
        );
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRegion(new com.qcloud.cos.region.Region(tencentCloudConfig.getRegion()));

        COSClient cosClient = new COSClient(credentials, clientConfig);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                tencentCloudConfig.getBucket(),
                cosPath,
                HttpMethodName.PUT
        );
        request.setExpiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000));

        URL url = cosClient.generatePresignedUrl(request);
        cosClient.shutdown();

        return url.toString();
    }

    public String generatePresignedUrl(String cosPath) {
        COSCredentials credentials = new BasicCOSCredentials(
                tencentCloudConfig.getSecretId(),
                tencentCloudConfig.getSecretKey()
        );
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRegion(new com.qcloud.cos.region.Region(tencentCloudConfig.getRegion()));

        COSClient cosClient = new COSClient(credentials, clientConfig);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                tencentCloudConfig.getBucket(),
                cosPath,
                HttpMethodName.GET
        );
        request.setExpiration(new Date(System.currentTimeMillis() + 30 * 60 * 1000));

        URL url = cosClient.generatePresignedUrl(request);
        cosClient.shutdown();

        return url.toString();
    }
}
