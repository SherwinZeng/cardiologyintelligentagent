package com.sherwinzeng.cardiology.cardiologyrecord.services;

public interface ConsultationSummaryService {

    /** 轮询 Redis 延迟队列，执行已到期的总结任务 */
    void pollDueSummaries();
}
