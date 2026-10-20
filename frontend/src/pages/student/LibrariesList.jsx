import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import api from '../../services/api';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import { Building2, ArrowRight, BookOpen, Layers, CheckCircle2 } from 'lucide-react';

export default function LibrariesList() {
  const [facilities, setFacilities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchFacilities = async () => {
      try {
        const res = await api.get('/facilities');
        setFacilities(res.data || []);
      } catch (err) {
        console.error('Failed to load facilities:', err);
        setError('Failed to retrieve campus facility list. Please try again.');
      } finally {
        setLoading(false);
      }
    };
    fetchFacilities();
  }, []);

  if (loading) {
    return (
      <div className="page-loading-wrapper">
        <LoadingSpinner text="Loading campus libraries & facilities..." />
      </div>
    );
  }

  return (
    <div className="libraries-list-page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Campus Libraries & Facilities</h1>
          <p className="page-description">
            Select a facility to view real-time seating availability, PC stations, and submit booking requests.
          </p>
        </div>
      </div>

      {error && <ErrorMessage message={error} />}

      <div className="facility-grid">
        {facilities.map((fac) => {
          const isSports = fac.category === 'SPORTS' || fac.type === 'SPORTS' || fac.name?.toLowerCase().includes('sports');
          const isELibrary = fac.name?.toLowerCase().includes('e-library');
          const targetUrl = isSports
            ? '/student/sports'
            : isELibrary
            ? '/student/elibrary'
            : `/student/libraries/${fac.id}`;
          const targetLabel = isSports
            ? 'View Sports Complex'
            : isELibrary
            ? 'Book Seats (Floor Plan)'
            : 'View & Book Resources';

          return (
            <div key={fac.id} className="facility-card">
              <div>
                <div className="facility-card-header">
                  <h3 className="facility-title">{fac.name}</h3>
                  <span className="facility-type-tag">{fac.category}</span>
                </div>
                <p className="facility-desc">{fac.description}</p>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: 'var(--navy-600)', marginBottom: 16 }}>
                  <CheckCircle2 size={15} style={{ color: 'var(--color-available)' }} />
                  <span>Active automated resource allocation enabled</span>
                </div>
              </div>
              <div className="facility-card-footer">
                <span style={{ fontSize: 12, color: 'var(--navy-500)', fontWeight: 500 }}>
                  Facility #{fac.id}
                </span>
                <Link to={targetUrl} className="btn btn-primary btn-sm">
                  <span>{targetLabel}</span>
                  <ArrowRight size={14} />
                </Link>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
