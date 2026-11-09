export interface SegmentOption<T extends string> {
  value: T;
  label: string;
}

interface SegmentedControlProps<T extends string> {
  label: string;
  options: readonly SegmentOption<T>[];
  value: T | null;
  onChange: (value: T) => void;
}

export function SegmentedControl<T extends string>(_props: SegmentedControlProps<T>) {
  return null;
}
