"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { HR_ROLES, MANAGERS, hasRole } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { AdminPage } from "@/components/AdminPage";

export default function AttendancePage() {
  const { user } = useAuth();
  const isManager = hasRole(user?.role, MANAGERS);
  const [rows, setRows] = useState<Record<string, unknown>[]>([]);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => {
    api.getBasecampAttendance().then(setRows).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  async function punch(checkIn: boolean) {
    try {
      await api.upsertBasecampAttendance({
        checkIn,
        checkOut: !checkIn,
        status: "PRESENT",
        workDate: new Date().toISOString().slice(0, 10),
      });
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed");
    }
  }

  return (
    <AdminPage
      title="Attendance"
      subtitle={isManager ? "Team attendance (managers see all)" : "Your attendance — check in / out"}
      allowedRoles={HR_ROLES}
      actions={
        <div className="flex gap-2">
          <button onClick={() => punch(true)} className="rounded-lg bg-emerald-700 px-3 py-2 text-sm text-white">Check in</button>
          <button onClick={() => punch(false)} className="rounded-lg border px-3 py-2 text-sm">Check out</button>
        </div>
      }
    >
      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}
      <div className="overflow-hidden rounded-xl border bg-white">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-[11px] uppercase text-slate-400">
            <tr>
              <th className="px-4 py-3 text-left">Staff</th>
              <th className="px-4 py-3 text-left">Date</th>
              <th className="px-4 py-3 text-left">In</th>
              <th className="px-4 py-3 text-left">Out</th>
              <th className="px-4 py-3 text-left">Status</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={String(r.id)} className="border-t">
                <td className="px-4 py-3 font-medium">{String(r.userName)}</td>
                <td className="px-4 py-3">{String(r.workDate)}</td>
                <td className="px-4 py-3 text-xs text-slate-500">{r.checkInAt ? new Date(String(r.checkInAt)).toLocaleTimeString() : "—"}</td>
                <td className="px-4 py-3 text-xs text-slate-500">{r.checkOutAt ? new Date(String(r.checkOutAt)).toLocaleTimeString() : "—"}</td>
                <td className="px-4 py-3 text-xs font-semibold">{String(r.status)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </AdminPage>
  );
}
