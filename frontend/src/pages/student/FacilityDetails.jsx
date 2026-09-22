import { useState, useEffect, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import EmptyState from '../../components/common/EmptyState';
import BookingModal from '../../components/booking/BookingModal';
import {
  ArrowLeft,
  RefreshCw,
  SlidersHorizontal,
  Monitor,
  Armchair,
  Layers,
  CheckCircle,
  PlusCircle,
} from 'lucide-react';

export default function FacilityDetails() {
  const { facilityId } = useParams();

  const [facility, setFacility] = useState(null);
  const [resources, setResources] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const [filterType, setFilterType] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL');

  const [selectedResource, setSelectedResource] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const fetchFacilityData = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const [facRes, resListRes] = await Promise.all([
        api.get(`/facilities/${facilityId}`),
        api.get(`/resources/facility/${facilityId}`),
      ]);
      setFacility(facRes.data);
      setResources(resListRes.data || []);
    } catch (err) {
      console.error('Failed to load facility details:', err);
      setError('Failed to load resources for this facility. Please try again.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [facilityId]);

  useEffect(() => {
    fetchFacilityData();
  }, [fetchFacilityData]);

  const handleOpenBooking = (resource) => {
    if (resource.status !== 'AVAILABLE') return;
    setSelectedResource(resource);
    setIsModalOpen(true);
  };

  const handleCloseBooking = () => {
    setIsModalOpen(false);
    setSelectedResource(null);
  };

  // Stats calculation
  const totalCount = resources.length;
  const availableCount = resources.filter((r) => r.status === 'AVAILABLE').length;
  const reservedCount = resources.filter((r) => r.status === 'RESERVED').length;
  const occupiedCount = resources.filter((r) => r.status === 'OCCUPIED').length;
  const maintenanceCount = resources.filter((r) => r.status === 'MAINTENANCE').length;

  // Filtered resources
  const filteredResources = resources.filter((r) => {
    if (filterType !== 'ALL' && r.type !== filterType) return false;
    if (filterStatus !== 'ALL' && r.status !== filterStatus) return false;
    return true;
  });

  const uniqueTypes = Array.from(new Set(resources.map((r) => r.type)));

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading facility availability..." />
      </div>
    );
  }

  return (
    <div className="facility-details-page">
      <div style={{ marginBottom: 16 }}>
        <Link to="/student/libraries" className="btn btn-outline btn-sm" style={{ display: 'inline-flex' }}>
          <ArrowLeft size={14} />
          <span>Back to All Libraries</span>
        </Link>
      </div>

      <div className="page-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <h1 className="page-title" style={{ margin: 0 }}>{facility?.name}</h1>
            <span className="facility-type-tag">{facility?.category}</span>
          </div>
          <p className="page-description">{facility?.description}</p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchFacilityData(true)}
            disabled={refreshing}
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Live Refresh'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchFacilityData()} />}

      {/* Facility Summary Stats */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-available">
            <CheckCircle size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{availableCount}</span>
            <span className="stat-label">Available Now</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-reserved">
            <Layers size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{reservedCount}</span>
            <span className="stat-label">Reserved (Pending Check-in)</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-occupied">
            <Armchair size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{occupiedCount}</span>
            <span className="stat-label">In Active Use</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-neutral">
            <SlidersHorizontal size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{totalCount}</span>
            <span className="stat-label">Total Resources</span>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="section-card" style={{ padding: '16px 20px', marginBottom: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--navy-700)' }}>Filter By:</span>
            
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <label style={{ fontSize: 12, color: 'var(--navy-500)' }}>Status:</label>
              <select
                className="form-select"
                style={{ padding: '4px 8px', fontSize: 13 }}
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value)}
              >
                <option value="ALL">All Statuses ({totalCount})</option>
                <option value="AVAILABLE">Available ({availableCount})</option>
                <option value="RESERVED">Reserved ({reservedCount})</option>
                <option value="OCCUPIED">Occupied ({occupiedCount})</option>
                <option value="MAINTENANCE">Maintenance ({maintenanceCount})</option>
              </select>
            </div>

            {uniqueTypes.length > 1 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <label style={{ fontSize: 12, color: 'var(--navy-500)' }}>Type:</label>
                <select
                  className="form-select"
                  style={{ padding: '4px 8px', fontSize: 13 }}
                  value={filterType}
                  onChange={(e) => setFilterType(e.target.value)}
                >
                  <option value="ALL">All Types</option>
                  {uniqueTypes.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </div>
            )}
          </div>

          <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>
            Showing <strong>{filteredResources.length}</strong> of {totalCount} resources
          </span>
        </div>
      </div>

      {/* Resource Grid */}
      {filteredResources.length === 0 ? (
        <EmptyState
          title="No resources match the selected filter"
          description="Try resetting status or type filters to view all facility seating and equipment."
          actionLabel="Reset Filters"
          onAction={() => {
            setFilterStatus('ALL');
            setFilterType('ALL');
          }}
        />
      ) : (
        <div className="resource-grid">
          {filteredResources.map((res) => {
            const isAvailable = res.status === 'AVAILABLE';
            const statusClass = `status-${res.status.toLowerCase()}`;

            return (
              <div key={res.id} className={`resource-card ${statusClass}`}>
                <div>
                  <div className="resource-card-header">
                    <h3 className="resource-name">{res.name}</h3>
                    <StatusBadge status={res.status} size="small" />
                  </div>
                  <div className="resource-type">
                    {res.type === 'PC' && <Monitor size={13} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />}
                    {res.type === 'SEAT' && <Armchair size={13} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />}
                    Type: {res.type}
                  </div>
                </div>

                <div className="resource-card-footer">
                  <span style={{ fontSize: 11, color: 'var(--navy-400)', fontFamily: 'var(--font-mono)' }}>
                    ID: #{res.id}
                  </span>
                  {isAvailable ? (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => handleOpenBooking(res)}
                    >
                      <PlusCircle size={14} />
                      <span>Request</span>
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      disabled
                      title={`This resource is currently ${res.status.toLowerCase()}`}
                    >
                      Unavailable
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Booking Modal */}
      {selectedResource && (
        <BookingModal
          isOpen={isModalOpen}
          onClose={handleCloseBooking}
          resource={selectedResource}
          onSuccess={() => fetchFacilityData(true)}
        />
      )}
    </div>
  );
}
