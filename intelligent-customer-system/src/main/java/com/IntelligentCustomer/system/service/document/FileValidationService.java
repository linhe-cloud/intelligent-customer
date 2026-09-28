package com.IntelligentCustomer.system.service.document;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.framework.config.document.DocumentProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 文件验证服务类，用于处理文件上传前的各种验证操作
 * 包括文件非空检查、文件大小验证、文件类型验证、文件名安全检查以及文件内容验证
 */
@Service
public class FileValidationService {

    private final DocumentProperties properties; // 文档属性配置对象，包含最大文件大小、允许的文件扩展名等信息

    /**
     * 通过构造函数注入DocumentProperties依赖
     * @param properties 文档属性配置对象
     */
    public FileValidationService(DocumentProperties properties) {
        this.properties = properties;
    }

    /**
     * 验证上传的文件是否符合要求
     * @param file 上传的文件
     * @return ValidateFile 包含验证后文件信息的记录
     * @throws BusinessException 当文件不符合要求时抛出
     */
    public ValidateFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BusinessException("上传文件大小不能超过 " + properties.getMaxFileSize() + " 字节");
        }

        String originalFilename = file.getOriginalFilename();
        String safeFilename = sanitizeFilename(originalFilename); // 清理文件名，移除路径等不安全字符
        String extension = getExtension(safeFilename); // 获取文件扩展名

        if (!properties.getAllowedExtensions().contains(extension)) {
            throw new BusinessException("不允许上传的文件类型: " + extension);
        }

        validateFileSignature(file, extension); // 验证文件内容签名，确保文件真实类型与扩展名一致

        return new ValidateFile(
                safeFilename,
                extension,
                file.getContentType(),
                file.getSize()
        );
    }


    /**
     * 清理文件名，确保文件名安全
     * @param filename 原始文件名
     * @return 清理后的安全文件名
     * @throws BusinessException 当文件名无效时抛出
     */
    private String sanitizeFilename(String filename){
        if (filename == null || filename.isBlank()) {
            throw new BusinessException("文件名不能为空");
        }

        String normalized = filename
                .replace("\\", "/") // 统一使用正斜杠作为路径分隔符
                .substring(filename.replace("\\", "/").lastIndexOf("/") + 1) // 移除路径部分，只保留文件名
                .replaceAll("[\\r\\n]", "") // 移除回车换行符
                .trim(); // 去除首尾空格

        if (normalized.isBlank() || normalized.length() > 255) {
            throw new BusinessException("文件名无效");
        }

        return normalized;
    }

    public String getExtension(String filename) {
    /**
     * 从文件名中提取扩展名
     * @param filename 文件名
     * @return 文件扩展名（小写）
     * @throws BusinessException 当文件名不包含扩展名时抛出
     */
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new BusinessException("文件名必须包含扩展名");
        }

        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private void validateFileSignature(MultipartFile file, String extension) {
    /**
     * 验证文件内容签名，确保文件真实类型与扩展名一致
     * @param file 上传的文件
     * @param extension 文件扩展名
     * @throws BusinessException 当文件内容与扩展名不匹配或读取文件失败时抛出
     */
        try(InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readAllBytes();
 // 读取文件头部字节
            boolean valid = switch (extension) {
            // 根据文件扩展名验证对应的文件签名
                case "pdf" -> startsWith(header, "%PDF".getBytes());
                case "docx", "xlsx" -> isZipFile(header);
                case "xls" -> isOldOfficeFile(header);
                case "txt" -> true;
                default -> false; // 文本文件不需要特殊验证
            };

            if (!valid) {
                throw new BusinessException("文件格式不正确");
            }
        } catch (IOException e) {
            throw new BusinessException("读取文件内容失败", e);
        }
    }

    private boolean isZipFile(byte[] header) {
    /**
     * 检查文件是否为ZIP文件（包括DOCX、XLSX等）
     * @param header 文件头部字节
     * @return 如果是ZIP文件返回true，否则返回false
     */
        return header.length >= 4
                && header[0] == 0x50
                && header[1] == 0x4B
                && header[2] == 0x03
                && header[3] == 0x04;
    }

    private boolean isOldOfficeFile(byte[] header) {
    /**
     * 检查文件是否为旧版Office文件（如.xls）
     * @param header 文件头部字节
     * @return 如果是旧版Office文件返回true，否则返回false
     */
        byte[] signature = {
                (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
        };
        return startsWith(header, signature);
    }

    private boolean startsWith(byte[] source, byte[] prefix) {
    /**
     * 检查字节数组是否以指定的前缀开头
     * @param source 源字节数组
     * @param prefix 要匹配的前缀字节数组
     * @return 如果源数组以前缀开头返回true，否则返回false
     */
        if (source.length < prefix.length) {
            return false;
        }

        for (int i = 0; i < prefix.length; i++) {
            if (source[i] != prefix[i]) {
                return false;
            }
        }

        return true;
    }


    /**
 * ValidateFile 是一个不可变的数据类，用于验证文件的相关信息。
 * 使用 Java 14+ 引入的 record 类型实现，自动提供 equals()、hashCode()、toString() 等方法。
 */
    public record ValidateFile(
        /**
         * 原始文件名，包含文件扩展名
         * 例如："document.pdf"
         */
            String originalFilename,
        /**
         * 文件扩展名，不包含点号
         * 例如："pdf"
         */
            String extension,
        /**
         * 文件的 MIME 类型
         * 例如："application/pdf"
         */
            String mimeType,
        /**
         * 文件大小，以字节为单位
         * 例如：1024L 表示 1KB
         */
            long size
    ) {}
}
