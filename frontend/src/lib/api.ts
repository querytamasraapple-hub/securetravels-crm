import type { components } from "@/openapi/generated";

export type Lead = components["schemas"]["LeadResponse"];
export type LeadCreatePayload = components["schemas"]["LeadCreateRequest"];
export type LeadUpdatePayload = components["schemas"]["LeadUpdateRequest"];
export type LeadStatusPayload = components["schemas"]["LeadStatusRequest"];
export type LeadActivity = components["schemas"]["LeadActivityResponse"];
export type Trip = components["schemas"]["TripResponse"];
export type TripDetail = components["schemas"]["TripDetailResponse"];
export type TripCreatePayload = components["schemas"]["TripCreateRequest"];
export type TripUpdatePayload = components["schemas"]["TripUpdateRequest"];
export type Batch = components["schemas"]["BatchResponse"];
export type BatchCreatePayload = components["schemas"]["BatchCreateRequest"];
export type BatchUpdatePayload = components["schemas"]["BatchUpdateRequest"];

/** Calendar / dashboard departure row (from GET /api/batches/upcoming). */
export type UpcomingBatch = {
  id: string;
  tripId: string;
  tripName: string;
  tripCategory?: string;
  departureDate: string;
  maxCapacity: number;
  seatsBooked: number;
  available: number;
  fillPercent: number;
  capacityColor?: string;
  guideName?: string | null;
  status: string;
};
/**
 * Guides are the `category = GUIDE` slice of the vendor catalogue. The
 * standalone `/api/guides` endpoints were retired when vendors landed
 * (Module 2), but the trips page still called them, so the guides panel and
 * the batch guide-assignment dropdown were 404ing at runtime. These aliases
 * point the client at the real vendor routes.
 */
export type Guide = components["schemas"]["VendorResponse"];
export type GuideCreatePayload = components["schemas"]["VendorCreateRequest"];
export type GuideUpdatePayload = components["schemas"]["VendorUpdateRequest"];
export type Booking = components["schemas"]["BookingResponse"];
export type BookingCreatePayload = components["schemas"]["BookingCreateRequest"];
export type BookingStatusPayload = components["schemas"]["BookingStatusRequest"];
export type Payment = components["schemas"]["PaymentResponse"];
export type PaymentCreatePayload = components["schemas"]["PaymentCreateRequest"];
export type PaymentStatusPayload = components["schemas"]["PaymentStatusRequest"];
export type PaymentSummary = components["schemas"]["PaymentSummaryResponse"];
export type OpsHandoff = components["schemas"]["OperationsHandoffResponse"];
export type OpsArrangementsPayload = components["schemas"]["OpsArrangementsRequest"];
export type OpsNotePayload = components["schemas"]["OpsNoteRequest"];
export type LoginResponse = components["schemas"]["AuthResponse"];
export type Task = components["schemas"]["TaskResponse"];
export type NotificationItem = components["schemas"]["NotificationResponse"];
export type CustomerListItem = components["schemas"]["CustomerListResponse"];
export type CustomerDetail = components["schemas"]["CustomerDetailResponse"];
export type CustomerTripRow = components["schemas"]["TripRow"];
export type CustomerUpdatePayload = components["schemas"]["CustomerUpdateRequest"];
export type DashboardSummary = components["schemas"]["DashboardSummaryResponse"];
export type DashboardPerformance = components["schemas"]["PerformanceResponse"];
export type TargetRow = components["schemas"]["TargetDto"];
export type TargetsList = components["schemas"]["TargetListResponse"];
export type TargetsProgress = components["schemas"]["TargetProgressResponse"];
export type TargetUpsertPayload = Omit<
  components["schemas"]["TargetUpsertRequest"],
  "userId"
> & { userId?: string | null };
export type SalesUser = { id: string; fullName: string; email: string };

export type TeamUser = {
  id: string;
  email: string;
  fullName: string;
  role: "SALES" | "OPS" | "MANAGER" | "ADMIN" | "CEO" | "FINANCE";
  phone?: string | null;
  active: boolean;
  createdAt?: string;
};

