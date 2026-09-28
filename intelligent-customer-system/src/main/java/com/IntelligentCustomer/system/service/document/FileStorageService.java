package com.IntelligentCustomer.system.service.document;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.framework.config.document.DocumentProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * 文件存储服务类，负责处理文件的存储、删除和哈希计算等功能
 * 使用Spring框架的@Service注解标记为服务组件
 */
@Service
public class FileStorageService {

    private final DocumentProperties properties; // 文档属性配置对象

    /**
     * 构造函数，通过依赖注入方式获取DocumentProperties实例
     * @param properties 文档属性配置对象
     */
    public FileStorageService(DocumentProperties properties) {
        this.properties = properties;
    }

    /**
     * 存储文件方法
     * @param file 要存储的文件
     * @param fileId 文件ID
     * @param extension 文件扩展名
     * @return StoredFile 包含文件路径、存储文件名和SHA256哈希值的对象
     */
    public StoredFile store(
            MultipartFile file,
            String fileId,
            String extension
    ) {
        LocalDate today = LocalDate.now(); // 获取当前日期

        // 构建存储目录路径，按年月分类
        Path directory = Path.of(
                properties.getUploadDirectory(), // 获取上传目录
                String.valueOf(today.getYear()), // 年份目录
                String.format("%02d", today.getMonthValue()) // 月份目录
        ).toAbsolutePath().normalize();

        String storedFilename = fileId + "." + extension; // 构建存储文件名
        Path target = directory.resolve(storedFilename).normalize(); // 构建目标文件路径

        // 安全检查，确保目标路径在上传目录下
        if (!target.startsWith(directory)) {
            throw new BusinessException("文件存储路径不合法");
        }

        try {
            Files.createDirectories(directory); // 创建目录
            String hash = calculateSha256(file); // 计算文件SHA256哈希值
            file.transferTo(target); // 转移文件到目标位置

            return new StoredFile(
                    target.toString(), // 文件完整路径
                    storedFilename,   // 存储的文件名
                    hash             // 文件哈希值
            );
        } catch (IOException e) {
            throw new BusinessException("保存上传文件失败", e);
        }
    }

    /**
     * 删除文件方法
     * @param filePath 要删除的文件路径
     */
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) { // 检查路径是否为空
            return;
        }

        try {
            Files.deleteIfExists(Path.of(filePath)); // 删除文件
        } catch (IOException e) {
            throw new BusinessException("删除文件失败", e);
        }
    }

    /**
     * 计算文件的SHA256哈希值
     * @param file 要计算哈希值的文件
     * @return 文件的SHA256哈希值字符串
     */
    private String calculateSha256(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // 创建SHA256摘要实例
            byte[] buffer = new byte[8192]; // 创建缓冲区
            int length;

            // 读取文件并更新摘要
            while ((length = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }

            return HexFormat.of().formatHex(digest.digest()); // 返回十六进制格式的哈希值
        } catch (Exception e) {
            throw new BusinessException("计算文件摘要失败", e);
        }
    }

    /**
     * 存储文件信息的记录类（Java 14+的record特性）
     * @param filePath 文件完整路径
     * @param storedFilename 存储的文件名
     * @param sha256 文件的SHA256哈希值
     */
    public record StoredFile(
            String filePath,
            String storedFilename,
            String sha256
    ) {
    }
}