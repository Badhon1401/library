"use client"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import Link from "next/link"

export default function HomePage() {
  const features = [
    {
      icon: "📸",
      title: "Image Upload",
      description: "Upload images to detect books, people, and actions with advanced AI",
    },
    {
      icon: "🎥",
      title: "Video Analysis",
      description: "Analyze videos frame by frame to extract comprehensive detection data",
    },
    {
      icon: "📖",
      title: "Book Detection",
      description: "Automatically detect books and extract OCR text including titles and authors",
    },
    {
      icon: "👥",
      title: "People Recognition",
      description: "Identify people and classify age groups with action detection",
    },
    {
      icon: "💬",
      title: "Query Interface",
      description: "Ask natural language questions about your media and get instant answers",
    },
    {
      icon: "📊",
      title: "Export Reports",
      description: "Download analysis results as PDF or text reports for documentation",
    },
  ]

  return (
    <div className="bg-gradient-to-b from-background to-background-secondary min-h-screen">
      {/* Hero Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="text-center mb-16">
          <h1 className="text-5xl md:text-6xl font-bold text-foreground mb-4 text-balance">
            AI-Powered Library Vision System
          </h1>
          <p className="text-xl text-foreground-secondary mb-8 text-balance">
            Detect books, analyze people, and answer questions about your images and videos with cutting-edge AI
          </p>
          <Link href="/analyze">
            <Button size="lg" className="mr-4">
              Get Started
            </Button>
          </Link>
          <Button size="lg" variant="outline">
            Learn More
          </Button>
        </div>
      </section>

      {/* Features Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <h2 className="text-3xl font-bold text-center text-foreground mb-12">Powerful Features</h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <Card key={index}>
              <CardHeader>
                <div className="text-4xl mb-4">{feature.icon}</div>
                <CardTitle className="text-lg">{feature.title}</CardTitle>
              </CardHeader>
              <CardContent>
                <CardDescription>{feature.description}</CardDescription>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      {/* CTA Section */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <Card className="bg-gradient-to-r from-primary to-primary-dark border-0">
          <CardContent className="text-center py-12">
            <h3 className="text-3xl font-bold text-background mb-4">Ready to analyze?</h3>
            <p className="text-background text-lg mb-8">
              Upload your first image or video and experience the power of AI-driven analysis
            </p>
            <Link href="/analyze">
              <Button size="lg" variant="secondary">
                Start Analyzing Now
              </Button>
            </Link>
          </CardContent>
        </Card>
      </section>
    </div>
  )
}
