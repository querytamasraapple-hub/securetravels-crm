package com.securetravels.crm.automation.runtime;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRun, UUID> {

    List<WorkflowRun> findByWorkflowIdAndStatusIn(UUID workflowId, Set<WorkflowRunStatus> statuses);

    List<WorkflowRun> findAllByStatusIn(Set<WorkflowRunStatus> statuses);

    long countByWorkflowIdAndCreatedAtAfter(UUID workflowId, Instant after);

    long countByWorkflowIdAndStatusIn(UUID workflowId, Set<WorkflowRunStatus> statuses);
}