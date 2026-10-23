import './CrowdAnalytics.css';
import { useState, useEffect, useCallback } from 'react';
import api from '../../services/api';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import ErrorMessage from '../../components/common/ErrorMessage';
import {
  TrendingUp,
  Clock,
  ShieldAlert,
  Sparkles,
  RefreshCw,
  AlertCircle,
  HelpCircle,
  Activity,
  BarChart3,
  Compass,
} from 'lucide-react';

import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  ReferenceLine,
} from 'recharts';

const COLORS = {
  primary: '#6366f1',
  green: '#10b981',
  amber: '#f59e0b',
  red: '#ef4444',
  grey: '#e2e8f0',
  text: '#64748b',
};

const chartGrid = {
  strokeDasharray: '3 3',
  stroke: '#dbe3ee',
};

const chartMargin = {
  top: 12,
  right: 20,
  left: 8,
  bottom: 8,
};

function ChartEmptyState({ children }) {
  return (
    <div
      style={{
        minHeight: 220,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: 24,
        color: 'var(--navy-500)',
        fontSize: 14,
      }}
    >
      {children}
    </div>
  );
}

function ChartCard({ title, description, children }) {
  return (
    <section
      className="section-card"
      style={{ minWidth: 0, marginBottom: 0 }}
    >
      <div className="section-card-header">
        <h2 className="section-title" style={{ margin: 0 }}>
          {title}
        </h2>
      </div>

      {description && (
        <p
          style={{
            color: 'var(--navy-500)',
            fontSize: 13,
            lineHeight: 1.6,
            marginTop: 10,
          }}
        >
          {description}
        </p>
      )}

      {children}
    </section>
  );
}

