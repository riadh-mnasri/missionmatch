const TAG_COLOR_COUNT = 6;

export function tagColorClass(label: string): string {
  let hash = 0;
  for (let i = 0; i < label.length; i++) {
    hash = (hash * 31 + label.charCodeAt(i)) % TAG_COLOR_COUNT;
  }
  return `tag tag--${hash + 1}`;
}
