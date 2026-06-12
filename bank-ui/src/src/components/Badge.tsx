import React from 'react';

interface BadgeProps {
  status: 'ACTIVE' | 'CLOSED';
}

const Badge: React.FC<BadgeProps> = ({ status }) => {
  const styles: React.CSSProperties =
    status === 'ACTIVE'
      ? {
          background: '#e6f9f0',
          color: '#1a7a4a',
          border: '1px solid rgba(26, 122, 74, 0.18)',
        }
      : {
          background: '#fdecea',
          color: '#c0392b',
          border: '1px solid rgba(192, 57, 43, 0.18)',
        };

  return (
    <span
      style={{
        ...styles,
        display: 'inline-flex',
        alignItems: 'center',
        gap: '5px',
        padding: '3px 10px',
        borderRadius: '999px',
        fontSize: '0.78rem',
        fontWeight: 600,
        letterSpacing: '0.04em',
        whiteSpace: 'nowrap',
        fontFamily: 'var(--font-body)',
      }}
    >
      <span
        style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: status === 'ACTIVE' ? '#1a7a4a' : '#c0392b',
          display: 'inline-block',
          flexShrink: 0,
        }}
      />
      {status}
    </span>
  );
};

export default Badge;
