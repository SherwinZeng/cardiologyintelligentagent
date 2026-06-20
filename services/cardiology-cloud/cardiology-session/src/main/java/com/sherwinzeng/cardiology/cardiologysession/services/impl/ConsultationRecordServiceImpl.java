package com.sherwinzeng.cardiology.cardiologysession.services.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.BaseResponse;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.auth.AuthUserType;
import com.sherwinzeng.cardiology.cardiologycloudcommonutils.json.JsonSerialization;
import com.sherwinzeng.cardiology.cardiologysession.entity.ConsultationRecord;
import com.sherwinzeng.cardiology.cardiologysession.repository.ConsultationRecordMapper;
import com.sherwinzeng.cardiology.cardiologysession.response.ConsultationRecordPageResponse;
import com.sherwinzeng.cardiology.cardiologysession.response.ConsultationRecordResponse;
import com.sherwinzeng.cardiology.cardiologysession.services.ConsultationRecordService;
import com.sherwinzeng.cardiology.cardiologysession.support.AuthHeaderSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ConsultationRecordServiceImpl implements ConsultationRecordService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 5;
    private static final int MAX_PAGE_SIZE = 50;

    private final ConsultationRecordMapper consultationRecordMapper;

    @Override
    public String listRecords(
            String uid,
            Integer page,
            Integer pageSize,
            String urgency,
            String keyword,
            String startDate,
            String endDate,
            String userType,
            String authenticatedUid
    ) {
        AuthHeaderSupport.assertUidMatch(uid, authenticatedUid);
        if (AuthUserType.GUEST.equals(userType)) {
            throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "游客问诊不会生成长期记录，请登录正式账号后使用");
        }

        int resolvedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int resolvedPageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        String normalizedUrgency = null;
        if (StringUtils.hasText(urgency) && !"all".equalsIgnoreCase(urgency.trim())) {
            String value = urgency.trim().toLowerCase(Locale.ROOT);
            if ("red".equals(value) || "high".equals(value)) {
                normalizedUrgency = "red";
            } else if ("yellow".equals(value) || "moderate".equals(value) || "medium".equals(value)) {
                normalizedUrgency = "yellow";
            } else if ("green".equals(value) || "low".equals(value)) {
                normalizedUrgency = "green";
            }
        }
        String trimmedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        LocalDateTime startAt = null;
        if (StringUtils.hasText(startDate)) {
            try {
                startAt = LocalDate.parse(startDate.trim()).atStartOfDay();
            } catch (DateTimeParseException exception) {
                throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "开始日期格式应为 yyyy-MM-dd");
            }
        }
        LocalDateTime endAt = null;
        if (StringUtils.hasText(endDate)) {
            try {
                endAt = LocalDate.parse(endDate.trim()).atTime(23, 59, 59);
            } catch (DateTimeParseException exception) {
                throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "结束日期格式应为 yyyy-MM-dd");
            }
        }
        Page<ConsultationRecord> pageQuery = new Page<>(resolvedPage, resolvedPageSize);
        IPage<ConsultationRecord> result = consultationRecordMapper.selectRecordPage(
                pageQuery,
                uid,
                normalizedUrgency,
                trimmedKeyword,
                startAt,
                endAt
        );
        ConsultationRecordPageResponse response = new ConsultationRecordPageResponse();
        response.setRecords(result.getRecords().stream().map(record -> {
            ConsultationRecordResponse item = new ConsultationRecordResponse();
            item.setId(record.getId());
            item.setSessionId(record.getSessionId());
            item.setUid(record.getUid());
            item.setTitle(record.getTitle());
            item.setUrgency(record.getUrgency());
            item.setSummary(record.getSummary());
            item.setMessageCount(record.getMessageCount());
            item.setStartedAt(record.getStartedAt());
            item.setEndedAt(record.getEndedAt());
            item.setGeneratedAt(record.getGeneratedAt());
            return item;
        }).toList());
        response.setTotal(result.getTotal());
        response.setPage(result.getCurrent());
        response.setPageSize(result.getSize());
        response.setHasMore(result.getCurrent() * result.getSize() < result.getTotal());
        return JsonSerialization.toJson(BaseResponse.success(response));
    }
}
