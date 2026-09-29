package com.securetravels.crm.automation;

import com.securetravels.crm.BaseIT;
import com.securetravels.crm.automation.definition.ActionType;
import com.securetravels.crm.automation.definition.StepDefinition;
import com.securetravels.crm.automation.definition.TriggerDefinition;
import com.securetravels.crm.automation.definition.WorkflowDefinition;
import com.securetravels.crm.automation.domain.WorkflowVersion;
import com.securetravels.crm.automation.event.AutomationEvent;
import com.securetravels.crm.automation.event.AutomationEventRecord;
import com.securetravels.crm.automation.event.AutomationEventRecordRepository;
import com.securetravels.crm.automation.event.DurableEventPublisher;
import com.securetravels.crm.automation.runtime.AutomationEventRelay;
import com.securetravels.crm.automation.runtime.TriggerSweep;
import com.securetravels.crm.automation.runtime.WorkflowRun;
import com.securetravels.crm.automation.runtime.WorkflowRunRepository;
import com.securetravels.crm.automation.runtime.WorkflowRunService;
import com.securetravels.crm.automation.runtime.WorkflowRunStatus;
import com.securetravels.crm.automation.runtime.WorkflowRunStep;
import com.securetravels.crm.automation.runtime.WorkflowRunStepRepository;
import com.securetravels.crm.automation.runtime.WorkflowRunStepStatus;
import com.securetravels.crm.automation.runtime.WorkflowStepEffectRepository;
import com.securetravels.crm.common.config.AppProperties;
import com.securetravels.crm.lead.Lead;
import com.securetravels.crm.lead.LeadRepository;
import com.securetravels.crm.task.TaskRepository;
import com.securetravels.crm.user.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 3 — the guardrails. Each scenario flips the LIVE operator config
 * (the same {@code AppProperties.Automation} bean the engine and schedulers
 * read on every tick), exercises the behaviour, and restores the defaults in
 * {@code @AfterEach}.
 *
 * <p>Covers: kill switch halting every engine path (relay, trigger sweep,
 * step execution) and resuming exactly afterwards; dry run recording effect
 * steps as SKIPPED without touching disk; the per-workflow rate limit on run
 * creation; the per-workflow active-run quota (blast radius); and the
 * coordination depth cap stopping a cyclic BRANCH run from spinning forever.
 */
class AutomationGuardrailsIT extends BaseIT {

    @Autowired private WorkflowService workflows;
    @Autowired private DurableEventPublisher publisher;
    @Autowired private AutomationEventRelay relay;
    @Autowired private TriggerSweep triggerSweep;
    @Autowired private WorkflowRunService engine;
    @Autowired private AppProperties props;

    @Autowired private AutomationEventRecordRepository eventRecords;
    @Autowired private WorkflowRunRepository runs;
    @Autowired private WorkflowRunStepRepository stepLedger;
    @Autowired private WorkflowStepEffectRepository effects;
    @Autowired private LeadRepository leads;
    @Autowired private TaskRepository tasks;

    private static final Map<String, Object> TASK_EFFECT = Map.of(
            "type", "INITIAL_CALL", "assignee", "OWNER", "dueInMinutes", 60);
    private static final Map<String, Object> FIELD_EFFECT = Map.of(
            "field", "followUpDate", "value", "today+1");

    @AfterEach
    void restoreConfig() {
        props.getAutomation().setKillSwitchEnabled(false);
        props.getAutomation().setDryRunEnabled(false);
        props.getAutomation().setRateLimitPerWorkflowPerMinute(0);
        props.getAutomation().setMaxActiveRunsPerWorkflow(0);
        props.getAutomation().setCoordinateDepthCap(10_000);
    }

    // ---------------------------------------------------------------------
    // Kill switch
    // ---------------------------------------------------------------------

    @Test
    void killSwitchHaltsEveryPathAndResumesExactlyWhenLifted() {
        UUID sales = newSales("kill");
        UUID leadId = seedLead(sales);
        WorkflowVersion version = activate("kill",
                def(TriggerDefinition.event("lead.updated"),
                        List.of(task("touch", 1), stop(2))));
        props.getAutomation().setKillSwitchEnabled(true);

        publisher.publish(new AutomationEvent("lead", "updated", leadId, Instant.now()));
        relay.drain();
        // The outbox row was never claimed: nothing consumed, nothing dropped.
        assertThat(eventRecords.findAll()).hasSize(1);
        assertThat(eventRecords.findAll().get(0).getStatus())
                .isEqualTo(AutomationEventRecord.Status.PENDING);
        assertThat(runs.findAll()).isEmpty();

        // The trigger sweep honours the switch too.
        triggerSweep.runOnce();
        assertThat(eventRecords.findAll()).hasSize(1);

        // Direct starts are declined while the switch is up.
        assertThat(engine.startRun(version, "lead", "updated", leadId,
                Instant.now(), "direct-" + UUID.randomUUID())).isNull();

        props.getAutomation().setKillSwitchEnabled(false);
        relay.drain();
        assertThat(runs.findAll()).hasSize(1);
        assertThat(runs.findAll().get(0).getStatus()).isEqualTo(WorkflowRunStatus.SUCCEEDED);
        assertThat(tasks.findAll()).hasSize(1);
    }

