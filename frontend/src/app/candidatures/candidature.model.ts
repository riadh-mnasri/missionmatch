export type CandidatureStatus = 'TO_APPLY' | 'APPLIED' | 'INTERVIEW' | 'REJECTED' | 'ACCEPTED';

export interface Candidature {
  id: string;
  missionId: string;
  freelancerId: string;
  status: CandidatureStatus;
}

// Mirrors the transition rules enforced by the Candidature aggregate. Duplicated here only to
// decide which buttons to show; the backend remains the source of truth and re-validates on PATCH.
export const ALLOWED_TRANSITIONS: Record<CandidatureStatus, CandidatureStatus[]> = {
  TO_APPLY: ['APPLIED'],
  APPLIED: ['INTERVIEW', 'REJECTED'],
  INTERVIEW: ['ACCEPTED', 'REJECTED'],
  REJECTED: [],
  ACCEPTED: [],
};

export const CANDIDATURE_COLUMNS: { status: CandidatureStatus; label: string }[] = [
  { status: 'TO_APPLY', label: 'To apply' },
  { status: 'APPLIED', label: 'Applied' },
  { status: 'INTERVIEW', label: 'Interview' },
  { status: 'ACCEPTED', label: 'Accepted' },
  { status: 'REJECTED', label: 'Rejected' },
];
