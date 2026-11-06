package com.customerfeedback.platform.service;

import com.customerfeedback.platform.dto.FeedbackResponse;
import com.customerfeedback.platform.dto.PagedResponse;
import com.customerfeedback.platform.entity.Feedback;
import com.customerfeedback.platform.repository.FeedbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FeedbackQueryService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackQueryService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<FeedbackResponse> listFeedback(UUID batchId, Pageable pageable) {
        Page<Feedback> page;
        if (batchId != null) {
            page = feedbackRepository.findByBatch_BatchIdOrderByOriginalRowNumberAsc(batchId, pageable);
        } else {
            page = feedbackRepository.findAll(pageable);
        }
        return PagedResponse.from(page.map(this::toResponse));
    }

    public FeedbackResponse toResponse(Feedback f) {
        return new FeedbackResponse(
                f.getFeedbackId(),
                f.getBatch().getBatchId(),
                f.getFeedbackDate(),
                f.getChannel(),
                f.getProduct(),
                f.getRating(),
                f.getCustomerSegment(),
                f.getRegion(),
                f.getFeedbackText(),
                f.getOriginalRowNumber(),
                f.getProcessingStatus(),
                f.getCreatedAt()
        );
    }
}
