import React, { useState, useEffect, useCallback, useRef } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputNumber } from 'primereact/inputnumber';
import { Dropdown } from 'primereact/dropdown';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { ProgressBar } from 'primereact/progressbar';
import { Chip } from 'primereact/chip';
import { getLoanTypes, grantLoan, getLoansByClient, getRepaymentPlan, payInstalment } from '../api';
import { LoanType, Loan, RepaymentInstalment } from '../types';
import './LoansPage.css';

const CATEGORY_OPTIONS = [
  { label: 'Consumer Loan', value: 'CONSUMER' },
  { label: 'Mortgage Loan', value: 'MORTGAGE' },
];

// ── Inline repayment plan ─────────────────────────────────────────────────────

const RepaymentPlanInline: React.FC<{ loanId: number; toast: React.RefObject<Toast> }> = ({ loanId, toast }) => {
  const [plan, setPlan] = useState<RepaymentInstalment[]>([]);
  const [loading, setLoading] = useState(true);
  const [payingMonth, setPayingMonth] = useState<number | null>(null);

  const loadPlan = useCallback(async () => {
    setLoading(true);
    try { setPlan(await getRepaymentPlan(loanId)); }
    catch (e: any) { toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message }); }
    finally { setLoading(false); }
  }, [loanId, toast]);

  useEffect(() => { loadPlan(); }, [loadPlan]);

  const handlePay = async (inst: RepaymentInstalment) => {
    setPayingMonth(inst.monthNumber);
    try {
      await payInstalment(loanId, inst.monthNumber);
      await loadPlan();
      toast.current?.show({ severity: 'success', summary: 'Paid', detail: `Month ${inst.monthNumber} marked as paid.` });
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
    } finally { setPayingMonth(null); }
  };

  const paid = plan.filter(i => i.paid).length;
  const pct  = plan.length > 0 ? Math.round((paid / plan.length) * 100) : 0;
  const fmt  = (v: number) => `BGN ${v.toFixed(2)}`;

  if (loading) return <ProgressBar mode="indeterminate" style={{ height: 3 }} />;

  return (
    <div className="lp-plan-inline">
      <div className="lp-plan-bar">
        <span className="lp-plan-bar-label">{paid}/{plan.length} paid</span>
        <ProgressBar value={pct} style={{ flex: 1, height: 8 }} />
        <span className="lp-plan-bar-pct">{pct}%</span>
      </div>
      <DataTable value={plan} size="small" scrollable scrollHeight="360px" stripedRows
        rowClassName={(r: RepaymentInstalment) => ({ 'lp-row-paid': r.paid })}>
        <Column field="monthNumber" header="Month" style={{ width: 70 }} />
        <Column field="totalPayment" header="Total" body={(r) => fmt(r.totalPayment)} />
        <Column field="principalPart" header="Principal" body={(r) => fmt(r.principalPart)} />
        <Column field="interestPart" header="Interest" body={(r) => fmt(r.interestPart)} />
        <Column field="remainingBalance" header="Remaining" body={(r) => fmt(r.remainingBalance)} />
        <Column header="Status" style={{ width: 100 }}
          body={(r: RepaymentInstalment) =>
            r.paid ? <Tag severity="success" value="Paid" icon="pi pi-check" />
                   : <Tag severity="warning" value="Pending" icon="pi pi-clock" />}
        />
        <Column header="" style={{ width: 90 }}
          body={(r: RepaymentInstalment) =>
            r.paid ? null : (
              <Button label="Pay" icon="pi pi-check-circle" size="small" className="bs-btn-navy"
                loading={payingMonth === r.monthNumber} onClick={() => handlePay(r)} />
            )}
        />
      </DataTable>
    </div>
  );
};

// ── Loan Type Cards ───────────────────────────────────────────────────────────

