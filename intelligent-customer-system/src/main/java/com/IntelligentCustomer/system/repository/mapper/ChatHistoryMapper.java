package com.IntelligentCustomer.system.repository.mapper;

import com.IntelligentCustomer.system.domain.entity.ChatHistory;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    default ChatHistory findByEventId(String eventId) {
        return selectOne(new LambdaQueryWrapper<ChatHistory>()
                .eq(ChatHistory::getEventId, eventId)
                .last("LIMIT 1"));
    }
}
