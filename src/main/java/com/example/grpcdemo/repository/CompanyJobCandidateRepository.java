package com.example.grpcdemo.repository;

import com.example.grpcdemo.entity.CompanyJobCandidateEntity;
import com.example.grpcdemo.entity.JobCandidateInviteStatus;
import com.example.grpcdemo.entity.JobCandidateInterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 岗位候选人存取仓储。
 */
public interface CompanyJobCandidateRepository extends JpaRepository<CompanyJobCandidateEntity, String> {

    @Query("SELECT c FROM CompanyJobCandidateEntity c "
            + "WHERE c.positionId = :positionId "
            + "AND (:keyword IS NULL OR "
            + "LOWER(c.candidateName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.candidateEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(c.candidatePhone) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
            + "AND (:inviteStatuses IS NULL OR c.inviteStatus IN :inviteStatuses) "
            + "AND (:interviewStatuses IS NULL OR c.interviewStatus IN :interviewStatuses) "
            + "ORDER BY c.createdAt DESC")
    Page<CompanyJobCandidateEntity> searchByFilters(@Param("positionId") String positionId,
                                                    @Param("keyword") String keyword,
                                                    @Param("inviteStatuses") List<JobCandidateInviteStatus> inviteStatuses,
                                                    @Param("interviewStatuses") List<JobCandidateInterviewStatus> interviewStatuses,
                                                    Pageable pageable);

    long countByPositionIdAndInviteStatus(String positionId, JobCandidateInviteStatus status);

    long countByPositionIdAndInterviewStatus(String positionId, JobCandidateInterviewStatus status);

    long countByPositionId(String positionId);

    Optional<CompanyJobCandidateEntity> findByJobCandidateIdAndPositionId(String jobCandidateId, String positionId);

    List<CompanyJobCandidateEntity> findByCandidateEmailIgnoreCase(String candidateEmail);

    List<CompanyJobCandidateEntity> findByCandidatePhone(String candidatePhone);
}
