package com.sherwinzeng.cardiology.cardiologysession.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心血管问诊消息表 chat_message
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    /** 消息主键，自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属会话 ID，关联 chat_session.session_id */
    private String sessionId;

    /** 发言用户 uid，须与 session 归属一致 */
    private String uid;

    /** 消息角色：user=用户输入，assistant=铭铭回复 */
    @TableField("`role`")
    private String role;

    /** 消息正文 */
    private String content;

    /** 分诊级别（assistant）：green/yellow/red */
    private String urgency;

    /** 临床印象（assistant） */
    private String explanation;

    /** 处理建议（assistant） */
    private String advice;

    /** 免责声明（assistant） */
    private String disclaimer;

    /** 消息创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
