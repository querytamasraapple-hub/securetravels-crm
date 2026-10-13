/**
 * Role-based access for the CRM UI + Finance ERP portal.
 * Primary nav mirrors Basecamp OS (screenshot); extras live under Workspace.
 * Backend still enforces mutations.
 */

export type Role = "SALES" | "OPS" | "MANAGER" | "ADMIN" | "CEO" | "FINANCE";

export const CRM_STAFF: Role[] = ["SALES", "OPS", "MANAGER", "ADMIN", "CEO"];
export const ALL_STAFF: Role[] = [...CRM_STAFF, "FINANCE"];
export const SALES_DESK: Role[] = ["SALES", "MANAGER", "ADMIN", "CEO"];
export const OPS_DESK: Role[] = ["OPS", "MANAGER", "ADMIN", "CEO"];
export const MANAGERS: Role[] = ["MANAGER", "ADMIN", "CEO"];
export const ADMINS: Role[] = ["ADMIN", "CEO"];
export const FINANCE_PORTAL: Role[] = ["FINANCE", "MANAGER", "ADMIN", "CEO"];
export const FINANCE_WRITE: Role[] = ["FINANCE", "ADMIN", "CEO"];
export const PAYROLL_ROLES: Role[] = ["MANAGER", "ADMIN", "CEO", "FINANCE"];
export const HR_ROLES: Role[] = CRM_STAFF;

/** Icon keys rendered in Sidebar (inline SVGs). */
export type NavIcon =
  | "dashboard"
  | "leads"
  | "bookings"
  | "treks"
  | "batches"
  | "customers"
  | "payments"
  | "expenses"
  | "staff"
  | "attendance"
  | "vendors"
  | "documents"
  | "inventory"
  | "payroll"
  | "leave"
  | "analytics"
  | "visitors"
  | "notifications"
  | "settings"
  | "audit"
  | "tasks"
  | "messages"
  | "ops"
  | "calendar"
  | "guides"
  | "finance";

export type NavItem = {
  href: string;
  label: string;
  roles: Role[];
  icon?: NavIcon;
};

export type NavSection = {
  id: string;
  label: string;
  items: NavItem[];
};

