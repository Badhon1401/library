"use client"

import type * as React from "react"
import { Button } from "./button"

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
  onConfirm?: () => void
  confirmText?: string
  cancelText?: string
}

export function Modal({
  isOpen,
  onClose,
  title,
  children,
  onConfirm,
  confirmText = "Confirm",
  cancelText = "Cancel",
}: ModalProps) {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-card rounded-lg shadow-lg max-w-md w-full mx-4">
        <div className="border-b border-border p-6">
          <h2 className="text-xl font-bold">{title}</h2>
        </div>
        <div className="p-6">{children}</div>
        <div className="border-t border-border p-4 flex gap-2 justify-end">
          <Button variant="outline" onClick={onClose}>
            {cancelText}
          </Button>
          {onConfirm && <Button onClick={onConfirm}>{confirmText}</Button>}
        </div>
      </div>
    </div>
  )
}
