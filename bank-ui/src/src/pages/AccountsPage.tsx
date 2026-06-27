import React, { useState, useCallback, useRef } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { SelectButton } from 'primereact/selectbutton';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Message } from 'primereact/message';
import { openAccount, getAccountsByClient, closeAccount, getIndividualByEgn, getCorporateByEik } from '../api';
import { BankAccount } from '../types';
import './AccountsPage.css';

type SearchMode = 'ID' | 'EGN' | 'EIK';
const SEARCH_MODES = [
  { label: 'ID', value: 'ID' },
  { label: 'EGN', value: 'EGN' },
  { label: 'EIK', value: 'EIK' },
];

const AccountsPage: React.FC = () => {
  const toast = useRef<Toast>(null);

  const [clientId, setClientId]   = useState<number | null>(null);
  const [iban, setIban]           = useState('');
  const [opening, setOpening]     = useState(false);

  const [searchMode, setSearchMode]     = useState<SearchMode>('ID');
  const [searchQuery, setSearchQuery]   = useState('');
  const [resolvedId, setResolvedId]     = useState<number | null>(null);
  const [searchLabel, setSearchLabel]   = useState('');
  const [accounts, setAccounts]         = useState<BankAccount[]>([]);
  const [searching, setSearching]       = useState(false);
  const [hasSearched, setHasSearched]   = useState(false);
  const [closingId, setClosingId]       = useState<number | null>(null);

  const loadAccounts = useCallback(async (id: number) => {
    setSearching(true);
    try { setAccounts(await getAccountsByClient(id)); }
    catch (e: any) { toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message }); setAccounts([]); }
    finally { setSearching(false); }
  }, []);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) {
      toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Enter a search value.' }); return;
    }
    setSearching(true);
    try {
      let clientId: number;
      if (searchMode === 'EGN') {
        const client = await getIndividualByEgn(searchQuery.trim());
        clientId = client.id;
        setSearchLabel(`EGN ${searchQuery.trim()} (Client #${clientId})`);
      } else if (searchMode === 'EIK') {
        const client = await getCorporateByEik(searchQuery.trim());
        clientId = client.id;
        setSearchLabel(`EIK ${searchQuery.trim()} (Client #${clientId})`);
      } else {
        clientId = parseInt(searchQuery.trim(), 10);
        if (isNaN(clientId) || clientId <= 0) {
          toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Enter a valid numeric Client ID.' });
          setSearching(false); return;
        }
        setSearchLabel(`#${clientId}`);
      }
      setResolvedId(clientId);
      setHasSearched(true);
      await loadAccounts(clientId);
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
      setSearching(false);
    }
  };

  const handleOpen = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!clientId || !iban.trim()) {
      toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Client ID and IBAN are required.' }); return;
    }
    setOpening(true);
    try {
      const acc = await openAccount({ clientId, iban: iban.trim() });
      toast.current?.show({ severity: 'success', summary: 'Account Opened', detail: `${acc.iban} is now active.` });
      setClientId(null); setIban('');
      if (resolvedId === clientId) await loadAccounts(clientId);
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
    } finally { setOpening(false); }
  };

  const handleClose = async (acc: BankAccount) => {
    setClosingId(acc.id);
    try {
      await closeAccount(acc.id);
      if (resolvedId) await loadAccounts(resolvedId);
      toast.current?.show({ severity: 'success', summary: 'Closed', detail: `${acc.iban} has been closed.` });
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
    } finally { setClosingId(null); }
  };

  const active = accounts.filter(a => a.status === 'ACTIVE').length;
  const closed = accounts.filter(a => a.status === 'CLOSED').length;

  return (
    <div className="ap-page-wrap">
      <Toast ref={toast} position="bottom-right" />
      <div className="bs-page-header">
        <h1 className="bs-page-title">Accounts</h1>
        <p className="bs-page-sub">Open, monitor and close client bank accounts</p>
      </div>

      <div className="ap-top-grid">
        {/* Open account */}
        <Card>
          <div className="ap-form-header">
            <div className="ap-form-icon"><i className="pi pi-plus-circle" /></div>
            <div>
              <div className="ap-form-title">Open New Account</div>
              <div className="ap-form-sub">Create a new IBAN account for a client</div>
            </div>
          </div>
          <form className="ap-form" onSubmit={handleOpen}>
            <div className="ap-field">
              <label>Client ID</label>
              <InputNumber value={clientId} onValueChange={e => setClientId(e.value ?? null)}
                placeholder="e.g. 1" min={1} disabled={opening} useGrouping={false} />
            </div>
            <div className="ap-field">
              <label>IBAN</label>
              <InputText value={iban} onChange={e => setIban(e.target.value)}
                placeholder="BG80BNBG96611020345678" disabled={opening} />
            </div>
            <Button type="submit" label="Open Account" icon="pi pi-plus"
              className="bs-btn-navy" loading={opening} style={{ marginTop: 4 }} />
          </form>
        </Card>

        {/* Search */}
        <Card>
          <div className="ap-form-header">
            <div className="ap-form-icon ap-form-icon--search"><i className="pi pi-search" /></div>
            <div>
              <div className="ap-form-title">Find Client Accounts</div>
              <div className="ap-form-sub">Search by Client ID, EGN, or EIK</div>
            </div>
          </div>
          <form className="ap-form" onSubmit={handleSearch}>
            <div className="ap-field">
              <label>Search by</label>
              <SelectButton value={searchMode} options={SEARCH_MODES}
                onChange={e => { setSearchMode(e.value); setSearchQuery(''); }}
                disabled={searching} />
            </div>
            <div className="ap-field">
              <label>{searchMode === 'ID' ? 'Client ID' : searchMode === 'EGN' ? 'EGN (10 digits)' : 'EIK (9–13 digits)'}</label>
              <InputText value={searchQuery} onChange={e => setSearchQuery(e.target.value)}
                placeholder={searchMode === 'ID' ? 'e.g. 1' : searchMode === 'EGN' ? 'e.g. 1234567890' : 'e.g. 123456789'}
                disabled={searching} />
            </div>
            <Button type="submit" label="Load Accounts" icon="pi pi-search"
              className="bs-btn-navy" loading={searching} style={{ marginTop: 4 }} />
          </form>
        </Card>
      </div>

      {hasSearched && (
        <>
          {/* Summary strip */}
          <div className="bs-stats" style={{ marginBottom: 16 }}>
            <div className="bs-stat">
              <div className="bs-stat-icon bs-stat-icon--blue"><i className="pi pi-credit-card" /></div>
              <div><div className="bs-stat-value">{accounts.length}</div><div className="bs-stat-label">Total Accounts</div></div>
            </div>
            <div className="bs-stat">
              <div className="bs-stat-icon bs-stat-icon--green"><i className="pi pi-check-circle" /></div>
              <div><div className="bs-stat-value">{active}</div><div className="bs-stat-label">Active</div></div>
            </div>
            <div className="bs-stat">
              <div className="bs-stat-icon bs-stat-icon--amber"><i className="pi pi-times-circle" /></div>
              <div><div className="bs-stat-value">{closed}</div><div className="bs-stat-label">Closed</div></div>
            </div>
          </div>

          <Card>
            <div className="cp-table-header">
              <span className="cp-table-title">Accounts - Client {searchLabel}</span>
              <div style={{ display: 'flex', gap: 8 }}>
                <Tag value={`${active} active`} severity="success" />
                {closed > 0 && <Tag value={`${closed} closed`} severity="danger" />}
              </div>
            </div>
            {accounts.length === 0 && !searching && (
              <Message severity="info" text="No accounts found for this client." style={{ width: '100%' }} />
            )}
            <DataTable value={accounts} loading={searching} stripedRows paginator rows={10}
              emptyMessage="No accounts found."
              rowClassName={(r: BankAccount) => ({ 'ap-row-closed': r.status === 'CLOSED' })}>
              <Column field="id" header="ID" style={{ width: 70 }} sortable body={(r: BankAccount) => <span className="cp-muted">#{r.id}</span>} />
              <Column field="iban" header="IBAN" body={(r: BankAccount) => <span className="cp-mono">{r.iban}</span>} />
              <Column field="balance" header="Balance" sortable
                body={(r: BankAccount) => <span className="ap-balance">BGN {r.balance.toFixed(2)}</span>} />
              <Column header="Status" style={{ width: 110 }}
                body={(r: BankAccount) => <Tag value={r.status} severity={r.status === 'ACTIVE' ? 'success' : 'danger'} />} />
              <Column header="Actions" style={{ width: 155 }}
                body={(r: BankAccount) => (
                  <Button label="Close Account" icon="pi pi-ban" size="small" severity="danger" outlined
                    disabled={r.status === 'CLOSED' || closingId === r.id}
                    loading={closingId === r.id} onClick={() => handleClose(r)} />
                )} />
            </DataTable>
          </Card>
        </>
      )}
    </div>
  );
};  // closing </ap-page-wrap> is the outer div above

export default AccountsPage;