const LoanTypeCards: React.FC = () => {
  const [types, setTypes] = useState<LoanType[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getLoanTypes().then(setTypes).finally(() => setLoading(false));
  }, []);

  if (loading) return <ProgressBar mode="indeterminate" style={{ height: 3, marginBottom: 16 }} />;

  return (
    <div className="lp-type-grid">
      {types.map(lt => (
        <div key={lt.id} className={`lp-type-card lp-type-card--${lt.category.toLowerCase()}`}>
          <div className="lp-type-top">
            <div className="lp-type-icon">
              <i className={lt.category === 'CONSUMER' ? 'pi pi-credit-card' : 'pi pi-home'} />
            </div>
            <span className="lp-type-badge">{lt.category}</span>
          </div>
          <div className="lp-type-name">{lt.category === 'CONSUMER' ? 'Consumer Loan' : 'Mortgage Loan'}</div>
          <div className="lp-type-rate">{(lt.annualInterestRate * 100).toFixed(2)}%<span> p.a.</span></div>
          <div className="lp-type-stats">
            <div className="lp-type-stat">
              <span className="lp-type-stat-label">Max Amount</span>
              <span className="lp-type-stat-val">BGN {lt.maxAmount.toLocaleString()}</span>
            </div>
            <div className="lp-type-stat">
              <span className="lp-type-stat-label">Max Term</span>
              <span className="lp-type-stat-val">{lt.maxTermMonths} mo.</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

// ── Main Page ─────────────────────────────────────────────────────────────────

const LoansPage: React.FC = () => {
  const toast = useRef<Toast>(null);

  const [grantClientId, setGrantClientId] = useState<number | null>(null);
  const [category, setCategory]           = useState<'CONSUMER' | 'MORTGAGE'>('CONSUMER');
  const [amount, setAmount]               = useState<number | null>(null);
  const [termMonths, setTermMonths]       = useState<number | null>(null);
  const [granting, setGranting]           = useState(false);

  const [searchId, setSearchId]           = useState<number | null>(null);
  const [loans, setLoans]                 = useState<Loan[]>([]);
  const [searching, setSearching]         = useState(false);
  const [hasSearched, setHasSearched]     = useState(false);
  const [expandedRows, setExpandedRows]   = useState<any>(null);

  const loadLoans = useCallback(async (id: number) => {
    setSearching(true);
    try { setLoans(await getLoansByClient(id)); }
    catch (e: any) { toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message }); setLoans([]); }
    finally { setSearching(false); }
  }, []);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchId || searchId <= 0) {
      toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'Enter a valid Client ID.' }); return;
    }
    setHasSearched(true); setExpandedRows(null);
    await loadLoans(searchId);
  };

  const handleGrant = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!grantClientId || !amount || !termMonths) {
      toast.current?.show({ severity: 'warn', summary: 'Validation', detail: 'All fields are required.' }); return;
    }
    setGranting(true);
    try {
      const loan = await grantLoan({ clientId: grantClientId, loanCategory: category, amount, termMonths });
      toast.current?.show({
        severity: 'success', summary: 'Loan Granted',
        detail: `Loan #${loan.id} — BGN ${loan.amount.toFixed(2)} for ${loan.termMonths} months.`,
      });
      setGrantClientId(null); setAmount(null); setTermMonths(null);
      if (searchId === grantClientId) await loadLoans(grantClientId);
    } catch (e: any) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: e.message });
    } finally { setGranting(false); }
  };

  const totalPaid   = loans.reduce((s, l) => s + l.paidInstalments, 0);
  const totalInst   = loans.reduce((s, l) => s + l.totalInstalments, 0);
  const totalAmount = loans.reduce((s, l) => s + l.amount, 0);

  const progressBody = (row: Loan) => {
    const pct = row.totalInstalments > 0
      ? Math.round((row.paidInstalments / row.totalInstalments) * 100) : 0;
    return (
      <div className="lp-progress-cell">
        <div className="lp-progress-nums">{row.paidInstalments}/{row.totalInstalments}</div>
        <div className="lp-mini-bar"><div className="lp-mini-fill" style={{ width: `${pct}%` }} /></div>
      </div>
    );
  };

  return (
    <div className="lp-page-wrap">
      <Toast ref={toast} position="bottom-right" />
      <div className="bs-page-header">
        <h1 className="bs-page-title">Loans</h1>
        <p className="bs-page-sub">Grant loans, view schedules and track repayment progress</p>
      </div>

      {/* Loan products */}
      <section style={{ marginBottom: 24 }}>
        <div className="bs-section-title">Available Loan Products</div>
        <LoanTypeCards />
      </section>

      <div className="lp-top-grid">
        {/* Grant form */}
        <Card>
          <div className="ap-form-header">
            <div className="ap-form-icon" style={{ background: '#fdf4ff', color: '#9333ea' }}>
              <i className="pi pi-send" />
            </div>
            <div>
              <div className="ap-form-title">Grant New Loan</div>
              <div className="ap-form-sub">Issue a loan and generate the repayment plan</div>
            </div>
          </div>
          <form onSubmit={handleGrant}>
            <div className="lp-form-grid">
              <div className="lp-field">
                <label>Client ID</label>
                <InputNumber value={grantClientId} onValueChange={e => setGrantClientId(e.value ?? null)}
                  placeholder="e.g. 1" min={1} disabled={granting} useGrouping={false} />
              </div>
              <div className="lp-field">
                <label>Category</label>
                <Dropdown value={category} options={CATEGORY_OPTIONS}
                  onChange={e => setCategory(e.value)} disabled={granting} />
              </div>
              <div className="lp-field">
                <label>Amount (BGN)</label>
                <InputNumber value={amount} onValueChange={e => setAmount(e.value ?? null)}
                  placeholder="e.g. 10 000" min={1} minFractionDigits={2} maxFractionDigits={2}
                  disabled={granting} useGrouping={false} />
              </div>
              <div className="lp-field">
                <label>Term (months)</label>
                <InputNumber value={termMonths} onValueChange={e => setTermMonths(e.value ?? null)}
                  placeholder="e.g. 36" min={1} disabled={granting} useGrouping={false} />
              </div>
            </div>
            <div className="lp-form-footer">
              <Button type="submit" label="Grant Loan" icon="pi pi-send"
                className="bs-btn-navy" loading={granting} />
            </div>
          </form>
        </Card>

        {/* Search form */}
        <Card>
          <div className="ap-form-header">
            <div className="ap-form-icon ap-form-icon--search"><i className="pi pi-search" /></div>
            <div>
              <div className="ap-form-title">Find Client Loans</div>
              <div className="ap-form-sub">Load all loans for a specific client</div>
            </div>
          </div>
          <form onSubmit={handleSearch}>
            <div className="lp-field" style={{ marginBottom: 12 }}>
              <label>Client ID</label>
              <InputNumber value={searchId} onValueChange={e => setSearchId(e.value ?? null)}
                placeholder="Enter Client ID" min={1} disabled={searching} useGrouping={false} />
            </div>
            <Button type="submit" label="Load Loans" icon="pi pi-search"
              className="bs-btn-navy" loading={searching} />
          </form>
        </Card>
      </div>

      {hasSearched && (
        <>
          {/* Stats */}
          <div className="bs-stats" style={{ marginBottom: 16 }}>
            <div className="bs-stat">
              <div className="bs-stat-icon bs-stat-icon--purple"><i className="pi pi-wallet" /></div>
              <div><div className="bs-stat-value">{loans.length}</div><div className="bs-stat-label">Total Loans</div></div>
            </div>
            <div className="bs-stat">
              <div className="bs-stat-icon bs-stat-icon--blue"><i className="pi pi-dollar" /></div>
              <div>
                <div className="bs-stat-value" style={{ fontSize: '1.1rem' }}>BGN {totalAmount.toLocaleString('en', { minimumFractionDigits: 0, maximumFractionDigits: 0 })}</div>
                <div className="bs-stat-label">Total Amount</div>
              </div>
            </div>
            <div className="bs-stat">
              <div className="bs-stat-icon bs-stat-icon--green"><i className="pi pi-check-square" /></div>
              <div><div className="bs-stat-value">{totalPaid}<span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>/{totalInst}</span></div><div className="bs-stat-label">Instalments Paid</div></div>
            </div>
          </div>

          <Card>
            <div className="cp-table-header">
              <span className="cp-table-title">Loans — Client #{searchId}</span>
              <div style={{ display: 'flex', gap: 6 }}>
                {loans.filter(l => l.loanCategory === 'CONSUMER').length > 0 &&
                  <Chip label={`${loans.filter(l => l.loanCategory === 'CONSUMER').length} Consumer`} />}
                {loans.filter(l => l.loanCategory === 'MORTGAGE').length > 0 &&
                  <Chip label={`${loans.filter(l => l.loanCategory === 'MORTGAGE').length} Mortgage`} />}
              </div>
            </div>
            <DataTable value={loans} loading={searching}
              expandedRows={expandedRows} onRowToggle={e => setExpandedRows(e.data)}
              rowExpansionTemplate={(r: Loan) => <RepaymentPlanInline loanId={r.id} toast={toast} />}
              dataKey="id" stripedRows paginator rows={10} emptyMessage="No loans found.">
              <Column expander style={{ width: 44 }} />
              <Column field="id" header="ID" style={{ width: 70 }} sortable body={(r: Loan) => <span className="cp-muted">#{r.id}</span>} />
              <Column header="Category" style={{ width: 130 }}
                body={(r: Loan) => <Tag value={r.loanCategory} severity={r.loanCategory === 'CONSUMER' ? 'info' : 'warning'} />} />
              <Column field="amount" header="Amount" sortable
                body={(r: Loan) => <span className="ap-balance">BGN {r.amount.toFixed(2)}</span>} />
              <Column field="termMonths" header="Term" style={{ width: 90 }} sortable
                body={(r: Loan) => `${r.termMonths} mo.`} />
              <Column header="Repayment Progress" body={progressBody} style={{ minWidth: 170 }} />
            </DataTable>
          </Card>
        </>
      )}
    </div>
  );
};

export default LoansPage;
