package com.sherwinzeng.cardiology.cardiologysession.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatMessage;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
