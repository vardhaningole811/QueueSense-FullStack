import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import {
  QrCode,
  LogOut,
  RefreshCw,
  Clock,
  CheckCircle2,
  Calendar,
  XCircle,
  PlusCircle,
} from 'lucide-react';

export default function MyRequests() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('ALL');
  const [actionLoadingId, setActionLoadingId] = useState(null);

  const fetchRequests = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const res = await api.get('/requests/my');
      setRequests(res.data || []);
    } catch (err) {
      console.error('Failed to load requests:', err);
      setError('Unable to load your requests. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchRequests();
  }, [fetchRequests]);

  const handleCancel = async (requestId) => {
    if (!window.confirm('Are you sure you want to cancel this request?')) return;
    setActionLoadingId(requestId);
    try {
      await api.post(`/requests/${requestId}/cancel`);
      await fetchRequests(true);
    } catch (err) {
      console.error('Failed to cancel request:', err);
      alert(err.response?.data?.message || 'Failed to cancel request.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleCheckOut = async (requestId) => {
    setActionLoadingId(requestId);
    try {
      await api.post(`/requests/${requestId}/check-out`);
      await fetchRequests(true);
    } catch (err) {
      console.error('Failed to check out:', err);
      alert(err.response?.data?.message || 'Check out failed.');
    } finally {
      setActionLoadingId(null);
    }
  };

  const filteredRequests = requests.filter((req) => {
    if (activeTab === 'ALL') return true;
    return req.status === activeTab;
  });

  const countByStatus = {
    ALL: requests.length,
    PENDING: requests.filter((r) => r.status === 'PENDING').length,
    APPROVED: requests.filter((r) => r.status === 'APPROVED').length,
    CHECKED_IN: requests.filter((r) => r.status === 'CHECKED_IN').length,
    COMPLETED: requests.filter((r) => r.status === 'COMPLETED').length,
    REJECTED: requests.filter((r) => r.status === 'REJECTED').length,
  };

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading your usage requests..." />
      </div>
    );
  }

  return (
    <div className="my-requests-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">My Resource Requests</h1>
          <p className="page-description">
            Track your reservation statuses, access digital QR passes, and manage active campus study sessions.
          </p>
        </div>
        <div className="header-actions">
          <Link to="/student/libraries" className="btn btn-secondary btn-sm">
            <PlusCircle size={14} />
            <span>New Request</span>
          </Link>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchRequests(true)}
            disabled={refreshing}
            title="Fetch latest status from server"
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Sync Requests'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchRequests()} />}

      {/* Filter Tabs */}
      <div className="tab-group">
        <button
          type="button"
          className={`tab-btn ${activeTab === 'ALL' ? 'active' : ''}`}
          onClick={() => setActiveTab('ALL')}
        >
          All Requests ({countByStatus.ALL})
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'PENDING' ? 'active' : ''}`}
          onClick={() => setActiveTab('PENDING')}
        >
          Pending ({countByStatus.PENDING})
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'APPROVED' ? 'active' : ''}`}
          onClick={() => setActiveTab('APPROVED')}
        >
          Approved ({countByStatus.APPROVED})
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'CHECKED_IN' ? 'active' : ''}`}
          onClick={() => setActiveTab('CHECKED_IN')}
        >
          Active Sessions ({countByStatus.CHECKED_IN})
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'COMPLETED' ? 'active' : ''}`}
          onClick={() => setActiveTab('COMPLETED')}
        >
          Completed ({countByStatus.COMPLETED})
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'REJECTED' ? 'active' : ''}`}
          onClick={() => setActiveTab('REJECTED')}
        >
          Rejected ({countByStatus.REJECTED})
        </button>
      </div>

      {/* Requests Table */}
      {filteredRequests.length === 0 ? (
        <EmptyState
          title={`No ${activeTab !== 'ALL' ? activeTab.toLowerCase() : ''} requests found`}
          description={
            activeTab === 'ALL'
              ? 'You have not submitted any resource requests yet. Browse campus libraries to reserve a seat or PC.'
              : `You currently have no requests with status: ${activeTab}.`
          }
          actionLabel={activeTab === 'ALL' ? 'Browse Campus Libraries' : 'View All Requests'}
          onAction={() => (activeTab === 'ALL' ? (window.location.href = '/student/libraries') : setActiveTab('ALL'))}
        />
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Request ID</th>
                <th>Facility & Resource</th>
                <th>Arrival & Duration</th>
                <th>Group Size</th>
                <th>Created At</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredRequests.map((req) => {
                const isApproved = req.status === 'APPROVED';
                const isCheckedIn = req.status === 'CHECKED_IN';
                const isPending = req.status === 'PENDING';
                const isActionLoading = actionLoadingId === req.id;

                return (
                  <tr key={req.id}>
                    <td>
                      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 600 }}>
                        {req.requestCode}
                      </div>
                      <span style={{ fontSize: 11, color: 'var(--navy-400)' }}>Ref #{req.id}</span>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--navy-900)' }}>{req.resourceName}</div>
                      <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>{req.facilityName}</span>
                    </td>
                    <td>
                      <div style={{ fontSize: 13, color: 'var(--navy-800)' }}>{req.expectedArrival}</div>
                      <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>Duration: {req.expectedDuration}</span>
                    </td>
                    <td>
                      <span style={{ fontSize: 13, color: 'var(--navy-700)' }}>
                        {req.groupSize} {req.groupSize === 1 ? 'student' : 'students'}
                      </span>
                    </td>
                    <td style={{ fontSize: 12, color: 'var(--navy-500)' }}>
                      {req.createdAt ? new Date(req.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}
                    </td>
                    <td>
                      <StatusBadge status={req.status} />
                      {req.rejectionReason && (
                        <div style={{ fontSize: 11, color: 'var(--color-occupied)', marginTop: 4 }}>
                          Reason: {req.rejectionReason}
                        </div>
                      )}
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        {isApproved && (
                          <Link to={`/student/confirmation/${req.id}`} className="btn btn-primary btn-sm">
                            <QrCode size={14} />
                            <span>Digital Pass & QR</span>
                          </Link>
                        )}

                        {isCheckedIn && (
                          <button
                            type="button"
                            className="btn btn-danger-solid btn-sm"
                            onClick={() => handleCheckOut(req.id)}
                            disabled={isActionLoading}
                          >
                            <LogOut size={13} />
                            <span>{isActionLoading ? 'Checking out...' : 'Check Out'}</span>
                          </button>
                        )}

                        {isPending && (
                          <button
                            type="button"
                            className="btn btn-danger btn-sm"
                            onClick={() => handleCancel(req.id)}
                            disabled={isActionLoading}
                          >
                            <XCircle size={13} />
                            <span>{isActionLoading ? 'Cancelling...' : 'Cancel'}</span>
                          </button>
                        )}

                        {!isApproved && !isCheckedIn && !isPending && (
                          <span style={{ fontSize: 12, color: 'var(--navy-400)' }}>No actions</span>
                        )}
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
