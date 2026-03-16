package cn.iocoder.yudao.module.bpm.util;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import io.minio.MinioClient;

import java.io.InputStream;
import java.util.Objects;

public class MinioUtil {
    /**
     * 获取一个连接minio服务端的客户端
     *
     * @return MinioClient
     */
    public MinioClient getMinioClient(String endpoint, String accessKey, String secretKey) throws Exception {
        try {
            MinioClient minioClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
            return minioClient;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 创建桶
     *
     * @param bucketName 桶名称
     */
    public Boolean createBucket(MinioClient minioClient, String bucketName) throws Exception{
        if (!StringUtils.hasLength(bucketName)) {
            throw new Exception("bucketName cannot be empty");
        }
        try {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            return true;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 检查桶是否存在
     *
     * @param bucketName 桶名称
     * @return boolean true-存在 false-不存在
     */
    public boolean checkBucketExist(MinioClient minioClient, String bucketName) throws Exception {
        try {
            if (!StringUtils.hasLength(bucketName)) {
                throw new Exception("bucketName cannot be empty");
            }
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 检测某个桶内是否存在某个文件
     *
     * @param bucketName 桶名称
     * @param objectName 文件名称
     */
    public boolean getBucketFileExist(MinioClient minioClient, String bucketName, String objectName) throws Exception{
        if (!StringUtils.hasLength(objectName) || !StringUtils.hasLength(bucketName)) {
            throw new Exception("objectName or bucketName cannot be empty");
        }
        try {
            // 判断文件是否存在
            return (minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()) &&
                    minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(objectName).build()) != null);

        } catch (ErrorResponseException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 删除文件夹
     *
     * @param bucketName 桶名
     * @param objectName 文件夹名
     * @param isDeep     是否递归删除
     * @return
     */
    public Boolean deleteBucketFolder(MinioClient minioClient, String bucketName, String objectName, Boolean isDeep) throws Exception {
        if (!StringUtils.hasLength(bucketName) || !StringUtils.hasLength(objectName)) {
            throw new Exception("objectName or bucketName cannot be empty");
        }
        try {
            ListObjectsArgs args = ListObjectsArgs.builder().bucket(bucketName).prefix(objectName + "/").recursive(isDeep).build();
            Iterable<Result<Item>> listObjects = minioClient.listObjects(args);
            listObjects.forEach(objectResult -> {
                try {
                    Item item = objectResult.get();
                    System.out.println(item.objectName());
                    minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(item.objectName()).build());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            return true;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 文件上传文件
     *
     * @param bucketName 桶名
     * @param objectName 文件名,如果有文件夹则格式为 "文件夹名/文件名"
     * @param file       文件
     * @return
     */
    public Boolean uploadFile(MinioClient minioClient, String bucketName, String objectName, MultipartFile file) throws Exception {

        if (!StringUtils.hasLength(bucketName) || !StringUtils.hasLength(objectName) || Objects.isNull(file)) {
            throw new Exception("bucketName or objectName or file cannot be empty");
        }
        try {
            //资源的媒体类型
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//默认未知二进制流
            InputStream inputStream = file.getInputStream();
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucketName).object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            ObjectWriteResponse response = minioClient.putObject(args);
            inputStream.close();
            return response.etag() != null;
        } catch (Exception e) {
            throw e;
        }
    }


}
