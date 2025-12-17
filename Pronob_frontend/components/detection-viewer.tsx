"use client"

import { useState } from "react"
import type { AnalysisResult } from "@/context/analysis-context"
import { BoundingBox } from "./ui/bounding-box"
import { Button } from "./ui/button"

interface DetectionViewerProps {
  analysis: AnalysisResult
}

const BOOK_COLOR = "rgb(99, 102, 241)" // primary color
const PERSON_COLOR = "rgb(0, 217, 255)" // accent color

export default function DetectionViewer({ analysis }: DetectionViewerProps) {
  const [showBooks, setShowBooks] = useState(true)
  const [showPeople, setShowPeople] = useState(true)

  if (!analysis.mediaUrl) {
    return <div className="text-center py-8 text-foreground-secondary">No media available to display</div>
  }

  const containerWidth = 800
  const containerHeight = 600

  return (
    <div className="space-y-4">
      {/* Controls */}
      <div className="flex gap-2 flex-wrap">
        <Button variant={showBooks ? "primary" : "outline"} size="sm" onClick={() => setShowBooks(!showBooks)}>
          Books ({analysis.books.length})
        </Button>
        <Button variant={showPeople ? "primary" : "outline"} size="sm" onClick={() => setShowPeople(!showPeople)}>
          People ({analysis.people.length})
        </Button>
      </div>

      {/* Media Viewer with Bounding Boxes */}
      <div className="relative w-full bg-background-tertiary rounded-lg overflow-hidden">
        <div
          style={{
            width: "100%",
            paddingBottom: "75%",
            position: "relative",
            backgroundColor: "#000",
          }}
        >
          {analysis.mediaType === "image" ? (
            <img
              src={analysis.mediaUrl || "/placeholder.svg"}
              alt="Analysis"
              style={{
                position: "absolute",
                top: 0,
                left: 0,
                width: "100%",
                height: "100%",
                objectFit: "contain",
              }}
            />
          ) : (
            <video
              src={analysis.mediaUrl}
              style={{
                position: "absolute",
                top: 0,
                left: 0,
                width: "100%",
                height: "100%",
                objectFit: "contain",
              }}
              controls
            />
          )}

          {/* Bounding Boxes Overlay */}
          <div
            style={{
              position: "absolute",
              top: 0,
              left: 0,
              width: "100%",
              height: "100%",
            }}
          >
            {showBooks &&
              analysis.books.map((book) => (
                <BoundingBox
                  key={book.id}
                  x={book.boundingBox.x}
                  y={book.boundingBox.y}
                  width={book.boundingBox.width}
                  height={book.boundingBox.height}
                  label={`📖 ${book.title || "Book"}`}
                  color={BOOK_COLOR}
                  containerWidth={containerWidth}
                  containerHeight={containerHeight}
                />
              ))}

            {showPeople &&
              analysis.people.map((person) => (
                <BoundingBox
                  key={person.id}
                  x={person.boundingBox.x}
                  y={person.boundingBox.y}
                  width={person.boundingBox.width}
                  height={person.boundingBox.height}
                  label={`👤 ${person.ageCategory}`}
                  color={PERSON_COLOR}
                  containerWidth={containerWidth}
                  containerHeight={containerHeight}
                />
              ))}
          </div>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-background-tertiary rounded-lg p-4 text-center">
          <p className="text-foreground-secondary text-sm">Books Detected</p>
          <p className="text-2xl font-bold text-primary">{analysis.books.length}</p>
        </div>
        <div className="bg-background-tertiary rounded-lg p-4 text-center">
          <p className="text-foreground-secondary text-sm">People Detected</p>
          <p className="text-2xl font-bold text-accent">{analysis.people.length}</p>
        </div>
        <div className="bg-background-tertiary rounded-lg p-4 text-center">
          <p className="text-foreground-secondary text-sm">Total Objects</p>
          <p className="text-2xl font-bold text-foreground">{analysis.books.length + analysis.people.length}</p>
        </div>
      </div>
    </div>
  )
}
