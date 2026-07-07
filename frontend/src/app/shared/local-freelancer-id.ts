const STORAGE_KEY = 'missionmatch.freelancerId';

// There is no auth system yet, so the browser assigns itself a stable identity
// on first visit and reuses it across the Profile and Matches pages.
export function localFreelancerId(): string {
  const existing = localStorage.getItem(STORAGE_KEY);
  if (existing) return existing;

  const generated = crypto.randomUUID();
  localStorage.setItem(STORAGE_KEY, generated);
  return generated;
}
