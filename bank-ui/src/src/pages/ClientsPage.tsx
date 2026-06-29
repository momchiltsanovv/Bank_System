import React, { useState, useEffect, useCallback, useRef } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { TabView, TabPanel } from 'primereact/tabview';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Dialog } from 'primereact/dialog';
import { ProgressBar } from 'primereact/progressbar';
import { Divider } from 'primereact/divider';
import {
  createIndividualClient, createCorporateClient,
  getIndividualClients, getCorporateClients,
  getAccountsByClient, closeAccount,
  getLoansByClient, getRepaymentPlan, payInstalment,
} from '../api';
import {
  IndividualClient, CorporateClient, BankAccount, Loan, RepaymentInstalment,
} from '../types';
import './ClientsPage.css';

// ── Repayment Plan Dialog ────────────────────────────────────────────────────

const RepaymentPlanDialog: React.FC<{
  loanId: number | null; onHide: () => void; toast: React.RefObject<Toast>;
}> = ({ loanId, onHide, toast }) => {
  const [plan, setPlan] = useState<RepaymentInstalment[]>([]);
  const [loading, setLoading] = useState(false);
  const [payingMonth, setPayingMonth] = useState<number | null>(null);

  useEffect(() => {
    if (loanId === null) return;
    setLoading(true);
    getRepaymentPlan(loanId)
      .then(setPlan)
      .catch((e) => toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message }))
      .finally(() => setLoading(false));
  }, [loanId, toast]);

  const handlePay = async (inst: RepaymentInstalment) => {
    if (!loanId) return;
    setPayingMonth(inst.monthNumber);
    try {
      await payInstalment(loanId, inst.monthNumber);
      setPlan(await getRepaymentPlan(loanId));
      toast.current?.show({ severity: 'success', summary: 'Paid', detail: `Month ${inst.monthNumber} marked as paid.` });
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
    } finally { setPayingMonth(null); }
  };

  const paid = plan.filter(i => i.paid).length;
  const pct  = plan.length > 0 ? Math.round((paid / plan.length) * 100) : 0;
  const fmt  = (v: number) => `BGN ${v.toFixed(2)}`;
  const nextPayableMonth = plan.find(i => !i.paid)?.monthNumber ?? null;

  return (
    <Dialog
      header={`Repayment Plan - Loan #${loanId}`}
      visible={loanId !== null} onHide={onHide}
      style={{ width: '92vw', maxWidth: 940 }} modal dismissableMask
    >
      {loading ? <ProgressBar mode="indeterminate" style={{ height: 4 }} /> : <>
        <div className="cp-plan-meta">
          <div className="cp-plan-meta-item">
            <span className="cp-plan-meta-label">Paid Instalments</span>
            <span className="cp-plan-meta-val">{paid} / {plan.length}</span>
          </div>
          <div className="cp-plan-meta-item">
            <span className="cp-plan-meta-label">Completion</span>
            <span className="cp-plan-meta-val cp-plan-meta-val--pct">{pct}%</span>
          </div>
          <div className="cp-plan-progress-wrap">
            <ProgressBar value={pct} style={{ height: 10 }} />
          </div>
        </div>
        <DataTable value={plan} size="small" scrollable scrollHeight="420px" stripedRows
          rowClassName={(r: RepaymentInstalment) => ({ 'cp-row-paid': r.paid })}>
          <Column field="monthNumber" header="Month" style={{ width: 75 }} />
          <Column field="totalPayment" header="Total Payment" body={(r) => fmt(r.totalPayment)} />
          <Column field="principalPart" header="Principal" body={(r) => fmt(r.principalPart)} />
          <Column field="interestPart" header="Interest" body={(r) => fmt(r.interestPart)} />
          <Column field="remainingBalance" header="Remaining" body={(r) => fmt(r.remainingBalance)} />
          <Column header="Status" style={{ width: 100 }}
            body={(r: RepaymentInstalment) =>
              r.paid ? <Tag severity="success" value="Paid" icon="pi pi-check" />
                     : <Tag severity="warning" value="Pending" icon="pi pi-clock" />}
          />
          <Column header="" style={{ width: 95 }}
            body={(r: RepaymentInstalment) => {
              const canPay = r.monthNumber === nextPayableMonth;
              return r.paid ? null : (
                <Button label="Pay" icon="pi pi-check-circle" size="small"
                  className="bs-btn-navy" loading={payingMonth === r.monthNumber}
                  disabled={!canPay || payingMonth !== null}
                  onClick={() => handlePay(r)} />
              );
            }}
          />
        </DataTable>
      </>}
    </Dialog>
  );
};

