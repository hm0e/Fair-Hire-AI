import React from "react"
import { cn } from "../../lib/utils"

export function Badge({ className, variant = "default", dot = false, dotColor, children, ...props }) {
  const variants = {
    default: "bg-indigo-50 text-indigo-700 border-indigo-200 dark:bg-indigo-500/10 dark:text-indigo-300 dark:border-indigo-500/25",
    secondary: "bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-800/80 dark:text-slate-300 dark:border-white/10",
    success: "bg-emerald-50 text-emerald-700 border-emerald-200 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/25",
    warning: "bg-amber-50 text-amber-800 border-amber-200 dark:bg-amber-500/10 dark:text-amber-300 dark:border-amber-500/25",
    danger: "bg-rose-50 text-rose-700 border-rose-200 dark:bg-rose-500/10 dark:text-rose-300 dark:border-rose-500/25",
    cyan: "bg-sky-50 text-sky-700 border-sky-200 dark:bg-cyan-500/10 dark:text-cyan-300 dark:border-cyan-500/25",
    purple: "bg-purple-50 text-purple-700 border-purple-200 dark:bg-purple-500/10 dark:text-purple-300 dark:border-purple-500/25",
    outline: "text-slate-600 border-slate-200 bg-white dark:text-slate-300 dark:border-white/15 dark:bg-white/[0.02]",
  }

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold tracking-wide transition-all",
        variants[variant],
        className
      )}
      {...props}
    >
      {dot && (
        <span 
          className={cn(
            "w-1.5 h-1.5 rounded-full animate-pulse",
            dotColor || "bg-current"
          )} 
        />
      )}
      {children}
    </span>
  )
}
