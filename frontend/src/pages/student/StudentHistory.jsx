import { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';
import { History as HistoryIcon, RefreshCw, Calendar, Clock, BookOpen } from 'lucide-react';

export default function StudentHistory() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const fetchHistory = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const res = await api.get('/history');
      setHistory(res.data || []);
    } catch (err) {
      console.error('Failed to load history:', err);
      setError('Unable to load session history. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const formatDateTime = (isoString) => {
    if (!isoString) return '—';
    const date = new Date(isoString);
    return date.toLocaleString([], {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const calculateDuration = (startIso, endIso) => {
    if (!startIso) return '—';
    const start = new Date(startIso);
    const end = endIso ? new Date(endIso) : new Date();
    const diffMins = Math.round((end - start) / (1000 * 60));
    if (diffMins < 60) return `${diffMins} mins`;
    const hours = Math.floor(diffMins / 60);
    const mins = diffMins % 60;
    return `${hours} hr ${mins} mins`;
  };

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading your campus usage history..." />
      </div>
    );
  }

  return (
    <div className="student-history-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Campus Resource Usage History</h1>
          <p className="page-description">
            Audit log of all your verified check-ins, study sessions, and resource checkouts.
          </p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchHistory(true)}
            disabled={refreshing}
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Live Refresh'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchHistory()} />}

      {history.length === 0 ? (
        <EmptyState
          icon={HistoryIcon}
          title="No usage history recorded yet"
          description="Once you complete study sessions in any campus facility, your verified check-in and checkout logs will appear here."
          actionLabel="Browse Facilities"
          onAction={() => (window.location.href = '/student/libraries')}
        />
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Session ID</th>
                <th>Facility</th>
                <th>Resource</th>
                <th>Check-in Time</th>
                <th>Check-out Time</th>
                <th>Recorded Duration</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {history.map((session) => (
                <tr key={session.id}>
                  <td style={{ fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 600 }}>
                    SES-{String(session.id).padStart(5, '0')}
                  </td>
                  <td>{session.resource?.facility?.name || 'Campus Facility'}</td>
                  <td><strong>{session.resource?.name || '—'}</strong></td>
                  <td>{formatDateTime(session.startedAt)}</td>
                  <td>{session.endedAt ? formatDateTime(session.endedAt) : <em style={{ color: 'var(--primary)' }}>Active Session</em>}</td>
                  <td>{calculateDuration(session.startedAt, session.endedAt)}</td>
                  <td>
                    <StatusBadge status={session.endedAt ? 'COMPLETED' : 'CHECKED_IN'} size="small" />
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
