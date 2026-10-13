"use client";

import { useCallback, useEffect, useState } from "react";
import { api, type NotificationItem } from "@/lib/api";
import { CRM_STAFF } from "@/lib/rbac";
import { AdminPage } from "@/components/AdminPage";

export default function NotificationsPage() {
  const [items, setItems] = useState<NotificationItem[]>([]);
  const [unread, setUnread] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => {
    api.getNotifications(false)
      .then((f) => {
        setItems(f.items ?? []);
        setUnread(f.unread ?? 0);
      })
      .catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  return (
    <AdminPage
      title="Notifications"
      subtitle={`${unread} unread`}
      allowedRoles={CRM_STAFF}
      actions={
        <button onClick={() => api.markAllNotificationsRead().then(reload)} className="rounded-lg border px-3 py-2 text-sm">
          Mark all read
        </button>
      }
    >
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      <div className="space-y-2">
        {items.map((n) => {
          const read = !!(n as { readAt?: string | null }).readAt || !!(n as { read?: boolean }).read;
          return (
            <div key={n.id as string} className={`rounded-xl border bg-white px-4 py-3 ${read ? "opacity-70" : "border-orange-200"}`}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <div className="text-sm font-medium text-slate-900">{n.title}</div>
                  <div className="mt-0.5 text-sm text-slate-600">{n.body}</div>
                </div>
                {!read && (
                  <button onClick={() => api.markNotificationRead(n.id as string).then(reload)} className="text-xs text-emerald-700 hover:underline">
                    Mark read
                  </button>
                )}
              </div>
            </div>
          );
        })}
        {items.length === 0 && <p className="text-sm text-slate-400">No notifications</p>}
      </div>
    </AdminPage>
  );
}
