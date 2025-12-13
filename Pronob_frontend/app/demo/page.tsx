"use client"

import { useEffect } from "react"
import { useAnalysis } from "@/context/analysis-context"
import { mockAnalysisResult } from "@/lib/mock-data"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import DetectionViewer from "@/components/detection-viewer"
import ResultsPanel from "@/components/results-panel"
import Link from "next/link"

export default function DemoPage() {
  const { setCurrentAnalysis } = useAnalysis()

  useEffect(() => {
    setCurrentAnalysis(mockAnalysisResult)
  }, [setCurrentAnalysis])

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-foreground mb-2">Demo Analysis Results</h1>
        <p className="text-foreground-secondary">
          This is a demo showing how the detection results and analysis interface work
        </p>
      </div>

      <div className="grid lg:grid-cols-3 gap-8 mb-8">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Detections</CardTitle>
              <CardDescription>Visual representation of detected books and people</CardDescription>
            </CardHeader>
            <CardContent>
              <DetectionViewer analysis={mockAnalysisResult} />
            </CardContent>
          </Card>
        </div>

        <div>
          <ResultsPanel analysis={mockAnalysisResult} />
        </div>
      </div>

      <Card>
        <CardContent className="pt-6">
          <p className="text-foreground-secondary mb-4">
            This demo shows sample detection results. To analyze real media:
          </p>
          <Link href="/analyze">
            <Button>Upload Real Media</Button>
          </Link>
        </CardContent>
      </Card>
    </div>
  )
}
