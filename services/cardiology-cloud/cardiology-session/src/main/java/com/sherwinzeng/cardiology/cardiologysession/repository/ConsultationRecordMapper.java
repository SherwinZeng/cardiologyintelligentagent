package com.sherwinzeng.cardiology.cardiologysession.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sherwinzeng.cardiology.cardiologysession.entity.ConsultationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ConsultationRecordMapper extends BaseMapper<ConsultationRecord> {

    IPage<ConsultationRecord> selectRecordPage(
            Page<ConsultationRecord> page,
            @Param("uid") String uid,
            @Param("urgency") String urgency,
            @Param("keyword") String keyword,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
