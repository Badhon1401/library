"use client"

import { useState } from "react"
import type { AnalysisResult } from "@/context/analysis-context"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card"
import { Button } from "./ui/button"
import { Tabs } from "./ui/tabs"

interface ResultsPanelProps {
  analysis: AnalysisResult
}

export default function ResultsPanel({ analysis }: ResultsPanelProps) {
  const [expandedBook, setExpandedBook] = useState<string | null>(null)

  const handleDownloadPDF = () => {
    const content = generateReportContent()
    const element = document.createElement("a")
    element.setAttribute("href", "data:text/plain;charset=utf-8," + encodeURIComponent(content))
    element.setAttribute("download", "analysis-report.txt")
    element.style.display = "none"
    document.body.appendChild(element)
    element.click()
    document.body.removeChild(element)
  }

  const generateReportContent = () => {
    let report = "LIBRARY VISION ANALYSIS REPORT\n"
    report += "================================\n\n"

    report += "DETECTED BOOKS:\n"
    report += "--------------\n"
    analysis.books.forEach((book, idx) => {
      report += `\n${idx + 1}. ${book.title || "Unknown Title"}\n`
      report += `   Author: ${book.author || "Unknown"}\n`
      report += `   ISBN: ${book.isbn || "N/A"}\n`
      report += `   Confidence: ${(book.confidence * 100).toFixed(1)}%\n`
      if (book.extractedText) {
        report += `   Extracted Text: ${book.extractedText}\n`
      }
    })

    report += "\n\nDETECTED PEOPLE:\n"
    report += "---------------\n"
    analysis.people.forEach((person, idx) => {
      report += `\n${idx + 1}. Age Category: ${person.ageCategory}\n`
      report += `   Action: ${person.action}\n`
      report += `   Confidence: ${(person.confidence * 100).toFixed(1)}%\n`
    })

    return report
  }

  const tabs = [
    {
      label: `Books (${analysis.books.length})`,
      content: (
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {analysis.books.length === 0 ? (
            <p className="text-foreground-secondary text-sm">No books detected</p>
          ) : (
            analysis.books.map((book) => (
              <div
                key={book.id}
                className="bg-background-tertiary rounded-lg p-3 cursor-pointer hover:bg-background-secondary transition-colors"
                onClick={() => setExpandedBook(expandedBook === book.id ? null : book.id)}
              >
                <p className="font-semibold text-foreground text-sm">{book.title || "Unknown Title"}</p>
                {expandedBook === book.id && (
                  <div className="mt-2 text-xs text-foreground-secondary space-y-1">
                    <p>Author: {book.author || "Unknown"}</p>
                    <p>ISBN: {book.isbn || "N/A"}</p>
                    <p>Confidence: {(book.confidence * 100).toFixed(1)}%</p>
                    {book.extractedText && (
                      <p className="italic">
                        Text: {book.extractedText.substring(0, 100)}
                        {book.extractedText.length > 100 ? "..." : ""}
                      </p>
                    )}
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      ),
    },
    {
      label: `People (${analysis.people.length})`,
      content: (
        <div className="space-y-2 max-h-96 overflow-y-auto">
          {analysis.people.length === 0 ? (
            <p className="text-foreground-secondary text-sm">No people detected</p>
          ) : (
            analysis.people.map((person, idx) => (
              <div key={person.id} className="bg-background-tertiary rounded-lg p-3">
                <p className="font-semibold text-foreground text-sm">Person {idx + 1}</p>
                <div className="mt-1 text-xs text-foreground-secondary space-y-1">
                  <p>
                    Age: <span className="text-accent font-medium">{person.ageCategory}</span>
                  </p>
                  <p>Action: {person.action}</p>
                  <p>Confidence: {(person.confidence * 100).toFixed(1)}%</p>
                </div>
              </div>
            ))
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Detection Summary</CardTitle>
        </CardHeader>
        <CardContent>
          <Tabs tabs={tabs} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Export</CardTitle>
          <CardDescription>Download analysis results</CardDescription>
        </CardHeader>
        <CardContent>
          <Button className="w-full" onClick={handleDownloadPDF}>
            Download Report
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
