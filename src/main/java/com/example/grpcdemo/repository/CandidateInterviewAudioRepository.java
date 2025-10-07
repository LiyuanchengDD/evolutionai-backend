package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CandidateInterviewAudioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 面试音频片段仓储。
 */
public interface CandidateInterviewAudioRepository extends JpaRepository<CandidateInterviewAudioEntity, String> {

    List<CandidateInterviewAudioEntity> findByJobCandidateIdAndInterviewRecordIdOrderByQuestionSequenceAscCreatedAtAsc(
            String jobCandidateId, String interviewRecordId);

    Optional<CandidateInterviewAudioEntity> findByAudioIdAndJobCandidateId(String audioId, String jobCandidateId);

    Optional<CandidateInterviewAudioEntity> findByInterviewRecordIdAndQuestionSequence(String interviewRecordId,
                                                                                       Integer questionSequence);

    void deleteByInterviewRecordId(String interviewRecordId);
}