export default function CrowdAnalytics() {
  const [facilities, setFacilities] = useState([]);
  const [selectedFacilityId, setSelectedFacilityId] = useState('');
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Load facilities.
  useEffect(() => {
    let cancelled = false;

    async function loadFacilities() {
      try {
        const res = await api.get('/facilities');
        const facs = Array.isArray(res.data) ? res.data : [];

        if (cancelled) return;

        setFacilities(facs);

        if (facs.length > 0) {
          const library = facs.find((facility) =>
            facility.name?.toLowerCase().includes('library')
          );

          setSelectedFacilityId(String((library || facs[0]).id));
        } else {
          setLoading(false);
          setError('No facilities are available to display.');
        }
      } catch (err) {
        if (cancelled) return;

        console.error('Failed to load facilities:', err);
        setLoading(false);
        setError(
          'Could not load facilities. Please check that the backend is running.'
        );
      }
    }

    loadFacilities();

    return () => {
      cancelled = true;
    };
  }, []);

  // Load analytics for the selected facility.
  const fetchAnalytics = useCallback(
    async (facilityId, isManual = false) => {
      if (!facilityId) return;

      if (isManual) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError(null);

      try {
        const res = await api.get(
          `/analytics/facilities/${facilityId}/forecast`
        );

        setAnalytics(res.data);
      } catch (err) {
        console.error('Failed to fetch crowd analytics:', err);
        setError(
          'Unable to load crowd analytics. Check the backend and try again.'
        );
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    []
  );

  useEffect(() => {
    if (selectedFacilityId) {
      fetchAnalytics(selectedFacilityId);
    }
  }, [selectedFacilityId, fetchAnalytics]);

  const getCrowdBadgeClass = (level) => {
    switch (level) {
      case 'LOW':
        return 'badge-crowd-low';
      case 'MODERATE':
        return 'badge-crowd-moderate';
      case 'HIGH':
        return 'badge-crowd-high';
      case 'FULL':
        return 'badge-crowd-full';
      default:
        return 'badge-crowd-unknown';
    }
  };

  const getProgressColor = (percentage) => {
    if (percentage == null) return 'var(--navy-400)';
    if (percentage < 40) return COLORS.green;
    if (percentage < 75) return COLORS.amber;
    if (percentage < 90) return '#ea580c';
    return COLORS.red;
  };

  // Validate and prepare the data used by all charts.
  const capacity = Number(analytics?.usableCapacity);
  const activeSessions = Number(analytics?.recordedActiveSessions);
  const availableResources = Number(analytics?.availableUsableResources);
  const maintenanceResources = Number(analytics?.maintenanceResources);

  const hasCapacity =
    analytics?.usableCapacity != null &&
    Number.isFinite(capacity) &&
    capacity >= 0;

  const hasSessionCount =
    analytics?.recordedActiveSessions != null &&
    Number.isFinite(activeSessions) &&
    activeSessions >= 0;

  const hasAvailableCount =
    analytics?.availableUsableResources != null &&
    Number.isFinite(availableResources) &&
    availableResources >= 0;

  const hasMaintenanceCount =
    analytics?.maintenanceResources != null &&
    Number.isFinite(maintenanceResources) &&
    maintenanceResources >= 0;

  const safeSessions = hasSessionCount ? activeSessions : 0;

  // Clamp the donut slices to capacity so the slices always add up correctly.
  const donutSessions = hasCapacity
    ? Math.min(capacity, safeSessions)
    : 0;

  const remainingCapacity = hasCapacity
    ? Math.max(0, capacity - donutSessions)
    : 0;

  const calculatedOccupancy =
    hasCapacity && capacity > 0
      ? Number(((donutSessions / capacity) * 100).toFixed(1))
      : null;

  const forecasts = Array.isArray(analytics?.forecasts)
    ? analytics.forecasts
        .filter(
          (forecast) =>
            Number.isFinite(Number(forecast.horizonMinutes)) &&
            forecast.predictedOccupancy != null &&
            Number.isFinite(Number(forecast.predictedOccupancy))
        )
        .map((forecast) => ({
          ...forecast,
          horizonMinutes: Number(forecast.horizonMinutes),
          predictedOccupancy: Math.max(
            0,
            Number(forecast.predictedOccupancy)
          ),
        }))
        .sort((a, b) => a.horizonMinutes - b.horizonMinutes)
    : [];

  // Maintenance resources are excluded from usable capacity.
  // "Remaining usable" is not assumed to mean physically occupied.
  const resourceChartData =
    hasCapacity && hasAvailableCount && hasMaintenanceCount
      ? [
          {
            category: 'Resources',
            Available: Math.min(capacity, availableResources),
            'Remaining usable': Math.max(
              0,
              capacity - Math.min(capacity, availableResources)
            ),
            Maintenance: maintenanceResources,
          },
        ]
      : [];

  return (
    <div className="crowd-analytics-page">
      {/* Header */}
      <div className="page-header" style={{ marginBottom: 20 }}>
        <div>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              flexWrap: 'wrap',
              gap: 10,
              marginBottom: 4,
            }}
          >
            <div className="analytics-header-icon">
              <TrendingUp size={22} color="var(--primary-600)" />
            </div>

            <h1 className="page-title" style={{ margin: 0 }}>
              ML Crowd Analytics &amp; Flow Forecast
            </h1>

            <span className="badge-prototype">
              PROTOTYPE — VALIDATION PENDING
            </span>
          </div>

          <p className="page-description">
            Recorded facility occupancy, resource availability and
            machine-learning forecasts for the next 15, 30 and 60 minutes.
          </p>
        </div>

        <div
          className="header-actions"
          style={{
            display: 'flex',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 12,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <label
              htmlFor="facility-select"
              style={{
                fontSize: 13,
                fontWeight: 600,
                color: 'var(--navy-600)',
              }}
            >
              Facility:
            </label>

            <select
              id="facility-select"
              className="form-select"
              value={selectedFacilityId}
              onChange={(event) =>
                setSelectedFacilityId(event.target.value)
              }
              disabled={loading || refreshing}
              style={{ minWidth: 180, fontWeight: 600 }}
            >
              {facilities.map((facility) => (
                <option key={facility.id} value={facility.id}>
                  {facility.name} ({facility.type})
                </option>
              ))}
            </select>
          </div>

          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => fetchAnalytics(selectedFacilityId, true)}
            disabled={loading || refreshing || !selectedFacilityId}
            title="Refresh occupancy metrics and forecasts"
          >
            <RefreshCw
              size={14}
              className={refreshing ? 'animate-spin' : ''}
            />
            <span>{refreshing ? 'Refreshing...' : 'Live Sync'}</span>
          </button>
        </div>
      </div>

      {error && (
        <ErrorMessage
          message={error}
          onRetry={() => fetchAnalytics(selectedFacilityId)}
        />
      )}

      {loading ? (
        <div className="page-loading-wrapper">
          <LoadingSpinner text="Loading recorded occupancy and crowd forecasts..." />
        </div>
      ) : !analytics ? (
        <div className="empty-state-card">
          <AlertCircle size={32} color="var(--navy-400)" />
          <h3>No Analytics Available</h3>
          <p>
            Select a campus facility to view occupancy and forecast data.
          </p>
        </div>
      ) : (
        <>
          {/* Prototype disclaimer */}
          <div className="prototype-notice-banner">
            <div className="notice-icon">
              <ShieldAlert size={20} />
            </div>

            <div className="notice-content">
              <strong>Academic Prototype &amp; Simulation Disclosure</strong>
              <p>
                {analytics.modelTransparency?.disclaimer ||
                  'Forecasts are prototype predictions and require validation against verified physical observations.'}
              </p>
            </div>
          </div>

          {/* ML service status */}
          {analytics.forecastStatus === 'ML_SERVICE_OFFLINE' && (
            <div className="offline-alert-banner">
              <AlertCircle size={18} />

              <div>
                <strong>ML Inference Service Offline:</strong> Recorded
                occupancy is available, but future predictions are currently
                unavailable. Start the Python service using{' '}
                <code>python .\ml\src\service.py</code> and refresh.
              </div>
            </div>
          )}

          {/* Section 1: Recorded occupancy metrics */}
          <section
            className="section-card"
            style={{ marginBottom: 24 }}
          >
            <div className="section-card-header">
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <Activity size={18} color="var(--primary-600)" />

                <h2 className="section-title" style={{ margin: 0 }}>
                  Current Recorded Occupancy — {analytics.facilityName}
                </h2>
              </div>

              <span
                className={`badge-crowd ${getCrowdBadgeClass(
                  analytics.recordedCrowdLevel
                )}`}
              >
                {analytics.recordedCrowdLevel || 'UNKNOWN'} DENSITY
              </span>
            </div>

            <div className="analytics-metrics-grid">
              <div className="metric-box">
                <span className="metric-box-title">
                  Active Digital Sessions
                </span>
                <span className="metric-box-value">
                  {analytics.recordedActiveSessions ?? 'N/A'}
                </span>
                <span className="metric-box-subtitle">
                  Checked-in students
                </span>
              </div>

              <div className="metric-box">
                <span className="metric-box-title">
                  Verified Usable Capacity
                </span>
                <span className="metric-box-value">
                  {analytics.usableCapacity ?? 'N/A'}
                </span>
                <span className="metric-box-subtitle">
                  Excludes {analytics.maintenanceResources ?? 'N/A'}{' '}
                  maintenance resource(s)
                </span>
              </div>

              <div className="metric-box">
                <span className="metric-box-title">
                  Available Usable Resources
                </span>
                <span
                  className="metric-box-value"
                  style={{ color: COLORS.green }}
                >
                  {analytics.availableUsableResources ?? 'N/A'}
                </span>
                <span className="metric-box-subtitle">
                  Ready for immediate check-in
                </span>
              </div>

              <div className="metric-box">
                <span className="metric-box-title">
                  Recorded Occupancy
                </span>
                <span className="metric-box-value">
                  {analytics.recordedOccupancyPercentage != null
                    ? `${analytics.recordedOccupancyPercentage}%`
                    : 'N/A'}
                </span>
                <span className="metric-box-subtitle">
                  Based on recorded sessions and usable capacity
                </span>
              </div>
            </div>

            {analytics.recordedOccupancyPercentage != null && (
              <div style={{ marginTop: 20 }}>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 12,
                    fontSize: 12,
                    marginBottom: 6,
                    fontWeight: 600,
                  }}
                >
                  <span>Recorded Occupancy Gauge</span>
                  <span>
                    {analytics.recordedOccupancyPercentage}% filled
                  </span>
                </div>

                <div className="progress-bar-track">
                  <div
                    className="progress-bar-fill"
                    style={{
                      width: `${Math.min(
                        100,
                        Math.max(
                          0,
                          Number(analytics.recordedOccupancyPercentage) || 0
                        )
                      )}%`,
                      backgroundColor: getProgressColor(
                        Number(analytics.recordedOccupancyPercentage)
                      ),
                    }}
                  />
                </div>
              </div>
            )}

            <div className="clarification-box" style={{ marginTop: 16 }}>
              <HelpCircle
                size={15}
                style={{ flexShrink: 0, marginTop: 2 }}
              />
              <span>
                <strong>Occupancy Semantics:</strong>{' '}
                {analytics.occupancyTypeClarification ||
                  'Recorded digital sessions may differ from actual physical occupancy.'}
              </span>
            </div>
          </section>

          {/* Additional visualizations */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns:
                'repeat(auto-fit, minmax(min(100%, 360px), 1fr))',
              gap: 20,
              marginBottom: 24,
              alignItems: 'stretch',
            }}
          >
            {/* Chart 1: Occupancy donut */}
            <ChartCard
              title="Current Occupancy"
              description="Recorded digital sessions compared with remaining usable capacity. This is not a physical headcount."
            >
              {hasCapacity && capacity > 0 && hasSessionCount ? (
                <>
                  <div
                    style={{
                      width: '100%',
                      height: 260,
                      position: 'relative',
                    }}
                  >
                    <ResponsiveContainer width="100%" height="100%">
                      <PieChart>
                        <Pie
                          data={[
                            {
                              name: 'Recorded active sessions',
                              value: donutSessions,
                            },
                            {
                              name: 'Remaining capacity',
                              value: remainingCapacity,
                            },
                          ]}
                          dataKey="value"
                          nameKey="name"
                          cx="50%"
                          cy="50%"
                          innerRadius={65}
                          outerRadius={92}
                          paddingAngle={2}
                        >
                          <Cell fill={COLORS.primary} />
                          <Cell fill={COLORS.grey} />
                        </Pie>

                        <Tooltip />
                        <Legend />
                      </PieChart>
                    </ResponsiveContainer>

                    <div
                      style={{
                        position: 'absolute',
                        inset: '0 0 0 0',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        pointerEvents: 'none',
                        paddingBottom: 20,
                      }}
                    >
                      <strong style={{ fontSize: 25 }}>
                        {calculatedOccupancy}%
                      </strong>
                      <span
                        style={{
                          fontSize: 11,
                          color: 'var(--navy-500)',
                        }}
                      >
                        Recorded
                      </span>
                    </div>
                  </div>

                  <p
                    style={{
                      textAlign: 'center',
                      fontSize: 13,
                      color: 'var(--navy-500)',
                    }}
                  >
                    {activeSessions} active sessions · {capacity} usable
                    resources
                  </p>
                </>
              ) : (
                <ChartEmptyState>
                  A positive usable capacity and valid session count are
                  required for this chart.
                </ChartEmptyState>
              )}
            </ChartCard>

            {/* Chart 2: Resource availability */}
            <ChartCard
              title="Resource Availability"
              description="Available and remaining usable resources, with maintenance resources shown separately."
            >
              {resourceChartData.length > 0 ? (
                <>
                  <div style={{ width: '100%', height: 280 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart
                        data={resourceChartData}
                        margin={chartMargin}
                      >
                        <CartesianGrid {...chartGrid} />
                        <XAxis dataKey="category" />
                        <YAxis allowDecimals={false} />
                        <Tooltip />
                        <Legend />
                        <Bar
                          dataKey="Available"
                          fill={COLORS.green}
                          radius={[5, 5, 0, 0]}
                        />
                        <Bar
                          dataKey="Remaining usable"
                          fill={COLORS.amber}
                          radius={[5, 5, 0, 0]}
                        />
                        <Bar
                          dataKey="Maintenance"
                          fill={COLORS.red}
                          radius={[5, 5, 0, 0]}
                        />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>

                  <p
                    style={{
                      fontSize: 12,
                      color: 'var(--navy-500)',
                    }}
                  >
                    Remaining usable resources are not necessarily occupied.
                    Maintenance is excluded from usable capacity.
                  </p>
                </>
              ) : (
                <ChartEmptyState>
                  Complete resource counts are required for this chart.
                </ChartEmptyState>
              )}
            </ChartCard>

            {/* Chart 3: Forecast vs capacity */}
            <ChartCard
              title="Forecast vs Capacity"
              description="Compare predicted occupancy with the facility's usable capacity."
            >
              {forecasts.length > 0 && hasCapacity ? (
                <div style={{ width: '100%', height: 300 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart
                      data={forecasts.map((forecast) => ({
                        horizon: `+${forecast.horizonMinutes} min`,
                        predicted: forecast.predictedOccupancy,
                      }))}
                      margin={chartMargin}
                    >
                      <CartesianGrid {...chartGrid} />
                      <XAxis dataKey="horizon" />
                      <YAxis
                        allowDecimals={false}
                        domain={[
                          0,
                          (dataMax) =>
                            Math.max(dataMax, capacity) * 1.1 || 1,
                        ]}
                      />
                      <Tooltip />
                      <Legend />

                      <ReferenceLine
                        y={capacity}
                        stroke={COLORS.red}
                        strokeDasharray="5 5"
                        label={{
                          value: 'Capacity',
                          position: 'insideTopRight',
                        }}
                      />

                      <Line
                        type="monotone"
                        dataKey="predicted"
                        name="Predicted people"
                        stroke={COLORS.primary}
                        strokeWidth={3}
                        dot={{ r: 5 }}
                        activeDot={{ r: 7 }}
                      />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <ChartEmptyState>
                  Forecasts and usable capacity must be available to compare
                  predicted occupancy with capacity.
                </ChartEmptyState>
              )}
            </ChartCard>

            {/* Chart 4: Forecast comparison */}
            <ChartCard
              title="Forecast Comparison"
              description="Compare predicted occupancy across the available forecast horizons."
            >
              {forecasts.length > 0 ? (
                <div style={{ width: '100%', height: 300 }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                      data={forecasts.map((forecast) => ({
                        horizon: `+${forecast.horizonMinutes} min`,
                        people: forecast.predictedOccupancy,
                      }))}
                      margin={chartMargin}
                    >
                      <CartesianGrid {...chartGrid} />
                      <XAxis dataKey="horizon" />
                      <YAxis allowDecimals={false} domain={[0, 'auto']} />
                      <Tooltip />
                      <Bar
                        dataKey="people"
                        name="Predicted people"
                        fill={COLORS.primary}
                        radius={[6, 6, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <ChartEmptyState>
                  Forecast comparison will appear when the ML service returns
                  predictions.
                </ChartEmptyState>
              )}
            </ChartCard>
          </div>

          {/* Main forecast section */}
          <section
            className="section-card"
            style={{ marginBottom: 24 }}
          >
            <div className="section-card-header">
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <Sparkles size={18} color="var(--primary-600)" />
                <h2 className="section-title" style={{ margin: 0 }}>
                  Future Crowd Forecasts (15, 30 &amp; 60 Minutes Ahead)
                </h2>
              </div>

              <span
                style={{
                  fontSize: 12,
                  color: 'var(--navy-500)',
                }}
              >
                Predicted occupancy
              </span>
            </div>

            {forecasts.length > 0 ? (
              <>
                <div style={{ marginTop: 20, marginBottom: 28 }}>
                  <h3 style={{ fontSize: 15, marginBottom: 8 }}>
                    Predicted Crowd Trend
                  </h3>

                  <p
                    style={{
                      fontSize: 12,
                      color: 'var(--navy-500)',
                      marginBottom: 16,
                    }}
                  >
                    Estimated people at each forecast horizon. These are
                    prototype predictions, not verified physical headcounts.
                  </p>

                  <div style={{ width: '100%', height: 300 }}>
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart
                        data={forecasts.map((forecast) => ({
                          time: `+${forecast.horizonMinutes} min`,
                          people: forecast.predictedOccupancy,
                        }))}
                        margin={chartMargin}
                      >
                        <CartesianGrid {...chartGrid} />
                        <XAxis dataKey="time" />
                        <YAxis
                          allowDecimals={false}
                          domain={[0, 'auto']}
                          label={{
                            value: 'Predicted people',
                            angle: -90,
                            position: 'insideLeft',
                          }}
                        />
                        <Tooltip />
                        <Legend />
                        {hasCapacity && (
                          <ReferenceLine
                            y={capacity}
                            stroke={COLORS.red}
                            strokeDasharray="5 5"
                            label={{
                              value: 'Capacity',
                              position: 'insideTopRight',
                            }}
                          />
                        )}
                        <Line
                          type="monotone"
                          dataKey="people"
                          name="Predicted occupancy"
                          stroke={COLORS.primary}
                          strokeWidth={3}
                          dot={{ r: 5 }}
                          activeDot={{ r: 7 }}
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <h3 style={{ fontSize: 15, marginBottom: 14 }}>
                  Forecast Details
                </h3>

                <div className="forecast-grid">
                  {forecasts.map((forecast) => (
                    <div
                      key={forecast.horizonMinutes}
                      className="forecast-card"
                    >
                      <div className="forecast-card-top">
                        <span className="forecast-horizon-pill">
                          +{forecast.horizonMinutes} Minutes
                        </span>
                        <span className="forecast-target-time">
                          {forecast.targetTimestamp || 'Target time unavailable'}
                        </span>
                      </div>

                      <div className="forecast-headcount-row">
                        <span className="forecast-headcount-value">
                          {forecast.predictedOccupancy}
                        </span>
                        <span className="forecast-headcount-unit">
                          predicted people
                        </span>
                      </div>

                      <div style={{ margin: '12px 0' }}>
                        <div
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            gap: 8,
                            fontSize: 12,
                            marginBottom: 4,
                          }}
                        >
                          <span style={{ color: 'var(--navy-500)' }}>
                            Predicted capacity usage
                          </span>

                          <span style={{ fontWeight: 600 }}>
                            {forecast.predictedOccupancyPercentage != null
                              ? `${forecast.predictedOccupancyPercentage}%`
                              : 'N/A'}
                          </span>
                        </div>

                        <div
                          className="progress-bar-track"
                          style={{ height: 6 }}
                        >
                          <div
                            className="progress-bar-fill"
                            style={{
                              width: `${Math.min(
                                100,
                                Math.max(
                                  0,
                                  Number(
                                    forecast.predictedOccupancyPercentage
                                  ) || 0
                                )
                              )}%`,
                              backgroundColor: getProgressColor(
                                Number(
                                  forecast.predictedOccupancyPercentage
                                )
                              ),
                            }}
                          />
                        </div>
                      </div>

                      <div
                        style={{
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          gap: 8,
                          flexWrap: 'wrap',
                        }}
                      >
                        <span
                          className={`badge-crowd ${getCrowdBadgeClass(
                            forecast.predictedCrowdLevel
                          )}`}
                        >
                          {forecast.predictedCrowdLevel || 'UNKNOWN'}
                        </span>

                        <span
                          style={{
                            fontSize: 11,
                            color: 'var(--navy-400)',
                          }}
                        >
                          {forecast.confidenceNote || 'Regression forecast'}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <ChartEmptyState>
                {analytics.forecastStatus === 'ML_SERVICE_OFFLINE'
                  ? 'Predictions are unavailable because the ML service is offline.'
                  : 'The API returned no valid forecasts for this facility.'}
              </ChartEmptyState>
            )}
          </section>

          {/* Decision support */}
          <div
            className="decision-support-grid"
            style={{ marginBottom: 24 }}
          >
            <div className="decision-card">
              <div
                className="decision-card-icon"
                style={{
                  backgroundColor: '#fef3c7',
                  color: '#d97706',
                }}
              >
                <Clock size={20} />
              </div>

              <div className="decision-card-body">
                <span className="decision-card-title">
                  Estimated Time Until Full
                </span>

                <span className="decision-card-highlight">
                  {analytics.estimatedTimeUntilFullMinutes != null
                    ? `~${analytics.estimatedTimeUntilFullMinutes} Minutes`
                    : 'Not enough data to estimate'}
                </span>

                <p className="decision-card-desc">
                  {analytics.timeUntilFullMessage ||
                    'Insufficient data to estimate time until full.'}
                </p>
              </div>
            </div>

            <div className="decision-card">
              <div
                className="decision-card-icon"
                style={{
                  backgroundColor: '#d1fae5',
                  color: '#059669',
                }}
              >
                <Compass size={20} />
              </div>

              <div className="decision-card-body">
                <span className="decision-card-title">
                  Best Time to Visit
                </span>

                <span className="decision-card-highlight">
                  Optimal Study Window
                </span>

                <p className="decision-card-desc">
                  {analytics.bestTimeToVisit ||
                    'No visit recommendation is available.'}
                </p>
              </div>
            </div>
          </div>

          {/* Model transparency */}
          <section className="section-card">
            <div className="section-card-header">
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                }}
              >
                <BarChart3 size={18} color="var(--primary-600)" />
                <h2 className="section-title" style={{ margin: 0 }}>
                  Machine Learning Transparency &amp; Model Evaluation
                </h2>
              </div>

              <span className="badge-prototype">ACADEMIC AUDIT</span>
            </div>

            <div className="transparency-table-container">
              <table className="transparency-table">
                <tbody>
                  <tr>
                    <td style={{ fontWeight: 600, width: '25%' }}>
                      Mode / Status
                    </td>
                    <td>
                      <span
                        className="badge-prototype"
                        style={{ display: 'inline-block' }}
                      >
                        {(analytics.modelTransparency?.isPrototype ??
                          analytics.modelTransparency?.prototype)
                          ? 'PROTOTYPE — REAL-WORLD VALIDATION PENDING'
                          : 'CHECK VALIDATION STATUS'}
                      </span>
                    </td>
                  </tr>

                  <tr>
                    <td style={{ fontWeight: 600 }}>Forecast Status</td>
                    <td>
                      {analytics.forecastStatus || 'Status unavailable'}
                    </td>
                  </tr>

                  <tr>
                    <td style={{ fontWeight: 600 }}>Data Source</td>
                    <td>
                      {analytics.modelTransparency?.dataSource ||
                        'Data source unavailable'}
                    </td>
                  </tr>

                  <tr>
                    <td style={{ fontWeight: 600 }}>Model Selected</td>
                    <td>
                      <strong>
                        {analytics.modelTransparency?.modelSelected ||
                          'Model information unavailable'}
                      </strong>
                    </td>
                  </tr>

                  <tr>
                    <td style={{ fontWeight: 600 }}>Target &amp; Horizons</td>
                    <td>
                      Occupancy forecast at +15, +30 and +60 minutes, where
                      supported by the returned predictions.
                    </td>
                  </tr>

                  <tr>
                    <td style={{ fontWeight: 600 }}>
                      Validation Performance
                    </td>
                    <td>
                      {analytics.modelTransparency?.validationMae != null ? (
                        <div
                          style={{
                            display: 'flex',
                            gap: 16,
                            flexWrap: 'wrap',
                          }}
                        >
                          <span>
                            <strong>Validation MAE:</strong>{' '}
                            {analytics.modelTransparency.validationMae}{' '}
                            people
                          </span>

                          {analytics.modelTransparency.validationRmse !=
                            null && (
                            <span>
                              <strong>Validation RMSE:</strong>{' '}
                              {analytics.modelTransparency.validationRmse}{' '}
                              people
                            </span>
                          )}

                          {analytics.modelTransparency.baselineMae != null && (
                            <span>
                              <strong>Baseline MAE:</strong>{' '}
                              {analytics.modelTransparency.baselineMae}
                            </span>
                          )}
                        </div>
                      ) : (
                        <span style={{ color: 'var(--navy-400)' }}>
                          Validation metrics are not available from the API.
                        </span>
                      )}
                    </td>
                  </tr>

                  <tr>
                    <td style={{ fontWeight: 600 }}>Models Compared</td>
                    <td>
                      Naive Persistence Baseline, Linear Regression,
                      Decision Tree Regressor, Random Forest Regressor and
                      Gradient Boosting Regressor — as documented by the
                      existing prototype.
                    </td>
                  </tr>

                  <tr>
                    <td style={{ fontWeight: 600 }}>Data Limitation</td>
                    <td>
                      Recorded digital sessions may differ from physical
                      occupancy. Synthetic-data predictions must not be
                      treated as verified real-world headcounts.
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </div>
  );
}