// ── Client Detail Expansion ───────────────────────────────────────────────────

const ClientDetailPanel: React.FC<{ clientId: number; toast: React.RefObject<Toast> }> = ({ clientId, toast }) => {
  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [loans, setLoans] = useState<Loan[]>([]);
  const [loadingAcc, setLoadingAcc] = useState(true);
  const [loadingLoans, setLoadingLoans] = useState(true);
  const [closingId, setClosingId] = useState<number | null>(null);
  const [planLoanId, setPlanLoanId] = useState<number | null>(null);

  const loadAcc = useCallback(async () => {
    setLoadingAcc(true);
    try { setAccounts(await getAccountsByClient(clientId)); }
    catch (e: any) { toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message }); }
    finally { setLoadingAcc(false); }
  }, [clientId, toast]);

  const loadLoans = useCallback(async () => {
    setLoadingLoans(true);
    try { setLoans(await getLoansByClient(clientId)); }
    catch (e: any) { toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message }); }
    finally { setLoadingLoans(false); }
  }, [clientId, toast]);

  useEffect(() => { loadAcc(); loadLoans(); }, [loadAcc, loadLoans]);

  const handleClose = async (acc: BankAccount) => {
    setClosingId(acc.id);
    try {
      await closeAccount(acc.id);
      await loadAcc();
      toast.current?.show({ severity: 'success', summary: 'Closed', detail: `Account ${acc.iban} closed.` });
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
    } finally { setClosingId(null); }
  };

  return (
    <div className="cp-detail">
      <div className="cp-detail-col">
        <div className="bs-section-title"><i className="pi pi-credit-card" style={{ fontSize: 13 }} />Bank Accounts</div>
        <DataTable value={accounts} loading={loadingAcc} size="small" emptyMessage="No accounts." stripedRows>
          <Column field="iban" header="IBAN" body={(r: BankAccount) => <span className="cp-mono">{r.iban}</span>} />
          <Column field="balance" header="Balance" body={(r: BankAccount) => <strong>BGN {r.balance.toFixed(2)}</strong>} />
          <Column header="Status" style={{ width: 100 }}
            body={(r: BankAccount) => <Tag value={r.status} severity={r.status === 'ACTIVE' ? 'success' : 'danger'} />} />
          <Column header="" style={{ width: 105 }}
            body={(r: BankAccount) => (
              <Button label="Close" icon="pi pi-times" size="small" severity="danger" outlined
                disabled={r.status === 'CLOSED' || closingId === r.id}
                loading={closingId === r.id} onClick={() => handleClose(r)} />
            )} />
        </DataTable>
      </div>

      <Divider layout="vertical" className="cp-detail-divider" />

      <div className="cp-detail-col">
        <div className="bs-section-title"><i className="pi pi-wallet" style={{ fontSize: 13 }} />Loans</div>
        <DataTable value={loans} loading={loadingLoans} size="small" emptyMessage="No loans." stripedRows>
          <Column field="id" header="ID" style={{ width: 55 }} body={(r: Loan) => <span className="cp-muted">#{r.id}</span>} />
          <Column header="Category" style={{ width: 115 }}
            body={(r: Loan) => <Tag value={r.loanCategory} severity={r.loanCategory === 'CONSUMER' ? 'info' : 'warning'} />} />
          <Column field="amount" header="Amount" body={(r: Loan) => `BGN ${r.amount.toFixed(2)}`} />
          <Column field="termMonths" header="Term" style={{ width: 75 }} body={(r: Loan) => `${r.termMonths} mo.`} />
          <Column header="Progress" style={{ width: 95 }}
            body={(r: Loan) => <span className="cp-muted">{r.paidInstalments}/{r.totalInstalments}</span>} />
          <Column header="" style={{ width: 105 }}
            body={(r: Loan) => (
              <Button label="Plan" icon="pi pi-list" size="small" text
                onClick={(e) => { e.stopPropagation(); setPlanLoanId(r.id); }} />
            )} />
        </DataTable>
      </div>

      <RepaymentPlanDialog loanId={planLoanId} onHide={() => setPlanLoanId(null)} toast={toast} />
    </div>
  );
};

