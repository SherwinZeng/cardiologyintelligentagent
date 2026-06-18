export interface I18nSection {
  title: string;
  items: string[];
}

function isI18nSection(value: unknown): value is I18nSection {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const section = value as I18nSection;
  return typeof section.title === 'string' && Array.isArray(section.items);
}

export function readI18nSections(value: unknown): I18nSection[] {
  let list: unknown[] = [];
  if (Array.isArray(value)) {
    list = value;
  } else if (value && typeof value === 'object') {
    list = Object.values(value);
  }
  return list.filter(isI18nSection);
}
