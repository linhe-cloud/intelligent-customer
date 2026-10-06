package com.IntelligentCustomer.system.service.search;

import com.IntelligentCustomer.common.exception.BusinessException;
import com.IntelligentCustomer.system.domain.dto.search.HybridSearchResult;
import com.IntelligentCustomer.system.domain.dto.search.KeywordSearchResult;
import com.IntelligentCustomer.system.domain.entity.KnowledgeChunk;
import com.IntelligentCustomer.system.domain.entity.KnowledgeVector;
import com.IntelligentCustomer.system.repository.mapper.KnowledgeChunkMapper;
import com.IntelligentCustomer.system.repository.milvus.MilvusVectorRepository;
import com.IntelligentCustomer.system.service.embedding.EmbeddingService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 搜索服务类，提供语义搜索和混合搜索功能
 */
@Service
public class SearchService {

    // RRF (Reciprocal Rank Fusion) 算法的常量参数K
    private static final int RRF_K = 60;
    // 最大返回结果数量
    private static final int MAX_TOP_K = 20;

    // 语义搜索权重
    private static final double SEMANTIC_WEIGHT = 0.6;
    // 关键词搜索权重
    private static final double KEYWORD_WEIGHT = 0.4;

    // 向量嵌入服务
    private final EmbeddingService embeddingService;
    // Milvus向量仓库
    private final MilvusVectorRepository milvusVectorRepository;
    // 知识块数据访问层
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    /**
     * 构造函数，注入所需服务
     * @param embeddingService 向量嵌入服务
     * @param milvusVectorRepository Milvus向量仓库
     * @param knowledgeChunkMapper 知识块数据访问层
     */
    public SearchService(

            EmbeddingService embeddingService,
            MilvusVectorRepository milvusVectorRepository,
            KnowledgeChunkMapper knowledgeChunkMapper
    ) {
        this.embeddingService = embeddingService;
        this.milvusVectorRepository = milvusVectorRepository;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
    }

    /**
     * 执行语义搜索
     * @param query 搜索查询字符串
     * @param topK 返回结果数量
     * @return 知识向量列表
     */
    public List<KnowledgeVector> semanticSearch(String query, int topK) {

        validateQuery(query);
        validateTopK(topK);

        float[] queryVector = embeddingService.embed(query.strip());
        return milvusVectorRepository.search(queryVector, topK);
    }

    /**
     * 执行混合搜索（结合语义搜索和关键词搜索）
     * @param query 搜索查询字符串
     * @param topK 返回结果数量
     * @return 混合搜索结果列表
     */
    public List<HybridSearchResult> hybridSearch(

            String query,
            int topK
    ) {
        validateQuery(query);
        validateTopK(topK);

        // 去除多余字符
        String normalizedQuery = query.strip();

        // 多取一些候选结果，再进行融合
        int candidateK = Math.min(topK * 3, 50);

        // 执行语义搜索
        List<KnowledgeVector> semanticResults = semanticSearch(normalizedQuery, candidateK);

        // 执行关键词搜索
        List<KeywordSearchResult> keywordResults = knowledgeChunkMapper.searchByKeyword(
                        normalizedQuery,
                        candidateK
                );

        // 合并结果
        Set<String> existingChunkKeys = loadExistingChunkKeys(semanticResults);

        /*
         * 使用 fileId + chunkIndex 作为稳定的切片标识。
         * 旧数据中 MySQL 和 Milvus 的 UUID 可能不同，但同一个文件的
         * 同一个切片仍然拥有一致的 fileId 和 chunkIndex。
         */
        Map<String, HybridSearchResult> mergedResults = new LinkedHashMap<>();

        // 合并语义检索结果，只保留 MySQL 中仍存在的知识切片
        int semanticRank = 0;
        for (int i = 0; i < semanticResults.size(); i++) {
            KnowledgeVector vector = semanticResults.get(i);

            String mergeKey = buildMergeKey(
                    vector.getFileId(),
                    vector.getChunkIndex(),
                    vector.getId() == null ? null : vector.getId().toString()
            );
            if (mergeKey == null || !existingChunkKeys.contains(mergeKey)) {
                continue;
            }

            semanticRank++;

            HybridSearchResult result = mergedResults.computeIfAbsent(
                    mergeKey,
                    key -> fromSemanticResult(vector)
            );

            // 语义结果使用 Milvus 中的 ID 作为最终返回 ID
            result.setId(vector.getId());
            result.setSemanticScore(vector.getScore());
            result.setSemanticRank(semanticRank);

            addRrfScore(
                    result,
                    SEMANTIC_WEIGHT,
                    semanticRank
            );
        }

        // 合并关键词检索结果
        for (int i = 0; i < keywordResults.size(); i++) {
            KeywordSearchResult keyword = keywordResults.get(i);

            String mergeKey = buildMergeKey(
                    keyword.getFileId(),
                    keyword.getChunkIndex(),
                    keyword.getId()
            );
            if (mergeKey == null) {
                continue;
            }

            int rank = i + 1;

            HybridSearchResult result = mergedResults.computeIfAbsent(
                    mergeKey,
                    key -> fromKeywordResult(keyword)
            );

            result.setKeywordScore(keyword.getKeywordScore());
            result.setKeywordRank(rank);

            addRrfScore(
                    result,
                    KEYWORD_WEIGHT,
                    rank
            );
        }

        return mergedResults.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                HybridSearchResult::getFusedScore,
                                Comparator.reverseOrder()
                        )
                )
                .limit(topK)
                .toList();
    }

    /**
     * 查询语义检索结果对应的当前知识切片。
     *
     * MySQL 是知识库文本的权威来源，Milvus 可能因为历史删除或重新上传
     * 暂时残留旧向量。混合检索不能把这些已经不存在的旧向量返回给调用方。
     */
    private Set<String> loadExistingChunkKeys(
            List<KnowledgeVector> semanticResults
    ) {
        Set<String> fileIds = semanticResults.stream()
                .map(KnowledgeVector::getFileId)
                .filter(fileId -> fileId != null && !fileId.isBlank())
                .collect(Collectors.toSet());

        Set<String> existingChunkKeys = new HashSet<>();
        for (String fileId : fileIds) {
            for (KnowledgeChunk chunk : knowledgeChunkMapper.findByFileId(fileId)) {
                String mergeKey = buildMergeKey(
                        chunk.getFileId(),
                        chunk.getChunkIndex(),
                        chunk.getId() == null ? null : chunk.getId().toString()
                );
                if (mergeKey != null) {
                    existingChunkKeys.add(mergeKey);
                }
            }
        }
        return existingChunkKeys;
    }

    /**
     * 从语义搜索结果创建混合搜索结果
     * @param vector 知识向量
     * @return 混合搜索结果
     */
    private HybridSearchResult fromSemanticResult(
            KnowledgeVector vector
    ) {
        HybridSearchResult result = new HybridSearchResult();

        result.setId(vector.getId());
        result.setFileId(vector.getFileId());
        result.setContent(vector.getContent());
        result.setSourceFile(vector.getSourceFile());
        result.setChunkIndex(vector.getChunkIndex());
        result.setCreateTime(vector.getCreatedAt());
        result.setFusedScore(0.0);

        return result;
    }

    /**
     * 从关键词搜索结果创建混合搜索结果
     * @param keyword 关键词搜索结果
     * @return 混合搜索结果
     */
    private HybridSearchResult fromKeywordResult(
            KeywordSearchResult keyword
    ) {
        HybridSearchResult result = new HybridSearchResult();

        result.setId(parseUuid(keyword.getId()));
        result.setFileId(keyword.getFileId());
        result.setContent(keyword.getContent());
        result.setSourceFile(keyword.getSourceFile());
        result.setChunkIndex(keyword.getChunkIndex());
        result.setCreateTime(keyword.getCreatedAt());
        result.setFusedScore(0.0);

        return result;
    }

