"use client"

import { useState } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"
import { Alert } from "@/components/ui/alert"
import MediaUploadForm from "@/components/media-upload-form"
import MediaPreview from "@/components/media-preview"

export default function AnalyzePage() {
  const [uploadedMedia, setUploadedMedia] = useState<{ url: string; type: "image" | "video" } | null>(null)

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="mb-8">
        <h1 className="text-4xl font-bold text-foreground mb-2">Media Analysis</h1>
        <p className="text-foreground-secondary">Upload an image or video to get started with AI analysis</p>
      </div>

      <div className="grid lg:grid-cols-2 gap-8">
        {/* Upload Form */}
        <Card>
          <CardHeader>
            <CardTitle>Upload Media</CardTitle>
            <CardDescription>Choose an image or video file to analyze</CardDescription>
          </CardHeader>
          <CardContent>
            <MediaUploadForm onMediaUploaded={setUploadedMedia} />
            <Alert variant="info" className="mt-6">
              Supported formats: JPG, PNG for images and MP4, WebM for videos. Max size: 50MB
            </Alert>
          </CardContent>
        </Card>

        {/* Preview */}
        {uploadedMedia && (
          <Card>
            <CardHeader>
              <CardTitle>Preview</CardTitle>
              <CardDescription>Review your media before analysis</CardDescription>
            </CardHeader>
            <CardContent>
              <MediaPreview media={uploadedMedia} />
              <Button className="w-full mt-4">Analyze Media</Button>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}
