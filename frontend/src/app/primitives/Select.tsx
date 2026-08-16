import React, { useId } from "react";

interface Option { label: string; value: string; }
interface Props extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, "children"> {
  label?: string;
  help?: string;
  error?: string;
  options: Option[];
}

export function Select({ label, help, error, options, id, className, ...rest }: Props) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  const describedBy = error ? `${fieldId}-err` : help ? `${fieldId}-help` : undefined;
  return (
    <div className={`app-field-wrap ${className ?? ""}`.trim()}>
      {label && <label className="app-field-label" htmlFor={fieldId}>{label}</label>}
      <div className="app-select-wrap">
        <select
          id={fieldId}
          className="app-field"
          aria-invalid={error ? "true" : undefined}
          aria-describedby={describedBy}
          {...rest}
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
        <svg className="app-select-chevron" width="12" height="12" viewBox="0 0 24 24"
          fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="m6 9 6 6 6-6" />
        </svg>
      </div>
      {error ? (
        <span id={`${fieldId}-err`} className="app-field-error">{error}</span>
      ) : help ? (
        <span id={`${fieldId}-help`} className="app-field-help">{help}</span>
      ) : null}
    </div>
  );
}
