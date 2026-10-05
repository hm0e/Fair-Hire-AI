import React from "react"
import { cn } from "../../lib/utils"

export function FairnessGauge({ score = 0, size = 150, strokeWidth = 11, className }) {
  const radius = 48
  const circumference = 2 * Math.PI * radius // ~301.59
  const clampedScore = Math.min(100, Math.max(0, score))
  const offset = circumference - (circumference * clampedScore) / 100

  const isOptimal = clampedScore >= 80
  const isModerate = clampedScore >= 65 && clampedScore < 80

  const getTier = () => {
    if (clampedScore >= 90) return { grade: "Grade A+ (EEOC Compliant)", color: "text-emerald-700 dark:text-emerald-400", bg: "bg-emerald-50 dark:bg-emerald-500/10 border-emerald-200 dark:border-emerald-500/30" }
    if (clampedScore >= 80) return { grade: "Grade A (High Fairness)", color: "text-teal-700 dark:text-teal-400", bg: "bg-teal-50 dark:bg-teal-500/10 border-teal-200 dark:border-teal-500/30" }
    if (clampedScore >= 65) return { grade: "Grade B (Moderate Bias)", color: "text-amber-800 dark:text-amber-400", bg: "bg-amber-50 dark:bg-amber-500/10 border-amber-200 dark:border-amber-500/30" }
    return { grade: "High Bias Alert", color: "text-rose-700 dark:text-rose-400", bg: "bg-rose-50 dark:bg-rose-500/10 border-rose-200 dark:border-rose-500/30" }
  }

  const tier = getTier()

  return (
    <div className={cn("relative flex flex-col items-center justify-center", className)}>
      <div className="relative flex items-center justify-center" style={{ width: size, height: size }}>
        
        <svg className="w-full h-full -rotate-90 relative z-10" viewBox="0 0 120 120">
          <defs>
            <linearGradient id="fairnessGaugeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
              {isOptimal ? (
                <>
                  <stop offset="0%" stopColor="#0ea5e9" />
                  <stop offset="100%" stopColor="#10b981" />
                </>
              ) : isModerate ? (
                <>
                  <stop offset="0%" stopColor="#6366f1" />
                  <stop offset="100%" stopColor="#f59e0b" />
                </>
              ) : (
                <>
                  <stop offset="0%" stopColor="#f43f5e" />
                  <stop offset="100%" stopColor="#f59e0b" />
                </>
              )}
            </linearGradient>
          </defs>

          {/* Background Outer Ring */}
          <circle
            cx="60"
            cy="60"
            r={radius}
            fill="none"
            stroke="currentColor"
            className="text-slate-200 dark:text-white/[0.08]"
            strokeWidth={strokeWidth}
          />

          {/* Active Gradient Arc */}
          <circle
            cx="60"
            cy="60"
            r={radius}
            fill="none"
            stroke="url(#fairnessGaugeGrad)"
            strokeWidth={strokeWidth}
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            className="transition-all duration-1000 ease-out"
          />
        </svg>

        {/* Center Readout */}
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center z-20">
          <span className="font-heading text-3xl sm:text-4xl font-black text-slate-900 dark:text-white tracking-tight leading-none">
            {clampedScore}
          </span>
          <span className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase tracking-widest mt-0.5 font-mono">
            / 100 Index
          </span>
        </div>
      </div>

      {/* Tier Label Badge */}
      <div className={cn("mt-2 px-3 py-0.5 rounded-full border text-[11px] font-bold tracking-wide font-mono", tier.bg, tier.color)}>
        {tier.grade}
      </div>
    </div>
  )
}
