# Library Vision System - Frontend

An AI-powered library management vision system that detects books, people, and actions in images and videos. The frontend provides an intuitive interface for uploading media, viewing detection results with bounding boxes, and querying analysis data.

## Features

- **Media Upload**: Drag-and-drop or camera capture for images and videos
- **Object Detection**: Visual display of detected books and people with bounding boxes
- **OCR Text Extraction**: Automatic book title, author, and ISBN extraction
- **Age Classification**: Detect and classify people by age group (child, adult, elderly)
- **Action Recognition**: Identify actions performed by detected people
- **Chat Interface**: Ask natural language queries about the analysis
- **Export Reports**: Download analysis results as text reports
- **Responsive Design**: Fully responsive across all devices
- **Dark Theme**: Modern dark UI with accent colors

## Tech Stack

- **Framework**: Next.js 16 with App Router
- **Language**: TypeScript
- **Styling**: Tailwind CSS with custom theming
- **State Management**: React Context API
- **UI Components**: Custom built component library
- **HTTP Client**: Fetch API with custom wrapper

## Project Structure

\`\`\`
├── app/
│   ├── page.tsx              # Home page with features
│   ├── analyze/              # Media upload and analysis page
│   ├── results/              # Detection results viewer
│   ├── chat/                 # Query interface
│   ├── demo/                 # Demo with mock data
│   ├── layout.tsx            # Root layout with theme
│   └── globals.css           # Global styles and theme tokens
├── components/
│   ├── ui/                   # Reusable UI components
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   ├── input.tsx
│   │   ├── loader.tsx
│   │   ├── modal.tsx
│   │   ├── bounding-box.tsx
│   │   └── ...
│   ├── navigation.tsx        # Top navigation bar
│   ├── media-upload-form.tsx # Upload form component
│   ├── media-preview.tsx     # Media preview component
│   ├── detection-viewer.tsx  # Bounding box viewer
│   ├── results-panel.tsx     # Results summary panel
│   └── chat-interface.tsx    # Query chat component
├── context/
│   └── analysis-context.tsx  # Global state management
├── lib/
│   ├── api-client.ts         # API client functions
│   ├── mock-data.ts          # Demo data
│   └── utils.ts              # Utility functions
└── public/                   # Static assets
\`\`\`

## Installation

### Prerequisites
- Node.js 18+
- npm or pnpm

### Setup

1. Clone the repository:
\`\`\`bash
git clone <repository-url>
cd library-vision-frontend
\`\`\`

2. Install dependencies:
\`\`\`bash
npm install
# or
pnpm install
\`\`\`

3. Set up environment variables:
Create a `.env.local` file in the root directory:
\`\`\`env
NEXT_PUBLIC_API_URL=http://localhost:3001/api
\`\`\`

4. Run the development server:
\`\`\`bash
npm run dev
# or
pnpm dev
\`\`\`

5. Open [http://localhost:3000](http://localhost:3000) in your browser.

## Usage

### Home Page
- Browse features and get an overview of the system
- Access the main upload page or view demo results

### Upload Media
1. Navigate to the "Analyze" page
2. Choose upload method (File upload or Camera capture)
3. Select an image or video file
4. Preview the media
5. Click "Analyze Media" to send to backend

### View Results
- See detected books and people with bounding boxes
- Toggle detection overlays on/off
- View detailed information in the side panel
- Export analysis as a text report

### Query Interface
- Ask natural language questions about the analysis
- Receive AI-powered answers based on detection results
- Clear chat history as needed
- Examples:
  - "Is there a book named Harry Potter?"
  - "How many people are reading?"
  - "Is there a child in this video?"

### Demo Mode
Visit [http://localhost:3000/demo](http://localhost:3000/demo) to see a working example with mock data.

## API Integration

The frontend connects to a backend API with the following endpoints:

### Analyze Media
\`\`\`
POST /api/analyze-media
Content-Type: application/json

{
  "mediaUrl": "data:image/jpeg;base64,...",
  "mediaType": "image" | "video"
}

Response:
{
  "books": [{
    "id": "string",
    "title": "string",
    "author": "string",
    "isbn": "string",
    "extractedText": "string",
    "confidence": 0-1,
    "boundingBox": { x, y, width, height }
  }],
  "people": [{
    "id": "string",
    "ageCategory": "child" | "adult" | "elderly",
    "action": "string",
    "confidence": 0-1,
    "boundingBox": { x, y, width, height }
  }]
}
\`\`\`

### Ask Query
\`\`\`
POST /api/ask-query
Content-Type: application/json

{
  "query": "Is there a book named Harry Potter?",
  "analysisId": "string"
}

Response:
{
  "answer": "Yes, I detected..."
}
\`\`\`

## Customization

### Theme Customization
Edit `app/globals.css` to customize colors:
\`\`\`css
@theme inline {
  --color-background: #0a0e27;
  --color-primary: #6366f1;
  --color-accent: #00d9ff;
  /* ... more colors ... */
}
\`\`\`

### Component Styling
All UI components use Tailwind CSS and are located in `components/ui/`. Customize their appearance by modifying the class names.

### API Endpoint
Change the API base URL in `.env.local`:
\`\`\`env
NEXT_PUBLIC_API_URL=https://api.yourdomain.com
\`\`\`

## State Management

The application uses React Context API for state management:

- **AnalysisContext**: Manages current analysis, chat history, loading state, and errors
- **useAnalysis()**: Hook to access context values from any component

## Error Handling

- Network errors are caught and displayed to users
- Form validation provides immediate feedback
- API errors are logged and shown in alerts
- File upload validation checks size and type

## Performance Optimization

- Images are lazy-loaded
- Bounding boxes use CSS transforms for smooth rendering
- Chat messages are virtualized in a scrollable container
- Context updates are optimized with useCallback

## Browser Support

- Chrome/Chromium (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## Building for Production

\`\`\`bash
npm run build
npm start
\`\`\`

The production build will be optimized and ready for deployment.

## Deployment

This Next.js application can be deployed to:
- Vercel (recommended)
- Netlify
- Self-hosted servers
- Docker containers

### Deploy to Vercel
\`\`\`bash
npm install -g vercel
vercel
\`\`\`

## Troubleshooting

### Media Upload Issues
- Ensure file size is under 50MB
- Verify file format is supported (JPG, PNG, MP4, WebM)
- Check browser permissions for camera access

### API Connection Errors
- Verify `NEXT_PUBLIC_API_URL` environment variable is set correctly
- Ensure backend API is running and accessible
- Check browser console for CORS errors

### Performance Issues
- Clear browser cache
- Reduce media file size
- Use a modern browser
- Check network tab for slow API responses

## Development

### Running Tests
\`\`\`bash
npm run test
\`\`\`

### Linting
\`\`\`bash
npm run lint
\`\`\`

### Type Checking
\`\`\`bash
npm run type-check
\`\`\`

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

MIT License - see LICENSE file for details

## Support

For issues and questions:
- Open a GitHub issue
- Check existing documentation
- Review the demo page at `/demo`

## Roadmap

- [ ] Real-time camera feed analysis
- [ ] Batch processing multiple files
- [ ] Advanced filtering and search
- [ ] User authentication
- [ ] Results history and bookmarks
- [ ] Mobile app version
- [ ] Multi-language support
- [ ] Custom model training

## Acknowledgments

Built with:
- Next.js
- React
- Tailwind CSS
- TypeScript
