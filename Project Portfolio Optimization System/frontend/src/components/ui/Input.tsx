import type { InputHTMLAttributes, ReactNode } from "react";
import { cn } from "../../lib/cn";

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        "h-11 w-full rounded-md border-[length:var(--theme-border-width)] border-hairline-strong bg-panel px-3.5 text-[15px] text-paper outline-none transition-[border-color,box-shadow] placeholder:text-mute focus:border-paper focus:shadow-[0_0_0_1px_var(--color-paper)] focus-visible:outline-none",
        className,
      )}
      {...props}
    />
  );
}

export function Field({
  label,
  htmlFor,
  children,
}: {
  label: string;
  htmlFor: string;
  children: ReactNode;
}) {
  return (
    <label className="grid gap-2" htmlFor={htmlFor}>
      <span className="text-[13px] font-medium text-dim">{label}</span>
      {children}
    </label>
  );
}
