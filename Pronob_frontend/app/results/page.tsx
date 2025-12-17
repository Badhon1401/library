"use client"

import { useAnalysis } from "@/context/analysis-context"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Alert } from "@/components/ui/alert"
import DetectionViewer from "@/components/detection-viewer"
import ResultsPanel from "@/components/results-panel"
import Link from "next/link"

export default function ResultsPage() {
  const { currentAnalysis } = useAnalysis()

  if (!currentAnalysis) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <Alert variant="warning" title="No Analysis Found">
          <p className="mb-4">No analysis results available. Please upload and analyze media first.</p>
          <Link href="/analyze">
            <Button>Go to Analysis</Button>
          </Link>
        </Alert>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-foreground mb-2">Analysis Results</h1>
        <p className="text-foreground-secondary">Review detection results below</p>
      </div>

      <div className="grid lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Detections</CardTitle>
              <CardDescription>Visual representation of detected books and people</CardDescription>
            </CardHeader>
            <CardContent>
              <DetectionViewer analysis={currentAnalysis} />
            </CardContent>
          </Card>
        </div>

        <div>
          <ResultsPanel analysis={currentAnalysis} />
        </div>
      </div>
    </div>
  )
}
