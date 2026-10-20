import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import SportsBookingModal from '../../components/sports/SportsBookingModal';
import {
  ArrowLeft,
  RefreshCw,
  Trophy,
  Dumbbell,
  CheckCircle,
  AlertTriangle,
  Lock,
  Layers,
  Sparkles,
  QrCode,
  LogOut,
  XCircle,
  Clock,
  Info,
  Calendar,
  AlertCircle,
  ShieldCheck,
  ChevronRight,
} from 'lucide-react';

export default function SportsComplex() {
  const [availability, setAvailability] = useState(null);
  const [courtSlots, setCourtSlots] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Selected resource and slot for booking modal
  const [selectedResource, setSelectedResource] = useState(null);
  const [selectedQuantity, setSelectedQuantity] = useState(1);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  // Selected quantities on cards for equipment
  const [cardQuantities, setCardQuantities] = useState({});

  const fetchAvailability = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const [availRes, slotsRes] = await Promise.all([
        api.get('/sports/student/availability'),
        api.get('/sports/student/court-slots').catch((err) => {
          console.warn('Court slots load failed:', err);
          return { data: [] };
        }),
      ]);
      setAvailability(availRes.data);
      setCourtSlots(slotsRes.data || []);
    } catch (err) {
      console.error('Failed to load sports complex availability:', err);
      setError('Unable to load sports complex inventory. Please verify connection and try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchAvailability();
  }, [fetchAvailability]);

  const handleOpenBooking = (resource, qty = 1, slot = null) => {
    if (availability?.hasActiveSportsBooking) return;
    if (resource.bookingClosed) return;

    setSelectedResource(resource);
    setSelectedQuantity(qty);
    setSelectedSlot(slot);
    setIsModalOpen(true);
  };

  const handleCloseBooking = () => {
    setIsModalOpen(false);
    setSelectedResource(null);
    setSelectedSlot(null);
  };

  const handleQuantityChange = (resourceId, qty) => {
    setCardQuantities((prev) => ({
      ...prev,
      [resourceId]: Number(qty),
    }));
  };

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading sports complex availability & active allocations..." />
      </div>
    );
  }

  const equipmentList = availability?.equipment || [];
  const courtsAndNetsList = availability?.courtsAndNets || [];
  const hasActiveBooking = !!availability?.hasActiveSportsBooking;

  // Stats
  const totalEquipAvailable = equipmentList.reduce((acc, curr) => acc + (curr.availableQuantity || 0), 0);
  const totalCourtsAvailable = courtsAndNetsList.reduce((acc, curr) => acc + (curr.availableQuantity || 0), 0);
  const totalClosedCount = [...equipmentList, ...courtsAndNetsList].filter(
    (r) => r.bookingClosed || r.usableQuantity === 0
  ).length;

  return (
    <div className="sports-complex-page">
      <div style={{ marginBottom: 16 }}>
        <Link to="/student/libraries" className="btn btn-outline btn-sm" style={{ display: 'inline-flex' }}>
          <ArrowLeft size={14} />
          <span>Back to Facilities</span>
        </Link>
      </div>

      <div className="page-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <h1 className="page-title" style={{ margin: 0 }}>Sports Complex & Athletic Facilities</h1>
            <span
              className="facility-type-tag"
              style={{ backgroundColor: '#ecfdf5', color: '#047857', border: '1px solid #a7f3d0' }}
            >
              ATHLETIC FACILITY
            </span>
          </div>
          <p className="page-description">
            Live availability of college sports equipment, indoor courts, and outdoor cricket nets. Instant reservation
            with rolling 1-hour court windows.
          </p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchAvailability(true)}
            disabled={refreshing}
            title="Refresh availability from backend"
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Live Sync'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchAvailability()} />}

      {/* Notice Banner: Active Booking Rule */}
      {hasActiveBooking ? (
        <div
          className="active-request-banner"
          style={{
            borderLeftColor: 'var(--color-reserved)',
            backgroundColor: '#fffdf5',
            marginBottom: 24,
          }}
        >
          <div className="banner-header">
            <div className="banner-title-area">
              <AlertTriangle size={18} style={{ color: 'var(--color-reserved)' }} />
              <h2 className="banner-title">Active Sports Complex Booking in Progress</h2>
            </div>
            <StatusBadge status={availability.activeBookingStatus || 'PENDING'} />
          </div>
          <p style={{ fontSize: 13, color: 'var(--navy-700)', marginBottom: 12 }}>
            You currently hold active reservation <strong>#{availability.activeBookingCode}</strong> for{' '}
            <strong>{availability.activeBookingResourceName}</strong>. To ensure fair access for all campus students,
            you must complete, check out, or cancel your existing session before requesting another sports equipment or
            court.
          </p>
          <div className="banner-actions">
            {availability.activeBookingStatus === 'APPROVED' && (
              <Link to="/student/requests" className="btn btn-primary btn-sm">
                <QrCode size={14} />
                <span>View Digital Pass & QR</span>
              </Link>
            )}
            <Link to="/student/requests" className="btn btn-secondary btn-sm">
              <span>Manage Existing Booking &rarr;</span>
            </Link>
          </div>
        </div>
      ) : (
        <div
          className="section-card"
          style={{
            padding: '12px 18px',
            marginBottom: 24,
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            backgroundColor: 'var(--navy-50)',
            border: '1px solid var(--border-color)',
          }}
        >
          <ShieldCheck size={18} style={{ color: 'var(--color-available)', flexShrink: 0 }} />
          <span style={{ fontSize: 13, color: 'var(--navy-700)' }}>
            <strong>Fair Access Rules:</strong> Each student account may maintain 1 active Sports Complex booking at a
            time. Courts and cricket nets use rolling 1-hour slots opening 30 minutes in advance.
          </span>
        </div>
      )}

      {/* Summary Stats Grid */}
      <div className="stats-grid" style={{ marginBottom: 28 }}>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-available">
            <Dumbbell size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{totalEquipAvailable}</span>
            <span className="stat-label">Equipment Units Available</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-reserved">
            <Trophy size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{totalCourtsAvailable}</span>
            <span className="stat-label">Courts & Nets Open</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-neutral">
            <Layers size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{equipmentList.length + courtsAndNetsList.length}</span>
            <span className="stat-label">Total Sports Resources</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-occupied">
            <Clock size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{totalClosedCount}</span>
            <span className="stat-label">Under Maint. / Closed</span>
          </div>
        </div>
      </div>

      {/* SECTION 1: Sports Equipment */}
      <div style={{ marginBottom: 36 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Dumbbell size={20} style={{ color: 'var(--primary)' }} />
            <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--navy-900)', margin: 0 }}>
              Individual Equipment & Balls
            </h2>
          </div>
          <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>
            Showing {equipmentList.length} equipment types &bull; Select quantity per request
          </span>
        </div>

        <div className="resource-grid">
          {equipmentList.map((res) => {
            const isAvailable = res.availableQuantity > 0 && !res.bookingClosed;
            const isClosed = res.bookingClosed;
            const selectedQty = cardQuantities[res.id] || 1;

            let badgeStatus = 'AVAILABLE';
            let badgeText = res.statusText;
            if (isClosed) {
              badgeStatus = 'MAINTENANCE';
              badgeText = 'Closed';
            } else if (res.usableQuantity === 0) {
              badgeStatus = 'MAINTENANCE';
              badgeText = 'Out of Service';
            } else if (res.availableQuantity === 0) {
              badgeStatus = 'OCCUPIED';
              badgeText = 'Fully Booked';
            } else if (res.availableQuantity < res.totalQuantity) {
              badgeStatus = 'AVAILABLE';
              badgeText = `${res.availableQuantity} of ${res.totalQuantity} Available`;
            }

            return (
              <div
                key={res.id}
                className={`resource-card ${
                  isAvailable ? 'status-available' : isClosed ? 'status-maintenance' : 'status-occupied'
                }`}
                style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
              >
                <div>
                  <div className="resource-card-header" style={{ alignItems: 'flex-start' }}>
                    <div>
                      <h3 className="resource-name" style={{ fontSize: 16, marginBottom: 2 }}>
                        {res.name}
                      </h3>
                      <span style={{ fontSize: 11, color: 'var(--navy-500)', display: 'block' }}>
                        {res.bookingUnit}
                      </span>
                    </div>
                    <StatusBadge status={badgeStatus} label={badgeText} size="small" />
                  </div>

                  {/* Closure reason alert if closed */}
                  {isClosed && (
                    <div
                      style={{
                        margin: '8px 0',
                        padding: '6px 10px',
                        backgroundColor: '#fef2f2',
                        borderRadius: 'var(--radius-sm)',
                        border: '1px solid #fecaca',
                        fontSize: 12,
                        color: '#991b1b',
                      }}
                    >
                      <AlertCircle
                        size={13}
                        style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }}
                      />
                      {res.closureReason ? `Closure reason: ${res.closureReason}` : 'Temporarily closed by administration'}
                    </div>
                  )}

                  {/* Inventory breakdown pill */}
                  <div
                    style={{
                      marginTop: 12,
                      padding: '8px 10px',
                      backgroundColor: 'var(--navy-50)',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: 12,
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
                      <span style={{ color: 'var(--navy-600)' }}>Available Now:</span>
                      <strong style={{ color: isAvailable ? 'var(--color-available)' : 'var(--navy-700)' }}>
                        {res.availableQuantity} units
                      </strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
                      <span style={{ color: 'var(--navy-500)' }}>Active Borrowed:</span>
                      <span style={{ color: 'var(--navy-700)' }}>{res.reservedQuantity} units</span>
                    </div>
                    {res.unavailableQuantity > 0 && (
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: '#b45309' }}>In Maintenance/Damaged:</span>
                        <span style={{ color: '#b45309', fontWeight: 600 }}>{res.unavailableQuantity} units</span>
                      </div>
                    )}
                  </div>
                </div>

                <div
                  className="resource-card-footer"
                  style={{
                    marginTop: 16,
                    paddingTop: 12,
                    borderTop: '1px solid var(--border-color)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    gap: 8,
                  }}
                >
                  {/* Quantity selector for equipment */}
                  {isAvailable && !hasActiveBooking ? (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <label style={{ fontSize: 12, color: 'var(--navy-600)', fontWeight: 500 }}>Qty:</label>
                      <select
                        className="form-select"
                        style={{ padding: '3px 8px', fontSize: 13, width: 60, height: 32 }}
                        value={selectedQty}
                        onChange={(e) => handleQuantityChange(res.id, e.target.value)}
                      >
                        {Array.from({ length: res.availableQuantity }, (_, i) => i + 1).map((q) => (
                          <option key={q} value={q}>
                            {q}
                          </option>
                        ))}
                      </select>
                    </div>
                  ) : (
                    <span style={{ fontSize: 11, color: 'var(--navy-400)' }}>Total Stock: {res.totalQuantity}</span>
                  )}

                  {isAvailable ? (
                    hasActiveBooking ? (
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        disabled
                        title="You already have an active Sports Complex booking"
                      >
                        Active Limit Reached
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => handleOpenBooking(res, selectedQty)}
                      >
                        <span>Request ({selectedQty})</span>
                      </button>
                    )
                  ) : (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      disabled
                      title={isClosed ? 'Bookings are currently closed' : 'No units currently available'}
                    >
                      {isClosed ? 'Closed' : 'Unavailable'}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* SECTION 2: Courts & Nets */}
      <div style={{ marginBottom: 36 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Trophy size={20} style={{ color: 'var(--color-reserved)' }} />
            <h2 style={{ fontSize: 18, fontWeight: 700, color: 'var(--navy-900)', margin: 0 }}>
              Courts & Cricket Nets
            </h2>
          </div>
          <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>
            Showing {courtsAndNetsList.length} facilities &bull; Rolling 1-Hour Time Slots (30m window)
          </span>
        </div>

        {/* Rolling Window Rule Callout */}
        <div
          style={{
            marginBottom: 20,
            padding: '12px 16px',
            backgroundColor: '#eff6ff',
            borderRadius: 'var(--radius-md)',
            border: '1px solid #bfdbfe',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            fontSize: 13,
            color: '#1e40af',
          }}
        >
          <Clock size={20} style={{ flexShrink: 0 }} />
          <div>
            <strong>Rolling 1-Hour Time-Slot Booking Rule:</strong> Badminton, tennis, padel, pickleball courts, and
            cricket nets can only be booked when the slot begins within the next <strong>30 minutes</strong> or less.
            Slots further in the future unlock automatically as time progresses.
          </div>
        </div>

        <div className="resource-grid">
          {courtsAndNetsList.map((res) => {
            const isAvailable = res.availableQuantity > 0 && !res.bookingClosed;
            const isClosed = res.bookingClosed;
            const isUnderMaintenance = !isClosed && res.usableQuantity === 0;

            const resSlots = courtSlots.filter((s) => s.resourceId === res.id);
            const bookableSlots = resSlots.filter((s) => s.canBook);

            let badgeStatus = 'AVAILABLE';
            let badgeText = res.statusText;
            if (isClosed) {
              badgeStatus = 'MAINTENANCE';
              badgeText = 'Closed';
            } else if (isUnderMaintenance) {
              badgeStatus = 'MAINTENANCE';
              badgeText = 'Under Maintenance';
            } else if (res.availableQuantity === 0) {
              badgeStatus = 'OCCUPIED';
              badgeText = 'Fully Booked';
            } else if (res.availableQuantity < res.totalQuantity) {
              badgeStatus = 'AVAILABLE';
              badgeText = `${res.availableQuantity} of ${res.totalQuantity} Available`;
            }

            return (
              <div
                key={res.id}
                className={`resource-card ${
                  isAvailable ? 'status-available' : isClosed ? 'status-maintenance' : 'status-occupied'
                }`}
                style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}
              >
                <div>
                  <div className="resource-card-header" style={{ alignItems: 'flex-start' }}>
                    <div>
                      <h3 className="resource-name" style={{ fontSize: 16, marginBottom: 2 }}>
                        {res.name}
                      </h3>
                      <span style={{ fontSize: 11, color: 'var(--navy-500)', display: 'block' }}>
                        Physical Court &bull; {res.bookingUnit}
                      </span>
                    </div>
                    <StatusBadge status={badgeStatus} label={badgeText} size="small" />
                  </div>

                  {/* Closure or maintenance notes */}
                  {isClosed && (
                    <div
                      style={{
                        margin: '8px 0',
                        padding: '6px 10px',
                        backgroundColor: '#fef2f2',
                        borderRadius: 'var(--radius-sm)',
                        border: '1px solid #fecaca',
                        fontSize: 12,
                        color: '#991b1b',
                      }}
                    >
                      <AlertCircle
                        size={13}
                        style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }}
                      />
                      {res.closureReason ? `Closure reason: ${res.closureReason}` : 'Bookings closed by facility operator'}
                    </div>
                  )}

                  {/* Court capacity breakdown */}
                  <div
                    style={{
                      marginTop: 10,
                      padding: '8px 10px',
                      backgroundColor: 'var(--navy-50)',
                      borderRadius: 'var(--radius-sm)',
                      fontSize: 12,
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
                      <span style={{ color: 'var(--navy-600)' }}>Open Courts/Nets:</span>
                      <strong style={{ color: isAvailable ? 'var(--color-available)' : 'var(--navy-700)' }}>
                        {res.availableQuantity} of {res.totalQuantity} ready
                      </strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
                      <span style={{ color: 'var(--navy-500)' }}>Currently Reserved:</span>
                      <span style={{ color: 'var(--navy-700)' }}>{res.reservedQuantity}</span>
                    </div>
                    {res.unavailableQuantity > 0 && (
                      <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                        <span style={{ color: '#b45309' }}>Under Maintenance:</span>
                        <span style={{ color: '#b45309', fontWeight: 600 }}>{res.unavailableQuantity} court(s)</span>
                      </div>
                    )}
                  </div>

                  {/* Real-time 1-hour slots schedule preview */}
                  {!isClosed && resSlots.length > 0 && (
                    <div style={{ marginTop: 12 }}>
                      <div
                        style={{
                          fontSize: 11,
                          fontWeight: 600,
                          color: 'var(--navy-700)',
                          marginBottom: 6,
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                        }}
                      >
                        <span style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                          <Clock size={12} style={{ color: 'var(--primary)' }} />
                          Rolling Slots Schedule:
                        </span>
                        <span style={{ fontSize: 10, color: 'var(--navy-500)' }}>
                          {bookableSlots.length} bookable now
                        </span>
                      </div>

                      <div
                        style={{
                          display: 'flex',
                          flexDirection: 'column',
                          gap: 5,
                          maxHeight: 120,
                          overflowY: 'auto',
                          paddingRight: 2,
                        }}
                      >
                        {resSlots.slice(0, 4).map((slot, sIdx) => {
                          const canBookSlot = slot.canBook && !hasActiveBooking;

                          let slotPillBg = '#ecfdf5';
                          let slotPillColor = '#059669';
                          let slotPillBorder = '#a7f3d0';
                          let slotBadgeText = 'Book Now';

                          if (slot.status === 'UPCOMING') {
                            slotPillBg = '#fffbeb';
                            slotPillColor = '#b45309';
                            slotPillBorder = '#fde68a';
                            slotBadgeText = slot.notes ? slot.notes.replace('Booking opens', 'Opens') : 'Opens Soon';
                          } else if (slot.status === 'RESERVED' || slot.status === 'OCCUPIED') {
                            slotPillBg = '#fef2f2';
                            slotPillColor = '#dc2626';
                            slotPillBorder = '#fecaca';
                            slotBadgeText = slot.status === 'OCCUPIED' ? 'Occupied' : 'Reserved';
                          } else if (slot.status === 'MAINTENANCE') {
                            slotPillBg = '#f3f4f6';
                            slotPillColor = '#6b7280';
                            slotPillBorder = '#e5e7eb';
                            slotBadgeText = 'Maintenance';
                          }

                          return (
                            <div
                              key={`${slot.resourceItemId}-${slot.slotStartTime}-${sIdx}`}
                              style={{
                                display: 'flex',
                                justifyContent: 'space-between',
                                alignItems: 'center',
                                padding: '4px 8px',
                                backgroundColor: slot.canBook ? '#ffffff' : '#f9fafb',
                                border: `1px solid ${slot.canBook ? 'var(--primary)' : 'var(--border-color)'}`,
                                borderRadius: 'var(--radius-sm)',
                                fontSize: 11,
                              }}
                            >
                              <div>
                                <span style={{ fontWeight: 600, color: 'var(--navy-800)' }}>{slot.slotLabel}</span>{' '}
                                <span style={{ color: 'var(--navy-500)' }}>({slot.resourceItemName})</span>
                              </div>
                              {canBookSlot ? (
                                <button
                                  type="button"
                                  onClick={() => handleOpenBooking(res, 1, slot)}
                                  className="btn btn-primary"
                                  style={{ padding: '2px 8px', fontSize: 10, height: 22 }}
                                >
                                  Book Slot
                                </button>
                              ) : (
                                <span
                                  style={{
                                    fontSize: 10,
                                    fontWeight: 600,
                                    padding: '2px 6px',
                                    borderRadius: 'var(--radius-full)',
                                    color: slotPillColor,
                                    backgroundColor: slotPillBg,
                                    border: `1px solid ${slotPillBorder}`,
                                  }}
                                >
                                  {slotBadgeText}
                                </span>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  )}
                </div>

                <div
                  className="resource-card-footer"
                  style={{
                    marginTop: 16,
                    paddingTop: 12,
                    borderTop: '1px solid var(--border-color)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <span style={{ fontSize: 11, color: 'var(--navy-400)' }}>Total Units: {res.totalQuantity}</span>

                  {isAvailable ? (
                    hasActiveBooking ? (
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        disabled
                        title="You already have an active Sports Complex booking"
                      >
                        Active Limit Reached
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => handleOpenBooking(res, 1, bookableSlots[0] || null)}
                      >
                        <span>Book Court &rarr;</span>
                      </button>
                    )
                  ) : (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      disabled
                      title={isClosed ? 'Court bookings are closed' : 'No courts currently available'}
                    >
                      {isClosed ? 'Closed' : 'Unavailable'}
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Booking Modal */}
      {selectedResource && (
        <SportsBookingModal
          isOpen={isModalOpen}
          onClose={handleCloseBooking}
          resource={selectedResource}
          initialQuantity={selectedQuantity}
          initialSlot={selectedSlot}
          onSuccess={() => fetchAvailability(true)}
        />
      )}
    </div>
  );
}
