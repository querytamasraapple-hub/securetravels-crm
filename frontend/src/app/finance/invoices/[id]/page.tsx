"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { api, type FinanceInvoice } from "@/lib/api";
import { FINANCE_PORTAL, canWriteFinance } from "@/lib/rbac";
import { useAuth } from "@/lib/auth";
import { Protected } from "@/components/Protected";
import { inr } from "@/components/finance/FinanceShell";

function GstBillView() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const canWrite = canWriteFinance(user?.role);
  const [inv, setInv] = useState<FinanceInvoice | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(() => {
    if (!id) return;
    setError(null);
    api.getFinanceInvoice(id).then(setInv).catch((e: Error) => setError(e.message));
  }, [id]);

  useEffect(() => {
    reload();
  }, [reload]);

  if (error) {
    return (
      <div className="mx-auto max-w-3xl p-8">
        <p className="text-red-600">{error}</p>
        <Link href="/finance/invoices" className="mt-4 inline-block text-sm text-emerald-700">← Back</Link>
      </div>
    );
  }
  if (!inv) {
    return <div className="flex h-screen items-center justify-center text-slate-400">Loading GST bill…</div>;
  }

  const isInter = inv.supplyType === "INTER";

  return (
    <div className="min-h-screen bg-[#f4f6f9] print:bg-white">
      <div className="mx-auto max-w-4xl px-4 py-6 print:max-w-none print:px-0 print:py-0">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-2 print:hidden">
          <Link href="/finance/invoices" className="text-sm font-medium text-slate-600 hover:text-slate-900">
            ← Invoices
          </Link>
          <div className="flex flex-wrap gap-2">
            {canWrite && (
              <button
                onClick={() => api.queueFinanceInvoiceWhatsapp(inv.id).then(reload).catch((e: Error) => setError(e.message))}
                className="rounded-lg bg-emerald-700 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-800"
              >
                Queue WhatsApp send
              </button>
            )}
            <button
              onClick={() => window.print()}
              className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700"
            >
              Print / PDF
            </button>
          </div>
        </div>

        <article className="rounded-2xl border border-slate-300 bg-white p-6 shadow-sm print:rounded-none print:border-black print:shadow-none sm:p-8">
          <header className="border-b-2 border-slate-900 pb-4">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <div className="text-xs font-semibold uppercase tracking-[0.16em] text-emerald-800">
                  {inv.sellerTradeName || "Trip On Himalaya"}
                </div>
                <h1 className="mt-1 text-xl font-bold text-slate-900">{inv.sellerLegalName}</h1>
                <p className="mt-1 max-w-md text-xs leading-relaxed text-slate-600 whitespace-pre-line">
                  {inv.sellerAddress}
                </p>
                <p className="mt-2 text-xs text-slate-700">
                  <span className="font-semibold">GSTIN:</span> {inv.sellerGstin || "—"}
                  {" · "}
                  <span className="font-semibold">State:</span> {inv.sellerState} ({inv.sellerStateCode})
                </p>
                <p className="text-xs text-slate-600">
                  Ph: {inv.sellerPhone} · {inv.sellerEmail}
                </p>
              </div>
              <div className="text-right">
                <div className="inline-block rounded border-2 border-slate-900 px-3 py-1 text-sm font-bold uppercase tracking-wide">
                  {(inv.invoiceType || "TAX_INVOICE").replaceAll("_", " ")}
                </div>
                <div className="mt-3 font-mono text-lg font-bold text-slate-900">{inv.invoiceNo}</div>
                <div className="mt-1 text-xs text-slate-600">Date: {inv.issueDate}</div>
                <div className="text-xs text-slate-600">Due: {inv.dueDate}</div>
                {inv.reverseCharge && (
                  <div className="mt-2 text-xs font-semibold text-rose-700">Reverse charge: Yes</div>
                )}
              </div>
            </div>
          </header>

          <section className="mt-4 grid gap-4 border-b border-slate-200 pb-4 md:grid-cols-2">
            <div>
              <div className="text-[10px] font-bold uppercase tracking-wide text-slate-400">Bill to</div>
              <div className="mt-1 text-sm font-semibold text-slate-900">{inv.customerName}</div>
              {inv.customerAddress && (
                <p className="mt-1 text-xs whitespace-pre-line text-slate-600">{inv.customerAddress}</p>
              )}
              <p className="mt-2 text-xs text-slate-700">
                <span className="font-semibold">WhatsApp:</span> +91 {inv.customerWhatsapp || "—"}
              </p>
              {inv.customerPhone && (
                <p className="text-xs text-slate-700">
                  <span className="font-semibold">Phone:</span> +91 {inv.customerPhone}
                </p>
              )}
              {inv.customerEmail && (
                <p className="text-xs text-slate-700">
                  <span className="font-semibold">Email:</span> {inv.customerEmail}
                </p>
              )}
              {inv.customerGstin && (
                <p className="text-xs text-slate-700">
                  <span className="font-semibold">GSTIN:</span> {inv.customerGstin}
                </p>
              )}
              <p className="text-xs text-slate-700">
                <span className="font-semibold">State:</span> {inv.customerState} ({inv.customerStateCode})
              </p>
            </div>
            <div>
              <div className="text-[10px] font-bold uppercase tracking-wide text-slate-400">Supply details</div>
              <p className="mt-1 text-xs text-slate-700">
                <span className="font-semibold">Place of supply:</span> {inv.placeOfSupply} ({inv.placeOfSupplyCode})
              </p>
              <p className="text-xs text-slate-700">
                <span className="font-semibold">Supply type:</span> {isInter ? "Inter-state (IGST)" : "Intra-state (CGST + SGST)"}
              </p>
              <p className="text-xs text-slate-700">
                <span className="font-semibold">SAC (default):</span> {inv.sacCode}
              </p>
              <p className="mt-2 text-xs">
                <span className="font-semibold text-slate-700">WhatsApp automation:</span>{" "}
                <span className="rounded bg-amber-50 px-1.5 py-0.5 font-semibold text-amber-800">
                  {inv.whatsappDeliveryStatus}
                </span>
              </p>
            </div>
          </section>

          <section className="mt-4 overflow-x-auto">
            <table className="min-w-full border border-slate-300 text-xs">
              <thead className="bg-slate-100">
                <tr className="text-left">
                  <th className="border border-slate-300 px-2 py-2">#</th>
                  <th className="border border-slate-300 px-2 py-2">Description of services</th>
                  <th className="border border-slate-300 px-2 py-2">HSN/SAC</th>
                  <th className="border border-slate-300 px-2 py-2 text-right">Qty</th>
                  <th className="border border-slate-300 px-2 py-2">Unit</th>
                  <th className="border border-slate-300 px-2 py-2 text-right">Rate</th>
                  <th className="border border-slate-300 px-2 py-2 text-right">Taxable</th>
                  <th className="border border-slate-300 px-2 py-2 text-right">GST%</th>
                </tr>
              </thead>
              <tbody>
                {(inv.lines ?? []).map((l) => (
                  <tr key={l.id ?? l.lineNo}>
                    <td className="border border-slate-300 px-2 py-2">{l.lineNo}</td>
                    <td className="border border-slate-300 px-2 py-2">{l.description}</td>
                    <td className="border border-slate-300 px-2 py-2 font-mono">{l.hsnSac}</td>
                    <td className="border border-slate-300 px-2 py-2 text-right tabular-nums">{l.quantity}</td>
                    <td className="border border-slate-300 px-2 py-2">{l.unit}</td>
                    <td className="border border-slate-300 px-2 py-2 text-right tabular-nums">{inr(l.rate)}</td>
                    <td className="border border-slate-300 px-2 py-2 text-right tabular-nums">{inr(l.taxableAmount)}</td>
                    <td className="border border-slate-300 px-2 py-2 text-right tabular-nums">{l.gstRate}%</td>
                  </tr>
                ))}
                {(inv.lines ?? []).length === 0 && (
                  <tr>
                    <td colSpan={8} className="border border-slate-300 px-2 py-4 text-center text-slate-400">
                      No line items (legacy invoice)
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </section>

          <section className="mt-4 grid gap-4 md:grid-cols-2">
            <div className="text-xs text-slate-600">
              <div className="font-semibold text-slate-800">Amount in words</div>
              <p className="mt-1 italic">{inv.amountInWords || "—"}</p>
              {inv.notes && (
                <>
                  <div className="mt-3 font-semibold text-slate-800">Notes</div>
                  <p className="mt-1 whitespace-pre-line">{inv.notes}</p>
                </>
              )}
              <div className="mt-3 font-semibold text-slate-800">Bank details (for payment)</div>
              <p className="mt-1">
                {inv.sellerBankName}<br />
                A/c: {inv.sellerBankAccount}<br />
                IFSC: {inv.sellerBankIfsc}
              </p>
            </div>
            <div className="border border-slate-300 text-xs">
              <Row label="Taxable value" value={inr(inv.subtotal)} />
              {!isInter && (
                <>
                  <Row label={`CGST @ ${inv.cgstRate ?? 0}%`} value={inr(inv.cgstAmount)} />
                  <Row label={`SGST @ ${inv.sgstRate ?? 0}%`} value={inr(inv.sgstAmount)} />
                </>
              )}
              {isInter && <Row label={`IGST @ ${inv.igstRate ?? 0}%`} value={inr(inv.igstAmount)} />}
              <Row label="Total tax" value={inr(inv.taxAmount)} />
              <div className="flex justify-between bg-slate-900 px-3 py-2 font-bold text-white">
                <span>Grand total</span>
                <span className="tabular-nums">{inr(inv.totalAmount)}</span>
              </div>
            </div>
          </section>

          <footer className="mt-6 border-t border-slate-200 pt-4 text-[11px] text-slate-500">
            <p className="font-semibold text-slate-700">Declaration</p>
            <p className="mt-1">{inv.declaration}</p>
            <div className="mt-8 flex justify-end">
              <div className="text-center">
                <div className="h-10" />
                <div className="border-t border-slate-400 pt-1 font-medium text-slate-800">
                  Authorised signatory
                </div>
                <div>{inv.sellerTradeName}</div>
              </div>
            </div>
            <p className="mt-4 text-center text-[10px] text-slate-400">
              Computer generated GST invoice · Trip On Himalaya Finance ERP · Status: {inv.status}
            </p>
          </footer>
        </article>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b border-slate-200 px-3 py-1.5">
      <span className="text-slate-600">{label}</span>
      <span className="tabular-nums font-medium text-slate-900">{value}</span>
    </div>
  );
}

export default function InvoiceDetailPage() {
  return (
    <Protected allowedRoles={FINANCE_PORTAL}>
      <GstBillView />
    </Protected>
  );
}
