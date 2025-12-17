import type React from "react"
import { Alert, AlertTitle, AlertDescription } from "./alert"

interface CustomAlertProps {
  variant?: "info" | "success" | "warning" | "error"
  title?: string
  children: React.ReactNode
  className?: string
}

export function CustomAlert({ variant = "info", title, className = "", children }: CustomAlertProps) {
  const variantMap = {
    info: "default",
    success: "default",
    warning: "destructive",
    error: "destructive",
  }

  return (
    <Alert variant={variantMap[variant]} className={className}>
      {title && <AlertTitle>{title}</AlertTitle>}
      <AlertDescription>{children}</AlertDescription>
    </Alert>
  )
}

export { Alert, AlertTitle, AlertDescription }