/** Basecamp OS sections — matches screenshot. FINANCE role never sees this. */
export const NAV_SECTIONS: NavSection[] = [
  {
    id: "overview",
    label: "Overview",
    items: [
      { href: "/dashboard", label: "Dashboard", roles: CRM_STAFF, icon: "dashboard" },
    ],
  },
  {
    id: "sales",
    label: "Leads & Sales",
    items: [
      { href: "/leads", label: "Leads", roles: SALES_DESK, icon: "leads" },
      { href: "/deals", label: "Deals / Opportunities", roles: MANAGERS, icon: "payments" },
      { href: "/activities", label: "Activities", roles: MANAGERS, icon: "tasks" },
      { href: "/forecasting", label: "Forecasting", roles: MANAGERS, icon: "analytics" },
      { href: "/bookings", label: "Bookings", roles: CRM_STAFF, icon: "bookings" },
      { href: "/customers", label: "Customers", roles: CRM_STAFF, icon: "customers" },
    ],
  },
  {
    id: "treks",
    label: "Treks & Packages",
    items: [
      { href: "/trips", label: "Manage Treks", roles: CRM_STAFF, icon: "treks" },
      { href: "/trips/packages", label: "Manage Packages", roles: CRM_STAFF, icon: "bookings" },
      { href: "/trips/itineraries", label: "Itineraries & Days", roles: CRM_STAFF, icon: "calendar" },
      { href: "/batches", label: "Batch Management", roles: CRM_STAFF, icon: "batches" },
      { href: "/trips/pricing", label: "Pricing & Discounts", roles: MANAGERS, icon: "payments" },
      { href: "/trips/inclusions", label: "Inclusions & Exclusions", roles: CRM_STAFF, icon: "documents" },
      { href: "/trips/highlights", label: "Highlights & FAQs", roles: MANAGERS, icon: "analytics" },
      { href: "/trips/media", label: "Images & Media", roles: MANAGERS, icon: "documents" },
      { href: "/trips/seo", label: "SEO & Content", roles: MANAGERS, icon: "documents" },
      { href: "/trips/reviews", label: "Reviews & Ratings", roles: MANAGERS, icon: "customers" },
      { href: "/trips/categories", label: "Categories & Tags", roles: MANAGERS, icon: "settings" },
      { href: "/trips/bulk", label: "Clone & Bulk Actions", roles: MANAGERS, icon: "tasks" },
      { href: "/trips/map", label: "Map & Route Builder", roles: MANAGERS, icon: "ops" },
    ],
  },
  {
    id: "marketing",
    label: "Marketing",
    items: [
      { href: "/campaigns", label: "Campaigns", roles: MANAGERS, icon: "leads" },
      { href: "/ad-channels", label: "Ad Channels", roles: MANAGERS, icon: "analytics" },
      { href: "/landing-pages", label: "Landing Pages", roles: MANAGERS, icon: "documents" },
      { href: "/forms", label: "Forms & Lead Capture", roles: MANAGERS, icon: "ops" },
      { href: "/automations", label: "Automation", roles: MANAGERS, icon: "tasks" },
      { href: "/messages", label: "WhatsApp & Email", roles: CRM_STAFF, icon: "messages" },
    ],
  },
  {
    id: "operations",
    label: "Operations",
    items: [
      { href: "/operations", label: "Overview", roles: OPS_DESK, icon: "ops" },
      { href: "/operations/departures", label: "Departures & Trips", roles: OPS_DESK, icon: "batches" },
      { href: "/operations/itinerary", label: "Trip Itinerary Management", roles: OPS_DESK, icon: "bookings" },
      { href: "/batches", label: "Batch & Group Management", roles: OPS_DESK, icon: "batches" },
      { href: "/operations/transport", label: "Transportation", roles: OPS_DESK, icon: "vendors" },
      { href: "/operations/stay", label: "Stay & Accommodation", roles: OPS_DESK, icon: "documents" },
      { href: "/guides", label: "Guide & Staff Management", roles: OPS_DESK, icon: "guides" },
      { href: "/inventory", label: "Inventory & Equipment", roles: OPS_DESK, icon: "inventory" },
      { href: "/operations/permits", label: "Permits & Compliance", roles: OPS_DESK, icon: "documents" },
      { href: "/operations/travel-desk", label: "Travel Desk & Support", roles: OPS_DESK, icon: "ops" },
      { href: "/operations/on-trip", label: "On-Trip Operations", roles: OPS_DESK, icon: "messages" },
      { href: "/operations/incidents", label: "Incidents & Risk", roles: OPS_DESK, icon: "attendance" },
      { href: "/documents", label: "Documents & Waivers", roles: OPS_DESK, icon: "documents" },
      { href: "/operations/desk", label: "Bookings Operations", roles: OPS_DESK, icon: "bookings" },
      { href: "/operations/safety", label: "Risk & Safety", roles: OPS_DESK, icon: "audit" },
      { href: "/reports", label: "Reports & Analytics", roles: OPS_DESK, icon: "analytics" },
      { href: "/calendar", label: "Calendar & Tasks", roles: OPS_DESK, icon: "calendar" },
      { href: "/tasks", label: "Tasks & Follow Ups", roles: OPS_DESK, icon: "tasks" },
      { href: "/team", label: "Team Management", roles: MANAGERS, icon: "staff" },
      { href: "/expenses", label: "Expenses", roles: MANAGERS, icon: "expenses" },
      { href: "/attendance", label: "Attendance", roles: HR_ROLES, icon: "attendance" },
      { href: "/vendors", label: "Vendors", roles: OPS_DESK, icon: "vendors" },
      { href: "/payroll", label: "Payroll", roles: ADMINS, icon: "payroll" },
      { href: "/leave", label: "Leave", roles: HR_ROLES, icon: "leave" },
      { href: "/payments", label: "Payments", roles: CRM_STAFF, icon: "payments" },
    ],
  },
  {
    id: "intelligence",
    label: "Intelligence",
    items: [
      { href: "/reports", label: "Reports & Analytics", roles: CRM_STAFF, icon: "analytics" },
      { href: "/integrations", label: "Integrations", roles: MANAGERS, icon: "settings" },
      { href: "/visitors", label: "Visitors", roles: OPS_DESK, icon: "visitors" },
      { href: "/notifications", label: "Notifications", roles: CRM_STAFF, icon: "notifications" },
      { href: "/settings", label: "Settings", roles: CRM_STAFF, icon: "settings" },
      { href: "/audit", label: "Audit log", roles: ADMINS, icon: "audit" },
    ],
  },
  {
    id: "finance",
    label: "Finance & ERP",
    items: [
      { href: "/finance", label: "Finance Dashboard", roles: MANAGERS, icon: "finance" },
      { href: "/finance/accounting", label: "Accounting", roles: MANAGERS, icon: "payments" },
      { href: "/finance/payments", label: "Income & Collections", roles: MANAGERS, icon: "payments" },
      { href: "/finance/expenses", label: "Expenses & Bills", roles: MANAGERS, icon: "expenses" },
      { href: "/finance/payouts", label: "Vendor & Payouts", roles: MANAGERS, icon: "vendors" },
      { href: "/finance/tax", label: "GST & Tax Compliance", roles: MANAGERS, icon: "documents" },
      { href: "/finance/payroll", label: "Payroll & HR", roles: MANAGERS, icon: "payroll" },
      { href: "/finance/inventory", label: "Inventory & Assets", roles: MANAGERS, icon: "inventory" },
      { href: "/finance/budgets", label: "Budgeting & Forecasting", roles: MANAGERS, icon: "analytics" },
      { href: "/finance/profitability", label: "Profitability & Analytics", roles: MANAGERS, icon: "analytics" },
      { href: "/finance/branches", label: "Multi-Company / Branches", roles: MANAGERS, icon: "settings" },
      { href: "/finance/reports", label: "Reports", roles: MANAGERS, icon: "documents" },
    ],
  },
  {
    id: "workspace",
    label: "Workspace",
    items: [
      { href: "/operations/desk", label: "Ops handoff desk", roles: CRM_STAFF, icon: "ops" },
      { href: "/guides", label: "Guides", roles: CRM_STAFF, icon: "guides" },
      { href: "/support", label: "Customer Support", roles: MANAGERS, icon: "customers" },
      { href: "/marketing", label: "Marketing hub", roles: MANAGERS, icon: "analytics" },
    ],
  },
];

