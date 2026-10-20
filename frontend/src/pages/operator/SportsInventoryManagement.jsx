import { useState, useEffect, useCallback } from 'react';
import api from '../../services/api';
import StatusBadge from '../../components/common/StatusBadge';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import Modal from '../../components/common/Modal';
import {
  Trophy,
  Dumbbell,
  RefreshCw,
  Wrench,
  AlertTriangle,
  CheckCircle,
  Plus,
  Minus,
  Edit,
  Lock,
  Unlock,
  Layers,
  ChevronDown,
  ChevronUp,
  AlertCircle,
  CheckCircle2,
  Calendar,
  Clock,
  Info,
} from 'lucide-react';

export default function SportsInventoryManagement() {
  const [inventoryDetails, setInventoryDetails] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('ALL');

  // Expanded physical units view per resource ID
  const [expandedResourceIds, setExpandedResourceIds] = useState(new Set());

  // Modal 1: Adjust Total Quantity
  const [qtyModalResource, setQtyModalResource] = useState(null);
  const [newTotalQty, setNewTotalQty] = useState(1);
  const [qtyModalLoading, setQtyModalLoading] = useState(false);
  const [qtyModalError, setQtyModalError] = useState(null);

  // Modal 2: Toggle Booking Closure
  const [closureModalResource, setClosureModalResource] = useState(null);
  const [closureReason, setClosureReason] = useState('');
  const [closureModalLoading, setClosureModalLoading] = useState(false);
  const [closureModalError, setClosureModalError] = useState(null);

  // Modal 3: Update Individual Physical Item Status
  const [itemModalData, setItemModalData] = useState(null); // { item, resource }
  const [itemNewStatus, setItemNewStatus] = useState('AVAILABLE');
  const [itemConditionNotes, setItemConditionNotes] = useState('');
  const [itemModalLoading, setItemModalLoading] = useState(false);
  const [itemModalError, setItemModalError] = useState(null);

  const fetchInventory = useCallback(async (isManual = false) => {
    if (isManual) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const res = await api.get('/sports/inventory');
      setInventoryDetails(res.data || []);
    } catch (err) {
      console.error('Failed to load sports inventory:', err);
      const msg = err.response?.data?.message || 'Unable to retrieve sports inventory details. Please verify privileges.';
      setError(msg);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    fetchInventory();
  }, [fetchInventory]);

  const toggleExpand = (resourceId) => {
    setExpandedResourceIds((prev) => {
      const next = new Set(prev);
      if (next.has(resourceId)) next.delete(resourceId);
      else next.add(resourceId);
      return next;
    });
  };

  // --- Handlers: Adjust Quantity Modal ---
  const handleOpenQtyModal = (resource) => {
    setQtyModalResource(resource);
    setNewTotalQty(resource.totalQuantity);
    setQtyModalError(null);
  };

  const handleCloseQtyModal = () => {
    setQtyModalResource(null);
    setQtyModalError(null);
  };

  const handleSubmitQty = async (e) => {
    e.preventDefault();
    if (!qtyModalResource) return;

    if (newTotalQty < 0) {
      setQtyModalError('Total quantity cannot be negative.');
      return;
    }

    if (newTotalQty < qtyModalResource.reservedQuantity) {
      setQtyModalError(
        `Cannot reduce inventory to ${newTotalQty}. There are currently ${qtyModalResource.reservedQuantity} units actively reserved or in use by students.`
      );
      return;
    }

    setQtyModalLoading(true);
    setQtyModalError(null);

    try {
      await api.post(`/sports/inventory/${qtyModalResource.id}/quantity`, {
        newTotalQuantity: Number(newTotalQty),
      });
      handleCloseQtyModal();
      await fetchInventory(true);
    } catch (err) {
      console.error('Failed to update total quantity:', err);
      setQtyModalError(err.response?.data?.message || 'Failed to update total quantity.');
    } finally {
      setQtyModalLoading(false);
    }
  };

  // --- Handlers: Toggle Closure Modal ---
  const handleOpenClosureModal = (resource) => {
    setClosureModalResource(resource);
    setClosureReason(resource.closureReason || '');
    setClosureModalError(null);
  };

  const handleCloseClosureModal = () => {
    setClosureModalResource(null);
    setClosureModalError(null);
  };

  const handleSubmitClosure = async (e) => {
    e.preventDefault();
    if (!closureModalResource) return;

    setClosureModalLoading(true);
    setClosureModalError(null);

    const willClose = !closureModalResource.bookingClosed;

    try {
      await api.post(`/sports/inventory/${closureModalResource.id}/toggle-closure`, {
        closed: willClose,
        reason: willClose ? closureReason : null,
      });
      handleCloseClosureModal();
      await fetchInventory(true);
    } catch (err) {
      console.error('Failed to toggle closure:', err);
      setClosureModalError(err.response?.data?.message || 'Failed to toggle resource booking closure.');
    } finally {
      setClosureModalLoading(false);
    }
  };

  // --- Handlers: Update Physical Unit Status Modal ---
  const handleOpenItemModal = (item, resource) => {
    setItemModalData({ item, resource });
    setItemNewStatus(item.status);
    setItemConditionNotes(item.conditionNotes || '');
    setItemModalError(null);
  };

  const handleCloseItemModal = () => {
    setItemModalData(null);
    setItemModalError(null);
  };

  const handleSubmitItemStatus = async (e) => {
    e.preventDefault();
    if (!itemModalData?.item) return;

    setItemModalLoading(true);
    setItemModalError(null);

    try {
      await api.patch(`/sports/inventory/items/${itemModalData.item.id}/status`, {
        status: itemNewStatus,
        conditionNotes: itemConditionNotes,
      });
      handleCloseItemModal();
      await fetchInventory(true);
    } catch (err) {
      console.error('Failed to update item status:', err);
      setItemModalError(err.response?.data?.message || 'Failed to update unit condition.');
    } finally {
      setItemModalLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading sports complex inventory and physical unit records..." />
      </div>
    );
  }

  // Filter list
  const filteredList = inventoryDetails.filter((detail) => {
    if (activeTab === 'EQUIPMENT') return detail.resource.type === 'EQUIPMENT';
    if (activeTab === 'COURT') return detail.resource.type === 'COURT';
    return true;
  });

  // Calculate high-level stats
  const totalPhysicalUnits = inventoryDetails.reduce((acc, d) => acc + (d.resource.totalQuantity || 0), 0);
  const totalUsableUnits = inventoryDetails.reduce((acc, d) => acc + (d.resource.usableQuantity || 0), 0);
  const totalDamagedOrMaint = inventoryDetails.reduce((acc, d) => acc + (d.resource.unavailableQuantity || 0), 0);
  const totalReservedInUse = inventoryDetails.reduce((acc, d) => acc + (d.resource.reservedQuantity || 0), 0);
  const totalClosedResources = inventoryDetails.filter((d) => d.resource.bookingClosed).length;

  return (
    <div className="sports-inventory-management-page">
      <div className="page-header">
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
            <h1 className="page-title" style={{ margin: 0 }}>Sports Inventory & Maintenance</h1>
            <span className="facility-type-tag" style={{ backgroundColor: '#fef3c7', color: '#92400e', border: '1px solid #fde68a' }}>
              OPERATOR / ADMIN
            </span>
          </div>
          <p className="page-description">
            Audit and regulate sports equipment stock, mark punctured or damaged items, manage court renovations, and control booking closures.
          </p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchInventory(true)}
            disabled={refreshing}
            title="Refresh latest inventory from server"
          >
            <RefreshCw size={14} className={refreshing ? 'animate-spin' : ''} />
            <span>{refreshing ? 'Syncing...' : 'Live Sync'}</span>
          </button>
        </div>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => fetchInventory()} />}

      {/* KPI Stats Grid */}
      <div className="stats-grid" style={{ marginBottom: 24 }}>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-neutral">
            <Layers size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{totalPhysicalUnits}</span>
            <span className="stat-label">Total Physical Assets</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-available">
            <CheckCircle size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{totalUsableUnits}</span>
            <span className="stat-label">Usable & Functional</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-reserved">
            <Clock size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{totalReservedInUse}</span>
            <span className="stat-label">Currently Issued / Reserved</span>
          </div>
        </div>
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-occupied">
            <Wrench size={22} />
          </div>
          <div className="stat-info">
            <span className="stat-value">{totalDamagedOrMaint}</span>
            <span className="stat-label">Damaged / In Maintenance</span>
          </div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="tab-group" style={{ marginBottom: 20 }}>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'ALL' ? 'active' : ''}`}
          onClick={() => setActiveTab('ALL')}
        >
          All Sports Resources ({inventoryDetails.length})
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'EQUIPMENT' ? 'active' : ''}`}
          onClick={() => setActiveTab('EQUIPMENT')}
        >
          Equipment & Balls ({inventoryDetails.filter((d) => d.resource.type === 'EQUIPMENT').length})
        </button>
        <button
          type="button"
          className={`tab-btn ${activeTab === 'COURT' ? 'active' : ''}`}
          onClick={() => setActiveTab('COURT')}
        >
          Courts & Nets ({inventoryDetails.filter((d) => d.resource.type === 'COURT').length})
        </button>
      </div>

      {/* Resources Detailed Cards */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {filteredList.map(({ resource, items }) => {
          const isExpanded = expandedResourceIds.has(resource.id);
          const isEquipment = resource.type === 'EQUIPMENT';
          const isClosed = resource.bookingClosed;

          return (
            <div
              key={resource.id}
              className="section-card"
              style={{
                padding: '18px 22px',
                borderLeft: isClosed
                  ? '4px solid var(--color-occupied)'
                  : resource.unavailableQuantity > 0
                  ? '4px solid var(--color-reserved)'
                  : '4px solid var(--color-available)',
              }}
            >
              {/* Header row */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 12 }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
                    <h3 style={{ fontSize: 17, fontWeight: 700, color: 'var(--navy-900)', margin: 0 }}>
                      {resource.name}
                    </h3>
                    <span className="facility-type-tag" style={{ fontSize: 11, textTransform: 'uppercase' }}>
                      {isEquipment ? 'Equipment' : 'Court / Net'}
                    </span>
                    {isClosed ? (
                      <span className="badge badge-occupied" style={{ fontSize: 11 }}>
                        <Lock size={12} style={{ display: 'inline', marginRight: 4 }} />
                        Bookings Closed
                      </span>
                    ) : (
                      <span className="badge badge-available" style={{ fontSize: 11 }}>
                        <Unlock size={12} style={{ display: 'inline', marginRight: 4 }} />
                        Open for Bookings
                      </span>
                    )}
                  </div>
                  <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>
                    Standard unit: <strong>{resource.bookingUnit}</strong>
                    {isClosed && resource.closureReason && (
                      <span style={{ color: '#dc2626', marginLeft: 8 }}>
                        &bull; Reason: {resource.closureReason}
                      </span>
                    )}
                  </span>
                </div>

                {/* Header Action Buttons */}
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => handleOpenQtyModal(resource)}
                    title="Change total quantity (adds or decommissions available units)"
                  >
                    <Edit size={13} />
                    <span>Adjust Stock ({resource.totalQuantity})</span>
                  </button>

                  <button
                    type="button"
                    className={`btn btn-sm ${isClosed ? 'btn-success' : 'btn-danger'}`}
                    onClick={() => handleOpenClosureModal(resource)}
                    title={isClosed ? 'Reopen bookings for this resource' : 'Close bookings for this resource'}
                  >
                    {isClosed ? <Unlock size={13} /> : <Lock size={13} />}
                    <span>{isClosed ? 'Reopen Bookings' : 'Close Bookings'}</span>
                  </button>

                  <button
                    type="button"
                    className="btn btn-outline btn-sm"
                    onClick={() => toggleExpand(resource.id)}
                  >
                    <span>{isExpanded ? 'Hide Physical Units' : `Inspect Units (${items.length})`}</span>
                    {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                  </button>
                </div>
              </div>

              {/* Inventory metrics bar */}
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))',
                  gap: 12,
                  marginTop: 16,
                  padding: '12px 16px',
                  backgroundColor: 'var(--navy-50)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)',
                }}
              >
                <div>
                  <span style={{ fontSize: 11, color: 'var(--navy-500)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Total Units
                  </span>
                  <strong style={{ fontSize: 16, color: 'var(--navy-900)' }}>{resource.totalQuantity}</strong>
                </div>
                <div>
                  <span style={{ fontSize: 11, color: 'var(--navy-500)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Usable Stock
                  </span>
                  <strong style={{ fontSize: 16, color: 'var(--color-available)' }}>{resource.usableQuantity}</strong>
                </div>
                <div>
                  <span style={{ fontSize: 11, color: 'var(--navy-500)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Available to Book
                  </span>
                  <strong style={{ fontSize: 16, color: isClosed ? 'var(--navy-500)' : 'var(--primary)' }}>
                    {isClosed ? '0 (Closed)' : resource.availableQuantity}
                  </strong>
                </div>
                <div>
                  <span style={{ fontSize: 11, color: 'var(--navy-500)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Active Allocations
                  </span>
                  <strong style={{ fontSize: 16, color: 'var(--navy-700)' }}>{resource.reservedQuantity}</strong>
                </div>
                <div>
                  <span style={{ fontSize: 11, color: 'var(--navy-500)', display: 'block', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Damaged / Maint.
                  </span>
                  <strong style={{ fontSize: 16, color: resource.unavailableQuantity > 0 ? '#b45309' : 'var(--navy-400)' }}>
                    {resource.unavailableQuantity}
                  </strong>
                </div>
              </div>

              {/* Physical Units Section (Expanded) */}
              {isExpanded && (
                <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--border-color)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
                    <h4 style={{ fontSize: 14, fontWeight: 600, color: 'var(--navy-800)', margin: 0 }}>
                      Physical Fleet / Individual Unit Inventory ({items.length} units tracked)
                    </h4>
                    <span style={{ fontSize: 12, color: 'var(--navy-500)' }}>
                      Click "Update Unit" on any item to mark as Damaged, Under Maintenance, or restore to Available.
                    </span>
                  </div>

                  <div className="table-container">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Unit Code</th>
                          <th>Physical Unit Name</th>
                          <th>Status</th>
                          <th>Condition / Maintenance Notes</th>
                          <th>Last Updated</th>
                          <th>Control Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {items.map((it) => {
                          let statusColorClass = 'badge-available';
                          if (it.status === 'DAMAGED') statusColorClass = 'badge-occupied';
                          else if (it.status === 'MAINTENANCE') statusColorClass = 'badge-reserved';
                          else if (it.status === 'LOST') statusColorClass = 'badge-maintenance';

                          return (
                            <tr key={it.id}>
                              <td style={{ fontFamily: 'var(--font-mono)', fontSize: 12, fontWeight: 600 }}>
                                {it.itemCode}
                              </td>
                              <td style={{ fontWeight: 600, color: 'var(--navy-900)' }}>{it.name}</td>
                              <td>
                                <span className={`badge ${statusColorClass}`} style={{ fontSize: 11 }}>
                                  {it.status}
                                </span>
                              </td>
                              <td style={{ fontSize: 13, color: it.conditionNotes ? 'var(--navy-800)' : 'var(--navy-400)' }}>
                                {it.conditionNotes || '—'}
                              </td>
                              <td style={{ fontSize: 12, color: 'var(--navy-500)' }}>
                                {it.updatedAt
                                  ? new Date(it.updatedAt).toLocaleDateString([], {
                                      month: 'short',
                                      day: 'numeric',
                                      hour: '2-digit',
                                      minute: '2-digit',
                                    })
                                  : '—'}
                              </td>
                              <td>
                                <button
                                  type="button"
                                  className="btn btn-secondary btn-sm"
                                  onClick={() => handleOpenItemModal(it, resource)}
                                  title="Change unit condition (e.g. mark punctured, repair)"
                                >
                                  <Wrench size={13} />
                                  <span>Update Unit</span>
                                </button>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* MODAL 1: Adjust Total Stock Quantity */}
      {qtyModalResource && (
        <Modal isOpen={!!qtyModalResource} onClose={handleCloseQtyModal} title={`Adjust Total Stock: ${qtyModalResource.name}`}>
          <form onSubmit={handleSubmitQty}>
            <div style={{ marginBottom: 16, padding: '12px', backgroundColor: 'var(--navy-50)', borderRadius: 'var(--radius-md)', fontSize: 13 }}>
              <div>Current Total Quantity: <strong>{qtyModalResource.totalQuantity} units</strong></div>
              <div>Currently Allocated / Reserved: <strong>{qtyModalResource.reservedQuantity} units</strong></div>
              <div style={{ marginTop: 4, color: 'var(--navy-600)', fontSize: 12 }}>
                Increasing stock will automatically provision new physical unit items. Reducing stock will safely decommission unreserved, available items.
              </div>
            </div>

            {qtyModalError && (
              <div className="error-message-card" style={{ marginBottom: 16 }}>
                <div className="error-message-header">
                  <AlertCircle className="error-icon" size={16} />
                  <div className="error-desc">{qtyModalError}</div>
                </div>
              </div>
            )}

            <div className="form-group">
              <label className="form-label">New Total Quantity</label>
              <input
                type="number"
                min={qtyModalResource.reservedQuantity}
                className="form-input"
                value={newTotalQty}
                onChange={(e) => setNewTotalQty(Number(e.target.value))}
                required
                disabled={qtyModalLoading}
              />
              <span className="form-help">
                Must be non-negative and at least {qtyModalResource.reservedQuantity} (to satisfy active student allocations).
              </span>
            </div>

            <div className="form-actions">
              <button type="button" onClick={handleCloseQtyModal} className="btn btn-secondary" disabled={qtyModalLoading}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={qtyModalLoading}>
                {qtyModalLoading ? <LoadingSpinner text="Updating..." size={16} /> : 'Save New Quantity'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 2: Toggle Booking Closure */}
      {closureModalResource && (
        <Modal
          isOpen={!!closureModalResource}
          onClose={handleCloseClosureModal}
          title={`${closureModalResource.bookingClosed ? 'Reopen' : 'Close'} Bookings: ${closureModalResource.name}`}
        >
          <form onSubmit={handleSubmitClosure}>
            <div style={{ marginBottom: 16, fontSize: 14, color: 'var(--navy-700)' }}>
              {closureModalResource.bookingClosed ? (
                <p>
                  Are you sure you want to <strong>reopen bookings</strong> for {closureModalResource.name}?
                  Students will be able to request this resource according to available stock.
                </p>
              ) : (
                <p>
                  Closing bookings will immediately prevent students from submitting new requests for{' '}
                  <strong>{closureModalResource.name}</strong> while keeping other sports facilities available.
                </p>
              )}
            </div>

            {closureModalError && (
              <div className="error-message-card" style={{ marginBottom: 16 }}>
                <div className="error-message-header">
                  <AlertCircle className="error-icon" size={16} />
                  <div className="error-desc">{closureModalError}</div>
                </div>
              </div>
            )}

            {!closureModalResource.bookingClosed && (
              <div className="form-group">
                <label className="form-label">Optional Closure / Maintenance Reason</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Resurfacing glass panels, Annual championship tournament, Heavy rain"
                  value={closureReason}
                  onChange={(e) => setClosureReason(e.target.value)}
                  disabled={closureModalLoading}
                />
                <span className="form-help">
                  This explanation will be clearly displayed to students viewing the Sports Complex page.
                </span>
              </div>
            )}

            <div className="form-actions">
              <button type="button" onClick={handleCloseClosureModal} className="btn btn-secondary" disabled={closureModalLoading}>
                Cancel
              </button>
              <button
                type="submit"
                className={`btn ${closureModalResource.bookingClosed ? 'btn-success' : 'btn-danger'}`}
                disabled={closureModalLoading}
              >
                {closureModalLoading ? (
                  <LoadingSpinner text="Saving..." size={16} />
                ) : closureModalResource.bookingClosed ? (
                  'Reopen Bookings'
                ) : (
                  'Confirm Closure'
                )}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 3: Update Individual Physical Unit Status */}
      {itemModalData && (
        <Modal
          isOpen={!!itemModalData}
          onClose={handleCloseItemModal}
          title={`Update Physical Unit: ${itemModalData.item.name} (${itemModalData.item.itemCode})`}
        >
          <form onSubmit={handleSubmitItemStatus}>
            <div style={{ marginBottom: 16, padding: '12px', backgroundColor: 'var(--navy-50)', borderRadius: 'var(--radius-md)', fontSize: 13 }}>
              <div>Resource: <strong>{itemModalData.resource.name}</strong></div>
              <div>Unit Code: <strong style={{ fontFamily: 'var(--font-mono)' }}>{itemModalData.item.itemCode}</strong></div>
              <div>Current Status: <strong>{itemModalData.item.status}</strong></div>
            </div>

            {itemModalError && (
              <div className="error-message-card" style={{ marginBottom: 16 }}>
                <div className="error-message-header">
                  <AlertCircle className="error-icon" size={16} />
                  <div className="error-desc">{itemModalError}</div>
                </div>
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Physical Item Status</label>
              <select
                className="form-select"
                value={itemNewStatus}
                onChange={(e) => setItemNewStatus(e.target.value)}
                disabled={itemModalLoading}
              >
                <option value="AVAILABLE">AVAILABLE (Usable & ready for students)</option>
                <option value="DAMAGED">DAMAGED (Unusable, punctured, broken)</option>
                <option value="MAINTENANCE">MAINTENANCE (Under repair / resurfacing)</option>
                <option value="LOST">LOST (Missing / decommissioned)</option>
              </select>
              <span className="form-help">
                Setting to DAMAGED, MAINTENANCE, or LOST automatically reduces usable inventory without deleting history.
              </span>
            </div>

            <div className="form-group">
              <label className="form-label">Condition Notes / Repair Log</label>
              <textarea
                className="form-textarea"
                rows={3}
                placeholder="e.g. Punctured during tournament, Torn net, Floor resurfacing scheduled"
                value={itemConditionNotes}
                onChange={(e) => setItemConditionNotes(e.target.value)}
                disabled={itemModalLoading}
              />
              <span className="form-help">Record physical inspection notes or repair progress.</span>
            </div>

            <div className="form-actions">
              <button type="button" onClick={handleCloseItemModal} className="btn btn-secondary" disabled={itemModalLoading}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={itemModalLoading}>
                {itemModalLoading ? <LoadingSpinner text="Saving..." size={16} /> : 'Save Unit Condition'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
