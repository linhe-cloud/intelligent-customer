package com.IntelligentCustomer.framework.config.milvus;

import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MilvusCollectionInitializer 类用于初始化Milvus向量数据库集合
 * 该类负责在应用启动时自动创建或验证Milvus集合的结构
 */
@Slf4j
@Component
public class MilvusCollectionInitializer {

    // 注入Milvus客户端实例
    private final MilvusClientV2 milvusClient;
    // 注入Milvus配置属性
    private final MilvusProperties properties;

    /**
     * 构造函数，注入Milvus客户端和配置属性
     * @param milvusClient Milvus客户端实例
     * @param properties Milvus配置属性
     */
    public MilvusCollectionInitializer(
            MilvusClientV2 milvusClient,
            MilvusProperties properties) {
        this.milvusClient = milvusClient;
        this.properties = properties;
    }

    /**
     * 初始化方法，使用@PostConstruct注解确保在Bean构造完成后自动执行
     * 检查并创建Milvus集合（如果配置为自动创建）
     */
    @PostConstruct
    public void initialize() {
        // 检查是否启用自动创建集合功能
        if (!properties.isAutoCreate()) {
            log.info("Milvus 自动创建 Collection 已关闭");
            return;
        }

        // 获取集合名称
        String collectionName = properties.getCollection();

        // 检查集合是否已存在
        boolean exists = milvusClient.hasCollection(
                HasCollectionReq.builder()
                        .databaseName(properties.getDatabase())
                        .collectionName(collectionName)
                        .build()
        );

        // 如果集合已存在，则直接返回
        if (exists) {
            log.info("Milvus Collection 已存在: {}", collectionName);
            return;
        }

        // 定义集合字段列表
        List<CreateCollectionReq.FieldSchema> fields = List.of(
                // ID字段：主键，VARCHAR类型，最大长度36
                CreateCollectionReq.FieldSchema.builder()
                        .name("id")
                        .dataType(DataType.VarChar)
                        .maxLength(36)
                        .isPrimaryKey(true)
                        .autoID(false)
                        .build(),

                // 文件ID字段：VARCHAR类型，最大长度64
                CreateCollectionReq.FieldSchema.builder()
                        .name("file_id")
                        .dataType(DataType.VarChar)
                        .maxLength(64)
                        .build(),

                // 内容字段：VARCHAR类型，最大长度65535
                CreateCollectionReq.FieldSchema.builder()
                        .name("content")
                        .dataType(DataType.VarChar)
                        .maxLength(65535)
                        .build(),

                // 向量字段：FLOAT VECTOR类型，维度由配置决定
                CreateCollectionReq.FieldSchema.builder()
                        .name("embedding")
                        .dataType(DataType.FloatVector)
                        .dimension(properties.getDimension())
                        .build(),

                // 源文件字段：VARCHAR类型，最大长度512
                CreateCollectionReq.FieldSchema.builder()
                        .name("source_file")
                        .dataType(DataType.VarChar)
                        .maxLength(512)
                        .build(),

                // 块索引字段：INT32类型
                CreateCollectionReq.FieldSchema.builder()
                        .name("chunk_index")
                        .dataType(DataType.Int32)
                        .build(),

                // 创建时间字段：VARCHAR类型，最大长度64
                CreateCollectionReq.FieldSchema.builder()
                        .name("created_at")
                        .dataType(DataType.VarChar)
                        .maxLength(64)
                        .build()
        );

        // 构建集合模式
        CreateCollectionReq.CollectionSchema schema =
                CreateCollectionReq.CollectionSchema.builder()
                        .fieldSchemaList(fields)
                        .build();

        // 配置向量索引参数
        IndexParam vectorIndex = IndexParam.builder()
                .fieldName("embedding")
                .indexType(IndexParam.IndexType.AUTOINDEX)
                .metricType(IndexParam.MetricType.COSINE)
                .build();

        // 构建创建集合请求
        CreateCollectionReq request = CreateCollectionReq.builder()
                .databaseName(properties.getDatabase())
                .collectionName(collectionName)
                .description("智能客服知识库向量集合")
                .collectionSchema(schema)
                .indexParams(List.of(vectorIndex))
                .build();

        // 执行创建集合操作
        milvusClient.createCollection(request);

        // 记录创建成功的日志
        log.info(
                "Milvus Collection 创建成功: {}, dimension={}",
                collectionName,
                properties.getDimension()
        );
    }
}