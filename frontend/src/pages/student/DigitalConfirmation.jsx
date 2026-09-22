import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { QRCodeSVG } from 'qrcode.react';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import {
  Compass,
  ArrowLeft,
  CheckCircle2,
  Clock,
  MapPin,
  User,
  ShieldCheck,
  AlertCircle,
  Printer,
  Calendar,
} from 'lucide-react';

export default function DigitalConfirmation() {
  const { requestId } = useParams();
  const navigate = useNavigate();

  const [request, setRequest] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchRequest = async () => {
      try {
        const res = await api.get(`/requests/${requestId}`);
        setRequest(res.data);
      } catch (err) {
        console.error('Failed to load request confirmation:', err);
        setError('Unable to load confirmation details. The request may not exist or has been removed.');
      } finally {
        setLoading(false);
      }
    };
    fetchRequest();
  }, [requestId]);

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Generating digital verification pass..." />
      </div>
    );
  }

  if (error || !request) {
    return (
      <div style={{ maxWidth: 600, margin: '40px auto' }}>
        <ErrorMessage message={error || 'Request not found.'} />
        <Link to="/student/requests" className="btn btn-secondary" style={{ marginTop: 16 }}>
          <ArrowLeft size={16} />
          <span>Return to My Requests</span>
        </Link>
      </div>
    );
  }

  const isApproved = request.status === 'APPROVED';
  const isCheckedIn = request.status === 'CHECKED_IN';
  const isValidPassState = isApproved || isCheckedIn;

  return (
    <div className="digital-confirmation-page">
      <div style={{ marginBottom: 20, maxWidth: 580, margin: '0 auto 20px' }}>
        <Link to="/student/requests" className="btn btn-outline btn-sm" style={{ display: 'inline-flex' }}>
          <ArrowLeft size={14} />
          <span>Back to My Requests</span>
        </Link>
      </div>

      <div className="confirmation-wrapper">
        <div className="digital-pass-card">
          {/* Header */}
          <div className="pass-header">
            <div className="pass-brand">
              <div className="brand-icon-box" style={{ background: '#ffffff', color: 'var(--navy-900)' }}>
                <Compass size={18} />
              </div>
              <div>
                <h2 className="pass-title">QueueSense Campus Pass</h2>
                <span className="pass-subtitle">Official Resource Allocation</span>
              </div>
            </div>
            <StatusBadge status={request.status} />
          </div>

          {/* Body */}
          <div className="pass-body">
            {!isValidPassState ? (
              <div style={{ textAlign: 'center', padding: '20px 0', width: '100%' }}>
                <AlertCircle size={44} style={{ color: 'var(--color-occupied)', margin: '0 auto 12px' }} />
                <h3 style={{ fontSize: 18, color: 'var(--navy-900)', marginBottom: 6 }}>
                  Pass Not Active
                </h3>
                <p style={{ fontSize: 14, color: 'var(--navy-600)', marginBottom: 20 }}>
                  This request is currently in <strong>{request.status}</strong> state and cannot be used for check-in.
                </p>
                <Link to="/student/requests" className="btn btn-secondary">
                  View All Requests
                </Link>
              </div>
            ) : (
              <>
                {/* QR Code Frame */}
                <div className="qr-frame">
                  <QRCodeSVG
                    value={request.requestCode}
                    size={180}
                    level="H"
                    includeMargin={true}
                  />
                  <div className="qr-code-text">{request.requestCode}</div>
                </div>

                {/* Details Grid */}
                <div className="pass-details-grid">
                  <div className="meta-group">
                    <span className="meta-label">
                      <User size={12} style={{ display: 'inline', marginRight: 4 }} />
                      Student ID
                    </span>
                    <span className="meta-value">{request.studentId || 'STU-2026-001'}</span>
                  </div>

                  <div className="meta-group">
                    <span className="meta-label">
                      <ShieldCheck size={12} style={{ display: 'inline', marginRight: 4 }} />
                      Student Name
                    </span>
                    <span className="meta-value">{request.studentName || 'Aarav Sharma'}</span>
                  </div>

                  <div className="meta-group">
                    <span className="meta-label">
                      <MapPin size={12} style={{ display: 'inline', marginRight: 4 }} />
                      Facility
                    </span>
                    <span className="meta-value">{request.facilityName}</span>
                  </div>

                  <div className="meta-group">
                    <span className="meta-label">Allocated Resource</span>
                    <span className="meta-value" style={{ color: 'var(--primary)' }}>
                      {request.resourceName}
                    </span>
                  </div>

                  <div className="meta-group">
                    <span className="meta-label">
                      <Calendar size={12} style={{ display: 'inline', marginRight: 4 }} />
                      Expected Arrival
                    </span>
                    <span className="meta-value">{request.expectedArrival}</span>
                  </div>

                  <div className="meta-group">
                    <span className="meta-label">
                      <Clock size={12} style={{ display: 'inline', marginRight: 4 }} />
                      Duration
                    </span>
                    <span className="meta-value">{request.expectedDuration}</span>
                  </div>
                </div>

                {/* Instructions */}
                <div className="pass-instructions">
                  <strong>Check-in Instructions:</strong>
                  <ul style={{ margin: '6px 0 0 18px', lineHeight: 1.4 }}>
                    <li>Present this digital QR code to the facility operator desk upon arrival.</li>
                    <li>Reservations are held for a 10-minute grace period from your expected arrival time.</li>
                    <li>Ensure you check out when concluding your session to release the resource.</li>
                  </ul>
                </div>

                {/* Actions */}
                <div className="pass-footer-actions">
                  <button
                    type="button"
                    className="btn btn-secondary"
                    style={{ flex: 1 }}
                    onClick={() => window.print()}
                  >
                    <Printer size={15} />
                    <span>Print Pass</span>
                  </button>
                  <Link to="/student/requests" className="btn btn-primary" style={{ flex: 1 }}>
                    <span>Done / View Requests</span>
                  </Link>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
