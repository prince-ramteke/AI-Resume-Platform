import type { ReactNode } from "react";
import { Link, type LinkProps } from "react-router-dom";
import {
  buttonClasses,
  type ButtonVariant,
  type ButtonSize,
} from "./Button";

interface LinkButtonProps extends LinkProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  leftIcon?: ReactNode;
}

/**
 * A router <Link> that looks like a <Button>. Use this instead of wrapping a
 * <Button> in a <Link> (invalid nested interactive content — the non-blocking
 * issue from the M6.3 review). Renders a single semantic anchor.
 */
export function LinkButton({
  variant = "primary",
  size = "md",
  leftIcon,
  className,
  children,
  ...props
}: LinkButtonProps) {
  return (
    <Link className={buttonClasses(variant, size, className)} {...props}>
      {leftIcon}
      {children}
    </Link>
  );
}
