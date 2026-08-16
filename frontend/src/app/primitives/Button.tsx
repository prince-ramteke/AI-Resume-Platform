import React from "react";
import { Link } from "react-router-dom";

type Variant = "primary" | "secondary" | "ghost" | "danger";
type Size = "sm" | "md" | "lg";

interface Common {
  variant?: Variant;
  size?: Size;
  full?: boolean;
  disabled?: boolean;
  children?: React.ReactNode;
  className?: string;
}

type AsButton = Common & React.ButtonHTMLAttributes<HTMLButtonElement> & {
  to?: undefined; href?: undefined;
};
type AsLink = Common & { to: string; href?: undefined } & Omit<React.AnchorHTMLAttributes<HTMLAnchorElement>, "href">;
type AsAnchor = Common & { href: string; to?: undefined } & React.AnchorHTMLAttributes<HTMLAnchorElement>;

export type ButtonProps = AsButton | AsLink | AsAnchor;

export function Button(props: ButtonProps) {
  const { variant = "primary", size = "md", full = false, disabled = false, children, className, ...rest } = props as Common & { to?: string; href?: string } & Record<string, unknown>;
  const cls = ["app-btn", className].filter(Boolean).join(" ");
  const dataAttrs = {
    "data-variant": variant,
    "data-size": size,
    "data-full": full ? "true" : undefined,
  };
  if ("to" in props && props.to) {
    return (
      <Link to={props.to} className={cls} aria-disabled={disabled || undefined} {...dataAttrs} {...(rest as Record<string, unknown>)}>
        {children}
      </Link>
    );
  }
  if ("href" in props && props.href) {
    return (
      <a href={props.href} className={cls} aria-disabled={disabled || undefined} {...dataAttrs} {...(rest as Record<string, unknown>)}>
        {children}
      </a>
    );
  }
  return (
    <button
      type={(rest as React.ButtonHTMLAttributes<HTMLButtonElement>).type ?? "button"}
      className={cls}
      disabled={disabled}
      {...dataAttrs}
      {...(rest as React.ButtonHTMLAttributes<HTMLButtonElement>)}
    >
      {children}
    </button>
  );
}
