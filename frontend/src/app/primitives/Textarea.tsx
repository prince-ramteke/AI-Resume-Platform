import React, { useId } from "react";

interface Props extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  help?: string;
  error?: string;
}

export function Textarea({ label, help, error, id, className, ...rest }: Props) {
  const autoId = useId();
  const fieldId = id ?? autoId;
  const describedBy = error ? `${fieldId}-err` : help ? `${fieldId}-help` : undefined;
  return (
    <div className={`app-field-wrap ${className ?? ""}`.trim()}>
      {label && <label className="app-field-label" htmlFor={fieldId}>{label}</label>}
      <textarea
        id={fieldId}
        className="app-field"
        aria-invalid={error ? "true" : undefined}
        aria-describedby={describedBy}
        {...rest}
      />
      {error ? (
        <span id={`${fieldId}-err`} className="app-field-error">{error}</span>
      ) : help ? (
        <span id={`${fieldId}-help`} className="app-field-help">{help}</span>
      ) : null}
    </div>
  );
}
