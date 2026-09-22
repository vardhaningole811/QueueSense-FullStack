import { Loader2 } from 'lucide-react';

export default function LoadingSpinner({ text = 'Loading...', size = 28 }) {
  return (
    <div className="spinner-container" role="status" aria-live="polite">
      <Loader2 size={size} className="spinner-icon animate-spin" />
      {text && <span className="spinner-text">{text}</span>}
    </div>
  );
}
