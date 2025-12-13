"use client"

import type React from "react"

import { useState, useRef, useEffect } from "react"
import { useAnalysis, type ChatMessage } from "@/context/analysis-context"
import { Button } from "./ui/button"
import { Input } from "./ui/input"
import { Loader } from "./ui/loader"
import { Alert } from "./ui/alert"
import { askQuery } from "@/lib/api-client"

interface ChatInterfaceProps {
  analysisId: string
}

export default function ChatInterface({ analysisId }: ChatInterfaceProps) {
  const { chatHistory, addChatMessage, clearChat, isLoading, setIsLoading, error, setError } = useAnalysis()

  const [input, setInput] = useState("")
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }

  useEffect(() => {
    scrollToBottom()
  }, [chatHistory])

  const handleSendMessage = async () => {
    if (!input.trim()) return

    const userMessage: ChatMessage = {
      id: Date.now().toString(),
      role: "user",
      content: input,
      timestamp: new Date(),
    }

    addChatMessage(userMessage)
    setInput("")
    setError(null)
    setIsLoading(true)

    try {
      const response = await askQuery({
        query: input,
        analysisId: analysisId,
      })

      const assistantMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: response.answer || "No response received",
        timestamp: new Date(),
      }

      addChatMessage(assistantMessage)
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : "Failed to send message"
      setError(errorMsg)

      const errorMessage: ChatMessage = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: `Error: ${errorMsg}`,
        timestamp: new Date(),
      }

      addChatMessage(errorMessage)
    } finally {
      setIsLoading(false)
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  return (
    <div className="flex flex-col h-96 bg-background-tertiary rounded-lg">
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {chatHistory.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-foreground-secondary">
            <div className="text-4xl mb-2">💬</div>
            <p>No messages yet. Ask a question to get started!</p>
          </div>
        ) : (
          chatHistory.map((message) => (
            <div key={message.id} className={`flex ${message.role === "user" ? "justify-end" : "justify-start"}`}>
              <div
                className={`max-w-xs px-4 py-2 rounded-lg ${
                  message.role === "user" ? "bg-primary text-background" : "bg-background-secondary text-foreground"
                }`}
              >
                <p className="text-sm">{message.content}</p>
                <span className="text-xs opacity-70 mt-1 block">
                  {message.timestamp.toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
              </div>
            </div>
          ))
        )}

        {isLoading && (
          <div className="flex justify-start">
            <div className="bg-background-secondary text-foreground px-4 py-2 rounded-lg">
              <Loader />
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {error && (
        <div className="px-4 pt-0">
          <Alert variant="error" title="Error">
            {error}
          </Alert>
        </div>
      )}

      <div className="border-t border-foreground-tertiary p-4 space-y-2">
        <div className="flex gap-2">
          <Input
            placeholder="Ask about the analysis..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={handleKeyPress}
            disabled={isLoading}
          />
          <Button onClick={handleSendMessage} disabled={isLoading || !input.trim()} isLoading={isLoading}>
            Send
          </Button>
        </div>

        {chatHistory.length > 0 && (
          <Button variant="ghost" size="sm" className="w-full" onClick={clearChat}>
            Clear Chat
          </Button>
        )}
      </div>
    </div>
  )
}