// ── Individual Tab ────────────────────────────────────────────────────────────

const IndividualTab: React.FC<{ toast: React.RefObject<Toast> }> = ({ toast }) => {
  const [clients, setClients] = useState<IndividualClient[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [expandedRows, setExpandedRows] = useState<any>(null);
  const [form, setForm] = useState({ firstName: '', lastName: '', egn: '' });

  const load = useCallback(async () => {
    setLoading(true);
    try { setClients(await getIndividualClients()); }
    catch (e: any) { toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message }); }
    finally { setLoading(false); }
  }, [toast]);

  useEffect(() => { load(); }, [load]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.firstName.trim() || !form.lastName.trim() || !form.egn.trim()) {
      toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'All fields are required.' }); return;
    }
    setSubmitting(true);
    try {
      await createIndividualClient(form);
      toast.current?.show({ severity: 'success', summary: 'Registered', detail: 'Individual client created.' });
      setForm({ firstName: '', lastName: '', egn: '' });
      await load();
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
    } finally { setSubmitting(false); }
  };

  return (
    <div className="cp-tab-content">
      <Card className="cp-form-card">
        <div className="cp-form-header">
          <div className="cp-form-header-icon"><i className="pi pi-user-plus" /></div>
          <div>
            <div className="cp-form-header-title">Register Individual Client</div>
            <div className="cp-form-header-sub">Add a new personal banking client</div>
          </div>
        </div>
        <form className="cp-form" onSubmit={handleSubmit}>
          <div className="cp-form-grid">
            <div className="cp-field">
              <label>First Name</label>
              <InputText value={form.firstName} placeholder="e.g. Ivan"
                onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))} disabled={submitting} />
            </div>
            <div className="cp-field">
              <label>Last Name</label>
              <InputText value={form.lastName} placeholder="e.g. Petrov"
                onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))} disabled={submitting} />
            </div>
            <div className="cp-field">
              <label>EGN <span className="cp-field-hint">(10 digits)</span></label>
              <InputText value={form.egn} placeholder="1234567890" maxLength={10}
                onChange={e => setForm(f => ({ ...f, egn: e.target.value }))} disabled={submitting} />
            </div>
          </div>
          <div className="cp-form-footer">
            <Button type="submit" label="Register Client" icon="pi pi-check"
              className="bs-btn-navy" loading={submitting} />
          </div>
        </form>
      </Card>

      <Card>
        <div className="cp-table-header">
          <span className="cp-table-title">Individual Clients</span>
          <Tag value={`${clients.length} clients`} severity="secondary" />
        </div>
        <DataTable value={clients} loading={loading}
          expandedRows={expandedRows} onRowToggle={e => setExpandedRows(e.data)}
          rowExpansionTemplate={(r: IndividualClient) => <ClientDetailPanel clientId={r.id} toast={toast} />}
          dataKey="id" stripedRows paginator rows={10} emptyMessage="No individual clients yet.">
          <Column expander style={{ width: 44 }} />
          <Column field="id" header="ID" style={{ width: 70 }} sortable body={(r: IndividualClient) => <span className="cp-muted">#{r.id}</span>} />
          <Column field="firstName" header="First Name" sortable />
          <Column field="lastName" header="Last Name" sortable />
          <Column field="egn" header="EGN" body={(r: IndividualClient) => <span className="cp-mono">{r.egn}</span>} />
        </DataTable>
      </Card>
    </div>
  );
};

// ── Corporate Tab ─────────────────────────────────────────────────────────────

