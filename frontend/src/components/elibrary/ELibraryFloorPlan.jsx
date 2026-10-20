import { useState, useMemo } from 'react';
import {
  ROOM_DIMENSIONS,
  RECEPTION_DESK,
  ENTRANCE,
  CENTRAL_PILLAR,
  EXIT_DOOR,
  WORKSTATION_LAYOUT,
  ARCHITECTURAL_PARTITIONS,
} from '../../data/elibraryLayout';
import { ZoomIn, ZoomOut, RotateCcw, Monitor } from 'lucide-react';
import './ELibraryFloorPlan.css';

export default function ELibraryFloorPlan({
  seats = [],
  selectedSeat = null,
  onSelectSeat = () => {},
}) {
  const [zoomLevel, setZoomLevel] = useState(1);

  // Map seat availability data by seatNumber
  const seatDataMap = useMemo(() => {
    const map = new Map();
    if (seats && Array.isArray(seats)) {
      seats.forEach((s) => {
        if (s.seatNumber != null) {
          map.set(s.seatNumber, s);
        }
      });
    }
    return map;
  }, [seats]);

  const handleZoom = (delta) => {
    setZoomLevel((prev) => {
      const next = prev + delta;
      return Math.min(Math.max(next, 0.75), 1.5);
    });
  };

  const handleResetZoom = () => {
    setZoomLevel(1);
  };

  return (
    <div className="floorplan-canvas-card">
      <div className="canvas-controls-bar">
        <div className="canvas-title-info">
          <Monitor size={18} style={{ color: 'var(--primary)' }} />
          <h2 className="canvas-title">E-Library Architectural Seating Grid</h2>
        </div>

        <div className="canvas-zoom-buttons">
          <span style={{ fontSize: 12, color: 'var(--navy-500)', marginRight: 6 }}>
            Zoom: {Math.round(zoomLevel * 100)}%
          </span>
          <button
            type="button"
            className="zoom-btn"
            onClick={() => handleZoom(0.15)}
            title="Zoom In"
            aria-label="Zoom in"
          >
            <ZoomIn size={16} />
          </button>
          <button
            type="button"
            className="zoom-btn"
            onClick={() => handleZoom(-0.15)}
            title="Zoom Out"
            aria-label="Zoom out"
          >
            <ZoomOut size={16} />
          </button>
          <button
            type="button"
            className="zoom-btn"
            onClick={handleResetZoom}
            title="Reset Zoom"
            aria-label="Reset zoom to default"
          >
            <RotateCcw size={14} />
          </button>
        </div>
      </div>

      <div className="svg-viewport-wrapper">
        <svg
          className="elibrary-svg-root"
          viewBox={ROOM_DIMENSIONS.viewBox}
          width={ROOM_DIMENSIONS.width * zoomLevel}
          height={ROOM_DIMENSIONS.height * zoomLevel}
          xmlns="http://www.w3.org/2000/svg"
          role="img"
          aria-label="College E-Library interactive floor plan"
        >
          {/* 1. Entrance Marker with Door Jamb and Arrow */}
          <g className="arch-entrance-group">
            <text
              x={ENTRANCE.x - 14}
              y={ENTRANCE.yStart + 32}
              className="arch-entrance-label"
              textAnchor="end"
            >
              {ENTRANCE.label}
            </text>
            {/* Vertical door frame jambs */}
            <line x1={ENTRANCE.x - 4} y1={ENTRANCE.yStart} x2={ENTRANCE.x - 4} y2={ENTRANCE.yEnd} stroke="#1e293b" strokeWidth="2.5" />
            <line x1={ENTRANCE.x + 4} y1={ENTRANCE.yStart} x2={ENTRANCE.x + 4} y2={ENTRANCE.yEnd} stroke="#1e293b" strokeWidth="2.5" />
            <line x1={ENTRANCE.x - 4} y1={ENTRANCE.yStart} x2={ENTRANCE.x + 4} y2={ENTRANCE.yStart} stroke="#1e293b" strokeWidth="2.5" />
            <line x1={ENTRANCE.x - 4} y1={ENTRANCE.yEnd} x2={ENTRANCE.x + 4} y2={ENTRANCE.yEnd} stroke="#1e293b" strokeWidth="2.5" />
            {/* Directional arrowhead pointing right into the room */}
            <polygon
              points={`${ENTRANCE.x + 4},${ENTRANCE.yStart} ${ENTRANCE.x + 24},${ENTRANCE.yStart + 8} ${ENTRANCE.x + 4},${ENTRANCE.yStart + 16}`}
              fill="#1e293b"
            />
          </g>

          {/* 2. Reception Desk */}
          <g className="arch-reception-group">
            <rect
              x={RECEPTION_DESK.x}
              y={RECEPTION_DESK.y}
              width={RECEPTION_DESK.width}
              height={RECEPTION_DESK.height}
              fill="#f8fafc"
              stroke="#1e293b"
              strokeWidth="2.8"
            />
            <text
              x={RECEPTION_DESK.x + RECEPTION_DESK.width / 2}
              y={RECEPTION_DESK.y + 50}
              className="arch-text-label"
              style={{ fontSize: 14, fontWeight: 700 }}
            >
              Reception
            </text>
            <text
              x={RECEPTION_DESK.x + RECEPTION_DESK.width / 2}
              y={RECEPTION_DESK.y + 72}
              className="arch-text-label"
              style={{ fontSize: 14, fontWeight: 700 }}
            >
              Desk
            </text>
          </g>

          {/* 3. Central Pillar */}
          <g className="arch-pillar-group">
            <rect
              x={CENTRAL_PILLAR.x}
              y={CENTRAL_PILLAR.y}
              width={CENTRAL_PILLAR.width}
              height={CENTRAL_PILLAR.height}
              fill="#f1f5f9"
              stroke="#1e293b"
              strokeWidth="2.8"
              rx="2"
            />
            <text
              x={CENTRAL_PILLAR.x + CENTRAL_PILLAR.width / 2}
              y={CENTRAL_PILLAR.y + CENTRAL_PILLAR.height / 2 + 5}
              className="arch-text-label"
              style={{ fontSize: 14, fontWeight: 800, fill: '#1e293b' }}
            >
              {CENTRAL_PILLAR.label}
            </text>
          </g>

          {/* 4. Exit Door */}
          <g className="arch-door-group">
            {/* Door jambs */}
            <line x1={EXIT_DOOR.xStart} y1={EXIT_DOOR.y} x2={EXIT_DOOR.xStart} y2={EXIT_DOOR.y + 12} stroke="#1e293b" strokeWidth="2.5" />
            <line x1={EXIT_DOOR.xEnd} y1={EXIT_DOOR.y} x2={EXIT_DOOR.xEnd} y2={EXIT_DOOR.y + 12} stroke="#1e293b" strokeWidth="2.5" />
            {/* Threshold frame */}
            <rect
              x={EXIT_DOOR.xStart}
              y={EXIT_DOOR.y}
              width={EXIT_DOOR.xEnd - EXIT_DOOR.xStart}
              height={10}
              fill="#f8fafc"
              stroke="#1e293b"
              strokeWidth="1.8"
            />
            {/* Label below door */}
            <text
              x={(EXIT_DOOR.xStart + EXIT_DOOR.xEnd) / 2}
              y={EXIT_DOOR.y + 28}
              className="arch-text-label"
              style={{ fontSize: 13, fontStyle: 'italic', fill: '#1e293b' }}
            >
              {EXIT_DOOR.label}
            </text>
          </g>

          {/* 5. Architectural Partitions and Desk Spines */}
          {ARCHITECTURAL_PARTITIONS.map((p, idx) => {
            if (p.type === 'line') {
              return (
                <line
                  key={`part-${idx}`}
                  x1={p.x1}
                  y1={p.y1}
                  x2={p.x2}
                  y2={p.y2}
                  className={p.className}
                />
              );
            }
            return null;
          })}

          {/* 6. 68 Numbered Workstation Nodes (BookMyShow Buttons) */}
          {WORKSTATION_LAYOUT.map((w) => {
            const liveData = seatDataMap.get(w.id);
            const status = liveData?.status || 'AVAILABLE';
            const isSelected = selectedSeat?.seatNumber === w.id;
            const isMyBooking = liveData?.isMyBooking || false;

            const statusClass = `seat-${status.toLowerCase()}`;
            const selectedClass = isSelected ? 'seat-selected' : '';
            const myBookingClass = isMyBooking ? 'seat-my-booking' : '';

            return (
              <g
                key={`seat-${w.id}`}
                id={`workstation-${w.id}`}
                className={`seat-node ${statusClass} ${selectedClass} ${myBookingClass}`}
                tabIndex={0}
                role="button"
                aria-label={`Workstation ${w.id}, status: ${status}, cluster: ${w.cluster}`}
                onClick={() => onSelectSeat(liveData || { seatNumber: w.id, status: 'AVAILABLE', resourceName: `Workstation ${w.id}` })}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    onSelectSeat(liveData || { seatNumber: w.id, status: 'AVAILABLE', resourceName: `Workstation ${w.id}` });
                  }
                }}
              >
                {/* Workstation base box */}
                <rect
                  x={w.x}
                  y={w.y}
                  width={w.width}
                  height={w.height}
                  rx="6"
                  className="seat-rect"
                />

                {/* Subtle Computer Screen Line Accent at top of button */}
                <rect
                  x={w.x + 8}
                  y={w.y + 4}
                  width={w.width - 16}
                  height="3"
                  rx="1.5"
                  fill="rgba(255, 255, 255, 0.45)"
                  className="seat-icon-screen"
                />

                {/* Workstation Number in bold high-contrast font */}
                <text
                  x={w.x + w.width / 2}
                  y={w.y + w.height / 2 + 3}
                  className="seat-text"
                >
                  {w.id}
                </text>
              </g>
            );
          })}
        </svg>
      </div>
    </div>
  );
}
