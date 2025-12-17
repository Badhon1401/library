"use client"

import type React from "react"

import { Analytics } from "@vercel/analytics/next"
import { AnalysisProvider } from "@/context/analysis-context"

export default function ClientLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <>
      <AnalysisProvider>{children}</AnalysisProvider>
      <Analytics />
    </>
  )
}
