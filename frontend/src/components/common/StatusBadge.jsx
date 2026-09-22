import {
  CheckCircle,
  Clock,
  AlertCircle,
  XCircle,
  CheckCircle2,
  Lock,
  Wrench,
  Ban,
  UserCheck,
} from 'lucide-react';

const STATUS_CONFIG = {
  // Resource Statuses
  AVAILABLE: {
    label: 'Available',
    className: 'badge-available',
    icon: CheckCircle,
  },
  RESERVED: {
    label: 'Reserved',
    className: 'badge-reserved',
    icon: Clock,
  },
  OCCUPIED: {
    label: 'Occupied',
    className: 'badge-occupied',
    icon: Lock,
  },
  MAINTENANCE: {
    label: 'Maintenance',
    className: 'badge-maintenance',
    icon: Wrench,
  },

  // Request Statuses
  PENDING: {
    label: 'Pending Approval',
    className: 'badge-pending',
    icon: Clock,
  },
  APPROVED: {
    label: 'Approved',
    className: 'badge-approved',
    icon: CheckCircle2,
  },
  CHECKED_IN: {
    label: 'Checked In',
    className: 'badge-checked-in',
    icon: UserCheck,
  },
  COMPLETED: {
    label: 'Completed',
    className: 'badge-completed',
    icon: CheckCircle,
  },
  REJECTED: {
    label: 'Rejected',
    className: 'badge-rejected',
    icon: XCircle,
  },
  EXPIRED: {
    label: 'Expired',
    className: 'badge-expired',
    icon: AlertCircle,
  },
  CANCELLED: {
    label: 'Cancelled',
    className: 'badge-cancelled',
    icon: Ban,
  },
};

export default function StatusBadge({ status, size = 'normal' }) {
  const normalizedStatus = (status || '').toUpperCase();
  const config = STATUS_CONFIG[normalizedStatus] || {
    label: status || 'Unknown',
    className: 'badge-default',
    icon: AlertCircle,
  };

  const Icon = config.icon;
  const iconSize = size === 'small' ? 12 : 14;

  return (
    <span className={`status-badge ${config.className} badge-${size}`}>
      <Icon size={iconSize} className="badge-icon" />
      <span className="badge-label">{config.label}</span>
    </span>
  );
}