const CorporateTab: React.FC<{ toast: React.RefObject<Toast> }> = ({ toast }) => {
  const [clients, setClients] = useState<CorporateClient[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [expandedRows, setExpandedRows] = useState<any>(null);
  const [form, setForm] = useState({ companyName: '', eik: '', representativeFirstName: '', representativeLastName: '' });

  const load = useCallback(async () => {
    setLoading(true);
    try { setClients(await getCorporateClients()); }
    catch (e: any) { toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message }); }
    finally { setLoading(false); }
  }, [toast]);

  useEffect(() => { load(); }, [load]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.companyName.trim() || !form.eik.trim() || !form.representativeFirstName.trim() || !form.representativeLastName.trim()) {
      toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'All fields are required.' }); return;
    }
    setSubmitting(true);
    try {
      await createCorporateClient(form);
      toast.current?.show({ severity: 'success', summary: 'Registered', detail: 'Corporate client created.' });
      setForm({ companyName: '', eik: '', representativeFirstName: '', representativeLastName: '' });
      await load();
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
    } finally { setSubmitting(false); }
  };

  return (
    <div className="cp-tab-content">
      <Card className="cp-form-card">
        <div className="cp-form-header">
          <div className="cp-form-header-icon cp-form-header-icon--corp"><i className="pi pi-building" /></div>
          <div>
            <div className="cp-form-header-title">Register Corporate Client</div>
            <div className="cp-form-header-sub">Add a new business banking client</div>
          </div>
        </div>
        <form className="cp-form" onSubmit={handleSubmit}>
          <div className="cp-form-grid">
            <div className="cp-field">
              <label>Company Name</label>
              <InputText value={form.companyName} placeholder="e.g. Acme EOOD"
                onChange={e => setForm(f => ({ ...f, companyName: e.target.value }))} disabled={submitting} />
            </div>
            <div className="cp-field">
              <label>EIK <span className="cp-field-hint">(9–13 digits)</span></label>
              <InputText value={form.eik} placeholder="123456789"
                onChange={e => setForm(f => ({ ...f, eik: e.target.value }))} disabled={submitting} />
            </div>
            <div className="cp-field">
              <label>Representative First Name</label>
              <InputText value={form.representativeFirstName} placeholder="e.g. Georgi"
                onChange={e => setForm(f => ({ ...f, representativeFirstName: e.target.value }))} disabled={submitting} />
            </div>
            <div className="cp-field">
              <label>Representative Last Name</label>
              <InputText value={form.representativeLastName} placeholder="e.g. Dimitrov"
                onChange={e => setForm(f => ({ ...f, representativeLastName: e.target.value }))} disabled={submitting} />
            </div>
          </div>
          <div className="cp-form-footer">
            <Button type="submit" label="Register Client" icon="pi pi-check"
              className="bs-btn-navy" loading={submitting} />
          </div>
        </form>
      </Card>

      <Card>
        <div className="cp-table-header">
          <span className="cp-table-title">Corporate Clients</span>
          <Tag value={`${clients.length} clients`} severity="secondary" />
        </div>
        <DataTable value={clients} loading={loading}
          expandedRows={expandedRows} onRowToggle={e => setExpandedRows(e.data)}
          rowExpansionTemplate={(r: CorporateClient) => <ClientDetailPanel clientId={r.id} toast={toast} />}
          dataKey="id" stripedRows paginator rows={10} emptyMessage="No corporate clients yet.">
          <Column expander style={{ width: 44 }} />
          <Column field="id" header="ID" style={{ width: 70 }} sortable body={(r: CorporateClient) => <span className="cp-muted">#{r.id}</span>} />
          <Column field="companyName" header="Company" sortable body={(r: CorporateClient) => <strong>{r.companyName}</strong>} />
          <Column field="eik" header="EIK" body={(r: CorporateClient) => <span className="cp-mono">{r.eik}</span>} />
          <Column header="Representative"
            body={(r: CorporateClient) => `${r.representativeFirstName} ${r.representativeLastName}`} />
        </DataTable>
      </Card>
    </div>
  );
};

// ── Page ──────────────────────────────────────────────────────────────────────

const ClientsPage: React.FC = () => {
  const toast = useRef<Toast>(null);

  return (
    <div className="cp-page-wrap">
      <Toast ref={toast} position="bottom-right" />
      <div className="bs-page-header">
        <h1 className="bs-page-title">Clients</h1>
        <p className="bs-page-sub">Register and manage individual and corporate banking clients</p>
      </div>
      <TabView>
        <TabPanel header="Individual Clients" leftIcon="pi pi-user mr-2">
          <IndividualTab toast={toast} />
        </TabPanel>
        <TabPanel header="Corporate Clients" leftIcon="pi pi-building mr-2">
          <CorporateTab toast={toast} />
        </TabPanel>
      </TabView>
    </div>
  );
};

export default ClientsPage;
