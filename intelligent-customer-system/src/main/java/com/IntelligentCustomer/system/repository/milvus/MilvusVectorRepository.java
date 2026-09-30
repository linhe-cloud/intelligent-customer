package com.IntelligentCustomer.system.repository.milvus;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.framework.config.milvus.MilvusProperties;
import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Milvus向量仓库类，用于处理知识向量数据的存储和检索操作
 * 使用@Repository注解标记为Spring的Repository组件
 */
@Repository
public class MilvusVectorRepository {

    /**
     * Milvus客户端，用于与Milvus数据库进行交互
     */
    private final MilvusClientV2 milvusClient;
    /**
     * Milvus配置属性，包含数据库连接和集合配置信息
     */
    private final MilvusProperties properties;

    /**
     * 构造函数，通过依赖注入初始化Milvus客户端和配置属性
     * @param milvusClient Milvus客户端实例
     * @param properties Milvus配置属性
     */
    public MilvusVectorRepository(MilvusClientV2 milvusClient, MilvusProperties properties) {
        this.milvusClient = milvusClient;
        this.properties = properties;
    }

/**
 * 保存知识向量列表到Milvus数据库
 * @param vectors 知识向量列表，不能为空
 * @throws BusinessException 当向量列表为空时抛出异常
 */
    public void save(List<KnowledgeVector> vectors) {
    // 检查向量列表是否为空或null
        if (vectors == null || vectors.isEmpty()) {
            throw new BusinessException("向量列表不能为空");
        }

    // 创建与输入向量列表大小相同的JsonObject列表
        List<JsonObject> rows = new ArrayList<>(vectors.size());
    // 遍历每个知识向量
        for (KnowledgeVector vector : vectors) {
        // 验证向量数据的有效性
            validateVector(vector);
        // 创建JsonObject对象用于存储向量数据
            JsonObject row = new JsonObject();
        // 添加向量ID
            row.addProperty("id", vector.getId().toString());
        // 添加源文件ID
            row.addProperty("file_id", vector.getFileId());
        // 添加向量内容
            row.addProperty("content", vector.getContent());
        // 添加向量嵌入数据，将数组转换为JsonArray
            row.add("embedding", toJsonArray(vector.getEmbedding()));
        // 添加源文件名称（重复添加，可能是为了兼容不同的查询场景）
            row.addProperty("source_file", vector.getSourceFile());
        // 添加块索引
            row.addProperty("chunk_index", vector.getChunkIndex());
        // 添加创建时间
            row.addProperty("created_at", vector.getCreatedAt().toString());
        // 将处理好的行数据添加到列表中
            rows.add(row);
        }

    // 使用Milvus客户端将数据插入到指定的集合中
        milvusClient.insert(InsertReq.builder()
                .databaseName(properties.getDatabase())  // 设置数据库名称
                .collectionName(properties.getCollection())  // 设置集合名称
                .data(rows)  // 设置要插入的数据
                .build());  // 构建插入请求并执行
    }

/**
 * 根据查询向量搜索最相似的topK个知识向量
 * @param queryVector 查询向量，用于相似度计算
 * @param topK 返回的最相似结果数量
 * @return 匹配的知识向量列表，按相似度从高到低排序
 * @throws BusinessException 当查询向量为空、维度不正确或topK值不合法时抛出
 */
    public List<KnowledgeVector> search(float[] queryVector, int topK) {
    // 检查查询向量是否为空
        if (queryVector == null || queryVector.length == 0) {
            throw new BusinessException("查询向量不能为空");
        }
    // 检查查询向量维度是否与配置一致
        if (queryVector.length != properties.getDimension()) {
            throw new BusinessException("查询向量维度不正确");
        }
    // 检查topK值是否合法
        if (topK <= 0) {
            throw new BusinessException("topK 必须大于 0");
        }

    // 构建并执行搜索请求
        SearchResp response = milvusClient.search(SearchReq.builder()
                .databaseName(properties.getDatabase())      // 设置数据库名
                .collectionName(properties.getCollection())   // 设置集合名
                .annsField("embedding")                      // 设置向量字段名
                .metricType(IndexParam.MetricType.COSINE)   // 使用余弦相似度
                .limit(topK)                                 // 设置返回结果数量
                .outputFields(List.of("id", "file_id", "content", "source_file",  // 设置返回字段
                        "chunk_index", "created_at"))
                .data(List.of(new FloatVec(queryVector)))    // 设置查询向量
                .build());

    // 处理搜索结果
        if (response == null || response.getSearchResults() == null
                || response.getSearchResults().isEmpty()) {
            return List.of();  // 无结果时返回空列表
        }

    // 将搜索结果转换为知识向量对象列表
        List<KnowledgeVector> result = new ArrayList<>();
        for (SearchResp.SearchResult item : response.getSearchResults().get(0)) {
            result.add(toKnowledgeVector(item));
        }
        return result;
    }
/**
 * 根据文件ID删除数据
 * @param fileId 文件ID，用于标识要删除的数据
 * @return int 被删除的记录数量
 * @throws BusinessException 当文件ID为空或包含非法字符时抛出
 */
    public int deleteByFileId(String fileId) {
    // 检查文件ID是否为空或空白字符串
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException("文件ID不能为空");
        }

