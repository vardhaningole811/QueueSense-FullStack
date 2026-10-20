import { useState, useEffect, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';
import ELibraryFloorPlan from '../../components/elibrary/ELibraryFloorPlan';
import SeatDetailsPanel from '../../components/elibrary/SeatDetailsPanel';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import {
  ArrowLeft,
  RefreshCw,
  Search,
  QrCode,
} from 'lucide-react';
import '../../components/elibrary/ELibraryFloorPlan.css';

export default function ELibrarySeatBooking() {

  const [floorPlanData, setFloorPlanData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Use stable primitive seat number as the single source of truth for selection
  const [selectedSeatNumber, setSelectedSeatNumber] = useState(null);
  const [searchNumber, setSearchNumber] = useState('');

  // Derive selectedSeat directly from floorPlanData using useMemo
  const selectedSeat = useMemo(() => {
    if (!selectedSeatNumber || !floorPlanData?.seats) return null;
    return floorPlanData.seats.find((s) => s.seatNumber === selectedSeatNumber) || null;
  }, [selectedSeatNumber, floorPlanData]);

  // Initial data loading effect (runs once on mount, completely decoupled from seat selection)
  useEffect(() => {
    let isMounted = true;

    const loadInitialData = async () => {
      setLoading(true);
      setError(null);
      try {
        const res = await api.get('/elibrary/floorplan');
        if (isMounted) {
          setFloorPlanData(res.data);
        }
      } catch (err) {
        if (isMounted) {
          console.error('Failed to load E-Library floor plan:', err);
          setError('Unable to load live floor plan and workstation availability. Please try again.');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    loadInitialData();

    // Auto-sync seat availability periodically in background without showing full-page loader
    const timer = setInterval(async () => {
      try {
        const res = await api.get('/elibrary/floorplan');
        if (isMounted) {
          setFloorPlanData(res.data);
        }
      } catch (err) {
        console.warn('Periodic floor plan sync failed (safely ignored):', err);
      }
    }, 15000);

    return () => {
      isMounted = false;
      clearInterval(timer);
    };
  }, []);

  // Manual or post-booking background sync (does NOT unmount or show full-page loading)
  const handleSyncData = useCallback(async () => {
    setRefreshing(true);
    setError(null);
    try {
      const res = await api.get('/elibrary/floorplan');
      setFloorPlanData(res.data);
    } catch (err) {
      console.error('Failed to sync E-Library floor plan:', err);
      setError('Unable to sync live seat availability. Please try again.');
    } finally {
      setRefreshing(false);
    }
  }, []);

  const handleSeatClick = useCallback((seat) => {
    const num = typeof seat === 'number' ? seat : seat?.seatNumber;
    if (num != null) {
      setSelectedSeatNumber(num);
    }
  }, []);

  const handleClearSelection = useCallback(() => {
    setSelectedSeatNumber(null);
  }, []);

  const handleBookingSuccess = useCallback(() => {
    handleSyncData();
  }, [handleSyncData]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    const num = parseInt(searchNumber.trim(), 10);
    if (!num || num < 1 || num > 68) {
      alert('Please enter a valid workstation number between 1 and 68.');
      return;
    }

    setSelectedSeatNumber(num);
    const el = document.getElementById(`workstation-${num}`);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  };

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading interactive E-Library floor plan & workstation status..." />
      </div>
    );
  }

  // Active student booking check
  const myActiveSeat = floorPlanData?.seats?.find((s) => s.isMyBooking);

  return (
    <div className="elibrary-booking-container">
      {/* Back button */}
      <div>
        <Link to="/student/libraries" className="btn btn-outline btn-sm" style={{ display: 'inline-flex' }}>
          <ArrowLeft size={14} />
          <span>Back to All Libraries</span>
        </Link>
      </div>

      {/* Page Header */}
      <div className="page-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <h1 className="page-title" style={{ margin: 0 }}>E-Library Seat Selection</h1>
            <span className="facility-type-tag">Interactive Map</span>
          </div>
          <p className="page-description">
            Choose an available computer workstation on the live campus floor plan below. Seats are color-coded in real time.
          </p>
        </div>

        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={handleSyncData}
            disabled={refreshing}
            title="Sync latest live seat status"
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Live Sync'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={handleSyncData} />}

      {/* Active User Reservation Notice if present */}
      {myActiveSeat && (
        <div
          className="active-request-banner"
          style={{
            borderLeftColor: '#10b981',
            backgroundColor: '#f0fdf4',
            marginBottom: 8,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <div
                style={{
                  width: 38,
                  height: 38,
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: '#dcfce7',
                  color: '#15803d',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                <QrCode size={20} />
              </div>
              <div>
                <h4 style={{ fontSize: 14, fontWeight: 700, color: '#166534', margin: 0 }}>
                  You Have An Active Booking on Workstation #{myActiveSeat.seatNumber}
                </h4>
                <p style={{ fontSize: 12, color: '#15803d', margin: '2px 0 0' }}>
                  Request Code: <strong>{myActiveSeat.currentRequestCode}</strong> &bull; Status: {myActiveSeat.status}
                </p>
              </div>
            </div>
            <Link to="/student/requests" className="btn btn-primary btn-sm">
              <span>View Active Pass</span>
              <ArrowLeft size={13} style={{ transform: 'rotate(180deg)' }} />
            </Link>
          </div>
        </div>
      )}

      {/* BookMyShow Style Status Legend & Search Toolbar */}
      <div className="floorplan-toolbar-card">
        <div className="legend-group">
          <div className="legend-item" title="Click to reserve">
            <span className="legend-badge legend-badge-available"></span>
            <span>Available</span>
            <span className="legend-count">{floorPlanData?.availableCount ?? 0}</span>
          </div>

          <div className="legend-item" title="Currently occupied by student">
            <span className="legend-badge legend-badge-occupied"></span>
            <span>Occupied</span>
            <span className="legend-count">{floorPlanData?.occupiedCount ?? 0}</span>
          </div>

          <div className="legend-item" title="Temporarily held or awaiting operator approval">
            <span className="legend-badge legend-badge-reserved"></span>
            <span>Reserved / Pending</span>
            <span className="legend-count">
              {(floorPlanData?.reservedCount ?? 0) + (floorPlanData?.pendingCount ?? 0)}
            </span>
          </div>

          <div className="legend-item" title="Technical maintenance in progress">
            <span className="legend-badge legend-badge-maintenance"></span>
            <span>Under Maintenance</span>
            <span className="legend-count">{floorPlanData?.maintenanceCount ?? 0}</span>
          </div>

          <div className="legend-item" title="Your currently selected seat">
            <span className="legend-badge legend-badge-selected"></span>
            <span>Selected</span>
            {selectedSeat && <span className="legend-count">#{selectedSeat.seatNumber}</span>}
          </div>
        </div>

        {/* Quick jump to seat input */}
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input
            type="number"
            min="1"
            max="68"
            placeholder="Workstation #"
            value={searchNumber}
            onChange={(e) => setSearchNumber(e.target.value)}
            className="form-input"
            style={{ width: 140, padding: '5px 10px', fontSize: 13 }}
          />
          <button type="submit" className="btn btn-secondary btn-sm" title="Find Workstation">
            <Search size={14} />
            <span>Find</span>
          </button>
        </form>
      </div>

      {/* Main Workspace (Canvas + Details Side Panel) */}
      <div className="floorplan-workspace">
        <ELibraryFloorPlan
          seats={floorPlanData?.seats || []}
          selectedSeat={selectedSeat}
          onSelectSeat={handleSeatClick}
        />

        <SeatDetailsPanel
          key={selectedSeatNumber ?? 'empty'}
          seat={selectedSeat}
          onClose={handleClearSelection}
          onBookingSuccess={handleBookingSuccess}
        />
      </div>
    </div>
  );
}