export type FinanceOverview = {
  collectedRevenue: number;
  outstandingReceivables: number;
  overdueAmount: number;
  refundedAmount: number;
  bookingPipeline: number;
  totalExpenses: number;
  netPosition: number;
  collectionRatePct: number;
  completedPaymentCount: number;
  overduePaymentCount: number;
  confirmedBookingCount: number;
  monthlySeries: { month: string; collected: number; expenses: number; outstanding: number }[];
  revenueByTrip: { name: string; amount: number; count: number }[];
  expensesByCategory: { name: string; amount: number; count: number }[];
  paymentsByStatus: { status: string; amount: number; count: number }[];
};

export type FinancePnL = {
  from: string;
  to: string;
  revenue: number;
  expenses: number;
  grossProfit: number;
  marginPct: number;
  expenseLines: { name: string; amount: number; count: number }[];
  revenueByTrip: { name: string; amount: number; count: number }[];
};

export type FinanceExpense = {
  id: string;
  category: string;
  title: string;
  amount: number;
  expenseDate: string;
  tripId?: string | null;
  tripName?: string | null;
  vendorName?: string | null;
  paymentMethod: string;
  status: string;
  notes?: string | null;
  recordedBy?: string | null;
  createdAt?: string;
};

export type FinanceExpenseCreatePayload = {
  category: string;
  title: string;
  amount: number;
  expenseDate: string;
  tripId?: string;
  vendorName?: string;
  paymentMethod?: string;
  status?: string;
  notes?: string;
};

export type FinanceAging = {
  current: number;
  days1to30: number;
  days31to60: number;
  days61to90: number;
  days90plus: number;
  totalOutstanding: number;
  lines: {
    bookingRef: string;
    amountType: string;
    amount: number;
    dueDate?: string | null;
    daysPastDue: number;
    bucket: string;
    status: string;
  }[];
};

export type FinanceCashflow = {
  from: string;
  to: string;
  totalInflow: number;
  totalOutflow: number;
  netCash: number;
  daily: { date: string; inflow: number; outflow: number; net: number }[];
};

export type FinanceTax = {
  from: string;
  to: string;
  taxableRevenue: number;
  outputGst: number;
  estimatedInputCredit: number;
  netGstPayable: number;
  gstRatePct: number;
  taxableByTrip: { name: string; amount: number; count: number }[];
};

export type FinanceTripPnL = {
  trips: {
    tripId: string;
    tripName: string;
    bookingCount: number;
    revenue: number;
    expenses: number;
    payouts: number;
    profit: number;
    marginPct: number;
  }[];
};

export type FinanceInvoiceLine = {
  id?: string;
  lineNo?: number;
  description: string;
  hsnSac: string;
  quantity: number;
  unit: string;
  rate: number;
  taxableAmount?: number;
  gstRate: number;
};

export type FinanceInvoice = {
  id: string;
  invoiceNo: string;
  bookingId?: string | null;
  customerName: string;
  customerEmail?: string | null;
  customerWhatsapp?: string | null;
  customerPhone?: string | null;
  customerGstin?: string | null;
  customerAddress?: string | null;
  customerState?: string | null;
  customerStateCode?: string | null;
  placeOfSupply?: string | null;
  placeOfSupplyCode?: string | null;
  sellerLegalName?: string | null;
  sellerTradeName?: string | null;
  sellerGstin?: string | null;
  sellerAddress?: string | null;
  sellerState?: string | null;
  sellerStateCode?: string | null;
  sellerPhone?: string | null;
  sellerEmail?: string | null;
  sellerBankName?: string | null;
  sellerBankAccount?: string | null;
  sellerBankIfsc?: string | null;
  issueDate: string;
  dueDate: string;
  invoiceType?: string;
  supplyType?: string;
  reverseCharge?: boolean;
  sacCode?: string;
  subtotal: number;
  cgstRate?: number;
  sgstRate?: number;
  igstRate?: number;
  cgstAmount?: number;
  sgstAmount?: number;
  igstAmount?: number;
  taxAmount: number;
  totalAmount: number;
  amountInWords?: string | null;
  currency: string;
  status: string;
  notes?: string | null;
  declaration?: string | null;
  whatsappDeliveryStatus?: string;
  whatsappQueuedAt?: string | null;
  whatsappSentAt?: string | null;
  whatsappLastError?: string | null;
  lines?: FinanceInvoiceLine[];
  createdAt?: string;
};

