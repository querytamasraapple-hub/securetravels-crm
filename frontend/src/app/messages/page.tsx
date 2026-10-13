"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  api,
  formatApiError,
  type InboxThread,
  type SalesUser,
  type TimelineEntry,
} from "@/lib/api";
import { useAuth } from "@/lib/auth";
import { MANAGERS, hasRole } from "@/lib/rbac";
import { Protected } from "@/components/Protected";
import { Sidebar } from "@/components/Sidebar";

const CHANNEL_STYLE: Record<string, string> = {
  WHATSAPP: "bg-emerald-50 text-emerald-700",
  EMAIL: "bg-sky-50 text-sky-700",
  SMS: "bg-amber-50 text-amber-700",
};

const STATUS_STYLE: Record<string, string> = {
  OPEN: "bg-rose-50 text-rose-700",
  PENDING: "bg-amber-50 text-amber-700",
  CLOSED: "bg-slate-100 text-slate-500",
};

function fmtWhen(iso?: string | null) {
  if (!iso) return "";
  return new Date(iso).toLocaleString("en-IN", {
    day: "numeric",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function subjectHref(t: InboxThread) {
  if (t.subjectType === "LEAD") return `/leads/${t.subjectId}`;
  if (t.subjectType === "BOOKING") return `/bookings`;
  if (t.subjectType === "CUSTOMER") return `/customers`;
  return null;
}

function MessagesView() {
  const { user } = useAuth();
  const canManage = hasRole(user?.role, MANAGERS);

  const [threads, setThreads] = useState<InboxThread[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [channel, setChannel] = useState("");
  const [status, setStatus] = useState("");
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selected, setSelected] = useState<InboxThread | null>(null);
  const [timeline, setTimeline] = useState<TimelineEntry[]>([]);
  const [timelineLoading, setTimelineLoading] = useState(false);
  const [salesUsers, setSalesUsers] = useState<SalesUser[]>([]);
  const [actionError, setActionError] = useState<string | null>(null);

  const loadList = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.getInboxThreads({
        channel: channel || undefined,
        status: status || undefined,
        unreadOnly: unreadOnly || undefined,
        page,
        size: 30,
      });
      setThreads(res.content ?? []);
      setTotal(res.totalElements ?? 0);
    } catch (err) {
      setError(formatApiError(err, "Could not load inbox"));
      setThreads([]);
    } finally {
      setLoading(false);
    }
  }, [channel, status, unreadOnly, page]);

  useEffect(() => {
    loadList();
  }, [loadList]);

  useEffect(() => {
    if (canManage) {
      api.getSalesUsers().then(setSalesUsers).catch(() => undefined);
    }
  }, [canManage]);

  const openThread = useCallback(async (id: string) => {
    setSelectedId(id);
    setActionError(null);
    setTimelineLoading(true);
    try {
      const thread = await api.getInboxThread(id);
      setSelected(thread);
      if (thread.unreadCount > 0) {
        const read = await api.markInboxThreadRead(id);
        setSelected(read);
        setThreads((prev) => prev.map((t) => (t.id === id ? read : t)));
      }
      const events = await api.getTimeline(thread.subjectType, thread.subjectId);
      // Show newest at bottom for chat feel
      setTimeline([...events].reverse());
    } catch (err) {
      setActionError(formatApiError(err, "Could not open conversation"));
      setTimeline([]);
    } finally {
      setTimelineLoading(false);
    }
  }, []);

  const unreadTotal = useMemo(
    () => threads.reduce((n, t) => n + (t.unreadCount || 0), 0),
    [threads],
  );

  async function setThreadStatus(next: string) {
    if (!selected) return;
    setActionError(null);
    try {
      const updated = await api.updateInboxThread(selected.id, { status: next });
      setSelected(updated);
      setThreads((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
    } catch (err) {
      setActionError(formatApiError(err, "Could not update status"));
    }
  }

  async function assignTo(userId: string) {
    if (!selected) return;
    setActionError(null);
    try {
      const updated = await api.updateInboxThread(selected.id, {
        assignedTo: userId || null,
      });
      setSelected(updated);
      setThreads((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
    } catch (err) {
      setActionError(formatApiError(err, "Could not assign thread"));
    }
  }

  const href = selected ? subjectHref(selected) : null;

  return (
    <div className="flex h-screen">
      <Sidebar />
      <main className="flex min-w-0 flex-1 flex-col overflow-hidden">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-4">
          <div>
            <h1 className="text-lg font-semibold text-slate-900">Messages</h1>
            <p className="text-xs text-slate-400">
              Unified inbox — WhatsApp, email and SMS conversations.
              {unreadTotal > 0 ? ` · ${unreadTotal} unread in view` : ""}
            </p>
          </div>
          <button
            onClick={loadList}
            className="rounded-md border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
          >
            Refresh
          </button>
        </header>

        <div className="flex min-h-0 flex-1">
          {/* Thread list */}
          <aside className="flex w-full max-w-md flex-col border-r border-slate-200 bg-white">
            <div className="flex flex-wrap gap-2 border-b border-slate-100 p-3">
              <select
                value={channel}
                onChange={(e) => {
                  setPage(0);
                  setChannel(e.target.value);
                }}
                className="rounded-md border border-slate-300 px-2 py-1 text-xs"
              >
                <option value="">All channels</option>
                <option value="WHATSAPP">WhatsApp</option>
                <option value="EMAIL">Email</option>
                <option value="SMS">SMS</option>
              </select>
              <select
                value={status}
                onChange={(e) => {
                  setPage(0);
                  setStatus(e.target.value);
                }}
                className="rounded-md border border-slate-300 px-2 py-1 text-xs"
              >
                <option value="">All status</option>
                <option value="OPEN">Open</option>
                <option value="PENDING">Pending</option>
                <option value="CLOSED">Closed</option>
              </select>
              <label className="flex items-center gap-1.5 text-xs text-slate-600">
                <input
                  type="checkbox"
                  checked={unreadOnly}
                  onChange={(e) => {
                    setPage(0);
                    setUnreadOnly(e.target.checked);
                  }}
                />
                Unread
              </label>
            </div>

            {error && (
              <p className="m-3 rounded-md bg-red-50 px-3 py-2 text-xs text-red-700">{error}</p>
            )}

            <div className="min-h-0 flex-1 overflow-y-auto">
              {loading ? (
                <p className="py-12 text-center text-sm text-slate-400">Loading inbox…</p>
              ) : threads.length === 0 ? (
                <div className="px-6 py-12 text-center text-sm text-slate-400">
                  <p className="font-medium text-slate-600">No conversations yet</p>
                  <p className="mt-2 text-xs">
                    Threads appear when customers reply on WhatsApp, email or SMS,
                    or when the team sends outbound messages.
                  </p>
                </div>
              ) : (
                <ul>
                  {threads.map((t) => {
                    const active = t.id === selectedId;
                    return (
                      <li key={t.id}>
                        <button
                          type="button"
                          onClick={() => openThread(t.id)}
                          className={`w-full border-b border-slate-100 px-4 py-3 text-left hover:bg-slate-50 ${
                            active ? "bg-slate-100" : ""
                          }`}
                        >
                          <div className="flex items-start justify-between gap-2">
                            <div className="min-w-0">
                              <div className="truncate text-sm font-semibold text-slate-900">
                                {t.customerName || t.customerMobile || "Unknown"}
                              </div>
                              <div className="mt-0.5 line-clamp-2 text-xs text-slate-500">
                                {t.lastPreview || "—"}
                              </div>
                            </div>
                            <div className="shrink-0 text-right">
                              <div className="text-[10px] text-slate-400">
                                {fmtWhen(t.lastMessageAt)}
                              </div>
                              {t.unreadCount > 0 && (
                                <span className="mt-1 inline-flex min-w-[1.25rem] items-center justify-center rounded-full bg-sky-600 px-1.5 text-[10px] font-bold text-white">
                                  {t.unreadCount}
                                </span>
                              )}
                            </div>
                          </div>
                          <div className="mt-2 flex flex-wrap gap-1.5">
                            <span
                              className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                CHANNEL_STYLE[t.channel] || ""
                              }`}
                            >
                              {t.channel}
                            </span>
                            <span
                              className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                                STATUS_STYLE[t.status] || ""
                              }`}
                            >
                              {t.status}
                            </span>
                            <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-500">
                              {t.subjectType}
                            </span>
                          </div>
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>

            {total > 30 && (
              <div className="flex items-center justify-between border-t border-slate-200 px-3 py-2 text-xs text-slate-500">
                <button
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                  className="disabled:opacity-40"
                >
                  ← Prev
                </button>
                <span>
                  Page {page + 1} · {total} threads
                </span>
                <button
                  disabled={(page + 1) * 30 >= total}
                  onClick={() => setPage((p) => p + 1)}
                  className="disabled:opacity-40"
                >
                  Next →
                </button>
              </div>
            )}
          </aside>

          {/* Conversation pane */}
          <section className="flex min-w-0 flex-1 flex-col bg-slate-50">
            {!selected ? (
              <div className="flex flex-1 items-center justify-center text-sm text-slate-400">
                Select a conversation to read the timeline
              </div>
            ) : (
              <>
                <div className="border-b border-slate-200 bg-white px-5 py-3">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h2 className="text-base font-semibold text-slate-900">
                        {selected.customerName || selected.customerMobile || "Conversation"}
                      </h2>
                      <p className="mt-0.5 text-xs text-slate-500">
                        {selected.channel} · {selected.subjectType}
                        {selected.customerMobile ? ` · ${selected.customerMobile}` : ""}
                        {selected.lastDirection ? ` · last ${selected.lastDirection}` : ""}
                      </p>
                    </div>
                    <div className="flex flex-wrap items-center gap-2">
                      {href && (
                        <Link
                          href={href}
                          className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                        >
                          Open record
                        </Link>
                      )}
                      {canManage && (
                        <>
                          <select
                            value={selected.status}
                            onChange={(e) => setThreadStatus(e.target.value)}
                            className="rounded-md border border-slate-300 px-2 py-1.5 text-xs"
                          >
                            <option value="OPEN">OPEN</option>
                            <option value="PENDING">PENDING</option>
                            <option value="CLOSED">CLOSED</option>
                          </select>
                          <select
                            value={selected.assignedTo ?? ""}
                            onChange={(e) => assignTo(e.target.value)}
                            className="rounded-md border border-slate-300 px-2 py-1.5 text-xs"
                          >
                            <option value="">Unassigned</option>
                            {salesUsers.map((u) => (
                              <option key={u.id} value={u.id}>
                                {u.fullName}
                              </option>
                            ))}
                          </select>
                        </>
                      )}
                    </div>
                  </div>
                  {selected.channel === "WHATSAPP" && (
                    <p className="mt-2 text-[11px] text-slate-400">
                      Service window:{" "}
                      {selected.templateSendAllowed
                        ? "open (free-form replies ok)"
                        : "closed — template send only"}
                      {selected.serviceWindowExpiresAt
                        ? ` · expires ${fmtWhen(selected.serviceWindowExpiresAt)}`
                        : ""}
                    </p>
                  )}
                  {actionError && (
                    <p className="mt-2 rounded-md bg-red-50 px-3 py-1.5 text-xs text-red-700">
                      {actionError}
                    </p>
                  )}
                </div>

                <div className="min-h-0 flex-1 space-y-3 overflow-y-auto p-5">
                  {timelineLoading ? (
                    <p className="py-10 text-center text-sm text-slate-400">Loading messages…</p>
                  ) : timeline.length === 0 ? (
                    <p className="py-10 text-center text-sm text-slate-400">
                      No timeline events for this subject yet.
                    </p>
                  ) : (
                    timeline.map((e) => {
                      const inbound = e.direction === "INBOUND";
                      return (
                        <div
                          key={`${e.seq}-${e.createdAt}`}
                          className={`flex ${inbound ? "justify-start" : "justify-end"}`}
                        >
                          <div
                            className={`max-w-[75%] rounded-2xl px-4 py-2.5 text-sm shadow-sm ${
                              inbound
                                ? "rounded-bl-md bg-white text-slate-800"
                                : e.direction === "SYSTEM"
                                  ? "rounded-md bg-slate-200 text-slate-600"
                                  : "rounded-br-md bg-slate-900 text-white"
                            }`}
                          >
                            <div className="mb-1 flex flex-wrap gap-2 text-[10px] opacity-70">
                              <span>{e.channel}</span>
                              <span>{e.kind}</span>
                              <span>{fmtWhen(e.createdAt)}</span>
                            </div>
                            <div className="whitespace-pre-wrap">
                              {e.body || e.summary || e.templateCode || "—"}
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}

export default function MessagesPage() {
  return (
    <Protected>
      <MessagesView />
    </Protected>
  );
}
