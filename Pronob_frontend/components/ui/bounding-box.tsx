interface BoundingBoxProps {
  x: number
  y: number
  width: number
  height: number
  label: string
  color?: string
  containerWidth: number
  containerHeight: number
}

export function BoundingBox({
  x,
  y,
  width,
  height,
  label,
  color = "rgb(99, 102, 241)",
  containerWidth,
  containerHeight,
}: BoundingBoxProps) {
  const style = {
    position: "absolute" as const,
    left: `${(x / containerWidth) * 100}%`,
    top: `${(y / containerHeight) * 100}%`,
    width: `${(width / containerWidth) * 100}%`,
    height: `${(height / containerHeight) * 100}%`,
    border: `2px solid ${color}`,
    backgroundColor: `${color}20`,
  }

  return (
    <div style={style}>
      <div
        style={{
          position: "absolute",
          top: "-24px",
          left: 0,
          backgroundColor: color,
          color: "#000",
          padding: "2px 6px",
          fontSize: "12px",
          fontWeight: "bold",
          borderRadius: "4px",
          whiteSpace: "nowrap",
        }}
      >
        {label}
      </div>
    </div>
  )
}
