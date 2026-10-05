import React from "react"
import {
  ShieldCheck,
  History,
  ArrowRight,
  Sun,
  Moon,
  Briefcase,
  UserCheck,
  Award,
  Layers,
  Sparkles
} from "lucide-react"
import { Button } from "./ui/button"

export function Navbar({ activeView, setActiveView, onNewScreening, theme, toggleTheme }) {
  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200 dark:border-white/[0.08] bg-white/90 dark:bg-[#080c14]/90 backdrop-blur-md transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Brand Logo */}
        <div 
          onClick={() => setActiveView("landing")}
          className="flex items-center gap-3 cursor-pointer group select-none"
        >
          <div className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center shadow-sm">
            <ShieldCheck className="w-4 h-4 text-white" />
          </div>

          <div className="flex items-center gap-2">
            <span className="font-heading text-lg font-bold tracking-tight text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-300 transition-colors">
              FairHire AI
            </span>
            <span className="text-[10px] font-mono font-semibold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 px-2 py-0.5 rounded-md">
              v2.5 Architecture
            </span>
          </div>
        </div>

        {/* Navigation Links based on Architecture */}
        <nav className="hidden lg:flex items-center gap-5 text-xs font-semibold">
          <button
            onClick={() => setActiveView("recruiter")}
            className={`transition-colors cursor-pointer py-1 flex items-center gap-1.5 ${
              activeView === "recruiter" 
                ? "text-indigo-600 dark:text-indigo-400 font-bold border-b-2 border-indigo-600 dark:border-indigo-500" 
                : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
            }`}
          >
            <Briefcase className="w-3.5 h-3.5" />
            Recruiter Studio
          </button>

          <button
            onClick={() => setActiveView("candidate")}
            className={`transition-colors cursor-pointer py-1 flex items-center gap-1.5 ${
              activeView === "candidate" 
                ? "text-indigo-600 dark:text-indigo-400 font-bold border-b-2 border-indigo-600 dark:border-indigo-500" 
                : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
            }`}
          >
            <UserCheck className="w-3.5 h-3.5" />
            Candidate Portal
          </button>

          <button
            onClick={() => setActiveView("admin")}
            className={`transition-colors cursor-pointer py-1 flex items-center gap-1.5 ${
              activeView === "admin" 
                ? "text-indigo-600 dark:text-indigo-400 font-bold border-b-2 border-indigo-600 dark:border-indigo-500" 
                : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
            }`}
          >
            <Award className="w-3.5 h-3.5" />
            Research & Admin
          </button>

          <button
            onClick={() => setActiveView("architecture")}
            className={`transition-colors cursor-pointer py-1 flex items-center gap-1.5 ${
              activeView === "architecture" 
                ? "text-indigo-600 dark:text-indigo-400 font-bold border-b-2 border-indigo-600 dark:border-indigo-500" 
                : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
            }`}
          >
            <Layers className="w-3.5 h-3.5" />
            System Design
          </button>

          <button
            onClick={() => setActiveView("history")}
            className={`transition-colors cursor-pointer py-1 flex items-center gap-1.5 ${
              activeView === "history" 
                ? "text-indigo-600 dark:text-indigo-400 font-bold border-b-2 border-indigo-600 dark:border-indigo-500" 
                : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
            }`}
          >
            <History className="w-3.5 h-3.5" />
            Audit Ledger
          </button>
        </nav>

        {/* Status, Theme Switcher & Action CTA */}
        <div className="flex items-center gap-3">
          
          {/* Light / Dark Mode Toggle */}
          <button
            type="button"
            onClick={toggleTheme}
            className="p-2 rounded-xl border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 hover:text-indigo-600 dark:hover:text-white shadow-sm transition-all cursor-pointer"
            title={`Switch to ${theme === "dark" ? "Light" : "Dark"} Mode`}
            aria-label="Toggle theme mode"
          >
            {theme === "dark" ? (
              <Sun className="w-4 h-4 text-amber-400" />
            ) : (
              <Moon className="w-4 h-4 text-indigo-600" />
            )}
          </button>

          <div className="hidden sm:flex items-center gap-2 px-2.5 py-1 rounded-lg border border-slate-200 dark:border-white/[0.08] bg-slate-50 dark:bg-white/[0.02] text-[11px] font-mono text-slate-500 dark:text-slate-400">
            <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
            <span>REST API Active</span>
          </div>

          <Button
            variant="default"
            size="sm"
            onClick={() => setActiveView("recruiter")}
            className="flex items-center gap-1.5 text-xs font-semibold h-8 px-3 rounded-lg"
          >
            <span>Launch Recruiter</span>
            <ArrowRight className="w-3 h-3" />
          </Button>
        </div>

      </div>
    </header>
  )
}
