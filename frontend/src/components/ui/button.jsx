import React from "react"
import { cn } from "../../lib/utils"

export const Button = React.forwardRef(({
  className,
  variant = "default",
  size = "default",
  children,
  ...props
}, ref) => {
  const baseStyles = "inline-flex items-center justify-center gap-2 font-medium transition-all duration-150 cursor-pointer disabled:opacity-50 disabled:pointer-events-none text-sm outline-none focus-visible:ring-2 focus-visible:ring-indigo-500 active:scale-[0.98] select-none"
  
  const variants = {
    default: "bg-indigo-600 hover:bg-indigo-500 text-white shadow-sm hover:shadow border border-indigo-400/20",
    secondary: "bg-white hover:bg-slate-50 text-slate-800 border border-slate-200 shadow-sm dark:bg-slate-800/90 dark:hover:bg-slate-700 dark:text-slate-200 dark:border-white/10",
    outline: "border border-slate-300 hover:border-slate-400 bg-white hover:bg-slate-50 text-slate-700 hover:text-slate-900 dark:border-slate-700 dark:hover:border-slate-500 dark:bg-white/[0.02] dark:hover:bg-white/[0.06] dark:text-slate-300 dark:hover:text-white",
    ghost: "bg-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-100 dark:text-slate-400 dark:hover:text-white dark:hover:bg-white/[0.05]",
    cyan: "bg-sky-50 text-sky-700 border border-sky-200 hover:bg-sky-100 dark:bg-cyan-500/10 dark:text-cyan-300 dark:border-cyan-500/30 dark:hover:bg-cyan-500/20",
    emerald: "bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-300 dark:border-emerald-500/30 dark:hover:bg-emerald-500/20",
    danger: "bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100 dark:bg-rose-500/10 dark:text-rose-300 dark:border-rose-500/30 dark:hover:bg-rose-500/20",
    glow: "bg-indigo-600 hover:bg-indigo-500 text-white shadow-md shadow-indigo-600/25 border border-indigo-400/30",
  }

  const sizes = {
    default: "h-10 px-4 py-2 rounded-xl text-sm",
    sm: "h-8 px-3 text-xs rounded-lg",
    lg: "h-12 px-6 text-base rounded-xl font-semibold",
    icon: "h-9 w-9 p-0 rounded-xl",
  }

  return (
    <button
      ref={ref}
      className={cn(baseStyles, variants[variant], sizes[size], className)}
      {...props}
    >
      {children}
    </button>
  )
})

Button.displayName = "Button"