/**
 * 解析字符串为UUID对象
 * @param value 要解析的字符串值
 * @return 解析成功返回对应的UUID对象，解析失败或输入为空返回null
 */
    private UUID parseUuid(String value) {
        // 检查输入值是否为null或空字符串
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            // 尝试将字符串转换为UUID对象
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            // 如果字符串格式不正确，捕获异常并返回null
            return null;
        }
    }

/**
 * 构建合并键的方法
 * 根据文件ID、分块索引和ID生成一个唯一的合并键
 *
 * @param fileId 文件ID，不能为空或空白字符串
 * @param chunkIndex 分块索引，不能为null
 * @param id 备用标识符，当 fileId 或 chunkIndex 无效时使用
 * @return 返回生成的合并键，如果所有参数都无效则返回null
 */
    private String buildMergeKey(
            String fileId,        // 文件ID，用于标识唯一文件
            Integer chunkIndex,   // 分块索引，用于标识文件中的分块位置
            String id            // 备用标识符，当 fileId 或 chunkIndex 无效时使用
    ) {
    // 检查 fileId 和 chunkIndex 是否有效
        if (fileId != null
                && !fileId.isBlank()
                && chunkIndex != null) {
        // 如果有效，返回 fileId 去除首尾空格后加上分块索引的组合
            return fileId.strip() + "#" + chunkIndex;
        }

    // 如果 fileId 或 chunkIndex 无效，检查 id 是否有效
    // 如果 id 为 null 或空白字符串则返回 null，否则返回 id
        return id == null || id.isBlank() ? null : id;
    }

    /**
     * 添加RRF融合分数
     * @param result 混合搜索结果
     * @param weight 权重
     * @param rank 排名
     */
    private void addRrfScore(
            HybridSearchResult result,

            double weight,
            int rank
    ) {
        double contribution = weight / (RRF_K + rank);

        result.setFusedScore(
                result.getFusedScore() + contribution
        );
    }

    /**
     * 验证搜索查询
     * @param query 搜索查询字符串
     */
    private void validateQuery(String query) {
        if (query == null || query.isBlank()) {

            throw new BusinessException("搜索内容不能为空");
        }
    }

    /**
     * 验证topK参数
     * @param topK 返回结果数量
     */
    private void validateTopK(int topK) {
        if (topK <= 0 || topK > MAX_TOP_K) {

            throw new BusinessException(
                    "topK 必须在 1 到 " + MAX_TOP_K + " 之间"
            );
        }
    }
}
