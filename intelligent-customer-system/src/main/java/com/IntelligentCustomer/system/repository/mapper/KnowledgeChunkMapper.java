package com.IntelligentCustomer.system.repository.mapper;

import com.IntelligentCustomer.system.domain.entity.KnowledgeChunk;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    default List<KnowledgeChunk> findByFileId(String fileId) {
        return selectList(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getFileId, fileId)
                .orderByAsc(KnowledgeChunk::getChunkIndex));
    }

    default int deleteByFileId(String fileId) {
        return delete(new LambdaQueryWrapper<KnowledgeChunk>()
                .eq(KnowledgeChunk::getFileId, fileId));
    }
}
