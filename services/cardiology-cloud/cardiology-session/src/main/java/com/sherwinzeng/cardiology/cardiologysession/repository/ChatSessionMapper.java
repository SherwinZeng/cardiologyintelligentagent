package com.sherwinzeng.cardiology.cardiologysession.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sherwinzeng.cardiology.cardiologysession.entity.ChatSession;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

    IPage<ChatSession> selectActivePage(
            Page<ChatSession> page,
            @Param("uid") String uid,
            @Param("keyword") String keyword
    );
}
