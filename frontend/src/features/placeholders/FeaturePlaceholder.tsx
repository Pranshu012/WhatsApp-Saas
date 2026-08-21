import React from 'react';
import { LucideIcon } from 'lucide-react';
import { EmptyState } from '../../components/EmptyState';

interface FeaturePlaceholderProps {
  title: string;
  subtitle: string;
  icon: LucideIcon;
  incrementBadge: string;
  emptyTitle: string;
  emptyDescription: string;
  actionLabel?: string;
  onAction?: () => void;
}

export const FeaturePlaceholder: React.FC<FeaturePlaceholderProps> = ({
  title,
  subtitle,
  icon: Icon,
  incrementBadge,
  emptyTitle,
  emptyDescription,
  actionLabel,
  onAction,
}) => {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-5 border-b border-gray-200">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-xl sm:text-2xl font-bold text-gray-900">{title}</h1>
            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-gray-100 text-gray-700">
              {incrementBadge}
            </span>
          </div>
          <p className="text-sm text-gray-500">{subtitle}</p>
        </div>
        {actionLabel && (
          <button
            type="button"
            onClick={onAction}
            className="inline-flex items-center justify-center px-4 py-2 text-sm font-medium text-white bg-brand-600 rounded-lg hover:bg-brand-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-500 min-h-[44px] min-w-[44px] self-start sm:self-auto"
          >
            {actionLabel}
          </button>
        )}
      </div>

      {/* Content */}
      <div className="py-6">
        <EmptyState
          icon={Icon}
          title={emptyTitle}
          description={emptyDescription}
          actionLabel={actionLabel}
          onAction={onAction}
        />
      </div>
    </div>
  );
};
