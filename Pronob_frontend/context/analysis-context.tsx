"use client"

import type React from "react"
import { createContext, useContext, useState, useCallback } from "react"

export interface Book {
  id: string
  title: string
  author: string
  isbn: string
  extractedText: string
  confidence: number
  boundingBox: {
    x: number
    y: number
    width: number
    height: number
  }
}

export interface Person {
  id: string
  ageCategory: "child" | "adult" | "elderly"
  action: string
  confidence: number
  boundingBox: {
    x: number
    y: number
    width: number
    height: number
  }
}

export interface AnalysisResult {
  mediaUrl: string
  mediaType: "image" | "video"
  books: Book[]
  people: Person[]
  rawResponse: unknown
}

export interface ChatMessage {
  id: string
  role: "user" | "assistant"
  content: string
  timestamp: Date
}

interface AnalysisContextType {
  currentAnalysis: AnalysisResult | null
  setCurrentAnalysis: (analysis: AnalysisResult | null) => void
  chatHistory: ChatMessage[]
  addChatMessage: (message: ChatMessage) => void
  clearChat: () => void
  isLoading: boolean
  setIsLoading: (loading: boolean) => void
  error: string | null
  setError: (error: string | null) => void
}

const AnalysisContext = createContext<AnalysisContextType | undefined>(undefined)

export function AnalysisProvider({ children }: { children: React.ReactNode }) {
  const [currentAnalysis, setCurrentAnalysis] = useState<AnalysisResult | null>(null)
  const [chatHistory, setChatHistory] = useState<ChatMessage[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const addChatMessage = useCallback((message: ChatMessage) => {
    setChatHistory((prev) => [...prev, message])
  }, [])

  const clearChat = useCallback(() => {
    setChatHistory([])
  }, [])

  return (
    <AnalysisContext.Provider
      value={{
        currentAnalysis,
        setCurrentAnalysis,
        chatHistory,
        addChatMessage,
        clearChat,
        isLoading,
        setIsLoading,
        error,
        setError,
      }}
    >
      {children}
    </AnalysisContext.Provider>
  )
}

export function useAnalysis() {
  const context = useContext(AnalysisContext)
  if (!context) {
    throw new Error("useAnalysis must be used within AnalysisProvider")
  }
  return context
}
