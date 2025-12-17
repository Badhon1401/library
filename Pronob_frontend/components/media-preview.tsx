"use client"

interface MediaPreviewProps {
  media: { url: string; type: "image" | "video" }
}

export default function MediaPreview({ media }: MediaPreviewProps) {
  return (
    <div className="w-full rounded-lg overflow-hidden bg-background-tertiary">
      {media.type === "image" ? (
        <img src={media.url || "/placeholder.svg"} alt="Preview" className="w-full h-auto object-cover max-h-96" />
      ) : (
        <video src={media.url} className="w-full h-auto object-cover max-h-96" controls />
      )}
    </div>
  )
}
