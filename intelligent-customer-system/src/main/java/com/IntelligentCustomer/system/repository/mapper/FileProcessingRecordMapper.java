package com.IntelligentCustomer.system.repository.mapper;

import com.IntelligentCustomer.system.domain.entity.FileProcessingRecord;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件处理记录数据访问接口
 * 继承BaseMapper以获得基础的CRUD操作能力
 */
@Mapper
public interface FileProcessingRecordMapper extends BaseMapper<FileProcessingRecord> {

/**
 * 根据文件ID查询文件处理记录
 * @param fileId 文件ID，用于唯一标识一个文件
 * @return 返回匹配文件ID的文件处理记录，如果没有找到则返回null
 */
    default FileProcessingRecord findByFileId(String fileId) {
    // 使用Lambda查询构造器创建查询条件
    // 筛选文件ID等于传入参数的记录
    // 添加LIMIT 1确保只返回一条记录
        return selectOne(new LambdaQueryWrapper<FileProcessingRecord>()
                .eq(FileProcessingRecord::getFileId, fileId)  // 设置查询条件：文件ID等于指定值
                .last("LIMIT 1"));  // 限制查询结果只返回一条记录
    }


/**
 * 更新文件处理状态为"处理中"的方法
 * @param fileId 文件ID，用于标识需要更新状态的文件
 */
    default void updateProcessing(String fileId) {
    // 创建一个新的文件处理记录对象
        FileProcessingRecord record = new FileProcessingRecord();
    // 设置处理状态为"PROCESSING"
        record.setStatus("PROCESSING");
    // 设置处理开始时间为当前时间
        record.setProcessStartTime(LocalDateTime.now());
    // 更新数据库中对应文件ID的记录
        update(record, new LambdaQueryWrapper<FileProcessingRecord>()
                .eq(FileProcessingRecord::getFileId, fileId));
    }

/**
 * 更新文件处理记录为成功状态
 * @param fileId 文件ID，用于标识需要更新的文件处理记录
 * @param chunks 已处理的文件块数量
 * @param embeddings 创建的嵌入向量数量
 */
    default void updateSuccess(String fileId, int chunks, int embeddings) {
        // 创建新的文件处理记录对象
        FileProcessingRecord record = new FileProcessingRecord();
        // 设置处理状态为成功
        record.setStatus("SUCCESS");
        // 设置已处理的文件块数量
        record.setProcessedChunks(chunks);
        // 设置创建的嵌入向量数量
        record.setEmbeddingsCreated(embeddings);
        // 设置处理结束时间为当前时间
        record.setProcessEndTime(LocalDateTime.now());
        // 更新数据库中对应的文件处理记录
        update(record, new LambdaQueryWrapper<FileProcessingRecord>()
                .eq(FileProcessingRecord::getFileId, fileId));
    }

/**
 * 更新文件处理记录为失败状态
 * @param fileId 文件ID，用于标识需要更新的文件
 * @param reason 失败原因，描述文件处理失败的具体原因
 */
    default void updateFailure(String fileId, String reason) {
        // 创建文件处理记录对象
        FileProcessingRecord record = new FileProcessingRecord();
        // 设置状态为"FAILED"
        record.setStatus("FAILED");
        // 设置失败原因
        record.setFailureReason(reason);
        // 设置处理结束时间为当前时间
        record.setProcessEndTime(LocalDateTime.now());
        // 执行更新操作，根据文件ID更新记录
        update(record, new LambdaQueryWrapper<FileProcessingRecord>()
                .eq(FileProcessingRecord::getFileId, fileId));
    }

/**
 * 将指定文件的状态重置为待处理(PENDING)状态
 * 该方法会清除文件处理过程中的所有相关记录，包括失败原因、处理块数、创建的嵌入等信息
 *
 * @param fileId 要重置的文件ID
 * @return 更新操作影响的记录数
 */
    default int resetToPending(String fileId) {
    // 使用update方法执行数据库更新操作
    // 参数1为null表示不更新实体类的任何字段
    // 参数2为UpdateWrapper，用于设置更新条件和要更新的字段
        return update(null, new UpdateWrapper<FileProcessingRecord>()
            // 将文件状态设置为PENDING（待处理）
                .set("status", "PENDING")
            // 清空失败原因字段
                .set("failure_reason", null)
            // 清空已处理块数字段
                .set("processed_chunks", null)
            // 清空已创建嵌入字段
                .set("embeddings_created", null)
            // 清空处理开始时间字段
                .set("process_start_time", null)
            // 清空处理结束时间字段
                .set("process_end_time", null)
            // 设置更新条件：文件ID匹配
                .eq("file_id", fileId)
            // 设置更新条件：文件状态为FAILED（失败）
            // 只有状态为FAILED的记录才会被更新
                .eq("status", "FAILED"));
    }

/**
 * 根据文件ID删除处理记录
 * @param fileId 文件ID，用于唯一标识一个文件
 * @return 返回删除的记录数，表示成功删除了多少条记录
 */
    default int deleteByFileId(String fileId) {
    // 使用Lambda查询构造器创建删除条件
    // 条件为：file_processing_record表中file_id字段等于传入的fileId
        return delete(new LambdaQueryWrapper<FileProcessingRecord>()
                .eq(FileProcessingRecord::getFileId, fileId));
    }

/**
 * 分页查询文件处理记录
 * @param status 文件处理状态，可以为null
 * @param limit 每页记录数
 * @param offset 偏移量
 * @return 返回符合条件的文件处理记录列表
 */
    default List<FileProcessingRecord> findAllPaged(String status, int limit, int offset) {
        // 创建Lambda查询包装器，按上传时间降序排序
        LambdaQueryWrapper<FileProcessingRecord> wrapper = new LambdaQueryWrapper<FileProcessingRecord>()
                .orderByDesc(FileProcessingRecord::getUploadTime) // 按上传时间降序排序
                .last("LIMIT " + limit + " OFFSET " + offset); // 添加分页限制条件
        // 如果状态不为空，则添加状态条件查询
        if (status != null && !status.isEmpty()) {
            wrapper.eq(FileProcessingRecord::getStatus, status); // 添加状态等于指定值的条件
        }
        // 执行查询并返回结果列表
        return selectList(wrapper);
    }

/**
 * 默认方法：计算符合指定状态条件的文件处理记录总数
 * @param status 文件处理状态，可为null或空字符串
 * @return 符合条件的记录总数，返回int类型
 */
    default int countAll(String status) {
        // 创建Lambda查询包装器，用于构建查询条件
        LambdaQueryWrapper<FileProcessingRecord> wrapper = new LambdaQueryWrapper<>();
        // 检查状态参数是否为非空且非空字符串
        if (status != null && !status.isEmpty()) {
            // 添加等于条件，查询状态匹配的记录
            wrapper.eq(FileProcessingRecord::getStatus, status);
        }
        // 执行查询并将结果转换为int类型返回
        return Math.toIntExact(selectCount(wrapper));
    }
}
