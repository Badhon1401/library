import type { AnalysisResult } from "@/context/analysis-context"

export const mockAnalysisResult: AnalysisResult = {
  mediaUrl: "/library-with-books-and-people.jpg",
  mediaType: "image",
  books: [
    {
      id: "1",
      title: "The Great Gatsby",
      author: "F. Scott Fitzgerald",
      isbn: "978-0743273565",
      extractedText: "The Great Gatsby - A novel about dreams and the American Dream",
      confidence: 0.95,
      boundingBox: {
        x: 50,
        y: 100,
        width: 150,
        height: 250,
      },
    },
    {
      id: "2",
      title: "Harry Potter and the Sorcerer's Stone",
      author: "J.K. Rowling",
      isbn: "978-0439708180",
      extractedText: "Harry Potter - A young wizard discovers his magical heritage",
      confidence: 0.92,
      boundingBox: {
        x: 250,
        y: 120,
        width: 140,
        height: 240,
      },
    },
    {
      id: "3",
      title: "To Kill a Mockingbird",
      author: "Harper Lee",
      isbn: "978-0061120084",
      extractedText: "To Kill a Mockingbird - A story of racial injustice and childhood innocence",
      confidence: 0.88,
      boundingBox: {
        x: 450,
        y: 150,
        width: 160,
        height: 260,
      },
    },
  ],
  people: [
    {
      id: "p1",
      ageCategory: "adult",
      action: "reading a book",
      confidence: 0.91,
      boundingBox: {
        x: 100,
        y: 350,
        width: 120,
        height: 200,
      },
    },
    {
      id: "p2",
      ageCategory: "child",
      action: "browsing books",
      confidence: 0.87,
      boundingBox: {
        x: 350,
        y: 380,
        width: 100,
        height: 180,
      },
    },
    {
      id: "p3",
      ageCategory: "elderly",
      action: "selecting a book",
      confidence: 0.85,
      boundingBox: {
        x: 550,
        y: 400,
        width: 110,
        height: 190,
      },
    },
  ],
  rawResponse: {
    status: "success",
    processingTime: 2500,
  },
}

export const mockChatMessages = [
  {
    id: "1",
    role: "user" as const,
    content: "Is there a book named Harry Potter?",
    timestamp: new Date(Date.now() - 300000),
  },
  {
    id: "2",
    role: "assistant" as const,
    content:
      'Yes, I detected "Harry Potter and the Sorcerer\'s Stone" by J.K. Rowling in the image. It has a confidence score of 92% and is located in the middle-right portion of the image.',
    timestamp: new Date(Date.now() - 280000),
  },
  {
    id: "3",
    role: "user" as const,
    content: "How many people are reading?",
    timestamp: new Date(Date.now() - 200000),
  },
  {
    id: "4",
    role: "assistant" as const,
    content:
      "I detected 3 people total: 1 adult who is reading a book, 1 child who is browsing books, and 1 elderly person who is selecting a book.",
    timestamp: new Date(Date.now() - 180000),
  },
]