export type FinanceInvoiceCreatePayload = {
  bookingId?: string;
  customerName: string;
  customerEmail?: string;
  customerWhatsapp: string;
  customerPhone?: string;
  customerGstin?: string;
  customerAddress?: string;
  customerState?: string;
  customerStateCode?: string;
  placeOfSupply?: string;
  placeOfSupplyCode?: string;
  issueDate: string;
  dueDate: string;
  invoiceType?: string;
  supplyType?: string;
  reverseCharge?: boolean;
  sacCode?: string;
  status?: string;
  notes?: string;
  declaration?: string;
  queueWhatsapp?: boolean;
  lines: {
    description: string;
    hsnSac?: string;
    quantity: number;
    unit?: string;
    rate: number;
    gstRate?: number;
  }[];
};

export type FinancePayout = {
  id: string;
  vendorName: string;
  category: string;
  tripId?: string | null;
  tripName?: string | null;
  amount: number;
  payoutDate: string;
  dueDate?: string | null;
  paymentMethod: string;
  status: string;
  referenceNo?: string | null;
  notes?: string | null;
  createdAt?: string;
};

export type FinancePayoutCreatePayload = {
  vendorName: string;
  category: string;
  tripId?: string;
  amount: number;
  payoutDate: string;
  dueDate?: string;
  paymentMethod?: string;
  status?: string;
  referenceNo?: string;
  notes?: string;
};

export type FinanceBudget = {
  periodMonth: string;
  lines: {
    category: string;
    budgetAmount: number;
    actualAmount: number;
    variance: number;
    usedPct: number;
  }[];
  totalBudget: number;
  totalActual: number;
  variance: number;
};

export type TeamUserCreatePayload = {
  email: string;
  fullName: string;
  role: TeamUser["role"];
  phone?: string;
  password: string;
};

export type TeamUserUpdatePayload = {
  fullName?: string;
  role?: TeamUser["role"];
  phone?: string | null;
  active?: boolean;
  password?: string;
};

export type InboxThread = {
  id: string;
  subjectType: "LEAD" | "CUSTOMER" | "BOOKING";
  subjectId: string;
  channel: "WHATSAPP" | "EMAIL" | "SMS";
  customerMobile?: string | null;
  customerName?: string | null;
  status: "OPEN" | "PENDING" | "CLOSED";
  unreadCount: number;
  assignedTo?: string | null;
  lastMessageAt?: string | null;
  lastDirection?: string | null;
  lastPreview?: string | null;
  serviceWindowExpiresAt?: string | null;
  templateSendAllowed?: boolean;
};

