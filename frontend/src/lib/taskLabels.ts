/** Shared labels / styles for follow-up tasks (lead panel + Tasks page). */

export const TASK_TYPE_LABEL: Record<string, string> = {
  INITIAL_CALL: "First call (within 5 min)",
  FOLLOW_UP_1D: "Follow-up · Day +1",
  FOLLOW_UP_3D: "Follow-up · Day +3",
  FOLLOW_UP_8D: "Follow-up · Day +8",
  FOLLOW_UP_15D: "Follow-up · Day +15",
  QUOTATION: "Quotation follow-up",
  PAYMENT_REMINDER: "Payment reminder",
  OPS: "Ops handoff",
  REVIEW: "Review",
  CUSTOM: "Custom",
};

export const TASK_STATUS_STYLE: Record<string, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  OVERDUE: "bg-red-100 text-red-700",
  COMPLETED: "bg-emerald-100 text-emerald-700",
  CANCELLED: "bg-slate-100 text-slate-500",
};

export const TASK_STATUSES = ["PENDING", "OVERDUE", "COMPLETED", "CANCELLED"] as const;
