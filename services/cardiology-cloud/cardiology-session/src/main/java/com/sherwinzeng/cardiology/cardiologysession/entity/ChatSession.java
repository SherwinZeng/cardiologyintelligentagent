package com.sherwinzeng.cardiology.cardiologysession.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心血管问诊会话表 chat_session
 */
@Data
@TableName("chat_session")
public class ChatSession {

    /** 会话 ID，对应 LangGraph thread_id，由客户端生成 UUID */
    @TableId(value = "session_id", type = IdType.INPUT)
    private String sessionId;

    /** 会话归属用户 uid */
    private String uid;

    /** 会话标题，默认「新建问诊」，首条消息后可用主诉更新 */
    private String title;

    /** 最近一条消息摘要，用于左侧列表预览 */
    private String preview;

    /** 消息条数（user + assistant 合计） */
    private Integer messageCount;

    /** 会话状态：active=进行中，archived=已归档 */
    private String status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 最近活跃时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