    @Test
    void killSwitchFreezesParkedRunsWithoutLosingTheirPlace() {
        UUID sales = newSales("freeze");
        UUID leadId = seedLead(sales);
        activate("freeze", def(TriggerDefinition.event("lead.updated"),
                List.of(wait(1), task("touch", 2), stop(3))));

        publisher.publish(new AutomationEvent("lead", "updated", leadId, Instant.now()));
        relay.drain();
        WorkflowRun parked = runs.findAll().get(0);
        assertThat(parked.getStatus()).isEqualTo(WorkflowRunStatus.WAITING);

        props.getAutomation().setKillSwitchEnabled(true);
        engine.resumeAt(parked.getId(), "touch");
        assertThat(runs.findById(parked.getId()).orElseThrow().getStatus())
                .isEqualTo(WorkflowRunStatus.WAITING);
        assertThat(tasks.findAll()).isEmpty();

        props.getAutomation().setKillSwitchEnabled(false);
        engine.resumeAt(parked.getId(), "touch");
        assertThat(runs.findById(parked.getId()).orElseThrow().getStatus())
                .isEqualTo(WorkflowRunStatus.SUCCEEDED);
        assertThat(tasks.findAll()).hasSize(1);
    }

    // ---------------------------------------------------------------------
    // Dry run
    // ---------------------------------------------------------------------

