package com.IntelligentCustomer.system.repository.mapper;

import com.IntelligentCustomer.system.domain.dto.search.KeywordSearchResult;
import com.IntelligentCustomer.system.domain.entity.KnowledgeChunk;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

import org.apache.ibatis.annotations.*;

/**
 * 知识块数据访问接口，继承自BaseMapper
 * 该接口提供了对KnowledgeChunk实型的基本CRUD操作以及自定义查询方法
 */
@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    /**
     * 根据文件ID查询知识块列表
     * @param fileId 文件ID，用于筛选对应的知识块
     * @return 返回按知识块索引升序排列的知识块列表
     */
    default List<KnowledgeChunk> findByFileId(String fileId) {
        return selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getFileId, fileId)  // 设置查询条件：文件ID等于传入的fileId
                .orderByAsc(KnowledgeChunk::getChunkIndex));  // 按知识块索引升序排序
    }

    /**
     * 根据文件ID删除知识块
     * @param fileId 文件ID，用于删除对应的知识块
     * @return 返回删除的记录数
     */
    default int deleteByFileId(String fileId) {
        return delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getFileId, fileId));  // 设置删除条件：文件ID等于传入的fileId
    }

/**
 * 根据关键词搜索知识库内容
 * 使用MySQL的全文搜索功能，通过MATCH AGAINST语句进行自然语言查询
 * 返回结果按关键词得分降序排列，并限制返回数量
 *
 * @param query 搜索关键词
 * @param limit 返回结果的最大数量
 * @return 包含搜索结果和关键词得分的列表
 */
    @Select("""
        SELECT
            id,                    -- 知识块ID
            file_id,               -- 所属文件ID
            content,               -- 知识块内容
            source_file,           -- 源文件路径
            chunk_index,           -- 块索引
            created_at,            -- 创建时间
            MATCH(content)         -- 计算关键词得分
                AGAINST (#{query} IN NATURAL LANGUAGE MODE) AS keyword_score
        FROM knowledge_chunk     -- 知识块表
        WHERE MATCH(content)    -- 匹配内容包含查询关键词
                AGAINST (#{query} IN NATURAL LANGUAGE MODE)
        ORDER BY keyword_score DESC  -- 按关键词得分降序排列
        LIMIT #{limit}          -- 限制返回结果数量
        """)
    @Results({
            @Result(
                    column = "id",
                    property = "id",
                    javaType = String.class,
                    id = true
            ),
            @Result(column = "file_id", property = "fileId"),
            @Result(column = "content", property = "content"),
            @Result(column = "source_file", property = "sourceFile"),
            @Result(column = "chunk_index", property = "chunkIndex"),
            @Result(column = "created_at", property = "createdAt"),
            @Result(column = "keyword_score", property = "keywordScore")
            })

    List<KeywordSearchResult> searchByKeyword(  // 搜索方法
            @Param("query") String query,   // 搜索参数：查询关键词
            @Param("limit") int limit       // 搜索参数：返回结果数量限制
    );
}
