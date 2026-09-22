import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import {
  ShieldCheck,
  CheckCircle,
  Clock,
  Lock,
  Layers,
  QrCode,
  ArrowRight,
  RefreshCw,
  Check,
  X,
  Building2,
  Users,
} from 'lucide-react';

export default function OperatorDashboard() {
  const { user } = useAuth();

  const [stats, setStats] = useState(null);
  const [pendingRequests, setPendingRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [actionId, setActionId] = useState(null);

  const fetchDashboardData = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const [statsRes, pendingRes] = await Promise.all([
        api.get('/operator/dashboard'),
        api.get('/operator/requests/pending'),
      ]);
      setStats(statsRes.data);
      setPendingRequests(pendingRes.data || []);
    } catch (err) {
      console.error('Failed to load operator dashboard:', err);
      setError('Unable to load facility operational metrics. Please ensure you are logged in with Operator privileges.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  const handleApprove = async (requestId) => {
    setActionId(requestId);
    try {
      await api.post(`/operator/requests/${requestId}/approve`);
      await fetchDashboardData(true);
    } catch (err) {
      console.error('Approve failed:', err);
      alert(err.response?.data?.message || 'Approval failed.');
    } finally {
      setActionId(null);
    }
  };

  const handleReject = async (requestId) => {
    const reason = window.prompt('Enter optional rejection reason:', 'Resource allocation conflict or maintenance');
    if (reason === null) return; // User cancelled prompt

    setActionId(requestId);
    try {
      await api.post(`/operator/requests/${requestId}/reject`, { reason });
      await fetchDashboardData(true);
    } catch (err) {
      console.error('Reject failed:', err);
      alert(err.response?.data?.message || 'Rejection failed.');
    } finally {
      setActionId(null);
    }
  };

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading facility operator metrics..." />
      </div>
    );
  }

  return (
    <div className="operator-dashboard-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Facility Operator Control Panel</h1>
          <p className="page-description">
            Operator: <strong>{user?.name}</strong> &bull; Assigned: <strong>{user?.assignedFacilityName || 'E-Library & Reading Room'}</strong>
          </p>
        </div>
        <div className="header-actions">
          <Link to="/operator/check-ins" className="btn btn-primary btn-sm">
            <QrCode size={14} />
            <span>Open QR Check-in</span>
          </Link>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchDashboardData(true)}
            disabled={refreshing}
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Sync Live Data'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchDashboardData()} />}

      {/* Real Database Statistics Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-pending">
            <Clock size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats?.pendingRequestsCount ?? 0}</span>
            <span className="stat-label">Pending Requests</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-available">
            <CheckCircle size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats?.availableResourcesCount ?? 0}</span>
            <span className="stat-label">Available Resources</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-reserved">
            <Layers size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats?.reservedResourcesCount ?? 0}</span>
            <span className="stat-label">Reserved (Approved)</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-occupied">
            <Lock size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats?.occupiedResourcesCount ?? 0}</span>
            <span className="stat-label">Currently Occupied</span>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-neutral">
            <ShieldCheck size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{stats?.todayCheckInsCount ?? 0}</span>
            <span className="stat-label">Today's Check-ins</span>
          </div>
        </div>
      </div>

      {/* Pending Requests Section */}
      <div className="section-card">
        <div className="section-card-header">
          <h2 className="section-card-title">
            <Clock size={18} />
            <span>Live Pending Approval Queue ({pendingRequests.length})</span>
          </h2>
          <Link to="/operator/requests" style={{ fontSize: 13, fontWeight: 500 }}>
            Manage All Requests &rarr;
          </Link>
        </div>

        {pendingRequests.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '32px 16px', color: 'var(--navy-500)' }}>
            <CheckCircle size={32} style={{ color: 'var(--color-available)', margin: '0 auto 8px' }} />
            <div style={{ fontSize: 15, fontWeight: 600, color: 'var(--navy-800)' }}>
              No Pending Requests in Queue
            </div>
            <p style={{ fontSize: 13, marginTop: 4 }}>
              All incoming student bookings for your facility have been processed.
            </p>
          </div>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Request ID</th>
                  <th>Student Info</th>
                  <th>Facility & Resource</th>
                  <th>Arrival Slot</th>
                  <th>Duration</th>
                  <th>Group Size</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {pendingRequests.map((req) => (
                  <tr key={req.id}>
                    <td>
                      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 600 }}>
                        {req.requestCode}
                      </div>
                      <span style={{ fontSize: 11, color: 'var(--navy-400)' }}>Ref #{req.id}</span>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--navy-900)' }}>
                        {req.studentName || 'Student'}
                      </div>
                      <span style={{ fontSize: 12, color: 'var(--navy-500)', fontFamily: 'var(--font-mono)' }}>
                        {req.studentId || 'STU-2026-001'}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{req.resourceName}</div>
                      <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>{req.facilityName}</span>
                    </td>
                    <td>{req.expectedArrival}</td>
                    <td>{req.expectedDuration}</td>
                    <td>
                      <span style={{ fontSize: 13 }}>
                        {req.groupSize} {req.groupSize === 1 ? 'student' : 'students'}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <button
                          type="button"
                          className="btn btn-success btn-sm"
                          onClick={() => handleApprove(req.id)}
                          disabled={actionId === req.id}
                        >
                          <Check size={14} />
                          <span>Approve</span>
                        </button>
                        <button
                          type="button"
                          className="btn btn-danger btn-sm"
                          onClick={() => handleReject(req.id)}
                          disabled={actionId === req.id}
                        >
                          <X size={14} />
                          <span>Reject</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Quick Action Navigation Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16 }}>
        <div className="section-card" style={{ marginBottom: 0 }}>
          <h3 style={{ fontSize: 15, fontWeight: 600, color: 'var(--navy-900)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
            <QrCode size={16} /> QR Verification Desk
          </h3>
          <p style={{ fontSize: 13, color: 'var(--navy-600)', marginBottom: 16 }}>
            Scan arriving student digital QR passes or enter request codes manually to record verified physical arrival.
          </p>
          <Link to="/operator/check-ins" className="btn btn-primary btn-sm">
            <span>Open Verification Terminal</span>
            <ArrowRight size={13} />
          </Link>
        </div>

        <div className="section-card" style={{ marginBottom: 0 }}>
          <h3 style={{ fontSize: 15, fontWeight: 600, color: 'var(--navy-900)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
            <Building2 size={16} /> Resource & Seating Status
          </h3>
          <p style={{ fontSize: 13, color: 'var(--navy-600)', marginBottom: 16 }}>
            Inspect all study stations, PCs, and discussion pods across facilities and toggle maintenance statuses.
          </p>
          <Link to="/operator/resources" className="btn btn-secondary btn-sm">
            <span>Manage Resources</span>
            <ArrowRight size={13} />
          </Link>
        </div>
      </div>
    </div>
  );
}
