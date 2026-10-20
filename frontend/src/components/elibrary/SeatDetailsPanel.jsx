import { useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';
import StatusBadge from '../common/StatusBadge';
import {
  Monitor,
  Calendar,
  Clock,
  Users,
  CheckCircle,
  AlertCircle,
  X,
  ArrowRight,
  ShieldAlert,
  QrCode,
  Sparkles,
} from 'lucide-react';

export default function SeatDetailsPanel({
  seat,
  onClose,
  onBookingSuccess,
}) {
  const [expectedArrival, setExpectedArrival] = useState('Immediate (Next 10 mins)');
  const [expectedDuration, setExpectedDuration] = useState('1 hour');
  const [groupSize, setGroupSize] = useState(1);

  const [step, setStep] = useState('SELECT'); // 'SELECT' | 'CONFIRM' | 'SUCCESS'
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [bookingResult, setBookingResult] = useState(null);

  if (!seat) {
    return (
      <div className="seat-detail-panel" style={{ textAlign: 'center', padding: '40px 20px' }}>
        <div
          style={{
            width: 52,
            height: 52,
            borderRadius: '50%',
            backgroundColor: 'var(--navy-100)',
            color: 'var(--navy-500)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto 14px',
          }}
        >
          <Monitor size={26} />
        </div>
        <h3 style={{ fontSize: 16, fontWeight: 700, color: 'var(--navy-800)', marginBottom: 6 }}>
          No Workstation Selected
        </h3>
        <p style={{ fontSize: 13, color: 'var(--navy-500)', lineHeight: 1.45, margin: 0 }}>
          Click any green workstation button on the floor plan to inspect its hardware details and configure a study reservation.
        </p>
      </div>
    );
  }

  const isAvailable = seat.status === 'AVAILABLE' && seat.bookable;
  const isMyBooking = seat.isMyBooking;

  const handleSubmitBooking = async (e) => {
    if (e) e.preventDefault();
    if (!seat.resourceId) {
      setError('Invalid resource ID. Please refresh and try again.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const payload = {
        resourceId: seat.resourceId,
        expectedArrival,
        expectedDuration,
        groupSize: Number(groupSize),
      };

      const res = await api.post('/requests', payload);
      setBookingResult(res.data);
      setStep('SUCCESS');
      if (onBookingSuccess) {
        onBookingSuccess(res.data);
      }
    } catch (err) {
      console.error('Workstation booking failed:', err);
      const msg =
        err.response?.data?.message ||
        'Failed to submit booking request. You may already have an active reservation or this seat was just reserved.';
      setError(msg);
      setStep('SELECT');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="seat-detail-panel">
      {/* Header */}
      <div className="panel-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
            <h3 className="panel-title">Workstation #{seat.seatNumber}</h3>
            <StatusBadge status={seat.status} size="small" />
          </div>
          <div className="panel-subtitle">
            {seat.resourceName || `Seat ${seat.seatNumber}`} &bull; {seat.type || 'PC'} Station
          </div>
        </div>
        <button
          type="button"
          className="btn btn-outline btn-sm"
          style={{ padding: '4px 8px', borderRadius: '50%' }}
          onClick={onClose}
          title="Clear Selection"
          aria-label="Close details"
        >
          <X size={14} />
        </button>
      </div>

      {/* Error alert */}
      {error && (
        <div className="error-message-card" style={{ marginBottom: 14 }}>
          <div className="error-message-header">
            <AlertCircle className="error-icon" size={16} />
            <div className="error-desc">{error}</div>
          </div>
        </div>
      )}

      {/* Step: SUCCESS */}
      {step === 'SUCCESS' && bookingResult && (
        <div style={{ textAlign: 'center', padding: '10px 0' }}>
          <div
            style={{
              width: 52,
              height: 52,
              borderRadius: '50%',
              backgroundColor: '#ecfdf5',
              color: '#059669',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 12px',
            }}
          >
            <CheckCircle size={28} />
          </div>
          <h4 style={{ fontSize: 16, fontWeight: 700, color: 'var(--navy-900)', margin: '0 0 4px' }}>
            Request Submitted!
          </h4>
          <p style={{ fontSize: 13, color: 'var(--navy-600)', marginBottom: 14 }}>
            Your booking request for <strong>{bookingResult.resourceName}</strong> has been logged.
          </p>

          <div
            style={{
              backgroundColor: 'var(--navy-50)',
              border: '1px solid var(--navy-200)',
              borderRadius: 'var(--radius-md)',
              padding: 12,
              marginBottom: 16,
              textAlign: 'left',
              fontSize: 13,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span style={{ color: 'var(--navy-500)' }}>Request Code:</span>
              <strong style={{ fontFamily: 'var(--font-mono)', color: 'var(--primary)' }}>
                {bookingResult.requestCode}
              </strong>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span style={{ color: 'var(--navy-500)' }}>Status:</span>
              <StatusBadge status={bookingResult.status} size="small" />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--navy-500)' }}>Expected Arrival:</span>
              <span style={{ fontWeight: 600 }}>{bookingResult.expectedArrival}</span>
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <Link to="/student/requests" className="btn btn-primary btn-sm">
              <span>View My Requests Queue</span>
              <ArrowRight size={14} />
            </Link>
            <button
              type="button"
              className="btn btn-outline btn-sm"
              onClick={() => {
                setStep('SELECT');
                onClose();
              }}
            >
              Back to Floor Plan
            </button>
          </div>
        </div>
      )}

      {/* Step: CONFIRM */}
      {step === 'CONFIRM' && (
        <div>
          <div className="booking-confirm-card">
            <div className="confirm-title">
              <Sparkles size={16} />
              <span>Confirm Workstation Reservation</span>
            </div>
            <div className="confirm-details-list">
              <div>
                Workstation: <strong>#{seat.seatNumber} ({seat.resourceName})</strong>
              </div>
              <div>
                Arrival Time: <strong>{expectedArrival}</strong>
              </div>
              <div>
                Duration: <strong>{expectedDuration}</strong>
              </div>
              <div>
                User: <strong>Student Account (Individual Study)</strong>
              </div>
            </div>
          </div>

          <p style={{ fontSize: 12, color: 'var(--navy-600)', margin: '14px 0', lineHeight: 1.45 }}>
            <strong>Note:</strong> Upon submitting, your request will enter the operator approval queue with status <strong>PENDING</strong>. Once approved, you can present your digital QR pass at the E-Library reception desk.
          </p>

          <div style={{ display: 'flex', gap: 10 }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ flex: 1 }}
              onClick={() => setStep('SELECT')}
              disabled={submitting}
            >
              Modify
            </button>
            <button
              type="button"
              className="btn btn-primary btn-sm"
              style={{ flex: 2 }}
              onClick={handleSubmitBooking}
              disabled={submitting}
            >
              {submitting ? 'Submitting...' : 'Confirm Request'}
            </button>
          </div>
        </div>
      )}

      {/* Step: SELECT (Default view) */}
      {step === 'SELECT' && (
        <div>
          {/* Workstation Specification Overview */}
          <div style={{ marginBottom: 14 }}>
            <div className="detail-row">
              <span className="detail-label">Hardware Type</span>
              <span className="detail-value">High-Speed Workstation PC</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Location</span>
              <span className="detail-value">Floor Level 1 &bull; Main Hall</span>
            </div>
            <div className="detail-row">
              <span className="detail-label">Network</span>
              <span className="detail-value">Gigabit LAN & Campus Intranet</span>
            </div>
            <div className="detail-row" style={{ borderBottom: 'none' }}>
              <span className="detail-label">Current Status</span>
              <span className="detail-value">
                <StatusBadge status={seat.status} size="small" />
              </span>
            </div>
          </div>

          {/* Status-specific banners and actions */}
          {isMyBooking && (
            <div className="booking-alert-banner alert-available" style={{ marginBottom: 16 }}>
              <QrCode size={18} style={{ color: '#059669', flexShrink: 0 }} />
              <div>
                <strong style={{ display: 'block', marginBottom: 2 }}>This is Your Active Booking!</strong>
                <span>
                  Request Code: <code>{seat.currentRequestCode}</code> &bull; Status: {seat.status}
                </span>
                <div style={{ marginTop: 8 }}>
                  <Link to="/student/requests" className="btn btn-primary btn-sm" style={{ padding: '3px 8px', fontSize: 12 }}>
                    <span>Manage My Pass</span>
                    <ArrowRight size={12} />
                  </Link>
                </div>
              </div>
            </div>
          )}

          {seat.status === 'OCCUPIED' && !isMyBooking && (
            <div className="booking-alert-banner alert-occupied">
              <ShieldAlert size={18} style={{ color: '#dc2626', flexShrink: 0 }} />
              <div>
                <strong style={{ display: 'block', marginBottom: 2 }}>Workstation In Active Use</strong>
                <span>This computer workstation is currently occupied by another student and is unavailable for booking.</span>
              </div>
            </div>
          )}

          {seat.status === 'MAINTENANCE' && (
            <div className="booking-alert-banner alert-maintenance">
              <AlertCircle size={18} style={{ color: '#64748b', flexShrink: 0 }} />
              <div>
                <strong style={{ display: 'block', marginBottom: 2 }}>Under Maintenance</strong>
                <span>This workstation is currently undergoing technical servicing and cannot be booked.</span>
              </div>
            </div>
          )}

          {(seat.status === 'RESERVED' || seat.status === 'PENDING') && !isMyBooking && (
            <div className="booking-alert-banner alert-reserved">
              <Clock size={18} style={{ color: '#d97706', flexShrink: 0 }} />
              <div>
                <strong style={{ display: 'block', marginBottom: 2 }}>
                  {seat.status === 'PENDING' ? 'Pending Approval' : 'Temporarily Reserved'}
                </strong>
                <span>
                  This workstation has a booking in progress and is temporarily locked to prevent conflicting requests.
                </span>
              </div>
            </div>
          )}

          {/* Booking options form if available */}
          {isAvailable && (
            <div className="booking-form-section">
              <div className="form-group" style={{ marginBottom: 12 }}>
                <label className="form-label">
                  <Calendar size={13} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
                  Expected Arrival
                </label>
                <select
                  className="form-select"
                  value={expectedArrival}
                  onChange={(e) => setExpectedArrival(e.target.value)}
                >
                  <option value="Immediate (Next 10 mins)">Immediate (Next 10 mins)</option>
                  <option value="In 15 minutes">In 15 minutes</option>
                  <option value="In 30 minutes">In 30 minutes</option>
                  <option value="In 45 minutes">In 45 minutes</option>
                  <option value="In 1 hour">In 1 hour</option>
                </select>
                <span className="form-help" style={{ fontSize: 11, color: 'var(--navy-500)' }}>
                  Workstation held for 10 minutes past expected arrival.
                </span>
              </div>

              <div className="form-group" style={{ marginBottom: 12 }}>
                <label className="form-label">
                  <Clock size={13} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
                  Session Duration
                </label>
                <select
                  className="form-select"
                  value={expectedDuration}
                  onChange={(e) => setExpectedDuration(e.target.value)}
                >
                  <option value="30 minutes">30 minutes</option>
                  <option value="1 hour">1 hour</option>
                  <option value="2 hours">2 hours</option>
                  <option value="3 hours">3 hours</option>
                  <option value="4 hours">4 hours</option>
                </select>
              </div>

              <div className="form-group" style={{ marginBottom: 16 }}>
                <label className="form-label">
                  <Users size={13} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
                  Group Size
                </label>
                <select
                  className="form-select"
                  value={groupSize}
                  onChange={(e) => setGroupSize(e.target.value)}
                >
                  <option value={1}>1 Student (Individual Workstation)</option>
                </select>
              </div>

              <button
                type="button"
                className="btn btn-primary"
                style={{ width: '100%', justifyContent: 'center' }}
                onClick={() => setStep('CONFIRM')}
              >
                <span>Continue to Booking</span>
                <ArrowRight size={15} />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
