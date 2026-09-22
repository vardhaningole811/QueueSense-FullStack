import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Modal from '../common/Modal';
import api from '../../services/api';
import { Clock, Users, Calendar, AlertCircle } from 'lucide-react';
import LoadingSpinner from '../common/LoadingSpinner';

export default function BookingModal({ isOpen, onClose, resource, onSuccess }) {
  const [expectedArrival, setExpectedArrival] = useState('Immediate (Next 10 mins)');
  const [expectedDuration, setExpectedDuration] = useState('1 hour');
  const [groupSize, setGroupSize] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const navigate = useNavigate();

  const handleBooking = async (e) => {
    e.preventDefault();
    if (!resource) return;

    setLoading(true);
    setError(null);

    try {
      const payload = {
        resourceId: resource.id,
        expectedArrival,
        expectedDuration,
        groupSize: Number(groupSize),
      };

      const response = await api.post('/requests', payload);
      if (onSuccess) {
        onSuccess(response.data);
      }
      onClose();
      navigate('/student/requests');
    } catch (err) {
      console.error('Failed to create booking request:', err);
      const msg = err.response?.data?.message || 'Failed to submit request. You may already have an active request or this resource is unavailable.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  if (!resource) return null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={`Request ${resource.name}`}>
      <form onSubmit={handleBooking}>
        <div style={{ marginBottom: 16 }}>
          <div style={{ fontSize: 13, color: 'var(--navy-500)', marginBottom: 4 }}>
            Facility: <strong style={{ color: 'var(--navy-800)' }}>{resource.facility?.name || 'Campus Facility'}</strong>
          </div>
          <div style={{ fontSize: 13, color: 'var(--navy-500)' }}>
            Resource Type: <strong style={{ color: 'var(--navy-800)' }}>{resource.type}</strong>
          </div>
        </div>

        {error && (
          <div className="error-message-card" style={{ marginBottom: 16 }}>
            <div className="error-message-header">
              <AlertCircle className="error-icon" size={16} />
              <div className="error-desc">{error}</div>
            </div>
          </div>
        )}

        <div className="form-group">
          <label className="form-label">
            <Calendar size={14} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
            Expected Arrival
          </label>
          <select
            className="form-select"
            value={expectedArrival}
            onChange={(e) => setExpectedArrival(e.target.value)}
            disabled={loading}
          >
            <option value="Immediate (Next 10 mins)">Immediate (Next 10 mins)</option>
            <option value="In 15 minutes">In 15 minutes</option>
            <option value="In 30 minutes">In 30 minutes</option>
            <option value="In 45 minutes">In 45 minutes</option>
            <option value="In 1 hour">In 1 hour</option>
          </select>
          <span className="form-help">Hold grace period is 10 minutes from expected arrival.</span>
        </div>

        <div className="form-group">
          <label className="form-label">
            <Clock size={14} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
            Estimated Duration
          </label>
          <select
            className="form-select"
            value={expectedDuration}
            onChange={(e) => setExpectedDuration(e.target.value)}
            disabled={loading}
          >
            <option value="30 minutes">30 minutes</option>
            <option value="1 hour">1 hour</option>
            <option value="2 hours">2 hours</option>
            <option value="3 hours">3 hours</option>
            <option value="4 hours">4 hours</option>
          </select>
        </div>

        <div className="form-group">
          <label className="form-label">
            <Users size={14} style={{ display: 'inline', marginRight: 4, verticalAlign: 'text-bottom' }} />
            Group Size (Students)
          </label>
          <select
            className="form-select"
            value={groupSize}
            onChange={(e) => setGroupSize(e.target.value)}
            disabled={loading}
          >
            <option value={1}>1 Student (Individual)</option>
            <option value={2}>2 Students (Pair Study)</option>
            <option value={3}>3 Students (Group Study)</option>
          </select>
        </div>

        <div className="form-actions">
          <button type="button" onClick={onClose} className="btn btn-secondary" disabled={loading}>
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? <LoadingSpinner text="Submitting..." size={16} /> : 'Submit Usage Request'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
