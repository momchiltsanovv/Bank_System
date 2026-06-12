export interface IndividualClient {
  id: number;
  firstName: string;
  lastName: string;
  egn: string;
}

export interface CorporateClient {
  id: number;
  companyName: string;
  eik: string;
  representativeFirstName: string;
  representativeLastName: string;
}

export interface BankAccount {
  id: number;
  iban: string;
  balance: number;
  status: 'ACTIVE' | 'CLOSED';
  clientId: number;
}

export interface LoanType {
  id: number;
  category: 'CONSUMER' | 'MORTGAGE';
  annualInterestRate: number;
  maxAmount: number;
  maxTermMonths: number;
}

export interface RepaymentInstalment {
  id: number;
  monthNumber: number;
  totalPayment: number;
  principalPart: number;
  interestPart: number;
  remainingBalance: number;
  paid: boolean;
}

export interface Loan {
  id: number;
  clientId: number;
  loanCategory: 'CONSUMER' | 'MORTGAGE';
  amount: number;
  termMonths: number;
  paidInstalments: number;
  totalInstalments: number;
  repaymentPlan: RepaymentInstalment[];
}

export interface LoanStatus {
  loanId: number;
  paidInstalments: number;
  totalInstalments: number;
  fullyPaid: boolean;
}
