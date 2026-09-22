import { useState, useEffect, useCallback } from 'react';
import api from '../../services/api';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import StatusBadge from '../../components/common/StatusBadge';
import { History as HistoryIcon, RefreshCw, Calendar, Clock } from 'lucide-react';

export default function OperatorHistory() {
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
      console.error('Failed to load facility history:', err);
      setError('Unable to load facility historical records. Please verify privileges.');
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
        <LoadingSpinner text="Loading facility usage log..." />
      </div>
    );
  }

  return (
    <div className="operator-history-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Campus Facility Usage Audit Log</h1>
          <p className="page-description">
            Complete chronological record of all student check-in, checkout, and resource utilization sessions.
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
            <span>{refreshing ? 'Syncing...' : 'Live Sync'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchHistory()} />}

      {history.length === 0 ? (
        <EmptyState
          icon={HistoryIcon}
          title="No session history recorded yet"
          description="Completed check-in and checkout sessions will be logged here automatically."
        />
      ) : (
        <div className="table-container">
          <table className="data-table">
            <thead>
              <tr>
                <th>Session ID</th>
                <th>Student</th>
                <th>Facility</th>
                <th>Resource</th>
                <th>Check-in Time</th>
                <th>Check-out Time</th>
                <th>Duration</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {history.map((session) => (
                <tr key={session.id}>
                  <td style={{ fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 600 }}>
                    SES-{String(session.id).padStart(5, '0')}
                  </td>
                  <td>
                    <div style={{ fontWeight: 600, color: 'var(--navy-900)' }}>
                      {session.student?.name || 'Student'}
                    </div>
                    <span style={{ fontSize: 11, color: 'var(--navy-500)', fontFamily: 'var(--font-mono)' }}>
                      {session.student?.email || '—'}
                    </span>
                  </td>
                  <td>{session.resource?.facility?.name || 'Campus Facility'}</td>
                  <td><strong>{session.resource?.name || '—'}</strong></td>
                  <td>{formatDateTime(session.startedAt)}</td>
                  <td>
                    {session.endedAt ? (
                      formatDateTime(session.endedAt)
                    ) : (
                      <em style={{ color: 'var(--primary)', fontWeight: 600 }}>Active In Facility</em>
                    )}
                  </td>
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
