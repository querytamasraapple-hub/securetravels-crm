package com.securetravels.crm.automation;

import com.securetravels.crm.BaseIT;
import com.securetravels.crm.automation.definition.ActionType;
import com.securetravels.crm.automation.definition.StepDefinition;
import com.securetravels.crm.automation.definition.TriggerDefinition;
import com.securetravels.crm.automation.definition.WorkflowDefinition;
import com.securetravels.crm.automation.event.AutomationEvent;
import com.securetravels.crm.automation.event.AutomationEventRecord;
import com.securetravels.crm.automation.event.AutomationEventRecordRepository;
import com.securetravels.crm.automation.event.DurableEventPublisher;
import com.securetravels.crm.automation.runtime.AutomationEventRelay;
import com.securetravels.crm.automation.runtime.WorkflowRun;
import com.securetravels.crm.automation.runtime.WorkflowRunRepository;
import com.securetravels.crm.automation.runtime.WorkflowRunFailureRepository;
import com.securetravels.crm.automation.runtime.WorkflowRunStatus;
import com.securetravels.crm.automation.runtime.WorkflowStepEffect;
import com.securetravels.crm.automation.runtime.WorkflowStepEffectRepository;
import com.securetravels.crm.lead.Lead;
import com.securetravels.crm.lead.LeadRepository;
import com.securetravels.crm.task.TaskRepository;
import com.securetravels.crm.user.Role;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 3 — the ADR-0007 load gate. The projected design-scale peak is 676
 * trigger events/day; the gate plays ten times that (6,760) as a burst and
 * asserts the two bars it exists to hold:
 *
 * <ol>
 *   <li><b>Latency bar:</b> every trigger emission completes within its
 *       business transaction with p95 added overhead ≤ 10 ms. Measured as the
 *       outbox INSERT itself (save + flush), committed by — and inside — the
 *       caller's own transaction, so the surrounding business commit is not
 *       counted twice.</li>
 *   <li><b>Exactly-once bar:</b> draining that burst produces exactly one run
 *       per event, every run succeeds, and every effect is applied exactly
 *       once — 6,760 tasks, 6,760 effect-guard rows, no retried attempt, no
 *       failure inbox row.</li>
 * </ol>
 *
 * <p>The drain must also complete within a bounded wall time: 6,760 events is
 * two orders of magnitude beyond anything the business projects, and the
 * engine still has to chew through it single-threaded in a manual loop (the
 * test profile keeps the relay dormant).
 */
class AutomationLoadIT extends BaseIT {

    /** 10x the ADR-0007 projected peak of 676 events/day, as a burst. */
    private static final int EVENTS = 6760;

    @Autowired private WorkflowService workflows;
    @Autowired private DurableEventPublisher publisher;
    @Autowired private AutomationEventRelay relay;
    @Autowired private PlatformTransactionManager tm;

    @PersistenceContext private EntityManager em;
    @Autowired private AutomationEventRecordRepository eventRecords;
    @Autowired private WorkflowRunRepository runs;
    @Autowired private WorkflowRunFailureRepository failures;
    @Autowired private WorkflowStepEffectRepository effects;
    @Autowired private LeadRepository leads;
    @Autowired private TaskRepository tasks;

