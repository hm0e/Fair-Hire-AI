import React from "react"
import { cn } from "../../lib/utils"

export function Card({ className, interactive = false, ...props }) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-slate-200 dark:border-white/[0.08] bg-white dark:bg-[#0c121e] text-slate-800 dark:text-slate-100 shadow-sm dark:shadow-xl transition-all duration-200 relative overflow-hidden",
        interactive && "hover:border-indigo-500/40 hover:shadow-md dark:hover:shadow-2xl hover:-translate-y-0.5",
        className
      )}
      {...props}
    />
  )
}

export function CardHeader({ className, ...props }) {
  return (
    <div
      className={cn("flex flex-col space-y-1.5 p-6 border-b border-slate-100 dark:border-white/[0.06]", className)}
      {...props}
    />
  )
}

export function CardTitle({ className, ...props }) {
  return (
    <h3
      className={cn("font-heading text-lg font-bold text-slate-900 dark:text-white tracking-tight leading-snug", className)}
      {...props}
    />
  )
}

export function CardDescription({ className, ...props }) {
  return (
    <p
      className={cn("text-xs sm:text-sm text-slate-500 dark:text-slate-400 leading-relaxed", className)}
      {...props}
    />
  )
}

export function CardContent({ className, ...props }) {
  return <div className={cn("p-6", className)} {...props} />
}

export function CardFooter({ className, ...props }) {
  return (
    <div
      className={cn("flex items-center p-6 pt-0 border-t border-slate-100 dark:border-white/[0.06]", className)}
      {...props}
    />
  )
}
