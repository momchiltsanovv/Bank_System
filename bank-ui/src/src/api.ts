import {
  IndividualClient,
  CorporateClient,
  BankAccount,
  LoanType,
  RepaymentInstalment,
  Loan,
  LoanStatus,
} from './types';

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(text || `HTTP ${res.status}`);
  }
  return res.json() as Promise<T>;
}

// ── Clients ──────────────────────────────────────────────────────────────────

export async function createIndividualClient(data: {
  firstName: string;
  lastName: string;
  egn: string;
}): Promise<IndividualClient> {
  const res = await fetch('/api/clients/individual', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<IndividualClient>(res);
}

export async function createCorporateClient(data: {
  companyName: string;
  eik: string;
  representativeFirstName: string;
  representativeLastName: string;
}): Promise<CorporateClient> {
  const res = await fetch('/api/clients/corporate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<CorporateClient>(res);
}

export async function getIndividualClients(): Promise<IndividualClient[]> {
  const res = await fetch('/api/clients/individual');
  return handleResponse<IndividualClient[]>(res);
}

export async function getCorporateClients(): Promise<CorporateClient[]> {
  const res = await fetch('/api/clients/corporate');
  return handleResponse<CorporateClient[]>(res);
}

// ── Accounts ─────────────────────────────────────────────────────────────────

export async function openAccount(data: {
  clientId: number;
  iban: string;
}): Promise<BankAccount> {
  const res = await fetch('/api/accounts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<BankAccount>(res);
}

export async function getAccountsByClient(clientId: number): Promise<BankAccount[]> {
  const res = await fetch(`/api/accounts/client/${clientId}`);
  return handleResponse<BankAccount[]>(res);
}

export async function closeAccount(id: number): Promise<BankAccount> {
  const res = await fetch(`/api/accounts/${id}/close`, { method: 'PATCH' });
  return handleResponse<BankAccount>(res);
}

// ── Loans ─────────────────────────────────────────────────────────────────────

export async function grantLoan(data: {
  clientId: number;
  loanCategory: string;
  amount: number;
  termMonths: number;
}): Promise<Loan> {
  const res = await fetch('/api/loans', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<Loan>(res);
}

export async function getLoansByClient(clientId: number): Promise<Loan[]> {
  const res = await fetch(`/api/loans/client/${clientId}`);
  return handleResponse<Loan[]>(res);
}

export async function getRepaymentPlan(loanId: number): Promise<RepaymentInstalment[]> {
  const res = await fetch(`/api/loans/${loanId}/repayment-plan`);
  return handleResponse<RepaymentInstalment[]>(res);
}

export async function payInstalment(
  loanId: number,
  monthNumber: number
): Promise<RepaymentInstalment> {
  const res = await fetch(`/api/loans/${loanId}/instalments/${monthNumber}/pay`, {
    method: 'PATCH',
  });
  return handleResponse<RepaymentInstalment>(res);
}

export async function getLoanStatus(loanId: number): Promise<LoanStatus> {
  const res = await fetch(`/api/loans/${loanId}/status`);
  return handleResponse<LoanStatus>(res);
}

export async function getLoanTypes(): Promise<LoanType[]> {
  const res = await fetch('/api/loans/types');
  return handleResponse<LoanType[]>(res);
}
