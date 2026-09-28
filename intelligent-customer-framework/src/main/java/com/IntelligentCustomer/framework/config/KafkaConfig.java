package com.IntelligentCustomer.framework.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {
    
    // 文档上传异步处理队列
    @Bean
    public NewTopic documentUploadTopic() {
        return new NewTopic("document-upload", 3, (short) 1);
        // 3个分区，1个副本（本地开发为1，生产环境改为3）
    }

    // 对话事件流
    @Bean
    public NewTopic chatMessageTopic() {
        return new NewTopic("chat-message", 6, (short) 1);
    }

    // 文档处理死信队列（失败消息兜底）
    @Bean
    public NewTopic documentUploadDLQ() {
        return new NewTopic("document-upload-dlq", 1, (short) 1);
    }

    // 配置重试策略和死信队列恢复器
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        FixedBackOff backOff = new FixedBackOff(3000L, 3); // 3秒间隔，最多重试3次
        return new DefaultErrorHandler(recoverer, backOff);
    }

    // 将 ErrorHandler 注册到监听器容器
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory, DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

}