export type InboxPage = {
  content: InboxThread[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type InboxListParams = {
  assignedTo?: string;
  status?: string;
  channel?: string;
  unreadOnly?: boolean;
  page?: number;
  size?: number;
};

export type TimelineEntry = {
  seq?: number;
  subjectType?: string;
  subjectId?: string;
  direction?: "INBOUND" | "OUTBOUND" | "SYSTEM";
  channel?: string;
  kind?: string;
  templateCode?: string | null;
  summary?: string | null;
  body?: string | null;
  provider?: string | null;
  customerMobile?: string | null;
  createdAt?: string;
};

// ---- Module 2 reports ----------------------------------------------------
export type SeasonValue = "SPRING" | "MONSOON" | "AUTUMN" | "WINTER";

export type ReportFilterParams = {
  consultantId?: string;
  tripId?: string;
  from?: string;
  to?: string;
  season?: SeasonValue;
  source?: string;
};

export type SalesFunnelData = components["schemas"]["SalesFunnelResponse"];
export type TripPerformanceData = components["schemas"]["TripPerformanceResponse"];
export type TeamPerformanceData = components["schemas"]["TeamPerformanceResponse"];
export type OperationsReadinessData = components["schemas"]["OperationsReadinessResponse"];
export type CustomerInsightsData = components["schemas"]["CustomerInsightsResponse"];
export type AuditSearchData = components["schemas"]["AuditSearchResponse"];

export type AuditSearchParams = {
  q?: string;
  entity?: string;
  actorId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
};

export type NotificationFeed = {
  items: NotificationItem[];
  unread: number;
};

export type TaskListParams = {
  leadId?: string;
  status?: string;
};

export type LeadPage = {
  content: Lead[];
  totalElements: number;
  totalPages: number;
  number: number;
};

export type LeadListParams = {
  status?: string;
  source?: string;
  heat?: string;
  tripId?: string;
  from?: string;
  to?: string;
  travelFrom?: string;
  travelTo?: string;
  ownerId?: string;
  search?: string;
  page?: number;
  size?: number;
};

export type ApiErrorBody = {
  code?: string;
  message?: string;
  fieldErrors?: { field: string; message: string }[];
};

/** Prefer field-level Bean Validation messages over the generic "Check your input". */
export function formatApiError(err: unknown, fallback = "Request failed"): string {
  if (err instanceof ApiClientError) {
    const fields = err.body.fieldErrors?.filter((f) => f.message).map((f) => f.message);
    if (fields && fields.length) return fields.join(" · ");
    if (err.body.message) return err.body.message;
    return err.message || fallback;
  }
  if (err instanceof Error) return err.message;
  return fallback;
}

const TOKENS = {
  access: "st_access",
  refresh: "st_refresh",
};

export const tokenStore = {
  getAccess: () => (typeof window === "undefined" ? null : localStorage.getItem(TOKENS.access)),
  getRefresh: () => (typeof window === "undefined" ? null : localStorage.getItem(TOKENS.refresh)),
  set: (access: string, refresh: string) => {
    localStorage.setItem(TOKENS.access, access);
    localStorage.setItem(TOKENS.refresh, refresh);
  },
  clear: () => {
    localStorage.removeItem(TOKENS.access);
    localStorage.removeItem(TOKENS.refresh);
  },
};

export class ApiClientError extends Error {
  status: number;
  body: ApiErrorBody;
  constructor(status: number, body: ApiErrorBody) {
    super(body.message || `Request failed (${status})`);
    this.name = "ApiClientError";
    this.status = status;
    this.body = body;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const access = tokenStore.getAccess();
  const headers = new Headers(init?.headers);
  headers.set("Content-Type", "application/json");
  if (access) headers.set("Authorization", `Bearer ${access}`);
  const res = await fetch(path, { ...init, headers });

  if (!res.ok) {
    if (res.status === 401) {
      tokenStore.clear();
      if (typeof window !== "undefined") window.location.href = "/login";
    }
    let body: ApiErrorBody = {};
    try {
      body = await res.json();
    } catch {
      /* ignore */
    }
    throw new ApiClientError(res.status, body);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const parts = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && v !== "")
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`);
  return parts.length ? `?${parts.join("&")}` : "";
}

export const api = {
  login: (email: string, password: string) =>
    request<LoginResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }),

  me: () => request<components["schemas"]["UserInfo"]>("/api/auth/me"),

  getLeads: (params: LeadListParams = {}) =>
    request<LeadPage>(`/api/leads${qs({ ...params, size: params.size ?? 20 })}`),

  getLead: (id: string) => request<Lead>(`/api/leads/${id}`),

  createLead: (payload: LeadCreatePayload) =>
    request<Lead>("/api/leads", { method: "POST", body: JSON.stringify(payload) }),

  updateLead: (id: string, payload: LeadUpdatePayload) =>
    request<Lead>(`/api/leads/${id}`, { method: "PATCH", body: JSON.stringify(payload) }),

  updateLeadStatus: (id: string, payload: LeadStatusPayload) =>
    request<Lead>(`/api/leads/${id}/status`, { method: "PATCH", body: JSON.stringify(payload) }),

  getLeadActivity: (id: string) => request<LeadActivity[]>(`/api/leads/${id}/activity`),

  getTrips: (active = true) => request<Trip[]>(`/api/trips${qs({ active })}`),

  getTrip: (id: string) => request<TripDetail>(`/api/trips/${id}`),

  createTrip: (payload: TripCreatePayload) =>
    request<TripDetail>("/api/trips", { method: "POST", body: JSON.stringify(payload) }),

  updateTrip: (id: string, payload: TripUpdatePayload) =>
    request<TripDetail>(`/api/trips/${id}`, { method: "PATCH", body: JSON.stringify(payload) }),

  listBatches: (tripId: string) => request<Batch[]>(`/api/trips/${tripId}/batches`),

  getUpcomingBatches: (params: { from?: string; to?: string; limit?: number } = {}) =>
    request<UpcomingBatch[]>(`/api/batches/upcoming${qs({ ...params })}`),

  createBatch: (tripId: string, payload: BatchCreatePayload) =>
    request<Batch>(`/api/trips/${tripId}/batches`, { method: "POST", body: JSON.stringify(payload) }),

  updateBatch: (id: string, payload: BatchUpdatePayload) =>
    request<Batch>(`/api/batches/${id}`, { method: "PATCH", body: JSON.stringify(payload) }),

  getGuides: (active = true) =>
    request<Guide[]>(`/api/vendors${qs({ category: "GUIDE", active })}`),

  createGuide: (payload: GuideCreatePayload) =>
    request<Guide>("/api/vendors", {
      method: "POST",
      body: JSON.stringify({ ...payload, category: "GUIDE" }),
    }),

  updateGuide: (id: string, payload: GuideUpdatePayload) =>
    request<Guide>(`/api/vendors/${id}`, { method: "PATCH", body: JSON.stringify(payload) }),

  getBookings: (params: { status?: string; tripId?: string; batchId?: string; customerId?: string } = {}) =>
    request<Booking[]>(`/api/bookings${qs({ ...params })}`),

  getBooking: (id: string) => request<Booking>(`/api/bookings/${id}`),

  createBooking: (payload: BookingCreatePayload) =>
    request<Booking>("/api/bookings", { method: "POST", body: JSON.stringify(payload) }),

  updateBookingStatus: (id: string, payload: BookingStatusPayload) =>
    request<Booking>(`/api/bookings/${id}/status`, { method: "PATCH", body: JSON.stringify(payload) }),

  getPayments: (params: { bookingId?: string; status?: string; amountType?: string } = {}) =>
    request<Payment[]>(`/api/payments${qs({ ...params })}`),

  getPaymentSummary: (bookingId: string) =>
    request<PaymentSummary>(`/api/payments/booking/${bookingId}/summary`),

  createPayment: (payload: PaymentCreatePayload) =>
    request<Payment>("/api/payments", { method: "POST", body: JSON.stringify(payload) }),

  updatePaymentStatus: (id: string, payload: PaymentStatusPayload) =>
    request<Payment>(`/api/payments/${id}/status`, { method: "PATCH", body: JSON.stringify(payload) }),

  getTasks: (params: TaskListParams = {}) =>
    request<Task[]>(`/api/tasks${qs({ ...params })}`),

  completeTask: (id: string) =>
    request<Task>(`/api/tasks/${id}/complete`, { method: "PATCH" }),

  getOperations: (params: {
    travelDateFrom?: string;
    travelDateTo?: string;
    hotelStatus?: string;
    transportStatus?: string;
    paymentStatus?: string;
  } = {}) => request<OpsHandoff[]>(`/api/operations${qs({ ...params })}`),

  getOperation: (id: string) => request<OpsHandoff>(`/api/operations/${id}`),

  updateOperationArrangements: (id: string, payload: OpsArrangementsPayload) =>
    request<OpsHandoff>(`/api/operations/${id}/arrangements`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    }),

  addOperationNote: (id: string, payload: OpsNotePayload) =>
    request<OpsHandoff>(`/api/operations/${id}/notes`, {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  generateTripSheet: (id: string) =>
    request<OpsHandoff>(`/api/operations/${id}/trip-sheet`, { method: "POST" }),

  getNotifications: (unreadOnly = false) =>
    request<NotificationFeed>(`/api/notifications${qs({ unreadOnly })}`),

  markNotificationRead: (id: string) =>
    request<NotificationItem>(`/api/notifications/${id}/read`, { method: "PATCH" }),

  markAllNotificationsRead: () =>
    request<{ updated: number }>("/api/notifications/read-all", { method: "POST" }),

  getCustomers: (search?: string) =>
    request<CustomerListItem[]>(`/api/customers${qs({ search })}`),

  getCustomer: (id: string) => request<CustomerDetail>(`/api/customers/${id}`),

  updateCustomer: (id: string, payload: CustomerUpdatePayload) =>
    request<CustomerDetail>(`/api/customers/${id}`, { method: "PATCH", body: JSON.stringify(payload) }),

  getDashboardSummary: (period?: "today" | "month") =>
    request<DashboardSummary>(`/api/dashboard/summary${qs({ period })}`),

  getDashboardPerformance: (month?: string) =>
    request<DashboardPerformance>(`/api/dashboard/performance${qs({ month })}`),

  getTargets: (month?: string) => request<TargetsList>(`/api/targets${qs({ month })}`),

  getTargetProgress: (month?: string) =>
    request<TargetsProgress>(`/api/targets/progress${qs({ month })}`),

  upsertTarget: (payload: TargetUpsertPayload) =>
    request<TargetRow>("/api/targets", { method: "PUT", body: JSON.stringify(payload) }),

  deleteTarget: (id: string) => request<void>(`/api/targets/${id}`, { method: "DELETE" }),

  getSalesUsers: () => request<SalesUser[]>("/api/users/sales"),

  getTeamUsers: (params: { role?: string; active?: boolean } = {}) =>
    request<TeamUser[]>(`/api/users${qs({ ...params })}`),

  createTeamUser: (payload: TeamUserCreatePayload) =>
    request<TeamUser>("/api/users", { method: "POST", body: JSON.stringify(payload) }),

  updateTeamUser: (id: string, payload: TeamUserUpdatePayload) =>
    request<TeamUser>(`/api/users/${id}`, { method: "PATCH", body: JSON.stringify(payload) }),

  getSalesFunnel: (params: ReportFilterParams = {}) =>
    request<SalesFunnelData>(`/api/analytics/funnel${qs(params)}`),

  getTripPerformance: (params: ReportFilterParams = {}) =>
    request<TripPerformanceData>(`/api/analytics/trips${qs(params)}`),

  getTeamPerformance: (params: ReportFilterParams = {}) =>
    request<TeamPerformanceData>(`/api/analytics/team${qs(params)}`),

  getOperationsReadiness: (params: ReportFilterParams = {}) =>
    request<OperationsReadinessData>(`/api/analytics/operations${qs(params)}`),

  getCustomerInsights: (params: ReportFilterParams = {}) =>
    request<CustomerInsightsData>(`/api/analytics/customers${qs(params)}`),

  getAuditLog: (params: AuditSearchParams = {}) =>
    request<AuditSearchData>(`/api/analytics/audit${qs(params)}`),

  getInboxThreads: (params: InboxListParams = {}) =>
    request<InboxPage>(`/api/v1/threads${qs({ ...params })}`),

  getInboxThread: (id: string) => request<InboxThread>(`/api/v1/threads/${id}`),

  markInboxThreadRead: (id: string) =>
    request<InboxThread>(`/api/v1/threads/${id}/read`, { method: "POST" }),

  updateInboxThread: (id: string, payload: { assignedTo?: string | null; status?: string }) =>
    request<InboxThread>(`/api/v1/threads/${id}`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    }),

  getTimeline: (subjectType: string, subjectId: string) =>
    request<TimelineEntry[]>(`/api/timeline/${subjectType}/${subjectId}`),

  getFinanceOverview: (params: { from?: string; to?: string } = {}) =>
    request<FinanceOverview>(`/api/finance/overview${qs(params)}`),

  getFinancePnL: (params: { from?: string; to?: string } = {}) =>
    request<FinancePnL>(`/api/finance/pnl${qs(params)}`),

  getFinanceExpenses: (params: { from?: string; to?: string } = {}) =>
    request<FinanceExpense[]>(`/api/finance/expenses${qs(params)}`),

  createFinanceExpense: (payload: FinanceExpenseCreatePayload) =>
    request<FinanceExpense>("/api/finance/expenses", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  voidFinanceExpense: (id: string) =>
    request<FinanceExpense>(`/api/finance/expenses/${id}/void`, { method: "POST" }),

  getFinanceAging: () => request<FinanceAging>("/api/finance/aging"),

  getFinanceCashflow: (params: { from?: string; to?: string } = {}) =>
    request<FinanceCashflow>(`/api/finance/cashflow${qs(params)}`),

  getFinanceTax: (params: { from?: string; to?: string } = {}) =>
    request<FinanceTax>(`/api/finance/tax${qs(params)}`),

  getFinanceTripPnL: () => request<FinanceTripPnL>("/api/finance/trip-pnl"),

  getFinanceInvoices: () => request<FinanceInvoice[]>("/api/finance/invoices"),

  getFinanceInvoice: (id: string) => request<FinanceInvoice>(`/api/finance/invoices/${id}`),

  createFinanceInvoice: (payload: FinanceInvoiceCreatePayload) =>
    request<FinanceInvoice>("/api/finance/invoices", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  updateFinanceInvoiceStatus: (id: string, status: string) =>
    request<FinanceInvoice>(`/api/finance/invoices/${id}/status`, {
      method: "POST",
      body: JSON.stringify({ status }),
    }),

  queueFinanceInvoiceWhatsapp: (id: string) =>
    request<FinanceInvoice>(`/api/finance/invoices/${id}/queue-whatsapp`, { method: "POST" }),

  getFinancePayouts: () => request<FinancePayout[]>("/api/finance/payouts"),

  createFinancePayout: (payload: FinancePayoutCreatePayload) =>
    request<FinancePayout>("/api/finance/payouts", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  updateFinancePayoutStatus: (id: string, status: string) =>
    request<FinancePayout>(`/api/finance/payouts/${id}/status`, {
      method: "POST",
      body: JSON.stringify({ status }),
    }),

  getFinanceBudgets: (month?: string) =>
    request<FinanceBudget>(`/api/finance/budgets${qs({ month })}`),

  upsertFinanceBudget: (payload: {
    periodMonth: string;
    category: string;
    budgetAmount: number;
    notes?: string;
  }) =>
    request<FinanceBudget>("/api/finance/budgets", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  getVendors: (params: { category?: string; active?: boolean } = {}) =>
    request<Guide[]>(`/api/vendors${qs({ ...params, active: params.active ?? true })}`),

  createVendor: (payload: Record<string, unknown>) =>
    request<Guide>("/api/vendors", { method: "POST", body: JSON.stringify(payload) }),

  getBasecampAttendance: (params: { from?: string; to?: string } = {}) =>
    request<Record<string, unknown>[]>(`/api/basecamp/attendance${qs(params)}`),

  upsertBasecampAttendance: (payload: Record<string, unknown>) =>
    request<Record<string, unknown>>("/api/basecamp/attendance", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  getBasecampLeave: () => request<Record<string, unknown>[]>("/api/basecamp/leave"),

  createBasecampLeave: (payload: Record<string, unknown>) =>
    request<Record<string, unknown>>("/api/basecamp/leave", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  reviewBasecampLeave: (id: string, status: string, note?: string) =>
    request<Record<string, unknown>>(`/api/basecamp/leave/${id}/review`, {
      method: "POST",
      body: JSON.stringify({ status, note }),
    }),

  getBasecampPayroll: (month?: string) =>
    request<Record<string, unknown>[]>(`/api/basecamp/payroll${qs({ month })}`),

  upsertBasecampPayroll: (payload: Record<string, unknown>) =>
    request<Record<string, unknown>>("/api/basecamp/payroll", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  getBasecampInventory: () => request<Record<string, unknown>[]>("/api/basecamp/inventory"),

  createBasecampInventory: (payload: Record<string, unknown>) =>
    request<Record<string, unknown>>("/api/basecamp/inventory", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  adjustBasecampInventory: (id: string, quantity: number) =>
    request<Record<string, unknown>>(`/api/basecamp/inventory/${id}/quantity`, {
      method: "POST",
      body: JSON.stringify({ quantity }),
    }),

  getBasecampVisitors: () => request<Record<string, unknown>[]>("/api/basecamp/visitors"),

  checkInBasecampVisitor: (payload: Record<string, unknown>) =>
    request<Record<string, unknown>>("/api/basecamp/visitors", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  checkOutBasecampVisitor: (id: string) =>
    request<Record<string, unknown>>(`/api/basecamp/visitors/${id}/checkout`, { method: "POST" }),

  getBasecampSettings: () => request<Record<string, string>>("/api/basecamp/settings"),

  updateBasecampSetting: (key: string, value: string) =>
    request<Record<string, string>>("/api/basecamp/settings", {
      method: "POST",
      body: JSON.stringify({ key, value }),
    }),
};