/** Manager Basecamp — full product nav from Trekora-style screenshot. */
const MANAGER_SECTION_IDS = new Set([
  "overview",
  "sales",
  "treks",
  "marketing",
  "operations",
  "finance",
  "intelligence",
]);

/** Horizontal tabs on Treks & Packages screens. */
export const TREKS_ERP_TABS: { href: string; label: string }[] = [
  { href: "/trips", label: "Overview" },
  { href: "/trips/packages", label: "Packages" },
  { href: "/trips/itineraries", label: "Itineraries" },
  { href: "/batches", label: "Batch Management" },
  { href: "/trips/media", label: "Images & Media" },
  { href: "/trips/reviews", label: "Reviews" },
  { href: "/trips/faqs", label: "FAQs" },
  { href: "/trips/highlights", label: "Highlights" },
  { href: "/trips/seo", label: "SEO & Content" },
  { href: "/trips/categories", label: "Settings" },
];

/** Operations team — Dashboard + Operations + essential tools. */
const OPS_SECTION_IDS = new Set(["overview", "operations", "intelligence"]);
const OPS_HIDDEN_HREFS = new Set([
  "/finance",
  "/audit",
  "/visitors",
  "/integrations",
  "/payroll",
  "/expenses",
  "/team",
  "/payments",
  "/leave",
]);

