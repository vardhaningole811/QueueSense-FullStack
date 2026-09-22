import { useState, useEffect, useCallback } from 'react';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import {
  Building2,
  RefreshCw,
  Wrench,
  CheckCircle,
  Monitor,
  Armchair,
  Layers,
} from 'lucide-react';

export default function OperatorResources() {
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [filterFacility, setFilterFacility] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [actionId, setActionId] = useState(null);

  const fetchResources = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const res = await api.get('/operator/resources');
      setResources(res.data || []);
    } catch (err) {
      console.error('Failed to load operator resources:', err);
      setError('Unable to load facility resources. Please verify privileges.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchResources();
  }, [fetchResources]);

  const handleToggleMaintenance = async (resource) => {
    const newStatus = resource.status === 'MAINTENANCE' ? 'AVAILABLE' : 'MAINTENANCE';
    if (!window.confirm(`Are you sure you want to set ${resource.name} status to ${newStatus}?`)) return;

    setActionId(resource.id);
    try {
      await api.patch(`/operator/resources/${resource.id}/status`, { status: newStatus });
      await fetchResources(true);
    } catch (err) {
      console.error('Failed to update status:', err);
      alert(err.response?.data?.message || 'Failed to update resource status.');
    } finally {
      setActionId(null);
    }
  };

  const facilities = Array.from(
    new Set(resources.map((r) => r.facility?.name).filter(Boolean))
  );

  const filteredResources = resources.filter((r) => {
    if (filterFacility !== 'ALL' && r.facility?.name !== filterFacility) return false;
    if (filterStatus !== 'ALL' && r.status !== filterStatus) return false;
    return true;
  });

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading facility resource statuses..." />
      </div>
    );
  }

  return (
    <div className="operator-resources-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Facility Resource Management</h1>
          <p className="page-description">
            Inspect seating and equipment allocation, toggle maintenance modes, and monitor active occupancy.
          </p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchResources(true)}
            disabled={refreshing}
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Live Sync'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchResources()} />}

      {/* Filter Toolbar */}
      <div className="section-card" style={{ padding: '16px 20px', marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--navy-700)' }}>Filter Resources:</span>

            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <label style={{ fontSize: 12, color: 'var(--navy-500)' }}>Facility:</label>
              <select
                className="form-select"
                style={{ padding: '4px 8px', fontSize: 13 }}
                value={filterFacility}
                onChange={(e) => setFilterFacility(e.target.value)}
              >
                <option value="ALL">All Facilities</option>
                {facilities.map((fac) => (
                  <option key={fac} value={fac}>{fac}</option>
                ))}
              </select>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <label style={{ fontSize: 12, color: 'var(--navy-500)' }}>Status:</label>
              <select
                className="form-select"
                style={{ padding: '4px 8px', fontSize: 13 }}
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
              >
                <option value="ALL">All Statuses</option>
                <option value="AVAILABLE">Available</option>
                <option value="RESERVED">Reserved</option>
                <option value="OCCUPIED">Occupied</option>
                <option value="MAINTENANCE">Maintenance</option>
              </select>
            </div>
          </div>

          <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>
            Showing <strong>{filteredResources.length}</strong> of {resources.length} resources
          </span>
        </div>
      </div>

      {/* Resources Table */}
      <div className="table-container">
        <table className="data-table">
          <thead>
            <tr>
              <th>Resource ID</th>
              <th>Name</th>
              <th>Facility</th>
              <th>Type</th>
              <th>Current Status</th>
              <th>Operational Control</th>
            </tr>
          </thead>
          <tbody>
            {filteredResources.map((res) => {
              const isMaintenance = res.status === 'MAINTENANCE';
              const canToggle = res.status === 'AVAILABLE' || res.status === 'MAINTENANCE';

              return (
                <tr key={res.id}>
                  <td style={{ fontFamily: 'var(--font-mono)', fontSize: 13, fontWeight: 600 }}>
                    #{res.id}
                  </td>
                  <td>
                    <strong style={{ color: 'var(--navy-900)' }}>{res.name}</strong>
                  </td>
                  <td>{res.facility?.name || 'Campus Facility'}</td>
                  <td>
                    <span style={{ fontSize: 13, color: 'var(--navy-700)' }}>{res.type}</span>
                  </td>
                  <td>
                    <StatusBadge status={res.status} size="small" />
                  </td>
                  <td>
                    {canToggle ? (
                      <button
                        type="button"
                        className={`btn ${isMaintenance ? 'btn-success' : 'btn-outline'} btn-sm`}
                        onClick={() => handleToggleMaintenance(res)}
                        disabled={actionId === res.id}
                      >
                        {isMaintenance ? (
                          <>
                            <CheckCircle size={13} />
                            <span>Set Available</span>
                          </>
                        ) : (
                          <>
                            <Wrench size={13} />
                            <span>Flag Maintenance</span>
                          </>
                        )}
                      </button>
                    ) : (
                      <span style={{ fontSize: 12, color: 'var(--navy-400)' }}>
                        In active cycle ({res.status.toLowerCase()})
                      </span>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