    // 检查文件ID是否包含非法字符（双引号）
        if (fileId.contains("\"")) {
            throw new BusinessException("文件ID不合法");
        }

    // 使用Milvus客户端执行删除操作
    // 构建删除请求，指定数据库名、集合名和过滤条件
    // 过滤条件使用file_id字段匹配指定的文件ID
        long deleted = milvusClient.delete(
                DeleteReq.builder()
                        .databaseName(properties.getDatabase())
                        .collectionName(properties.getCollection())
                        .filter("file_id == \"" + fileId + "\"")
                        .build()
        ).getDeleteCnt();  // 获取删除的记录数量

    // 将long类型转换为int类型并返回
        return Math.toIntExact(deleted);
    }

/**
 * 将搜索结果对象转换为知识向量对象
 * @param item 搜索结果对象，包含实体信息和相关属性
 * @return 返回转换后的KnowledgeVector对象，包含知识向量的各项属性
 */
    private KnowledgeVector toKnowledgeVector(SearchResp.SearchResult item) {
    // 获取搜索结果中的实体信息
        Map<String, Object> entity = item.getEntity();
    // 创建一个新的知识向量对象
        KnowledgeVector vector = new KnowledgeVector();
    // 设置知识向量的ID，从搜索结果ID转换而来
        vector.setId(UUID.fromString(String.valueOf(item.getId())));
    // 设置知识向量的内容，从实体中获取
        vector.setFileId(String.valueOf(entity.get("file_id")));
        vector.setContent(String.valueOf(entity.get("content")));
    // 设置知识向量的源文件，从实体中获取
        vector.setSourceFile(String.valueOf(entity.get("source_file")));
    // 设置知识向量的块索引，从实体中获取并转换为整数类型
        vector.setChunkIndex(((Number) entity.get("chunk_index")).intValue());
    // 设置知识向量的分数，如果搜索结果中有分数则设置，否则为null
        vector.setScore(item.getScore() == null ? null : item.getScore().doubleValue());

    // 获取实体中的创建时间
        Object createdAt = entity.get("created_at");
    // 如果创建时间不为null，则将其解析为LocalDateTime对象并设置
        if (createdAt != null) {
            vector.setCreatedAt(LocalDateTime.parse(String.valueOf(createdAt)));
        }
    // 返回转换后的知识向量对象
        return vector;
    }

/**
 * 验证知识向量的完整性和正确性
 * @param vector 待验证的知识向量对象
 * @throws BusinessException 当向量数据不完整或维度不正确时抛出
 */
    private void validateVector(KnowledgeVector vector) {
    // 检查向量是否为null或必要字段是否为空/空白
        if (vector == null || vector.getId() == null
                || vector.getFileId() == null || vector.getFileId().isBlank()
                || vector.getContent() == null || vector.getContent().isBlank() // 检查内容是否为null或空白字符串
                || vector.getEmbedding() == null // 检查嵌入向量是否为null
                || vector.getEmbedding().length != properties.getDimension() // 检查向量维度是否配置正确
                || vector.getSourceFile() == null || vector.getCreatedAt() == null) {
            throw new BusinessException("向量数据不完整或维度不正确");
        }
    }

/**
 * 将浮点数数组转换为JsonArray对象
 * @param vector 浮点数数组
 * @return 包含所有浮点数的JsonArray对象
 */
    private JsonArray toJsonArray(float[] vector) {
    // 创建一个新的JsonArray对象
        JsonArray array = new JsonArray();
    // 遍历浮点数数组中的每个元素
        for (float value : vector) {
        // 将当前浮点数值添加到JsonArray中
            array.add(value);
        }
    // 返回包含所有浮点数的JsonArray对象
        return array;
    }
}