/** Sales / STAFF Basecamp screenshot — pipeline + notifications + settings. */
const SALES_SECTION_IDS = new Set(["overview", "sales", "treks", "intelligence"]);
const SALES_HIDDEN_HREFS = new Set([
  "/reports",
  "/visitors",
  "/audit",
  "/payroll",
  "/expenses",
  "/team",
  "/attendance",
  "/vendors",
  "/documents",
  "/inventory",
  "/leave",
  "/customers",
  "/payments",
  "/tasks",
  "/messages",
  "/operations",
  "/calendar",
  "/guides",
  "/finance",
  "/deals",
  "/activities",
  "/forecasting",
  "/campaigns",
  "/ad-channels",
  "/landing-pages",
  "/forms",
  "/automations",
  "/integrations",
  "/support",
  "/marketing",
]);

/** Flat list for route guards (derived from sections). */
export const NAV_ITEMS: NavItem[] = NAV_SECTIONS.flatMap((s) => s.items);

/** Finance ERP portal nav — matches Trekora Finance & ERP screenshot. */
export const FINANCE_NAV_ITEMS: NavItem[] = [
  { href: "/finance", label: "Finance Dashboard", roles: FINANCE_PORTAL, icon: "finance" },
  { href: "/finance/accounting", label: "Accounting", roles: FINANCE_PORTAL, icon: "payments" },
  { href: "/finance/payments", label: "Income & Collections", roles: FINANCE_PORTAL, icon: "payments" },
  { href: "/finance/expenses", label: "Expenses & Bills", roles: FINANCE_PORTAL, icon: "expenses" },
  { href: "/finance/payouts", label: "Vendor & Payouts", roles: FINANCE_PORTAL, icon: "vendors" },
  { href: "/finance/tax", label: "GST & Tax Compliance", roles: FINANCE_PORTAL, icon: "documents" },
  { href: "/finance/payroll", label: "Payroll & HR", roles: FINANCE_PORTAL, icon: "payroll" },
  { href: "/finance/inventory", label: "Inventory & Assets", roles: FINANCE_PORTAL, icon: "inventory" },
  { href: "/finance/budgets", label: "Budgeting & Forecasting", roles: FINANCE_PORTAL, icon: "analytics" },
  { href: "/finance/profitability", label: "Profitability & Analytics", roles: FINANCE_PORTAL, icon: "analytics" },
  { href: "/finance/branches", label: "Multi-Company / Branches", roles: FINANCE_PORTAL, icon: "settings" },
  { href: "/finance/reports", label: "Reports", roles: FINANCE_PORTAL, icon: "documents" },
  { href: "/finance/aging", label: "AR Aging", roles: FINANCE_PORTAL, icon: "payments" },
  { href: "/finance/invoices", label: "Invoices", roles: FINANCE_PORTAL, icon: "documents" },
  { href: "/finance/cashflow", label: "Cash Flow", roles: FINANCE_PORTAL, icon: "analytics" },
  { href: "/finance/trips", label: "Trek P&L", roles: FINANCE_PORTAL, icon: "treks" },
  { href: "/finance/pnl", label: "P&L Statement", roles: FINANCE_PORTAL, icon: "analytics" },
];

/** Horizontal ERP tabs on Finance Dashboard (subset of portal modules). */
export const FINANCE_ERP_TABS: { href: string; label: string }[] = [
  { href: "/finance", label: "Overview" },
  { href: "/finance/payments", label: "Income & Collections" },
  { href: "/finance/expenses", label: "Expenses & Bills" },
  { href: "/finance/payouts", label: "Vendor & Payouts" },
  { href: "/finance/accounting", label: "Accounting" },
  { href: "/finance/tax", label: "GST & Taxes" },
  { href: "/finance/payroll", label: "Payroll" },
  { href: "/finance/inventory", label: "Inventory & Assets" },
  { href: "/finance/budgets", label: "Budgeting" },
  { href: "/finance/reports", label: "Reports" },
];

export function assignableRoles(actorRole: string | null | undefined): Role[] {
  if (actorRole === "CEO") return ["SALES", "OPS", "MANAGER", "ADMIN", "CEO", "FINANCE"];
  if (actorRole === "ADMIN") return ["SALES", "OPS", "MANAGER", "ADMIN", "FINANCE"];
  if (actorRole === "MANAGER") return ["SALES", "OPS"];
  return [];
}

export const canManageTeam = (role: string | null | undefined) => hasRole(role, MANAGERS);

