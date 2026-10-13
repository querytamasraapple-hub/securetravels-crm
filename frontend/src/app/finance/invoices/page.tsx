"use client";

import { FormEvent, Suspense, useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { api, type FinanceInvoice, type FinanceInvoiceCreatePayload } from "@/lib/api";
import { FINANCE_PORTAL, canWriteFinance } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { Protected } from "@/components/Protected";
import { FinanceShell, KpiCard, inr } from "@/components/finance/FinanceShell";

const STATUS: Record<string, string> = {
  DRAFT: "bg-slate-100 text-slate-600",
  SENT: "bg-sky-50 text-sky-700",
  PARTIALLY_PAID: "bg-amber-50 text-amber-700",
  PAID: "bg-emerald-50 text-emerald-700",
  OVERDUE: "bg-rose-50 text-rose-700",
  CANCELLED: "bg-slate-100 text-slate-400",
};

const WA: Record<string, string> = {
  PENDING: "bg-slate-100 text-slate-600",
  QUEUED: "bg-amber-50 text-amber-700",
  SENT: "bg-emerald-50 text-emerald-700",
  FAILED: "bg-rose-50 text-rose-700",
  SKIPPED: "bg-slate-100 text-slate-400",
};

type LineForm = {
  description: string;
  hsnSac: string;
  quantity: number;
  unit: string;
  rate: number;
  gstRate: number;
};

const emptyForm = (): FinanceInvoiceCreatePayload => ({
  customerName: "",
  customerEmail: "",
  customerWhatsapp: "",
  customerPhone: "",
  customerGstin: "",
  customerAddress: "",
  customerState: "Uttarakhand",
  customerStateCode: "05",
  placeOfSupply: "Uttarakhand",
  placeOfSupplyCode: "05",
  issueDate: new Date().toISOString().slice(0, 10),
  dueDate: new Date(Date.now() + 14 * 86400000).toISOString().slice(0, 10),
  invoiceType: "TAX_INVOICE",
  supplyType: "INTRA",
  reverseCharge: false,
  sacCode: "998555",
  status: "SENT",
  queueWhatsapp: true,
  notes: "",
  lines: [],
});

function InvoicesView() {
  const { user } = useAuth();
  const canWrite = canWriteFinance(user?.role);
  const searchParams = useSearchParams();
  const [rows, setRows] = useState<FinanceInvoice[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [show, setShow] = useState(false);
  const [form, setForm] = useState<FinanceInvoiceCreatePayload>(emptyForm());
  const [lines, setLines] = useState<LineForm[]>([
    {
      description: "Trekking / tour package services",
      hsnSac: "998555",
      quantity: 1,
      unit: "PKG",
      rate: 0,
      gstRate: 5,
    },
  ]);

  const reload = useCallback(() => {
    setError(null);
    api.getFinanceInvoices().then(setRows).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  useEffect(() => {
    if (canWrite && searchParams.get("new") === "1") setShow(true);
  }, [canWrite, searchParams]);

  const preview = useMemo(() => {
    const taxable = lines.reduce((s, l) => s + Number(l.quantity || 0) * Number(l.rate || 0), 0);
    const rate = Number(lines[0]?.gstRate ?? 5);
    const isInter = form.supplyType === "INTER";
    const cgst = isInter ? 0 : (taxable * rate) / 200;
    const sgst = isInter ? 0 : (taxable * rate) / 200;
    const igst = isInter ? (taxable * rate) / 100 : 0;
    return { taxable, cgst, sgst, igst, total: taxable + cgst + sgst + igst, rate };
  }, [lines, form.supplyType]);

  const open = rows.filter((r) => r.status !== "PAID" && r.status !== "CANCELLED");
  const openAmt = open.reduce((s, r) => s + Number(r.totalAmount), 0);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      const created = await api.createFinanceInvoice({
        ...form,
        customerPhone: form.customerPhone || form.customerWhatsapp,
        placeOfSupply: form.placeOfSupply || form.customerState,
        placeOfSupplyCode: form.placeOfSupplyCode || form.customerStateCode,
        lines: lines.map((l) => ({
          description: l.description,
          hsnSac: l.hsnSac || form.sacCode || "998555",
          quantity: Number(l.quantity),
          unit: l.unit || "NOS",
          rate: Number(l.rate),
          gstRate: Number(l.gstRate),
        })),
      });
      setShow(false);
      setForm(emptyForm());
      reload();
      window.location.href = `/finance/invoices/${created.id}`;
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not create GST invoice");
    }
  }

  return (
    <FinanceShell
      title="GST Invoices"
      subtitle="Tax invoice with WhatsApp number — ready for automated bill delivery"
      actions={
        canWrite ? (
          <button
            onClick={() => setShow((v) => !v)}
            className="rounded-lg bg-emerald-700 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-800"
          >
            {show ? "Close form" : "Create invoice"}
          </button>
        ) : null
      }
    >
      {error && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}
      <div className="mb-6 grid gap-4 md:grid-cols-3">
        <KpiCard label="Invoices" value={String(rows.length)} />
        <KpiCard tone="warn" label="Open invoices" value={String(open.length)} />
        <KpiCard tone="accent" label="Open amount" value={inr(openAmt)} />
      </div>

      {show && canWrite && (
        <form onSubmit={onSubmit} className="mb-6 space-y-5 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-100 pb-3">
            <div>
              <div className="text-sm font-semibold text-slate-900">Create GST tax invoice</div>
              <div className="text-xs text-slate-500">Seller: Trip On Himalaya · SAC 998555 · GST auto-split CGST/SGST or IGST</div>
            </div>
            <label className="flex items-center gap-2 text-xs font-medium text-slate-600">
              <input
                type="checkbox"
                checked={!!form.queueWhatsapp}
                onChange={(e) => setForm({ ...form, queueWhatsapp: e.target.checked })}
              />
              Queue WhatsApp automation on save
            </label>
          </div>

          <div className="grid gap-3 md:grid-cols-3">
            <label className="text-sm md:col-span-2">
              <span className="font-medium">Bill to (customer name) *</span>
              <input required value={form.customerName} onChange={(e) => setForm({ ...form, customerName: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" />
            </label>
            <label className="text-sm">
              <span className="font-medium">WhatsApp no. *</span>
              <input
                required
                inputMode="tel"
                placeholder="9876543210"
                value={form.customerWhatsapp}
                onChange={(e) => setForm({ ...form, customerWhatsapp: e.target.value })}
                className="mt-1 w-full rounded-lg border border-emerald-300 px-3 py-2"
              />
            </label>
            <label className="text-sm">
              <span className="font-medium">Phone</span>
              <input value={form.customerPhone ?? ""} onChange={(e) => setForm({ ...form, customerPhone: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" placeholder="Same as WhatsApp if blank" />
            </label>
            <label className="text-sm">
              <span className="font-medium">Email</span>
              <input type="email" value={form.customerEmail ?? ""} onChange={(e) => setForm({ ...form, customerEmail: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" />
            </label>
            <label className="text-sm">
              <span className="font-medium">Customer GSTIN (B2B)</span>
              <input value={form.customerGstin ?? ""} onChange={(e) => setForm({ ...form, customerGstin: e.target.value.toUpperCase() })} className="mt-1 w-full rounded-lg border px-3 py-2" placeholder="Optional" />
            </label>
            <label className="text-sm md:col-span-3">
              <span className="font-medium">Billing address</span>
              <textarea value={form.customerAddress ?? ""} onChange={(e) => setForm({ ...form, customerAddress: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" rows={2} />
            </label>
            <label className="text-sm">
              <span className="font-medium">State</span>
              <input value={form.customerState ?? ""} onChange={(e) => setForm({ ...form, customerState: e.target.value, placeOfSupply: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" />
            </label>
            <label className="text-sm">
              <span className="font-medium">State code</span>
              <input maxLength={2} value={form.customerStateCode ?? ""} onChange={(e) => setForm({ ...form, customerStateCode: e.target.value, placeOfSupplyCode: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" />
            </label>
            <label className="text-sm">
              <span className="font-medium">Supply type</span>
              <select value={form.supplyType} onChange={(e) => setForm({ ...form, supplyType: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2">
                <option value="INTRA">Intra-state (CGST + SGST)</option>
                <option value="INTER">Inter-state (IGST)</option>
              </select>
            </label>
            <label className="text-sm">
              <span className="font-medium">Issue date</span>
              <input type="date" required value={form.issueDate} onChange={(e) => setForm({ ...form, issueDate: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" />
            </label>
            <label className="text-sm">
              <span className="font-medium">Due date</span>
              <input type="date" required value={form.dueDate} onChange={(e) => setForm({ ...form, dueDate: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" />
            </label>
            <label className="text-sm">
              <span className="font-medium">Invoice type</span>
              <select value={form.invoiceType} onChange={(e) => setForm({ ...form, invoiceType: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2">
                <option value="TAX_INVOICE">Tax Invoice</option>
                <option value="PROFORMA">Proforma</option>
                <option value="BILL_OF_SUPPLY">Bill of Supply</option>
              </select>
            </label>
          </div>

          <div>
            <div className="mb-2 flex items-center justify-between">
              <div className="text-sm font-semibold text-slate-800">Line items (HSN/SAC)</div>
              <button
                type="button"
                onClick={() =>
                  setLines((prev) => [
                    ...prev,
                    { description: "", hsnSac: "998555", quantity: 1, unit: "NOS", rate: 0, gstRate: 5 },
                  ])
                }
                className="text-xs font-medium text-emerald-700 hover:underline"
              >
                + Add line
              </button>
            </div>
            <div className="space-y-2">
              {lines.map((l, idx) => (
                <div key={idx} className="grid gap-2 rounded-xl border border-slate-100 bg-slate-50/70 p-3 md:grid-cols-12">
                  <input
                    required
                    placeholder="Description"
                    value={l.description}
                    onChange={(e) => {
                      const next = [...lines];
                      next[idx] = { ...l, description: e.target.value };
                      setLines(next);
                    }}
                    className="rounded-lg border px-2 py-1.5 text-sm md:col-span-4"
                  />
                  <input
                    placeholder="SAC"
                    value={l.hsnSac}
                    onChange={(e) => {
                      const next = [...lines];
                      next[idx] = { ...l, hsnSac: e.target.value };
                      setLines(next);
                    }}
                    className="rounded-lg border px-2 py-1.5 text-sm md:col-span-1"
                  />
                  <input
                    type="number"
                    min={0.01}
                    step="0.01"
                    placeholder="Qty"
                    value={l.quantity || ""}
                    onChange={(e) => {
                      const next = [...lines];
                      next[idx] = { ...l, quantity: Number(e.target.value) };
                      setLines(next);
                    }}
                    className="rounded-lg border px-2 py-1.5 text-sm md:col-span-1"
                  />
                  <input
                    placeholder="Unit"
                    value={l.unit}
                    onChange={(e) => {
                      const next = [...lines];
                      next[idx] = { ...l, unit: e.target.value };
                      setLines(next);
                    }}
                    className="rounded-lg border px-2 py-1.5 text-sm md:col-span-1"
                  />
                  <input
                    type="number"
                    min={0.01}
                    step="0.01"
                    placeholder="Rate ₹"
                    value={l.rate || ""}
                    onChange={(e) => {
                      const next = [...lines];
                      next[idx] = { ...l, rate: Number(e.target.value) };
                      setLines(next);
                    }}
                    className="rounded-lg border px-2 py-1.5 text-sm md:col-span-2"
                  />
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    placeholder="GST %"
                    value={l.gstRate}
                    onChange={(e) => {
                      const next = [...lines];
                      next[idx] = { ...l, gstRate: Number(e.target.value) };
                      setLines(next);
                    }}
                    className="rounded-lg border px-2 py-1.5 text-sm md:col-span-1"
                  />
                  <div className="flex items-center justify-between text-xs text-slate-500 md:col-span-2">
                    <span className="tabular-nums font-medium text-slate-700">
                      {inr(Number(l.quantity || 0) * Number(l.rate || 0))}
                    </span>
                    {lines.length > 1 && (
                      <button
                        type="button"
                        onClick={() => setLines(lines.filter((_, i) => i !== idx))}
                        className="text-rose-600 hover:underline"
                      >
                        Remove
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="grid gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm md:grid-cols-4">
            <div>Taxable: <strong>{inr(preview.taxable)}</strong></div>
            {form.supplyType === "INTER" ? (
              <div>IGST {preview.rate}%: <strong>{inr(preview.igst)}</strong></div>
            ) : (
              <>
                <div>CGST {preview.rate / 2}%: <strong>{inr(preview.cgst)}</strong></div>
                <div>SGST {preview.rate / 2}%: <strong>{inr(preview.sgst)}</strong></div>
              </>
            )}
            <div className="font-semibold text-slate-900">Grand total: {inr(preview.total)}</div>
          </div>

          <label className="block text-sm">
            <span className="font-medium">Notes</span>
            <textarea value={form.notes ?? ""} onChange={(e) => setForm({ ...form, notes: e.target.value })} className="mt-1 w-full rounded-lg border px-3 py-2" rows={2} />
          </label>

          <button type="submit" className="rounded-lg bg-emerald-700 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-800">
            Create GST invoice
          </button>
        </form>
      )}

      <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table className="min-w-full text-left text-sm">
          <thead className="border-b border-slate-100 bg-slate-50 text-[11px] uppercase tracking-wide text-slate-400">
            <tr>
              <th className="px-4 py-3">Invoice</th>
              <th className="px-4 py-3">Customer / WhatsApp</th>
              <th className="px-4 py-3">Issue</th>
              <th className="px-4 py-3">Total</th>
              <th className="px-4 py-3">GST</th>
              <th className="px-4 py-3">WhatsApp</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-slate-400">
                  No GST invoices yet
                </td>
              </tr>
            )}
            {rows.map((r) => (
              <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                <td className="px-4 py-3">
                  <Link href={`/finance/invoices/${r.id}`} className="font-mono text-xs font-semibold text-emerald-800 hover:underline">
                    {r.invoiceNo}
                  </Link>
                  <div className="text-[10px] uppercase text-slate-400">{r.invoiceType?.replaceAll("_", " ")}</div>
                </td>
                <td className="px-4 py-3">
                  <div className="font-medium">{r.customerName}</div>
                  <div className="text-xs text-emerald-700">WA {r.customerWhatsapp ?? "—"}</div>
                </td>
                <td className="px-4 py-3 text-slate-500">{r.issueDate}</td>
                <td className="px-4 py-3 font-semibold tabular-nums">{inr(r.totalAmount)}</td>
                <td className="px-4 py-3 text-xs text-slate-500 tabular-nums">{inr(r.taxAmount)}</td>
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${WA[r.whatsappDeliveryStatus ?? ""] ?? "bg-slate-100"}`}>
                    {r.whatsappDeliveryStatus ?? "—"}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={`rounded-full px-2 py-0.5 text-[11px] font-semibold ${STATUS[r.status] ?? "bg-slate-100"}`}>
                    {r.status}
                  </span>
                </td>
                <td className="px-4 py-3 space-x-2 whitespace-nowrap">
                  <Link href={`/finance/invoices/${r.id}`} className="text-xs font-medium text-slate-700 hover:underline">
                    View bill
                  </Link>
                  {canWrite && r.status !== "PAID" && r.status !== "CANCELLED" && (
                    <button onClick={() => api.updateFinanceInvoiceStatus(r.id, "PAID").then(reload)} className="text-xs font-medium text-emerald-700 hover:underline">
                      Mark paid
                    </button>
                  )}
                  {canWrite && (
                    <button onClick={() => api.queueFinanceInvoiceWhatsapp(r.id).then(reload).catch((e: Error) => setError(e.message))} className="text-xs font-medium text-sky-700 hover:underline">
                      Queue WA
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </FinanceShell>
  );
}

export default function InvoicesPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <Suspense fallback={<div className="flex h-screen items-center justify-center text-slate-400">Loading…</div>}>
        <InvoicesView />
      </Suspense>
    </Protected>
  );
}
