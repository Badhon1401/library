function Loader() {
  return (
    <div className="flex items-center justify-center">
      <div className="relative w-8 h-8">
        <div className="absolute inset-0 border-4 border-transparent border-t-primary rounded-full animate-spin" />
      </div>
    </div>
  )
}

function LoaderOverlay() {
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-card rounded-lg p-8">
        <Loader />
        <p className="mt-4 text-muted-foreground text-center">Processing...</p>
      </div>
    </div>
  )
}

export { Loader, LoaderOverlay }
