import { useState, useEffect, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import {
  Clock,
  CheckCircle2,
  Check,
  X,
  RefreshCw,
  QrCode,
  Calendar,
  Layers,
  ArrowRight,
} from 'lucide-react';

export default function OperatorRequests() {
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState('PENDING');
  const [pendingRequests, setPendingRequests] = useState([]);
  const [approvedRequests, setApprovedRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [actionId, setActionId] = useState(null);

  const fetchRequests = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const [pendingRes, approvedRes] = await Promise.all([
        api.get('/operator/requests/pending'),
        api.get('/operator/requests/approved'),
      ]);
      setPendingRequests(pendingRes.data || []);
      setApprovedRequests(approvedRes.data || []);
    } catch (err) {
      console.error('Failed to load operator requests:', err);
      setError('Unable to load request queue. Please check your privileges.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleApprove = async (requestId) => {
    setActionId(requestId);
    try {
      await api.post(`/operator/requests/${requestId}/approve`);
      await fetchRequests(true);
    } catch (err) {
      console.error('Approve failed:', err);
      alert(err.response?.data?.message || 'Approval failed.');
    } finally {
      setActionId(null);
    }
  };

  const handleReject = async (requestId) => {
    const reason = window.prompt('Enter rejection reason (optional):', 'Facility maintenance or quota limit');
    if (reason === null) return;

    setActionId(requestId);
    try {
      await api.post(`/operator/requests/${requestId}/reject`, { reason });
      await fetchRequests(true);
    } catch (err) {
      console.error('Reject failed:', err);
      alert(err.response?.data?.message || 'Rejection failed.');
    } finally {
      setActionId(null);
    }
  };

  const handleProceedToCheckIn = (requestCode) => {
    navigate(`/operator/check-ins?code=${encodeURIComponent(requestCode)}`);
  };

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading operator request queue..." />
      </div>
    );
  }

  const currentList = activeTab === 'PENDING' ? pendingRequests : approvedRequests;

  return (
    <div className="operator-requests-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Request Queue & Verification</h1>
          <p className="page-description">
            Review incoming student requests, grant approval, and track pending physical arrivals.
          </p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchRequests(true)}
            disabled={refreshing}
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Live Sync'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchRequests()} />}

      {/* Tabs */}
      <div className="tab-group">
        <button
          type="button"
          className={`tab-btn ${activeTab === 'PENDING' ? 'active' : ''}`}
          onClick={() => setActiveTab('PENDING')}
        >
          Pending Requests ({pendingRequests.length})
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'APPROVED' ? 'active' : ''}`}
          onClick={() => setActiveTab('APPROVED')}
        >
          Approved & Awaiting Check-in ({approvedRequests.length})
        </button>
      </div>

      {currentList.length === 0 ? (
        <EmptyState
          title={`No ${activeTab === 'PENDING' ? 'pending' : 'approved'} requests in queue`}
          description={
            activeTab === 'PENDING'
              ? 'There are currently no new student requests awaiting operator action.'
              : 'There are currently no approved requests waiting for student physical check-in.'
          }
        />
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Request ID</th>
                <th>Student</th>
                <th>Facility & Resource</th>
                <th>Arrival Slot</th>
                <th>Duration</th>
                <th>Group</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {currentList.map((req) => (
                <tr key={req.id}>
                  <td>
                    <div style={{ fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 600 }}>
                      {req.requestCode}
                    </div>
                    <span style={{ fontSize: 11, color: 'var(--navy-400)' }}>Ref #{req.id}</span>
                  </td>
                  <td>
                    <div style={{ fontWeight: 600, color: 'var(--navy-900)' }}>{req.studentName}</div>
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
                    <StatusBadge status={req.status} size="small" />
                  </td>
                  <td>
                    {activeTab === 'PENDING' ? (
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
                    ) : (
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => handleProceedToCheckIn(req.requestCode)}
                      >
                        <QrCode size={13} />
                        <span>Verify Check-in</span>
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
