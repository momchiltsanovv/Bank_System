import React, { useState } from 'react';
import Layout from './components/Layout';
import ClientsPage from './pages/ClientsPage';
import AccountsPage from './pages/AccountsPage';
import LoansPage from './pages/LoansPage';

export type Page = 'clients' | 'accounts' | 'loans';

const App: React.FC = () => {
  const [currentPage, setCurrentPage] = useState<Page>('clients');

  const renderPage = () => {
    switch (currentPage) {
      case 'clients':
        return <ClientsPage />;
      case 'accounts':
        return <AccountsPage />;
      case 'loans':
        return <LoansPage />;
      default:
        return <ClientsPage />;
    }
  };

  return (
    <Layout currentPage={currentPage} onNavigate={setCurrentPage}>
      {renderPage()}
    </Layout>
  );
};

export default App;
