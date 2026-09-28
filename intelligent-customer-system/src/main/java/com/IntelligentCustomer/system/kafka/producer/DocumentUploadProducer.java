package com.IntelligentCustomer.system.kafka.producer;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.IntelligentCustomer.system.domain.dto.kafka.DocumentUploadMessage;
import com.IntelligentCustomer.common.exception.BusinessException;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * 文档上传消息生产者服务类
 * 负责将文档上传消息发送到Kafka消息队列
 */
@Service
public class DocumentUploadProducer {
    
    // 使用SLF4J日志记录器记录日志
    private static final Logger logger = LoggerFactory.getLogger(DocumentUploadProducer.class);
    // 定义Kafka主题名称常量
    private static final String TOPIC = "document-upload";

    // Kafka模板，用于发送消息到Kafka
    private final KafkaTemplate<String, String> kafkaTemplate;
    // 对象映射器，用于将对象转换为JSON字符串
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，通过依赖注入初始化Kafka模板和对象映射器
     * @param kafkaTemplate Kafka消息模板
     * @param objectMapper 对象映射器
     */
    public DocumentUploadProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送文档上传消息到Kafka
     * @param message 文档上传消息对象
     * @throws BusinessException 当消息发送失败时抛出业务异常
     */
    public void send(DocumentUploadMessage message) {
        try {
            // 将消息对象转换为JSON字符串
            String json = objectMapper.writeValueAsString(message);

            // 发送消息到Kafka，并处理发送结果
            kafkaTemplate.send(TOPIC, message.getFileId(), json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        // 消息发送失败，记录错误日志
                        logger.error("文档上传消息发送失败： fileId={}, error={}", message.getFileId(), ex.getMessage());
                    } else {
                        // 消息发送成功，记录成功日志
                        logger.info("文档上传消息发送成功： fileId={}, partition={}, offset={}", message.getFileId(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
        } catch (Exception e) {
            // 捕获并记录异常
            logger.error("文档上传消息发送失败： fileId={}", message.getFileId());
            // 抛出业务异常
            throw new BusinessException("文档上传消息发送失败", e);
        }
    }
}
