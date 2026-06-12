import React from 'react';

const Spinner: React.FC<{ size?: number }> = ({ size = 20 }) => {
  const style: React.CSSProperties = {
    display: 'inline-block',
    width: size,
    height: size,
    border: `2px solid rgba(201, 168, 76, 0.25)`,
    borderTopColor: '#c9a84c',
    borderRadius: '50%',
    animation: 'spin 0.7s linear infinite',
    flexShrink: 0,
  };

  return (
    <>
      <style>{`
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}</style>
      <span style={style} role="status" aria-label="Loading" />
    </>
  );
};

export default Spinner;
