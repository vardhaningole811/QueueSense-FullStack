import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Modal from '../common/Modal';
import api from '../../services/api';
import { Clock, Users, Calendar, AlertCircle, Layers, CheckCircle2, Lock, ShieldCheck } from 'lucide-react';
import LoadingSpinner from '../common/LoadingSpinner';

export default function SportsBookingModal({
  isOpen,
  onClose,
  resource,
  initialQuantity = 1,
  initialSlot = null,
  onSuccess,
}) {
  const [quantity, setQuantity] = useState(initialQuantity);
  const [expectedArrival, setExpectedArrival] = useState('Immediate (Next 10 mins)');
  const [expectedDuration, setExpectedDuration] = useState('1 hour');
  const [groupSize, setGroupSize] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Court slot management
  const [slots, setSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState(initialSlot);

  const navigate = useNavigate();

  const isEquipment = resource?.type === 'EQUIPMENT';
  const isCourt = !isEquipment && !!resource;
  const maxAvailable = resource?.availableQuantity || 0;

  useEffect(() => {
    if (resource) {
      setQuantity(Math.min(initialQuantity || 1, resource.availableQuantity || 1));
      setError(null);

      if (isCourt) {
        setLoadingSlots(true);
        api
          .get(`/sports/student/court-slots?resourceId=${resource.id}`)
          .then((res) => {
            const data = res.data || [];
            setSlots(data);
            if (initialSlot) {
              const matched = data.find(
                (s) =>
                  s.resourceItemId === initialSlot.resourceItemId &&
                  s.slotStartTime === initialSlot.slotStartTime
              );
              setSelectedSlot(matched || initialSlot);
            } else {
              const firstBookable = data.find((s) => s.canBook);
              setSelectedSlot(firstBookable || data[0] || null);
            }
          })
          .catch((err) => {
            console.error('Failed to load court slots:', err);
            setError('Could not load time slots for this court. Please retry.');
          })
          .finally(() => setLoadingSlots(false));
      }
    }
  }, [resource, initialQuantity, initialSlot, isCourt]);

  if (!resource) return null;

  const handleBooking = async (e) => {
    e.preventDefault();
    if (!resource) return;

    if (isEquipment) {
      if (quantity < 1) {
        setError('Quantity must be at least 1.');
        return;
      }
      if (quantity > maxAvailable) {
        setError(`Cannot request ${quantity} units. Only ${maxAvailable} available right now.`);
        return;
      }
    } else {
      // Court / Net validation
      if (!selectedSlot) {
        setError('Please select a 1-hour time slot to book.');
        return;
      }
      if (!selectedSlot.canBook) {
        setError(`Cannot book this slot: ${selectedSlot.notes || 'This slot is currently unavailable or opens later.'}`);
        return;
      }
    }

    setLoading(true);
    setError(null);

    try {
      const payload = {
        resourceId: resource.id,
        quantity: isEquipment ? Number(quantity) : 1,
        expectedArrival: isCourt && selectedSlot ? selectedSlot.slotLabel : expectedArrival,
        expectedDuration: isCourt ? '1 hour' : expectedDuration,
        groupSize: Number(groupSize),
      };

      if (isCourt && selectedSlot) {
        payload.resourceItemId = selectedSlot.resourceItemId;
        payload.slotStartTime = selectedSlot.slotStartTime;
        payload.slotEndTime = selectedSlot.slotEndTime;
      }

      const response = await api.post('/requests', payload);
      if (onSuccess) {
        onSuccess(response.data);
      }
      onClose();
      navigate('/student/requests');
    } catch (err) {
      console.error('Failed to create sports booking request:', err);
      const msg =
        err.response?.data?.message ||
        'Failed to submit request. You may already have an active Sports Complex booking or insufficient inventory.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Book ${resource.name}`}>
      <form onSubmit={handleBooking}>
        <div
          style={{
            marginBottom: 16,
            padding: '12px 14px',
            backgroundColor: 'var(--navy-50)',
            borderRadius: 'var(--radius-md)',
            border: '1px solid var(--border-color)',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
            <span style={{ fontSize: 13, color: 'var(--navy-600)' }}>Category:</span>
            <span className="facility-type-tag" style={{ textTransform: 'uppercase', fontSize: 11 }}>
              {isEquipment ? 'Sports Equipment' : 'Court / Cricket Net'}
            </span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
            <span style={{ fontSize: 13, color: 'var(--navy-600)' }}>Unit Standard:</span>
            <span style={{ fontSize: 13, fontWeight: 500, color: 'var(--navy-800)' }}>{resource.bookingUnit}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: 13, color: 'var(--navy-600)' }}>Available Now:</span>
            <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--color-available)' }}>
              {resource.availableQuantity} of {resource.totalQuantity} units
            </span>
          </div>
        </div>

        {error && (
          <div className="error-message-card" style={{ marginBottom: 16 }}>
            <div className="error-message-header">
              <AlertCircle className="error-icon" size={16} />
              <div className="error-desc">{error}</div>
            </div>
          </div>
        )}

        {/* 30-Minute Rolling Booking Window Rule Notice for Courts */}
        {isCourt && (
          <div
            style={{
              padding: '10px 12px',
              backgroundColor: '#eff6ff',
              borderRadius: 'var(--radius-md)',
              border: '1px solid #bfdbfe',
              marginBottom: 16,
              fontSize: 12,
              color: '#1e40af',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 600, marginBottom: 2 }}>
              <Clock size={14} />
              <span>Rolling 1-Hour Time Slot Rule</span>
            </div>
            <span>
              Booking opens <strong>exactly 30 minutes</strong> before each 1-hour slot starts. Slots further in the
              future unlock automatically as their 30-minute window arrives.
            </span>
          </div>
        )}

        {/* Court Slot Selection */}
        {isCourt && (
          <div className="form-group" style={{ marginBottom: 16 }}>
            <label className="form-label">
              <Clock size={14} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
              Choose Court & 1-Hour Slot
            </label>
            {loadingSlots ? (
              <div style={{ padding: '16px 0', textAlign: 'center' }}>
                <LoadingSpinner text="Loading real-time slot availability..." size={16} />
              </div>
            ) : slots.length === 0 ? (
              <div style={{ padding: 12, fontSize: 13, color: 'var(--navy-600)', backgroundColor: 'var(--navy-50)', borderRadius: 'var(--radius-sm)' }}>
                No active slots scheduled for today.
              </div>
            ) : (
              <div
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                  maxHeight: 220,
                  overflowY: 'auto',
                  border: '1px solid var(--border-color)',
                  borderRadius: 'var(--radius-md)',
                  padding: 8,
                }}
              >
                {slots.map((slot, index) => {
                  const isSelected =
                    selectedSlot &&
                    selectedSlot.resourceItemId === slot.resourceItemId &&
                    selectedSlot.slotStartTime === slot.slotStartTime;

                  const canSelect = slot.canBook;

                  let badgeColor = '#059669';
                  let badgeBg = '#ecfdf5';
                  let badgeBorder = '#a7f3d0';
                  let badgeText = 'Available Now';

                  if (slot.status === 'UPCOMING') {
                    badgeColor = '#b45309';
                    badgeBg = '#fffbeb';
                    badgeBorder = '#fde68a';
                    badgeText = slot.notes || 'Opens Soon';
                  } else if (slot.status === 'RESERVED') {
                    badgeColor = '#d97706';
                    badgeBg = '#fef3c7';
                    badgeBorder = '#fcd34d';
                    badgeText = 'Reserved';
                  } else if (slot.status === 'OCCUPIED') {
                    badgeColor = '#dc2626';
                    badgeBg = '#fef2f2';
                    badgeBorder = '#fecaca';
                    badgeText = 'Occupied';
                  } else if (slot.status === 'MAINTENANCE') {
                    badgeColor = '#6b7280';
                    badgeBg = '#f3f4f6';
                    badgeBorder = '#e5e7eb';
                    badgeText = 'Maintenance';
                  } else if (slot.status === 'PASSED') {
                    badgeColor = '#9ca3af';
                    badgeBg = '#f9fafb';
                    badgeBorder = '#f3f4f6';
                    badgeText = 'Ended';
                  }

                  return (
                    <div
                      key={`${slot.resourceItemId}-${slot.slotStartTime}-${index}`}
                      onClick={() => {
                        if (canSelect) {
                          setSelectedSlot(slot);
                          setError(null);
                        }
                      }}
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        padding: '8px 12px',
                        borderRadius: 'var(--radius-sm)',
                        border: isSelected
                          ? '2px solid var(--primary)'
                          : '1px solid var(--border-color)',
                        backgroundColor: isSelected
                          ? '#f0fdf4'
                          : canSelect
                          ? '#ffffff'
                          : '#f9fafb',
                        cursor: canSelect ? 'pointer' : 'not-allowed',
                        opacity: canSelect ? 1 : 0.75,
                        transition: 'all 0.15s ease',
                      }}
                    >
                      <div>
                        <div style={{ fontSize: 13, fontWeight: 600, color: 'var(--navy-900)' }}>
                          {slot.slotLabel} &bull;{' '}
                          <span style={{ fontWeight: 500, color: 'var(--navy-600)' }}>
                            {slot.resourceItemName}
                          </span>
                        </div>
                        <div style={{ fontSize: 11, color: 'var(--navy-500)', marginTop: 2 }}>
                          {slot.notes}
                        </div>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <span
                          style={{
                            fontSize: 11,
                            fontWeight: 600,
                            padding: '3px 8px',
                            borderRadius: 'var(--radius-full)',
                            color: badgeColor,
                            backgroundColor: badgeBg,
                            border: `1px solid ${badgeBorder}`,
                          }}
                        >
                          {badgeText}
                        </span>
                        {isSelected && <CheckCircle2 size={16} style={{ color: 'var(--primary)' }} />}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
            {selectedSlot && (
              <span className="form-help" style={{ marginTop: 6, display: 'block' }}>
                Selected: <strong>{selectedSlot.resourceItemName}</strong> ({selectedSlot.slotLabel})
              </span>
            )}
          </div>
        )}

        {/* Quantity Selection for Equipment */}
        {isEquipment && (
          <div className="form-group">
            <label className="form-label">
              <Layers size={14} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
              Quantity Needed
            </label>
            <select
              className="form-select"
              value={quantity}
              onChange={(e) => setQuantity(Number(e.target.value))}
              disabled={loading || maxAvailable === 0}
            >
              {Array.from({ length: maxAvailable }, (_, i) => i + 1).map((qty) => (
                <option key={qty} value={qty}>
                  {qty} {qty === 1 ? 'unit' : 'units'} ({resource.bookingUnit})
                </option>
              ))}
            </select>
            <span className="form-help">
              Select how many pieces of equipment you need. You cannot request more than the {maxAvailable} currently
              available.
            </span>
          </div>
        )}

        {/* Arrival Time for Equipment */}
        {isEquipment && (
          <div className="form-group">
            <label className="form-label">
              <Calendar size={14} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
              Expected Arrival Time
            </label>
            <select
              className="form-select"
              value={expectedArrival}
              onChange={(e) => setExpectedArrival(e.target.value)}
              disabled={loading}
            >
              <option value="Immediate (Next 10 mins)">Immediate (Next 10 mins)</option>
              <option value="In 15 minutes">In 15 minutes</option>
              <option value="In 30 minutes">In 30 minutes</option>
              <option value="In 45 minutes">In 45 minutes</option>
              <option value="In 1 hour">In 1 hour</option>
              <option value="In 2 hours">In 2 hours</option>
            </select>
            <span className="form-help">
              Grace period: 10 minutes from expected arrival before automatic hold release.
            </span>
          </div>
        )}

        {/* Duration for Equipment */}
        {isEquipment && (
          <div className="form-group">
            <label className="form-label">
              <Clock size={14} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
              Booking Duration
            </label>
            <select
              className="form-select"
              value={expectedDuration}
              onChange={(e) => setExpectedDuration(e.target.value)}
              disabled={loading}
            >
              <option value="30 minutes">30 minutes</option>
              <option value="1 hour">1 hour</option>
              <option value="1.5 hours">1.5 hours</option>
              <option value="2 hours">2 hours</option>
              <option value="3 hours">3 hours</option>
            </select>
          </div>
        )}

        {/* Group Size */}
        <div className="form-group">
          <label className="form-label">
            <Users size={14} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
            Participants / Players
          </label>
          <select
            className="form-select"
            value={groupSize}
            onChange={(e) => setGroupSize(Number(e.target.value))}
            disabled={loading}
          >
            <option value={1}>1 Player (Individual)</option>
            <option value={2}>2 Players (Pair)</option>
            <option value={3}>3 Players</option>
            <option value={4}>4 Players (Full Doubles / Squad)</option>
          </select>
        </div>

        <div className="form-actions">
          <button type="button" onClick={onClose} className="btn btn-secondary" disabled={loading}>
            Cancel
          </button>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={loading || (isEquipment ? maxAvailable === 0 : !selectedSlot || !selectedSlot.canBook)}
          >
            {loading ? (
              <LoadingSpinner text="Submitting..." size={16} />
            ) : isCourt ? (
              'Confirm 1-Hour Court Booking'
            ) : (
              'Submit Booking Request'
            )}
          </button>
        </div>
      </form>
    </Modal>
  );
}
