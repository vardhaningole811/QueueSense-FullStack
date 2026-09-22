import { AlertTriangle, RefreshCw } from 'lucide-react';

export default function ErrorMessage({ message, onRetry, title = 'An error occurred' }) {
  return (
    <div className="error-message-card" role="alert">
      <div className="error-message-header">
        <AlertTriangle className="error-icon" size={22} />
        <div>
          <h4 className="error-title">{title}</h4>
          <p className="error-desc">{message || 'Unable to complete the requested action. Please try again.'}</p>
        </div>
      </div>
      {onRetry && (
        <button type="button" onClick={onRetry} className="btn btn-outline btn-sm error-retry-btn">
          <RefreshCw size={14} />
          <span>Try Again</span>
        </button>
      )}
    </div>
  );
}
