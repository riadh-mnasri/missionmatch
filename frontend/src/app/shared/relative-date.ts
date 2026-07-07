export function relativeDate(isoDate: string): string {
  const target = new Date(isoDate + 'T00:00:00');
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const diffDays = Math.round((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return 'Starts today';
  if (diffDays === 1) return 'Starts tomorrow';
  if (diffDays > 1 && diffDays < 14) return `Starts in ${diffDays} days`;
  if (diffDays >= 14 && diffDays < 60) return `Starts in ${Math.round(diffDays / 7)} weeks`;
  if (diffDays >= 60) return `Starts in ${Math.round(diffDays / 30)} months`;
  if (diffDays === -1) return 'Started yesterday';
  return `Started ${Math.abs(diffDays)} days ago`;
}
