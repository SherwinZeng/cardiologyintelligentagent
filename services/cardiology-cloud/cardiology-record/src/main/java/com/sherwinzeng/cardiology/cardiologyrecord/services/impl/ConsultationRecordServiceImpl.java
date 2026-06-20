package com.sherwinzeng.cardiology.cardiologyrecord.services.impl;

import com.sherwinzeng.cardiology.cardiologycloudcommondata.exception.ChatBusinessException;
import com.sherwinzeng.cardiology.cardiologycloudcommondata.response.ResponseCode;
import com.sherwinzeng.cardiology.cardiologyrecord.entity.ChatSession;
import com.sherwinzeng.cardiology.cardiologyrecord.entity.ConsultationRecord;
import com.sherwinzeng.cardiology.cardiologyrecord.repository.ChatSessionMapper;
import com.sherwinzeng.cardiology.cardiologyrecord.repository.ConsultationRecordMapper;
import com.sherwinzeng.cardiology.cardiologyrecord.response.SessionSummaryResponse;
import com.sherwinzeng.cardiology.cardiologyrecord.services.ConsultationRecordService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ConsultationRecordServiceImpl implements ConsultationRecordService {

    private final ChatSessionMapper chatSessionMapper;
    private final ConsultationRecordMapper consultationRecordMapper;
    private final PlatformTransactionManager transactionManager;
    private TransactionTemplate transactionTemplate;

    @PostConstruct
    void initTransactionTemplate() {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void persistSummary(ChatSession session, SessionSummaryResponse summary) {
        LocalDateTime generatedAt = LocalDateTime.now();
        ConsultationRecord record = new ConsultationRecord();
        record.setSessionId(session.getSessionId());
        record.setUid(session.getUid());
        record.setTitle(summary.getTitle());
        record.setUrgency(summary.getUrgency());
        record.setSummary(summary.getSummary());
        record.setMessageCount(session.getMessageCount());
        record.setStartedAt(session.getCreatedAt());
        record.setEndedAt(session.getUpdatedAt());
        record.setGeneratedAt(generatedAt);

        transactionTemplate.executeWithoutResult(status -> {
            int done = chatSessionMapper.markSummaryDone(session.getSessionId(), summary.getTitle(), generatedAt);
            if (done == 0) {
                throw new ChatBusinessException(ResponseCode.BAD_REQUEST, "会话总结状态已变化");
            }
            ConsultationRecord existing = consultationRecordMapper.selectBySessionId(record.getSessionId());
            if (existing == null) {
                consultationRecordMapper.insert(record);
                return;
            }
            record.setId(existing.getId());
            consultationRecordMapper.updateById(record);
        });
    }
}
