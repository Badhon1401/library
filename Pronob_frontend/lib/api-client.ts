const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:3001/api"

export interface AnalyzeMediaRequest {
  mediaUrl: string
  mediaType: "image" | "video"
}

export interface QueryRequest {
  query: string
  analysisId: string
}

export async function analyzeMedia(request: AnalyzeMediaRequest) {
  try {
    const response = await fetch(`${API_BASE_URL}/analyze-media`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error(`API error: ${response.statusText}`)
    }

    return await response.json()
  } catch (error) {
    throw new Error(`Failed to analyze media: ${error instanceof Error ? error.message : "Unknown error"}`)
  }
}

export async function askQuery(request: QueryRequest) {
  try {
    const response = await fetch(`${API_BASE_URL}/ask-query`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error(`API error: ${response.statusText}`)
    }

    return await response.json()
  } catch (error) {
    throw new Error(`Failed to query: ${error instanceof Error ? error.message : "Unknown error"}`)
  }
}
