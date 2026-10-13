"use client";

import { Protected } from "@/components/Protected";
import { TreksManagement } from "@/components/treks/TreksManagement";

export default function TripsPage() {
  return (
    <Protected>
      <TreksManagement />
    </Protected>
  );
}
