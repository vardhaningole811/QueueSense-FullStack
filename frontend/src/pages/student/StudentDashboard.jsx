import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import {
  BookOpen,
  Calendar,
  Clock,
  QrCode,
  LogOut,
  ArrowRight,
  Sparkles,
  Building2,
  CheckCircle,
  RefreshCw,
} from 'lucide-react';

export default function StudentDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [facilities, setFacilities] = useState([]);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [checkoutLoading, setCheckoutLoading] = useState(false);

  const fetchDashboardData = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const [facRes, reqRes] = await Promise.all([
        api.get('/facilities'),
        api.get('/requests/my'),
      ]);
      setFacilities(facRes.data || []);
      setRequests(reqRes.data || []);
    } catch (err) {
      console.error('Failed to load student dashboard:', err);
      setError('Unable to load latest campus facility data. Please check your connection.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  // Determine primary active request
  // Priority: CHECKED_IN > APPROVED > PENDING
  const activeRequest =
    requests.find((r) => r.status === 'CHECKED_IN') ||
    requests.find((r) => r.status === 'APPROVED') ||
    requests.find((r) => r.status === 'PENDING') ||
    null;

  const handleCheckOut = async (requestId) => {
    setCheckoutLoading(true);
    try {
      await api.post(`/requests/${requestId}/check-out`);
      await fetchDashboardData(true);
    } catch (err) {
      console.error('Checkout failed:', err);
      alert(err.response?.data?.message || 'Checkout failed. Please try again.');
    } finally {
      setCheckoutLoading(false);
    }
  };

  const handleCancel = async (requestId) => {
    if (!window.confirm('Are you sure you want to cancel this pending request?')) return;
    try {
      await api.post(`/requests/${requestId}/cancel`);
      await fetchDashboardData(true);
    } catch (err) {
      console.error('Cancel failed:', err);
      alert(err.response?.data?.message || 'Cancel failed.');
    }
  };

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading campus facilities & request status..." />
      </div>
    );
  }

  return (
    <div className="student-dashboard">
      <div className="page-header">
        <div>
          <h1 className="page-title">Welcome, {user?.name}</h1>
          <p className="page-description">
            Student ID: <strong>{user?.studentId || 'STU-2026-001'}</strong> &bull; Department of Computer Science & Engineering
          </p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchDashboardData(true)}
            disabled={refreshing}
            title="Refresh latest status from backend"
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Sync Status'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchDashboardData()} />}

      {/* Priority Active Request Section */}
      {activeRequest ? (
        <div className="active-request-banner">
          <div className="banner-header">
            <div className="banner-title-area">
              <Sparkles size={18} style={{ color: 'var(--primary)' }} />
              <h2 className="banner-title">Active Booking Status</h2>
            </div>
            <StatusBadge status={activeRequest.status} />
          </div>

          <div className="banner-content">
            <div className="meta-group">
              <span className="meta-label">Facility</span>
              <span className="meta-value">{activeRequest.facilityName}</span>
            </div>
            <div className="meta-group">
              <span className="meta-label">Resource</span>
              <span className="meta-value">{activeRequest.resourceName}</span>
            </div>
            <div className="meta-group">
              <span className="meta-label">Request Code</span>
              <span className="meta-value" style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
                {activeRequest.requestCode}
              </span>
            </div>
            <div className="meta-group">
              <span className="meta-label">Expected Arrival / Slot</span>
              <span className="meta-value">{activeRequest.expectedArrival}</span>
            </div>
            <div className="meta-group">
              <span className="meta-label">Duration</span>
              <span className="meta-value">{activeRequest.expectedDuration}</span>
            </div>
          </div>

          <div className="banner-actions">
            {activeRequest.status === 'APPROVED' && (
              <Link to={`/student/confirmation/${activeRequest.id}`} className="btn btn-primary">
                <QrCode size={16} />
                <span>View Digital Confirmation & QR</span>
              </Link>
            )}

            {activeRequest.status === 'CHECKED_IN' && (
              <button
                type="button"
                className="btn btn-danger-solid"
                onClick={() => handleCheckOut(activeRequest.id)}
                disabled={checkoutLoading}
              >
                <LogOut size={16} />
                <span>{checkoutLoading ? 'Checking Out...' : 'Complete Session / Check Out'}</span>
              </button>
            )}

            {activeRequest.status === 'PENDING' && (
              <>
                <span style={{ fontSize: 13, color: 'var(--navy-600)', marginRight: 'auto' }}>
                  Awaiting operator verification. You can review your request or cancel if no longer needed.
                </span>
                <button
                  type="button"
                  className="btn btn-danger btn-sm"
                  onClick={() => handleCancel(activeRequest.id)}
                >
                  Cancel Request
                </button>
              </>
            )}

            <Link to="/student/requests" className="btn btn-secondary btn-sm" style={{ marginLeft: 'auto' }}>
              <span>View All Requests</span>
              <ArrowRight size={14} />
            </Link>
          </div>
        </div>
      ) : (
        <div className="active-request-banner" style={{ borderLeftColor: 'var(--navy-400)' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
            <div>
              <h2 className="banner-title" style={{ marginBottom: 4 }}>No Active Resource Reservation</h2>
              <p style={{ fontSize: 13, color: 'var(--navy-600)' }}>
                Check campus facilities below to find available study seats, PC terminals, and discussion rooms.
              </p>
            </div>
            <Link to="/student/libraries" className="btn btn-primary">
              <BookOpen size={16} />
              <span>Browse Facilities</span>
            </Link>
          </div>
        </div>
      )}

      {/* Facilities Quick View */}
      <div className="section-card">
        <div className="section-card-header">
          <h2 className="section-card-title">
            <Building2 size={18} />
            <span>Campus Study & Athletic Facilities</span>
          </h2>
          <Link to="/student/libraries" style={{ fontSize: 13, fontWeight: 500 }}>
            View Details &rarr;
          </Link>
        </div>

        <div className="facility-grid">
          {facilities.map((fac) => (
            <div key={fac.id} className="facility-card">
              <div>
                <div className="facility-card-header">
                  <h3 className="facility-title">{fac.name}</h3>
                  <span className="facility-type-tag">{fac.category}</span>
                </div>
                <p className="facility-desc">{fac.description}</p>
              </div>
              <div className="facility-card-footer">
                <span style={{ fontSize: 12, color: 'var(--color-available)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <CheckCircle size={14} /> Live Availability
                </span>
                <Link to={`/student/libraries/${fac.id}`} className="btn btn-secondary btn-sm">
                  <span>View Resources</span>
                  <ArrowRight size={13} />
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Recent Requests Feed */}
      {requests.length > 0 && (
        <div className="section-card">
          <div className="section-card-header">
            <h2 className="section-card-title">
              <Clock size={18} />
              <span>Recent Requests & Activity</span>
            </h2>
            <Link to="/student/requests" style={{ fontSize: 13, fontWeight: 500 }}>
              All Requests ({requests.length}) &rarr;
            </Link>
          </div>

          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Request ID</th>
                  <th>Facility</th>
                  <th>Resource</th>
                  <th>Time Slot</th>
                  <th>Duration</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {requests.slice(0, 5).map((req) => (
                  <tr key={req.id}>
                    <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12, fontWeight: 600 }}>
                      {req.requestCode}
                    </td>
                    <td>{req.facilityName}</td>
                    <td><strong>{req.resourceName}</strong></td>
                    <td>{req.expectedArrival}</td>
                    <td>{req.expectedDuration}</td>
                    <td>
                      <StatusBadge status={req.status} size="small" />
                    </td>
                    <td>
                      {req.status === 'APPROVED' ? (
                        <Link to={`/student/confirmation/${req.id}`} className="btn btn-primary btn-sm">
                          <QrCode size={13} />
                          <span>QR Pass</span>
                        </Link>
                      ) : (
                        <Link to="/student/requests" className="btn btn-outline btn-sm">
                          Details
                        </Link>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
