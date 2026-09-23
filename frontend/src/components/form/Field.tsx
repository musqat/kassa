type FieldProps = {
  label: string;
  type?: "text" | "password" | "email";
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  hint?: string;
  autoComplete?: string;
};

export function Field({
  label,
  type = "text",
  value,
  onChange,
  placeholder,
  hint,
  autoComplete,
}: FieldProps) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs tracking-[0.08em]">{label}</span>
      <input
        className="border-line-strong focus:border-ink h-11 rounded-[var(--radius-field)] border bg-white px-3.5 text-sm transition-colors outline-none"
        type={type}
        value={value}
        placeholder={placeholder}
        autoComplete={autoComplete}
        onChange={(event) => onChange(event.target.value)}
      />
      {hint && <span className="text-subtle text-xs">{hint}</span>}
    </label>
  );
}

export function FormError({ message }: { message: string | null }) {
  if (!message) return null;

  return <p className="text-danger text-xs leading-relaxed">{message}</p>;
}

export function SubmitButton({ children, disabled }: { children: string; disabled?: boolean }) {
  return (
    <button
      type="submit"
      disabled={disabled}
      className="bg-ink h-11 rounded-[var(--radius-field)] text-sm text-white transition-opacity hover:opacity-90 disabled:opacity-40"
    >
      {children}
    </button>
  );
}
