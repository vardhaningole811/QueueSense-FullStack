import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { Compass, Lock, Mail, ArrowRight, AlertCircle, Sparkles } from 'lucide-react';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const isExpired = new URLSearchParams(location.search).get('expired') === 'true';

  const handleSubmit = async (e) => {
    e?.preventDefault();
    if (!email || !password) {
      setError('Please enter both email and password.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const user = await login(email, password);
      if (user.role === 'ROLE_STUDENT') {
        navigate('/student/dashboard');
      } else if (user.role === 'ROLE_OPERATOR') {
        navigate('/operator/dashboard');
      } else {
        navigate('/');
      }
    } catch (err) {
      console.error('Login error:', err);
      const message = err.response?.data?.message || 'Invalid email or password. Please check your credentials.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = async (demoEmail, demoPassword) => {
    setEmail(demoEmail);
    setPassword(demoPassword);
    setLoading(true);
    setError(null);

    try {
      const user = await login(demoEmail, demoPassword);
      if (user.role === 'ROLE_STUDENT') {
        navigate('/student/dashboard');
      } else if (user.role === 'ROLE_OPERATOR') {
        navigate('/operator/dashboard');
      }
    } catch (err) {
      console.error('Demo login error:', err);
      setError('Demo login failed. Ensure the backend and PostgreSQL are running.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page-root">
      <div className="login-card">
        <div className="login-header">
          <div className="login-logo-box">
            <Compass size={28} />
          </div>
          <h1 className="login-title">QueueSense Campus Portal</h1>
          <p className="login-subtitle">Smart Facility Availability & Resource Management</p>
        </div>

        {isExpired && (
          <div className="error-message-card" style={{ marginBottom: 16 }}>
            <div className="error-message-header">
              <AlertCircle className="error-icon" size={18} />
              <div className="error-desc">Your session has expired. Please sign in again.</div>
            </div>
          </div>
        )}

        {error && (
          <div className="error-message-card" style={{ marginBottom: 16 }}>
            <div className="error-message-header">
              <AlertCircle className="error-icon" size={18} />
              <div className="error-desc">{error}</div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label className="form-label" htmlFor="email">
              University Email
            </label>
            <input
              id="email"
              type="email"
              className="form-input"
              placeholder="e.g. student@queuesense.demo"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary btn-lg"
            style={{ width: '100%', marginTop: 8 }}
            disabled={loading}
          >
            {loading ? (
              <LoadingSpinner text="Authenticating..." size={18} />
            ) : (
              <>
                <span>Sign In to Portal</span>
                <ArrowRight size={16} />
              </>
            )}
          </button>
        </form>

        <div className="demo-credentials-box">
          <span className="demo-label">1-Click Demo Evaluation Sign In</span>
          <div className="demo-buttons-grid">
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleDemoLogin('student@queuesense.demo', 'Student@123')}
              disabled={loading}
            >
              <Sparkles size={13} style={{ color: 'var(--primary)' }} />
              <span>Student View</span>
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => handleDemoLogin('operator@queuesense.demo', 'Operator@123')}
              disabled={loading}
            >
              <Sparkles size={13} style={{ color: 'var(--color-reserved)' }} />
              <span>Operator View</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
