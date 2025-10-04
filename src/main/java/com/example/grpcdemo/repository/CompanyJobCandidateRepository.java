package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CompanyJobCandidateEntity;
import com.example.grpcdemo.entity.JobCandidateInviteStatus;
import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 岗位候选人存取仓储。
 */
public interface CompanyJobCandidateRepository extends JpaRepository<CompanyJobCandidateEntity, String> {

    List<CompanyJobCandidateEntity> findByPositionIdOrderByCreatedAtDesc(String positionId);

    @Query("SELECT c FROM CompanyJobCandidateEntity c WHERE c.positionId = :positionId AND "
            + "(LOWER(c.candidateName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.candidateEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.candidatePhone) LIKE LOWER(CONCAT('%', :keyword, '%')))"
            + " ORDER BY c.createdAt DESC")
    List<CompanyJobCandidateEntity> searchByKeyword(@Param("positionId") String positionId,
                                                    @Param("keyword") String keyword);

    long countByPositionIdAndInviteStatus(String positionId, JobCandidateInviteStatus status);

    long countByPositionIdAndInterviewStatus(String positionId, JobCandidateInterviewStatus status);

    Optional<CompanyJobCandidateEntity> findByJobCandidateIdAndPositionId(String jobCandidateId, String positionId);
}
