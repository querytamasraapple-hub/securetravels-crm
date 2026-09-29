package com.securetravels.crm.automation.runtime;

import com.securetravels.crm.common.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Module 3 guardrails, read live from the operator-config so an incident
 * response is a config flip without touching the engine.
 *
 * <ul>
 *   <li><b>Kill switch:</b> {@code killSwitch()} halts all automation work —
 *       no new runs, no step execution, no sweeps, no recovery. Existing runs
 *       are left untouched and resume from exactly where they froze once the
 *       switch is lifted (step-ledger + effect-guard idempotency guarantees no
 *       double effects).</li>
 *   <li><b>Dry run:</b> {@code dryRun()} keeps the engine flowing — runs are
 *       created, conditions evaluated, verdicts recorded — but no effect
 *       action is ever performed on disk (steps are recorded SKIPPED
 *       instead). Orchestration steps (WAIT/BRANCH/STOP/APPROVAL) are not
 *       side effects and are unaffected.</li>
 * </ul>
 */
@Component
public class AutomationGuards {

    private final AppProperties.Automation cfg;

    public AutomationGuards(AppProperties props) {
        this.cfg = props.getAutomation();
    }

    public boolean killSwitch() {
        return cfg.isKillSwitchEnabled();
    }

    public boolean dryRun() {
        return cfg.isDryRunEnabled();
    }

    public int depthCap() {
        return Math.max(1, cfg.getCoordinateDepthCap());
    }

    /** Live operator configuration, so callers can read shared guard limits. */
    public AppProperties.Automation cfg() {
        return cfg;
    }
}