    @Test
    void dryRunRecordsEffectStepsAsSkippedWithoutPerformingThem() {
        UUID sales = newSales("dry");
        UUID leadId = seedLead(sales);
        activate("dry", def(TriggerDefinition.event("lead.updated"),
                List.of(task("task", 1), field("mark", 2), stop(3))));
        props.getAutomation().setDryRunEnabled(true);

        publisher.publish(new AutomationEvent("lead", "updated", leadId, Instant.now()));
        relay.drain();

        WorkflowRun run = runs.findAll().get(0);
        assertThat(run.getStatus()).isEqualTo(WorkflowRunStatus.SUCCEEDED);
        List<WorkflowRunStep> ledger = stepLedger.findAll().stream()
                .filter(s -> s.getRunId().equals(run.getId()))
                .sorted(java.util.Comparator.comparingInt(WorkflowRunStep::getStepOrder))
                .toList();
        assertThat(ledger).extracting(WorkflowRunStep::getStatus)
                .containsExactly(WorkflowRunStepStatus.SKIPPED,
                        WorkflowRunStepStatus.SKIPPED,
                        WorkflowRunStepStatus.SUCCEEDED);
        assertThat(ledger.stream()
                .filter(s -> s.getStatus() == WorkflowRunStepStatus.SKIPPED))
                .allSatisfy(s -> assertThat(s.getLastError()).contains("dry-run"));

        // The disk is untouched: no task, no field change, no effect guards.
        assertThat(tasks.findAll()).isEmpty();
        assertThat(leads.findById(leadId).orElseThrow().getFollowUpDate()).isNull();
        assertThat(effects.findAll()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // Rate limit and blast radius
    // ---------------------------------------------------------------------

    @Test
    void rateLimitDeclinesFurtherRunsWithinTheSameMinute() {
        UUID sales = newSales("rate");
        UUID leadA = seedLead(sales);
        UUID leadB = seedLead(sales);
        activate("rate", singleStop());
        props.getAutomation().setRateLimitPerWorkflowPerMinute(1);

        publisher.publish(new AutomationEvent("lead", "updated", leadA, Instant.now()));
        relay.drain();
        assertThat(runs.findAll()).hasSize(1);

        publisher.publish(new AutomationEvent("lead", "updated", leadB, Instant.now()));
        relay.drain();
        assertThat(runs.findAll()).hasSize(1);
    }

    @Test
    void activeRunQuotaDeclinesOnceTheBlastRadiusIsFull() {
        UUID sales = newSales("blast");
        UUID leadA = seedLead(sales);
        UUID leadB = seedLead(sales);
        activate("blast", def(TriggerDefinition.event("lead.updated"),
                List.of(wait(60), stop(2))));
        props.getAutomation().setMaxActiveRunsPerWorkflow(1);

        publisher.publish(new AutomationEvent("lead", "updated", leadA, Instant.now()));
        relay.drain();
        assertThat(runs.findAll()).hasSize(1);
        assertThat(runs.findAll().get(0).getStatus()).isEqualTo(WorkflowRunStatus.WAITING);

        publisher.publish(new AutomationEvent("lead", "updated", leadB, Instant.now()));
        relay.drain();
        assertThat(runs.findAll()).hasSize(1);

        engine.cancel(runs.findAll().get(0).getId());
    }

    // ---------------------------------------------------------------------
    // Coordination depth cap
    // ---------------------------------------------------------------------

    @Test
    void depthCapStopsACyclicBranchFromSpinningForever() {
        UUID sales = newSales("depth");
        UUID leadId = seedLead(sales);
        // Two mutually-targeting BRANCH steps: a↔b. The validator forbids a
        // step looping to itself, so an infinite cycle is exactly what a
        // mis-authored definition CAN still produce — which is what the cap is
        // for: the coordinator must give up and leave the run recoverable,
        // never hang, and never blow the stack.
        Map<String, Object> always = Map.of(
                "operator", "AND",
                "conditions", List.of(Map.of("field", "status", "op", "NOT_NULL")),
                "children", List.of());
        activate("depth", def(TriggerDefinition.event("lead.updated"),
                List.of(branch("a", 1, always, "b"), branch("b", 2, always, "a"))));
        props.getAutomation().setCoordinateDepthCap(10);

        long started = System.currentTimeMillis();
        publisher.publish(new AutomationEvent("lead", "updated", leadId, Instant.now()));
        relay.drain();
        assertThat(System.currentTimeMillis() - started).isLessThan(30_000);

        WorkflowRun run = runs.findAll().get(0);
        assertThat(run.getStatus()).isIn(WorkflowRunStatus.RUNNING, WorkflowRunStatus.WAITING);
        assertThat(run.getLastError()).isNull();
        engine.cancel(run.getId());
    }

    // ------------------------------------------------------------------ helpers

    private WorkflowVersion activate(String slug, WorkflowDefinition definition) {
        UUID actor = newOps(slug);
        UUID workflowId = workflows.createWorkflow(slug, "Guardrail " + slug, null, actor).getId();
        workflows.saveDraft(workflowId, definition, actor);
        return workflows.activate(workflowId, actor);
    }

    private WorkflowDefinition def(TriggerDefinition trigger, List<StepDefinition> steps) {
        return new WorkflowDefinition(WorkflowDefinition.CURRENT_SCHEMA_VERSION, "Guardrail", null,
                trigger, null, steps);
    }

    private WorkflowDefinition singleStop() {
        return def(TriggerDefinition.event("lead.updated"),
                List.of(new StepDefinition("stop", 1, ActionType.STOP, null, Map.of(), null)));
    }

    private StepDefinition wait(int minutes) {
        return new StepDefinition("wait", 1, ActionType.WAIT, null, Map.of("minutes", minutes), null);
    }

    private StepDefinition task(String id, int order) {
        return new StepDefinition(id, order, ActionType.CREATE_TASK, null, TASK_EFFECT, null);
    }

    private StepDefinition field(String id, int order) {
        return new StepDefinition(id, order, ActionType.UPDATE_FIELD, null, FIELD_EFFECT, null);
    }

    private StepDefinition stop(int order) {
        return new StepDefinition("stop", order, ActionType.STOP, null, Map.of(), null);
    }

    private StepDefinition branch(String id, int order, Map<String, Object> always, String target) {
        return new StepDefinition(id, order, ActionType.BRANCH, null,
                Map.of("cases", List.of(Map.of("when", always, "goto", target)),
                        "default", target), null);
    }

    private UUID newOps(String slug) {
        return createUser("guard-ops-" + slug + "@securetravels.in", "Ops", Role.OPS, "P@ssw0rd");
    }

    private UUID newSales(String slug) {
        return createUser("guard-sales-" + slug + "@securetravels.in", "Sales", Role.SALES, "P@ssw0rd");
    }

    private UUID seedLead(UUID ownerId) {
        Lead lead = new Lead();
        lead.setCustomerName("Guard lead " + UUID.randomUUID().toString().substring(0, 8));
        lead.setMobileNumber("9198" + (100000 + (int) (Math.random() * 899999)));
        lead.setMobileDigits("9");
        lead.setSource(Lead.Source.WEBSITE);
        lead.setStatus(Lead.Status.INTERESTED);
        lead.setOwnerId(ownerId);
        lead.setConsentGiven(true);
        lead.setCreatedBy(ownerId);
        return leads.save(lead).getId();
    }
}