import { useState, useEffect, useRef } from 'react';
import { useLocation, Link } from 'react-router-dom';
import { Html5QrcodeScanner } from 'html5-qrcode';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import {
  QrCode,
  Keyboard,
  CheckCircle2,
  AlertCircle,
  Camera,
  ArrowRight,
  ShieldCheck,
  User,
  MapPin,
  Calendar,
  Clock,
  RotateCcw,
} from 'lucide-react';

export default function OperatorCheckIn() {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const initialCode = queryParams.get('code') || '';

  const [activeTab, setActiveTab] = useState(initialCode ? 'MANUAL' : 'SCAN');
  const [manualCode, setManualCode] = useState(initialCode);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [verifiedRequest, setVerifiedRequest] = useState(null);
  const [scannerActive, setScannerActive] = useState(false);

  const scannerRef = useRef(null);

  // Verification function
  const handleVerify = async (codeToVerify, method = 'MANUAL_CODE') => {
    if (!codeToVerify || !codeToVerify.trim()) {
      setError('Please enter or scan a valid Request Code.');
      return;
    }

    setLoading(true);
    setError(null);
    setVerifiedRequest(null);

    try {
      const response = await api.post('/check-in/verify', {
        requestCode: codeToVerify.trim(),
        checkInMethod: method,
      });
      setVerifiedRequest(response.data);
    } catch (err) {
      console.error('Check-in verification failed:', err);
      const msg =
        err.response?.data?.message ||
        'Check-in verification failed. Please verify that the request exists, is in APPROVED state, and has not expired or already checked in.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleManualSubmit = (e) => {
    e.preventDefault();
    handleVerify(manualCode, 'MANUAL_CODE');
  };

  // Initialize html5-qrcode scanner when activeTab === 'SCAN'
  useEffect(() => {
    if (activeTab === 'SCAN' && !verifiedRequest) {
      setScannerActive(true);
      const scanner = new Html5QrcodeScanner(
        'qr-reader-container',
        {
          fps: 10,
          qrbox: { width: 250, height: 250 },
          rememberLastUsedCamera: true,
          showTorchButtonIfSupported: true,
        },
        false
      );

      scanner.render(
        (decodedText) => {
          console.log('QR Code scanned:', decodedText);
          scanner.clear().catch(console.error);
          setScannerActive(false);
          handleVerify(decodedText, 'QR_SCAN');
        },
        (scanError) => {
          // Normal frame scanner message, ignore
        }
      );

      scannerRef.current = scanner;

      return () => {
        if (scannerRef.current) {
          scannerRef.current.clear().catch(console.error);
        }
      };
    }
  }, [activeTab, verifiedRequest]);

  const handleReset = () => {
    setVerifiedRequest(null);
    setError(null);
    setManualCode('');
    setActiveTab('SCAN');
  };

  return (
    <div className="operator-checkin-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Operator Check-in Terminal</h1>
          <p className="page-description">
            Scan arriving student QR passes or enter reservation codes to verify credentials and activate facility sessions.
          </p>
        </div>
      </div>

      {/* Success View */}
      {verifiedRequest ? (
        <div className="confirmation-wrapper" style={{ maxWidth: 640 }}>
          <div className="digital-pass-card">
            <div className="pass-header" style={{ backgroundColor: 'var(--color-available)' }}>
              <div className="pass-brand">
                <CheckCircle2 size={24} />
                <div>
                  <h2 className="pass-title">Check-in Verified & Confirmed</h2>
                  <span className="pass-subtitle">Resource Status: OCCUPIED</span>
                </div>
              </div>
              <StatusBadge status={verifiedRequest.status} />
            </div>

            <div className="pass-body">
              <div style={{ textAlign: 'center', marginBottom: 20 }}>
                <div style={{ fontSize: 13, color: 'var(--navy-500)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Verified Request Code
                </div>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: 20, fontWeight: 700, color: 'var(--navy-900)', marginTop: 4 }}>
                  {verifiedRequest.requestCode}
                </div>
              </div>

              <div className="pass-details-grid">
                <div className="meta-group">
                  <span className="meta-label">
                    <User size={12} style={{ display: 'inline', marginRight: 4 }} /> Student
                  </span>
                  <span className="meta-value">{verifiedRequest.studentName || 'Aarav Sharma'}</span>
                  <span style={{ fontSize: 11, color: 'var(--navy-500)', fontFamily: 'var(--font-mono)' }}>
                    {verifiedRequest.studentId || 'STU-2026-001'}
                  </span>
                </div>

                <div className="meta-group">
                  <span className="meta-label">
                    <MapPin size={12} style={{ display: 'inline', marginRight: 4 }} /> Facility
                  </span>
                  <span className="meta-value">{verifiedRequest.facilityName}</span>
                </div>

                <div className="meta-group">
                  <span className="meta-label">Allocated Resource</span>
                  <span className="meta-value" style={{ color: 'var(--color-occupied)' }}>
                    {verifiedRequest.resourceName}
                  </span>
                </div>

                <div className="meta-group">
                  <span className="meta-label">
                    <Clock size={12} style={{ display: 'inline', marginRight: 4 }} /> Time & Duration
                  </span>
                  <span className="meta-value">{verifiedRequest.expectedDuration}</span>
                </div>
              </div>

              <div className="pass-instructions" style={{ backgroundColor: 'var(--bg-available)', color: 'var(--text-available)', borderColor: 'var(--border-available)' }}>
                <strong>Check-in Successful:</strong>
                <p style={{ margin: '4px 0 0', fontSize: 13 }}>
                  The resource is now marked <strong>OCCUPIED</strong> in the database. An active usage session has been initialized.
                </p>
              </div>

              <div className="pass-footer-actions">
                <button type="button" className="btn btn-secondary" onClick={handleReset} style={{ flex: 1 }}>
                  <RotateCcw size={15} />
                  <span>Verify Next Student</span>
                </button>
                <Link to="/operator/dashboard" className="btn btn-primary" style={{ flex: 1 }}>
                  <span>Return to Dashboard</span>
                </Link>
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* Check-in Input Panel */
        <div style={{ maxWidth: 640, margin: '0 auto' }}>
          <div className="section-card">
            {/* Tabs */}
            <div className="tab-group" style={{ marginBottom: 20 }}>
              <button
                type="button"
                className={`tab-btn ${activeTab === 'SCAN' ? 'active' : ''}`}
                onClick={() => {
                  setError(null);
                  setActiveTab('SCAN');
                }}
              >
                <Camera size={14} style={{ display: 'inline', marginRight: 6, verticalAlign: 'text-bottom' }} />
                Camera QR Scanner
              </button>
              <button
                type="button"
                className={`tab-btn ${activeTab === 'MANUAL' ? 'active' : ''}`}
                onClick={() => {
                  if (scannerRef.current) scannerRef.current.clear().catch(console.error);
                  setError(null);
                  setActiveTab('MANUAL');
                }}
              >
                <Keyboard size={14} style={{ display: 'inline', marginRight: 6, verticalAlign: 'text-bottom' }} />
                Manual Code Entry
              </button>
            </div>

            {error && <ErrorMessage message={error} onRetry={() => setError(null)} />}

            {activeTab === 'SCAN' ? (
              <div>
                <div id="qr-reader-container" style={{ width: '100%', minHeight: 300 }}></div>
                <p style={{ textAlign: 'center', fontSize: 12, color: 'var(--navy-500)', marginTop: 12 }}>
                  Position the student's digital QR code pass within the camera frame.
                </p>
              </div>
            ) : (
              <form onSubmit={handleManualSubmit}>
                <div className="form-group">
                  <label className="form-label" htmlFor="requestCode">
                    Request Code / Identifier
                  </label>
                  <input
                    id="requestCode"
                    type="text"
                    className="form-input"
                    placeholder="e.g. QS-REQ-2026-0001 or QS-..."
                    value={manualCode}
                    onChange={(e) => setManualCode(e.target.value)}
                    style={{ fontFamily: 'var(--font-mono)', fontSize: 15, textTransform: 'uppercase' }}
                    required
                    disabled={loading}
                    autoFocus
                  />
                  <span className="form-help">
                    Found on the student's digital confirmation screen or request details.
                  </span>
                </div>

                <div className="form-actions" style={{ marginTop: 20 }}>
                  <button
                    type="submit"
                    className="btn btn-primary btn-lg"
                    style={{ width: '100%' }}
                    disabled={loading}
                  >
                    {loading ? (
                      <LoadingSpinner text="Validating credentials..." size={18} />
                    ) : (
                      <>
                        <ShieldCheck size={16} />
                        <span>Verify & Check In Student</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
