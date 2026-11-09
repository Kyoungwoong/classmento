export interface TagItem {
  id: number;
  name: string;
}

interface TagInputProps {
  label: string;
  value: TagItem[];
  onChange: (tags: TagItem[]) => void;
  suggest: (query: string) => Promise<TagItem[]>;
  create: (name: string) => Promise<TagItem>;
  max?: number;
}

export function TagInput(_props: TagInputProps) {
  return null;
}