    @Test
    void tenTimesProjectedPeakDrainsExactlyOnceUnderTheLatencyBar() {
        UUID owner = createUser("load-sales@securetravels.in", "Sales", Role.SALES, "P@ssw0rd");
        UUID actor = createUser("load-ops@securetravels.in", "Ops", Role.OPS, "P@ssw0rd");
        UUID workflowId = workflows.createWorkflow("load", "Load gate", null, actor).getId();
        workflows.saveDraft(workflowId, taskThenStop(), actor);
        workflows.activate(workflowId, actor);

        List<UUID> leadIds = seedLeads(owner, EVENTS);

        long[] timings = timePublish(EVENTS, leadIds, em);
        long p95 = p95(timings);
        System.out.printf("[load] publish overhead p95=%.2fms, max=%.2fms%n",
                p95 / 1_000_000.0, Arrays.stream(timings).max().orElse(0) / 1_000_000.0);

        long started = System.currentTimeMillis();
        int drains = 0;
        while (eventRecords.countByStatus(AutomationEventRecord.Status.PENDING) > 0
                && System.currentTimeMillis() - started < 300_000) {
            relay.drain();
            drains++;
        }
        long drainMillis = System.currentTimeMillis() - started;
        System.out.printf("[load] drained %d events in %dms across %d drain passes%n",
                EVENTS, drainMillis, drains);

        // Bar 1 — the latency gate (10 ms p95 on the emission overhead).
        assertThat(p95).as("p95 emission overhead within the business transaction")
                .isLessThanOrEqualTo(10_000_000L);

        // Bar 2 — exactly one run per event, every one green, nothing dropped.
        assertThat(eventRecords.countByStatus(AutomationEventRecord.Status.PENDING)).isZero();
        assertThat(eventRecords.countByStatus(AutomationEventRecord.Status.FAILED)).isZero();
        List<WorkflowRun> all = runs.findAll();
        assertThat(all).hasSize(EVENTS);
        assertThat(all).allSatisfy(r ->
                assertThat(r.getStatus()).isEqualTo(WorkflowRunStatus.SUCCEEDED));
        assertThat(failures.findAll()).isEmpty();
        assertThat(drainMillis).isLessThan(300_000);

        // Exactly-once effects: one effect row per run, attempt 1, and the
        // on-disk task exists for every one of them.
        assertThat(tasks.findAll()).hasSize(EVENTS);
        List<WorkflowStepEffect> effectRows = effects.findAll();
        assertThat(effectRows).hasSize(EVENTS);
        assertThat(effectRows).allSatisfy(e -> assertThat(e.getAttemptGroup()).isEqualTo(1));
    }

    // ------------------------------------------------------------------ helpers

    /** Sequential, unique event instants → 6,760 mutually-distinct event keys. */
    private long[] timePublish(int count, List<UUID> leadIds, EntityManager em) {
        TransactionTemplate tx = new TransactionTemplate(tm);
        long base = System.currentTimeMillis();
        long[] timings = new long[count];
        for (int i = 0; i < count; i++) {
            AutomationEvent event = new AutomationEvent("lead", "updated", leadIds.get(i),
                    Instant.ofEpochMilli(base - (count - i)));
            int idx = i;
            tx.executeWithoutResult(s -> {
                long t0 = System.nanoTime();
                publisher.publish(event);
                em.flush();
                timings[idx] = System.nanoTime() - t0;
            });
        }
        return timings;
    }

    private static long p95(long[] values) {
        long[] copy = values.clone();
        Arrays.sort(copy);
        int idx = (int) Math.ceil(0.95 * copy.length) - 1;
        return copy[Math.max(0, idx)];
    }

    private List<UUID> seedLeads(UUID owner, int count) {
        List<UUID> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i += 250) {
            List<Lead> batch = new ArrayList<>(Math.min(250, count - i));
            for (int j = 0; j < 250 && i + j < count; j++) {
                batch.add(leanLead(owner, i + j));
            }
            leads.saveAll(batch).forEach(l -> ids.add(l.getId()));
        }
        return ids;
    }

    private Lead leanLead(UUID owner, int seq) {
        Lead lead = new Lead();
        lead.setCustomerName("Load " + seq);
        lead.setMobileNumber("9198" + String.format("%08d", 200000 + seq));
        lead.setMobileDigits("9");
        lead.setSource(Lead.Source.WEBSITE);
        lead.setStatus(Lead.Status.INTERESTED);
        lead.setOwnerId(owner);
        lead.setConsentGiven(true);
        lead.setCreatedBy(owner);
        return lead;
    }

    private WorkflowDefinition taskThenStop() {
        return new WorkflowDefinition(WorkflowDefinition.CURRENT_SCHEMA_VERSION, "Load gate", null,
                TriggerDefinition.event("lead.updated"), null, List.of(
                        new StepDefinition("task", 1, ActionType.CREATE_TASK, null,
                                Map.of("type", "INITIAL_CALL", "assignee", "OWNER", "dueInMinutes", 60), null),
                        new StepDefinition("stop", 2, ActionType.STOP, null, Map.of(), null)));
    }
}