function asRole(role: string | null | undefined): Role | null {
  if (!role) return null;
  return ALL_STAFF.includes(role as Role) ? (role as Role) : null;
}

export function navSectionsForRole(role: string | null | undefined): NavSection[] {
  const r = asRole(role);
  if (!r || r === "FINANCE") return [];
  return NAV_SECTIONS.map((section) => ({
    ...section,
    items: section.items.filter((item) => {
      if (!item.roles.includes(r)) return false;
      // Manager menu matches product screenshot: hide Payroll / Audit.
      if (r === "MANAGER" && (item.href === "/payroll" || item.href === "/audit")) return false;
      // Sales / STAFF menu matches product screenshot.
      if (r === "SALES" && SALES_HIDDEN_HREFS.has(item.href)) return false;
      if (r === "OPS" && OPS_HIDDEN_HREFS.has(item.href)) return false;
      return true;
    }),
  }))
    .filter((section) => {
      if (r === "MANAGER" && !MANAGER_SECTION_IDS.has(section.id)) return false;
      if (r === "SALES" && !SALES_SECTION_IDS.has(section.id)) return false;
      if (r === "OPS" && !OPS_SECTION_IDS.has(section.id)) return false;
      return section.items.length > 0;
    });
}

/** Display label in sidebar footer (sales screenshot shows STAFF). */
export function roleDisplayLabel(role: string | null | undefined): string {
  if (role === "SALES") return "STAFF";
  if (role === "OPS") return "Operations Team";
  return role ?? "";
}

export function navForRole(role: string | null | undefined): NavItem[] {
  return navSectionsForRole(role).flatMap((s) => s.items);
}

export function financeNavForRole(role: string | null | undefined): NavItem[] {
  const r = asRole(role);
  if (!r) return [];
  return FINANCE_NAV_ITEMS.filter((item) => item.roles.includes(r));
}

export function homePathForRole(role: string | null | undefined): string {
  if (role === "FINANCE") return "/finance";
  return navForRole(role)[0]?.href ?? "/dashboard";
}

export function loginPathForRole(role: string | null | undefined): string {
  return role === "FINANCE" ? "/finance/login" : "/login";
}

export function isFinancePath(pathname: string): boolean {
  return pathname === "/finance" || pathname.startsWith("/finance/");
}

export function canAccessPath(role: string | null | undefined, pathname: string): boolean {
  const r = asRole(role);
  if (!r) return false;
  if (pathname === "/finance/login") return true;
  if (isFinancePath(pathname)) {
    return FINANCE_PORTAL.includes(r);
  }
  if (r === "FINANCE") return false;

  const matches = NAV_ITEMS.filter(
    (n) => pathname === n.href || pathname.startsWith(`${n.href}/`),
  ).sort((a, b) => b.href.length - a.href.length);
  const item = matches[0];
  if (!item) return true;
  return item.roles.includes(r);
}

export function hasRole(role: string | null | undefined, allowed: Role[]): boolean {
  const r = asRole(role);
  return !!r && allowed.includes(r);
}

export const canWriteLeads = (role: string | null | undefined) => hasRole(role, SALES_DESK);
export const canWritePayments = (role: string | null | undefined) =>
  hasRole(role, [...SALES_DESK, "FINANCE"]);
export const canWriteOperations = (role: string | null | undefined) => hasRole(role, OPS_DESK);
export const canManageCatalogue = (role: string | null | undefined) => hasRole(role, MANAGERS);
export const canManageTargets = (role: string | null | undefined) => hasRole(role, MANAGERS);
export const canEditCustomerFlags = (role: string | null | undefined) => hasRole(role, MANAGERS);
export const canViewAudit = (role: string | null | undefined) => hasRole(role, ADMINS);
export const canWriteFinance = (role: string | null | undefined) => hasRole(role, FINANCE_WRITE);
export const canAccessFinance = (role: string | null | undefined) => hasRole(role, FINANCE_PORTAL);
export const canEditPayroll = (role: string | null | undefined) => hasRole(role, ADMINS);
export const canEditSettings = (role: string | null | undefined) => hasRole(role, ADMINS);
