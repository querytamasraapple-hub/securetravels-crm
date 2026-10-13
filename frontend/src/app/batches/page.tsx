"use client";

import { Protected } from "@/components/Protected";
import { BatchManagement } from "@/components/batches/BatchManagement";

export default function BatchesPage() {
  return (
    <Protected>
      <BatchManagement />
    </Protected>
  );
}
