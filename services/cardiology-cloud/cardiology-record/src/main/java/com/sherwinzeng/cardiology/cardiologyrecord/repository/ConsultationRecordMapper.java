package com.sherwinzeng.cardiology.cardiologyrecord.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sherwinzeng.cardiology.cardiologyrecord.entity.ConsultationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ConsultationRecordMapper extends BaseMapper<ConsultationRecord> {

    ConsultationRecord selectBySessionId(@Param("sessionId") String sessionId);
}
