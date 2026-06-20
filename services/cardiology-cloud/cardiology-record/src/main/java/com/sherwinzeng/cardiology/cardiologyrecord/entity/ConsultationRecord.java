package com.sherwinzeng.cardiology.cardiologyrecord.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("consultation_record")
public class ConsultationRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String sessionId;
    private String uid;
    private String title;
    private String urgency;
    private String summary;
    private Integer messageCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime generatedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
