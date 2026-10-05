import React from "react"
import { Layers, ShieldCheck, BookOpen, ExternalLink, Activity } from "lucide-react"

export function Footer({ setActiveView }) {
  return (
    <footer className="w-full border-t border-slate-200 dark:border-white/[0.08] bg-white dark:bg-[#060910] text-slate-600 dark:text-slate-400 py-12 px-4 sm:px-6 lg:px-8 mt-auto relative z-10 transition-colors">
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-10 pb-10 border-b border-slate-100 dark:border-white/[0.06]">
        
        {/* Brand Column */}
        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white shadow-sm">
              <Layers className="w-4 h-4" />
            </div>
            <span className="font-heading text-lg font-bold text-slate-900 dark:text-white tracking-tight">FairHire AI</span>
            <span className="text-[10px] font-mono font-semibold bg-indigo-50 dark:bg-indigo-500/15 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-500/30 px-2 py-0.5 rounded-full">
              v2.0
            </span>
          </div>
          <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed max-w-sm">
            An empirical recruitment intelligence framework evaluating the trade-off between screening accuracy and algorithmic hiring fairness across Lexical, Neural, and Blind paradigms.
          </p>
          <div className="inline-flex items-center gap-2 text-xs text-indigo-700 dark:text-indigo-300 bg-indigo-50 dark:bg-indigo-500/10 border border-indigo-200 dark:border-indigo-500/20 px-3 py-1 rounded-full font-mono">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
            <span>Research Project &bull; SP College, Pune</span>
          </div>
        </div>

        {/* Quick Links */}
        <div>
          <h4 className="font-heading text-xs font-bold text-slate-900 dark:text-slate-200 uppercase tracking-widest mb-4">Platform Navigation</h4>
          <ul className="space-y-2.5 text-xs">
            <li>
              <button onClick={() => setActiveView("landing")} className="text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-white transition-colors cursor-pointer flex items-center gap-1.5">
                <span>Platform Overview &amp; Live Scanner</span>
              </button>
            </li>
            <li>
              <button onClick={() => setActiveView("studio")} className="text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-white transition-colors cursor-pointer flex items-center gap-1.5">
                <span>Screening Studio Workbench</span>
              </button>
            </li>
            <li>
              <button onClick={() => setActiveView("history")} className="text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-white transition-colors cursor-pointer flex items-center gap-1.5">
                <span>Audit History Ledger &amp; CSV Export</span>
              </button>
            </li>
          </ul>
        </div>

        {/* Academic References */}
        <div>
          <h4 className="font-heading text-xs font-bold text-slate-900 dark:text-slate-200 uppercase tracking-widest mb-4">Academic Grounding</h4>
          <ul className="space-y-2.5 text-xs text-slate-600 dark:text-slate-400">
            <li className="flex items-start gap-2">
              <span className="text-indigo-600 dark:text-indigo-400 font-bold">&bull;</span>
              <span>Gaucher, Friesen &amp; Kay (2011) &mdash; Evidence on Gendered Advertisements</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-indigo-600 dark:text-indigo-400 font-bold">&bull;</span>
              <span>Åslund &amp; Skans (2012) &mdash; Blind Recruitment Field Experiments</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-indigo-600 dark:text-indigo-400 font-bold">&bull;</span>
              <span>Bertrand &amp; Mullainathan (2004) &mdash; Audit Study of Labor Discrimination</span>
            </li>
          </ul>
        </div>

      </div>

      <div className="max-w-7xl mx-auto pt-6 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-4 font-mono">
        <div>
          &copy; 2026 FairHire AI. Research by Harsh More &amp; Saloni Paygude (Guide: Prof. Kirti Garud).
        </div>
        <div className="flex items-center gap-4 text-slate-500 dark:text-slate-400">
          <span className="flex items-center gap-1.5">
            <Activity className="w-3.5 h-3.5 text-emerald-500" />
            Port 5000 REST: OK
          </span>
          <span className="text-slate-300 dark:text-slate-600">&bull;</span>
          <span className="text-indigo-600 dark:text-indigo-300">
            React 19 + Tailwind CSS v4 + S-BERT
          </span>
        </div>
      </div>
    </footer>
  )
}
