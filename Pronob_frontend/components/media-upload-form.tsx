"use client"

import type React from "react"

import { useState, useRef } from "react"
import { Button } from "./ui/button"
import { Alert } from "./ui/alert"
import { useAnalysis } from "@/context/analysis-context"
import { analyzeMedia } from "@/lib/api-client"
import { useRouter } from "next/navigation"

interface MediaUploadFormProps {
  onMediaUploaded: (media: { url: string; type: "image" | "video" }) => void
}

export default function MediaUploadForm({ onMediaUploaded }: MediaUploadFormProps) {
  const router = useRouter()
  const { setCurrentAnalysis, setIsLoading, setError } = useAnalysis()
  const [uploadMethod, setUploadMethod] = useState<"file" | "camera">("file")
  const [isDragging, setIsDragging] = useState(false)
  const [error, setError_local] = useState<string | null>(null)
  const [isLoading, setIsLoading_local] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const videoInputRef = useRef<HTMLInputElement>(null)

  const ALLOWED_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"]
  const ALLOWED_VIDEO_TYPES = ["video/mp4", "video/webm"]
  const MAX_FILE_SIZE = 50 * 1024 * 1024

  const validateFile = (file: File): { valid: boolean; error?: string } => {
    if (file.size > MAX_FILE_SIZE) {
      return { valid: false, error: "File size exceeds 50MB limit" }
    }

    const isImage = ALLOWED_IMAGE_TYPES.includes(file.type)
    const isVideo = ALLOWED_VIDEO_TYPES.includes(file.type)

    if (!isImage && !isVideo) {
      return { valid: false, error: "File type not supported" }
    }

    return { valid: true }
  }

  const handleFileSelect = async (file: File) => {
    setError_local(null)
    const validation = validateFile(file)

    if (!validation.valid) {
      setError_local(validation.error || "Invalid file")
      return
    }

    setIsLoading_local(true)
    setIsLoading(true)

    try {
      const reader = new FileReader()
      reader.onload = async (e) => {
        const url = e.target?.result as string
        const isImage = ALLOWED_IMAGE_TYPES.includes(file.type)
        const mediaType = isImage ? "image" : "video"

        onMediaUploaded({ url, type: mediaType })

        try {
          const result = await analyzeMedia({
            mediaUrl: url,
            mediaType: mediaType,
          })

          // Process and store the analysis result
          setCurrentAnalysis({
            mediaUrl: url,
            mediaType: mediaType,
            books: result.books || [],
            people: result.people || [],
            rawResponse: result,
          })

          // Navigate to results page
          router.push("/results")
        } catch (apiError) {
          const errorMsg = apiError instanceof Error ? apiError.message : "Analysis failed"
          setError_local(errorMsg)
          setError(errorMsg)
        }
      }
      reader.readAsDataURL(file)
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : "Upload failed"
      setError_local(errorMsg)
      setError(errorMsg)
    } finally {
      setIsLoading_local(false)
      setIsLoading(false)
    }
  }

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }

  const handleDragLeave = () => {
    setIsDragging(false)
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)

    const files = e.dataTransfer.files
    if (files.length > 0) {
      handleFileSelect(files[0])
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex gap-2 mb-6">
        <Button variant={uploadMethod === "file" ? "primary" : "outline"} onClick={() => setUploadMethod("file")}>
          Upload File
        </Button>
        <Button variant={uploadMethod === "camera" ? "primary" : "outline"} onClick={() => setUploadMethod("camera")}>
          Camera Capture
        </Button>
      </div>

      {uploadMethod === "file" && (
        <>
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
              isDragging
                ? "border-primary bg-primary bg-opacity-10"
                : "border-foreground-tertiary hover:border-foreground-secondary"
            }`}
          >
            <div className="mb-4 text-4xl">📁</div>
            <p className="text-foreground mb-2">Drag and drop your file here</p>
            <p className="text-foreground-secondary text-sm mb-4">or click to browse</p>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*,video/*"
              onChange={(e) => {
                if (e.target.files?.[0]) {
                  handleFileSelect(e.target.files[0])
                }
              }}
              className="hidden"
            />
            <Button onClick={() => fileInputRef.current?.click()} disabled={isLoading} isLoading={isLoading}>
              Choose File
            </Button>
          </div>

          <p className="text-xs text-foreground-tertiary text-center">
            Supported: JPG, PNG (images) • MP4, WebM (videos) • Max 50MB
          </p>
        </>
      )}

      {uploadMethod === "camera" && (
        <div className="border-2 border-dashed border-foreground-tertiary rounded-lg p-8 text-center">
          <div className="mb-4 text-4xl">📷</div>
          <p className="text-foreground mb-4">Capture with your device camera</p>
          <input
            ref={videoInputRef}
            type="file"
            accept="image/*"
            capture="environment"
            onChange={(e) => {
              if (e.target.files?.[0]) {
                handleFileSelect(e.target.files[0])
              }
            }}
            className="hidden"
          />
          <Button onClick={() => videoInputRef.current?.click()} disabled={isLoading} isLoading={isLoading}>
            Open Camera
          </Button>
        </div>
      )}

      {error && (
        <Alert variant="error" title="Upload Error">
          <p>{error}</p>
        </Alert>
      )}
    </div>
  )
}
