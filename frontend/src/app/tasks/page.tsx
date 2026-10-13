"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { api, type Task } from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { MANAGERS, SALES_DESK, hasRole } from "@/lib/rbac";
import { TASK_STATUS_STYLE, TASK_TYPE_LABEL } from "@/lib/taskLabels";
import { Protected } from "@/components/Protected";
import { Sidebar } from "@/components/Sidebar";

function fmtWhen(iso?: string) {
  return iso ? new Date(iso).toLocaleString("en-IN") : "—";
}

function isOpen(status?: string) {
  return status === "PENDING" || status === "OVERDUE";
}

function TasksView() {
  const { user } = useAuth();
  const seesTeam = hasRole(user?.role, MANAGERS);
  const canOpenLeads = hasRole(user?.role, SALES_DESK);

  const [tasks, setTasks] = useState<Task[]>([]);
  const [status, setStatus] = useState<string>("OPEN");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      // Backend accepts a single status; "OPEN" is client-side (PENDING + OVERDUE).
      if (status === "OPEN") {
        const [pending, overdue] = await Promise.all([
          api.getTasks({ status: "PENDING" }),
          api.getTasks({ status: "OVERDUE" }),
        ]);
        const byId = new Map<string, Task>();
        for (const t of [...pending, ...overdue]) {
          if (t.id) byId.set(t.id, t);
        }
        setTasks(
          [...byId.values()].sort((a, b) => {
            const da = a.dueAt ? Date.parse(a.dueAt) : 0;
            const db = b.dueAt ? Date.parse(b.dueAt) : 0;
            return da - db;
          }),
        );
      } else if (status === "ALL") {
        setTasks(await api.getTasks({}));
      } else {
        setTasks(await api.getTasks({ status }));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load tasks");
      setTasks([]);
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    load();
  }, [load]);

  const counts = useMemo(() => {
    const open = tasks.filter((t) => isOpen(t.status)).length;
    const overdue = tasks.filter((t) => t.status === "OVERDUE").length;
    const escalated = tasks.filter((t) => !!t.escalatedAt && isOpen(t.status)).length;
    return { total: tasks.length, open, overdue, escalated };
  }, [tasks]);

  async function complete(id: string) {
    setBusyId(id);
    try {
      await api.completeTask(id);
      await load();
    } catch (err) {
      window.alert(err instanceof Error ? err.message : "Could not complete task");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <main className="flex-1 p-6">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">
              {seesTeam ? "Team tasks" : "My tasks"}
            </h1>
            <p className="text-sm text-slate-500">
              {seesTeam
                ? "All open follow-ups across the sales desk. Complete any assignee's task."
                : "Calls and follow-ups assigned to you. Backend scopes this list to your user."}
            </p>
          </div>
          <button
            onClick={load}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
          >
            Refresh
          </button>
        </div>

        <div className="mb-5 grid gap-3 sm:grid-cols-3">
          <Card label="In view" value={String(counts.total)} />
          <Card label="Open" value={String(counts.open)} />
          <Card
            label="Overdue / escalated"
            value={`${counts.overdue} / ${counts.escalated}`}
            warn={counts.overdue > 0}
          />
        </div>

        <div className="mb-4 flex flex-wrap items-center gap-2">
          {[
            { id: "OPEN", label: "Open" },
            { id: "OVERDUE", label: "Overdue" },
            { id: "PENDING", label: "Pending" },
            { id: "COMPLETED", label: "Completed" },
            { id: "ALL", label: "All" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setStatus(tab.id)}
              className={`rounded-md px-3 py-1.5 text-sm font-medium ${
                status === tab.id
                  ? "bg-slate-900 text-white"
                  : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {error && (
          <p className="mb-4 rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
        )}

        {loading ? (
          <p className="py-12 text-center text-sm text-slate-400">Loading tasks…</p>
        ) : tasks.length === 0 ? (
          <p className="rounded-xl border border-dashed border-slate-200 bg-white py-12 text-center text-sm text-slate-400">
            No tasks in this filter. New leads auto-create a 5-minute first-call task.
          </p>
        ) : (
          <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-200 bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3">Task</th>
                  <th className="px-4 py-3">Customer</th>
                  {seesTeam && <th className="px-4 py-3">Assignee</th>}
                  <th className="px-4 py-3">Due</th>
                  <th className="px-4 py-3">SLA</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {tasks.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900">
                        {TASK_TYPE_LABEL[t.type ?? ""] ?? t.type}
                      </div>
                      {t.notes && (
                        <div className="mt-0.5 line-clamp-1 text-xs text-slate-400">{t.notes}</div>
                      )}
                      {t.escalatedAt && (
                        <div className="mt-0.5 text-[11px] font-medium text-rose-600">
                          Escalated {fmtWhen(t.escalatedAt)}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {t.leadId && canOpenLeads ? (
                        <Link
                          href={`/leads/${t.leadId}`}
                          className="font-medium text-slate-800 hover:underline"
                        >
                          {t.customerName || "Lead"}
                        </Link>
                      ) : (
                        <span className="text-slate-600">{t.customerName || "—"}</span>
                      )}
                    </td>
                    {seesTeam && (
                      <td className="px-4 py-3 text-slate-600">{t.assigneeName ?? "—"}</td>
                    )}
                    <td className="px-4 py-3 text-slate-600">{fmtWhen(t.dueAt)}</td>
                    <td className="px-4 py-3 text-slate-600">{fmtWhen(t.slaDeadline)}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                          TASK_STATUS_STYLE[t.status ?? ""] || ""
                        }`}
                      >
                        {t.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      {isOpen(t.status) && t.id && (
                        <button
                          disabled={busyId === t.id}
                          onClick={() => complete(t.id as string)}
                          className="rounded-md bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white hover:bg-slate-800 disabled:opacity-50"
                        >
                          {busyId === t.id ? "…" : "Complete"}
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

      </main>
    </div>
  );
}

function Card({
  label,
  value,
  warn,
}: {
  label: string;
  value: string;
  warn?: boolean;
}) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-4 py-3">
      <div className="text-[11px] font-medium uppercase tracking-wide text-slate-400">{label}</div>
      <div
        className={`mt-1 text-xl font-semibold tabular-nums ${
          warn ? "text-rose-600" : "text-slate-900"
        }`}
      >
        {value}
      </div>
    </div>
  );
}

export default function TasksPage() {
  return (
    <Protected>
      <TasksView />
    </Protected>
  );
}
