import type { NextConfig } from "next";

// Server-side rewrite target. In Docker Compose this must be the backend
// service hostname (http://backend:8080) — localhost inside the frontend
// container is the frontend itself and yields ECONNREFUSED → 500 on login.
// For local `npm run dev`, default to localhost.
// Accept either an origin ("http://backend:8080") or a base that already
// includes the /api suffix, and normalise to a bare origin so the rewrite can
// append the prefix exactly once.
const RAW_API_TARGET =
  process.env.API_PROXY_TARGET ||
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  "http://localhost:8080/api";
const API_ORIGIN = RAW_API_TARGET.replace(/\/+$/, "").replace(/\/api$/, "");

const nextConfig: NextConfig = {
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${API_ORIGIN}/api/:path*` }];
  },
};

export default nextConfig;