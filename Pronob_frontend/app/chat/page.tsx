"use client"

import { useAnalysis } from "@/context/analysis-context"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Alert } from "@/components/ui/alert"
import ChatInterface from "@/components/chat-interface"
import Link from "next/link"

export default function ChatPage() {
  const { currentAnalysis } = useAnalysis()

  if (!currentAnalysis) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <Alert variant="warning" title="No Media Selected">
          <p className="mb-4">Please analyze media first before asking queries.</p>
          <Link href="/analyze">
            <Button>Start Analysis</Button>
          </Link>
        </Alert>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-foreground mb-2">Query Assistant</h1>
        <p className="text-foreground-secondary">Ask questions about the analyzed media</p>
      </div>

      <div className="grid lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Ask a Question</CardTitle>
              <CardDescription>Query the analysis results with natural language</CardDescription>
            </CardHeader>
            <CardContent>
              <ChatInterface analysisId="current" />
            </CardContent>
          </Card>
        </div>

        <div>
          <Card>
            <CardHeader>
              <CardTitle>Tips</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-foreground-secondary">
                <li>• "Is there a book named Harry Potter?"</li>
                <li>• "How many people are reading?"</li>
                <li>• "Is there a child in the image?"</li>
                <li>• "What actions are people performing?"</li>
              </ul>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
