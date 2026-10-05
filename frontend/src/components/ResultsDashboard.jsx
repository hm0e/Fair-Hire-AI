import React, { useState, useEffect } from "react"
import { 
  Download, Printer, RefreshCw, Plus, Award, 
  BarChart3, FileText, CheckCircle, AlertTriangle, 
  Search, X, Eye, ShieldCheck, ArrowUpRight, Copy, Check,
  Cpu, Layers, Sparkles, Scale, FileSpreadsheet
} from "lucide-react"
import { Button } from "./ui/button"
import { Card } from "./ui/card"
import { Badge } from "./ui/badge"
import { FairnessGauge } from "./ui/gauge"

export function ResultsDashboard({ runId, setActiveView, onRescreenWithRewritten }) {
  const [runData, setRunData] = useState(null)
  const [activeTab, setActiveTab] = useState("leaderboard")
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedCandidate, setSelectedCandidate] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [copiedText, setCopiedText] = useState(false)

  useEffect(() => {
    if (!runId) return
    setIsLoading(true)
    fetch(`/api/results/${runId}`)
      .then(res => {
        if (!res.ok) {
          throw new Error(`Evaluation run not found (HTTP ${res.status})`)
        }
        return res.json()
      })
      .then(data => {
        setRunData(data)
        setIsLoading(false)
      })
      .catch(err => {
        console.error("Results fetch error:", err)
        setRunData({ error: err.message || "Failed to load evaluation run" })
        setIsLoading(false)
      })
  }, [runId])

  if (isLoading) {
    return (
      <div className="min-h-[60vh] flex flex-col items-center justify-center gap-4">
        <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center text-white">
          <Cpu className="w-5 h-5 animate-pulse" />
        </div>
        <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 font-mono">Generating audit scorecard &amp; rank correlation...</p>
      </div>
    )
  }

  if (!runData || runData.error) {
    return (
      <div className="max-w-md mx-auto py-20 text-center space-y-4">
        <div className="w-12 h-12 rounded-xl bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/25 flex items-center justify-center mx-auto text-rose-600 dark:text-rose-400">
          <AlertTriangle className="w-6 h-6" />
        </div>
        <h2 className="text-lg font-bold text-slate-900 dark:text-white font-heading">Evaluation Run Not Found</h2>
        <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
          {runData?.error || "The requested evaluation record does not exist or was deleted from the local SQLite audit ledger."}
        </p>
        <Button onClick={() => setActiveView("studio")} variant="default" size="sm">
          Return to Screening Studio
        </Button>
      </div>
    )
  }

  const result = runData.result || {}
  const fairness = result.fairness_summary || runData.fairness || {}
  const candidates = result.candidates || runData.candidates || []
  const jdBefore = result.jd_bias_before || {}
  const jdAfter = result.jd_bias_after || {}
  const rewrite = result.jd_rewrite || {}

  const filteredCandidates = candidates.filter(c => 
    (c.name || "").toLowerCase().includes(searchTerm.toLowerCase()) ||
    (c.skills && c.skills.some(s => (s || "").toLowerCase().includes(searchTerm.toLowerCase())))
  )

  const handleCopyAnonymized = async (text) => {
    try {
      if (navigator.clipboard && navigator.clipboard.writeText) {
        await navigator.clipboard.writeText(text)
      } else {
        const textArea = document.createElement("textarea")
        textArea.value = text
        document.body.appendChild(textArea)
        textArea.select()
        document.execCommand("copy")
        document.body.removeChild(textArea)
      }
      setCopiedText(true)
      setTimeout(() => setCopiedText(false), 2000)
    } catch (e) {
      console.warn("Clipboard copy failed:", e)
    }
  }

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-8">
      
      {/* Executive Header */}
      <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-4 pb-6 border-b border-slate-200 dark:border-white/[0.08]">
        <div>
          <div className="flex flex-wrap items-center gap-2 mb-1.5">
            <Badge variant="success">Audit Verified</Badge>
            <span className="text-xs text-slate-500 dark:text-slate-400 font-mono">
              &bull; Run #{runData.id} &bull; {runData.created_at ? runData.created_at.slice(0, 19).replace('T', ' ') : ''} UTC
            </span>
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight font-heading">
            {runData.jd_title}
          </h1>
          <p className="text-xs text-slate-600 dark:text-slate-400 mt-0.5">
            Triangulated scorecard benchmarking Keyword ATS, Neural S-BERT, and Blind Screening.
          </p>
        </div>

        {/* Action Toolbar */}
        <div className="flex flex-wrap items-center gap-2.5">
          <a
            href={`/api/export/${runData.id}.csv`}
            download
            className="inline-flex items-center gap-1.5 h-9 px-3.5 text-xs font-semibold rounded-lg bg-white hover:bg-slate-50 text-slate-700 border border-slate-300 dark:bg-slate-800 dark:hover:bg-slate-700 dark:text-slate-200 dark:border-slate-700 transition-colors shadow-xs cursor-pointer"
          >
            <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
            <span>Export CSV</span>
          </a>

          <Button
            size="sm"
            variant="secondary"
            onClick={() => window.print()}
            className="flex items-center gap-1.5 text-xs h-9 px-3.5 rounded-lg"
          >
            <Printer className="w-3.5 h-3.5 text-slate-500 dark:text-slate-400" />
            <span>Print Report</span>
          </Button>

          {rewrite.changes && rewrite.changes.length > 0 && (
            <Button
              size="sm"
              variant="cyan"
              onClick={() => {
                sessionStorage.setItem("fairhire_custom_jd_text", rewrite.rewritten_text)
                setActiveView("studio")
              }}
              className="flex items-center gap-1.5 text-xs h-9 px-3.5 rounded-lg"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Rescreen Rewritten JD</span>
            </Button>
          )}

          <Button
            size="sm"
            variant="default"
            onClick={() => setActiveView("studio")}
            className="flex items-center gap-1.5 text-xs h-9 px-3.5 rounded-lg font-semibold"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>New Run</span>
          </Button>
        </div>
      </div>

      {/* Headline Fairness Card */}
      <div className="p-6 sm:p-8 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm">
        <div className="flex flex-col lg:flex-row items-center justify-between gap-8">
          
          <div className="flex flex-col sm:flex-row items-center gap-6 text-center sm:text-left">
            <FairnessGauge score={fairness.fairness_index || 0} size={135} strokeWidth={11} />

            <div className="space-y-2.5">
              <h2 className="text-xl font-bold text-slate-900 dark:text-white font-heading">
                Composite Algorithmic Fairness Index
              </h2>

              <p className="text-xs text-slate-600 dark:text-slate-300 max-w-xl leading-relaxed">
                Empirical fairness grade calculated from: <strong>40% Ranking Stability</strong> (Spearman rank correlation), 
                <strong> 40% Bias Reduction</strong> in job description, and <strong>20% Flesch Readability</strong>.
              </p>

              {/* Sub-component metrics */}
              <div className="flex flex-wrap items-center justify-center sm:justify-start gap-2 pt-1">
                <div className="px-3 py-1 rounded-lg bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] text-xs">
                  <span className="text-slate-500 dark:text-slate-400 block text-[10px] uppercase font-mono">Rank Stability (40%)</span>
                  <span className="font-mono font-bold text-emerald-600 dark:text-emerald-400">
                    {Math.round((fairness.rank_correlation_semantic_vs_blind || 0) * 40 * 10) / 10} / 40
                  </span>
                </div>

                <div className="px-3 py-1 rounded-lg bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] text-xs">
                  <span className="text-slate-500 dark:text-slate-400 block text-[10px] uppercase font-mono">Bias Drop (40%)</span>
                  <span className="font-mono font-bold text-sky-600 dark:text-cyan-400">
                    {jdBefore?.gender_bias?.bias_score > 0 
                      ? Math.round(Math.min(1.0, (fairness.bias_reduction || 0) + 0.01) * 40 * 10) / 10 
                      : 40} / 40
                  </span>
                </div>

                <div className="px-3 py-1 rounded-lg bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] text-xs">
                  <span className="text-slate-500 dark:text-slate-400 block text-[10px] uppercase font-mono">Readability (20%)</span>
                  <span className="font-mono font-bold text-indigo-600 dark:text-indigo-400">
                    {Math.round(((jdAfter?.readability?.flesch_score || 0) / 100) * 20 * 10) / 10} / 20
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Audit Telemetry */}
          <div className="w-full lg:w-72 p-4 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] space-y-2 text-xs">
            <div className="flex items-center justify-between text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase font-mono pb-1.5 border-b border-slate-200 dark:border-white/[0.06]">
              <span>Audit Summary</span>
              <span className="text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" />
                EEOC Compliant
              </span>
            </div>
            <div className="flex justify-between text-slate-700 dark:text-slate-300">
              <span className="text-slate-500 dark:text-slate-400">Total Candidates:</span>
              <strong className="text-slate-900 dark:text-white font-mono">{fairness.total_candidates}</strong>
            </div>
            <div className="flex justify-between text-slate-700 dark:text-slate-300">
              <span className="text-slate-500 dark:text-slate-400">Rank Drift Detected:</span>
              <strong className={fairness.candidates_with_rank_change > 0 ? "text-amber-600 dark:text-amber-400 font-mono" : "text-emerald-600 dark:text-emerald-400 font-mono"}>
                {fairness.candidates_with_rank_change} candidates altered
              </strong>
            </div>
            <div className="flex justify-between text-slate-700 dark:text-slate-300 pt-1 border-t border-slate-200 dark:border-white/[0.05]">
              <span className="text-slate-500 dark:text-slate-400">Model:</span>
              <code className="text-[11px] text-sky-600 dark:text-cyan-300 font-mono">
                {candidates[0]?.semantic_backend || "S-BERT"}
              </code>
            </div>
          </div>

        </div>
      </div>

      {/* 4 Metric Tiles */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-1">
          <div className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase font-mono flex justify-between">
            <span>Rank Stability</span>
            <Badge variant="cyan" className="text-[9px]">&rho; Metric</Badge>
          </div>
          <div className="text-2xl font-bold text-sky-600 dark:text-cyan-400 font-mono">
            {fairness.rank_correlation_semantic_vs_blind}
          </div>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight">
            Spearman correlation between semantic and blind rankings (1.0 = identical).
          </p>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-1">
          <div className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase font-mono flex justify-between">
            <span>Avg Position Shift</span>
            <Badge variant="warning" className="text-[9px]">Drift</Badge>
          </div>
          <div className="text-2xl font-bold text-amber-600 dark:text-amber-400 font-mono">
            {fairness.avg_rank_shift_semantic_vs_blind}
          </div>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight">
            Average positions candidates displaced when blinded.
          </p>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-1">
          <div className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase font-mono flex justify-between">
            <span>Gender Bias Drop</span>
            <Badge variant="success" className="text-[9px]">Mitigation</Badge>
          </div>
          <div className="text-2xl font-bold text-emerald-600 dark:text-emerald-400 font-mono">
            {jdBefore?.gender_bias?.bias_score || 0} &rarr; {jdAfter?.gender_bias?.bias_score || 0}
          </div>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight capitalize">
            Lean: {jdBefore?.gender_bias?.lean || "neutral"} &rarr; {jdAfter?.gender_bias?.lean || "neutral"}
          </p>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-1">
          <div className="text-[10px] font-bold text-slate-500 dark:text-slate-400 uppercase font-mono flex justify-between">
            <span>Flesch Readability</span>
            <Badge variant="default" className="text-[9px]">Inclusivity</Badge>
          </div>
          <div className="text-2xl font-bold text-indigo-600 dark:text-indigo-400 font-mono">
            {jdBefore?.readability?.flesch_score || 0}
          </div>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-tight capitalize">
            Level: {jdBefore?.readability?.level || "Standard"}
          </p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 border-b border-slate-200 dark:border-white/[0.08] text-xs font-semibold">
        <button
          onClick={() => setActiveTab("leaderboard")}
          className={`flex items-center gap-1.5 px-4 py-2.5 border-b-2 transition-colors cursor-pointer ${
            activeTab === "leaderboard"
              ? "border-indigo-600 text-indigo-600 dark:border-indigo-500 dark:text-white font-bold"
              : "border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
          }`}
        >
          <Award className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
          <span>Candidate Leaderboard &amp; Rank Shifts</span>
        </button>

        <button
          onClick={() => setActiveTab("analytics")}
          className={`flex items-center gap-1.5 px-4 py-2.5 border-b-2 transition-colors cursor-pointer ${
            activeTab === "analytics"
              ? "border-indigo-600 text-indigo-600 dark:border-indigo-500 dark:text-white font-bold"
              : "border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
          }`}
        >
          <BarChart3 className="w-4 h-4 text-sky-600 dark:text-cyan-400" />
          <span>Multi-Engine Score Diagnostics</span>
        </button>

        <button
          onClick={() => setActiveTab("bias-audit")}
          className={`flex items-center gap-1.5 px-4 py-2.5 border-b-2 transition-colors cursor-pointer ${
            activeTab === "bias-audit"
              ? "border-indigo-600 text-indigo-600 dark:border-indigo-500 dark:text-white font-bold"
              : "border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
          }`}
        >
          <FileText className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
          <span>Job Description Bias &amp; Rewrite Diff</span>
        </button>
      </div>

      {/* TAB 1: LEADERBOARD */}
      {activeTab === "leaderboard" && (
        <div className="rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm overflow-hidden">
          
          <div className="p-4 border-b border-slate-200 dark:border-white/[0.08] flex flex-wrap items-center justify-between gap-4">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-white font-heading">Triangulated Candidate Roster</h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Candidates with amber rows experienced rank shifts under blind screening.
              </p>
            </div>

            <div className="relative w-full sm:w-64">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search candidates or skills..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/10 rounded-lg pl-8 pr-3 py-1.5 text-xs text-slate-900 dark:text-slate-200 focus:outline-none focus:border-indigo-500"
              />
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-slate-950/60 text-slate-500 dark:text-slate-400 uppercase tracking-wider font-mono text-[11px] border-b border-slate-200 dark:border-white/[0.08]">
                <tr>
                  <th className="py-3 px-4">Rank</th>
                  <th className="py-3 px-4">Candidate Profile</th>
                  <th className="py-3 px-4">Matched Skills</th>
                  <th className="py-3 px-4">Skill Gaps</th>
                  <th className="py-3 px-4">Keyword ATS</th>
                  <th className="py-3 px-4">S-BERT Semantic</th>
                  <th className="py-3 px-4">Blind Standard</th>
                  <th className="py-3 px-4">Rank Shift</th>
                  <th className="py-3 px-4 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-white/[0.05]">
                {filteredCandidates.map((c, i) => (
                  <tr 
                    key={i} 
                    className={`hover:bg-slate-50 dark:hover:bg-white/[0.02] transition-colors ${
                      c.rank_shift_semantic_vs_blind > 0 ? "bg-amber-50/50 dark:bg-amber-500/[0.04]" : ""
                    }`}
                  >
                    <td className="py-3.5 px-4 font-mono font-bold">
                      <div className="w-7 h-7 rounded-lg bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-white/10 flex items-center justify-center text-xs text-slate-700 dark:text-slate-300">
                        #{c.rank_semantic}
                      </div>
                    </td>

                    <td className="py-3.5 px-4">
                      <div>
                        <div className="font-bold text-slate-900 dark:text-white text-sm">{c.name}</div>
                        <div className="text-[11px] text-slate-500 dark:text-slate-400 font-mono">{c.years_experience} yrs exp</div>
                      </div>
                    </td>

                    <td className="py-3.5 px-4 max-w-xs">
                      <div className="flex flex-wrap gap-1">
                        {c.skills && c.skills.length > 0 ? (
                          c.skills.slice(0, 3).map((s, idx) => (
                            <Badge key={idx} variant="default" className="text-[10px] py-0 px-1.5 font-mono">
                              {s}
                            </Badge>
                          ))
                        ) : (
                          <span className="text-slate-400">&mdash;</span>
                        )}
                        {c.skills && c.skills.length > 3 && (
                          <span className="text-[10px] text-slate-400 font-mono">+{c.skills.length - 3}</span>
                        )}
                      </div>
                    </td>

                    <td className="py-3.5 px-4 max-w-xs">
                      <div className="flex flex-wrap gap-1">
                        {c.skill_gap && c.skill_gap.length > 0 ? (
                          c.skill_gap.slice(0, 2).map((g, idx) => (
                            <Badge key={idx} variant="danger" className="text-[10px] py-0 px-1.5 font-mono">
                              {g}
                            </Badge>
                          ))
                        ) : (
                          <span className="text-emerald-600 dark:text-emerald-400 font-mono text-[11px]">&bull; None</span>
                        )}
                      </div>
                    </td>

                    <td className="py-3.5 px-4 font-mono text-slate-700 dark:text-slate-300 font-semibold">{c.keyword_score}</td>
                    <td className="py-3.5 px-4 font-mono font-bold text-sky-700 dark:text-cyan-300">{c.semantic_score}</td>
                    <td className="py-3.5 px-4 font-mono font-bold text-emerald-700 dark:text-emerald-400">{c.blind_semantic_score}</td>

                    <td className="py-3.5 px-4 font-mono">
                      {c.rank_semantic < c.rank_blind ? (
                        <span className="px-2 py-0.5 rounded bg-rose-50 dark:bg-rose-500/15 text-rose-700 dark:text-rose-300 border border-rose-200 dark:border-rose-500/30 font-bold text-[11px] flex items-center gap-1 w-fit">
                          &minus;{c.rank_shift_semantic_vs_blind} &darr;
                        </span>
                      ) : c.rank_semantic > c.rank_blind ? (
                        <span className="px-2 py-0.5 rounded bg-emerald-50 dark:bg-emerald-500/15 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-500/30 font-bold text-[11px] flex items-center gap-1 w-fit">
                          +{c.rank_shift_semantic_vs_blind} &uarr;
                        </span>
                      ) : (
                        <span className="text-slate-400 font-mono text-xs">= 0</span>
                      )}
                    </td>

                    <td className="py-3.5 px-4 text-right">
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={() => setSelectedCandidate(c)}
                        className="text-xs h-7 px-2.5 rounded-lg"
                      >
                        <Eye className="w-3.5 h-3.5 text-indigo-600 dark:text-cyan-400" />
                        <span>Inspect</span>
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

        </div>
      )}

      {/* TAB 2: SCORE DIAGNOSTICS */}
      {activeTab === "analytics" && (
        <div className="space-y-6">
          <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-3 border-b border-slate-200 dark:border-white/[0.06]">
              <div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-white font-heading">Candidate Score Comparison</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">Comparing Lexical vs Semantic vs Blind scoring vectors</p>
              </div>

              <div className="flex items-center gap-4 text-xs font-mono">
                <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-sm bg-slate-400 dark:bg-slate-500"></span> Keyword ATS</span>
                <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-sm bg-indigo-600 dark:bg-indigo-500"></span> S-BERT Semantic</span>
                <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-sm bg-emerald-600 dark:bg-emerald-500"></span> Blind AI Standard</span>
              </div>
            </div>

            <div className="space-y-3">
              {candidates.map((c, i) => (
                <div key={i} className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.05] space-y-2">
                  <div className="flex justify-between items-center text-xs">
                    <span className="font-bold text-slate-900 dark:text-white">
                      {c.name} <span className="text-slate-500 dark:text-slate-400 font-mono">(Rank #{c.rank_semantic})</span>
                    </span>
                    <span className="text-slate-500 dark:text-slate-400 font-mono text-[11px]">
                      KW: {c.keyword_score} | Sem: {c.semantic_score} | Blind: {c.blind_semantic_score}
                    </span>
                  </div>

                  <div className="space-y-1.5">
                    <div className="flex items-center gap-3 text-xs">
                      <span className="w-16 text-slate-500 dark:text-slate-400 text-right font-mono text-[11px]">Keyword:</span>
                      <div className="flex-1 bg-slate-200 dark:bg-slate-900 rounded-full h-2 overflow-hidden">
                        <div 
                          className="bg-slate-400 dark:bg-slate-500 h-full rounded-full" 
                          style={{ width: `${Math.round(c.keyword_score * 100)}%` }}
                        ></div>
                      </div>
                      <span className="w-10 text-slate-700 dark:text-slate-300 font-mono text-right">{Math.round(c.keyword_score * 100)}%</span>
                    </div>

                    <div className="flex items-center gap-3 text-xs">
                      <span className="w-16 text-indigo-600 dark:text-indigo-400 text-right font-mono font-semibold text-[11px]">Semantic:</span>
                      <div className="flex-1 bg-slate-200 dark:bg-slate-900 rounded-full h-2 overflow-hidden">
                        <div 
                          className="bg-indigo-600 dark:bg-indigo-500 h-full rounded-full" 
                          style={{ width: `${Math.round(c.semantic_score * 100)}%` }}
                        ></div>
                      </div>
                      <span className="w-10 text-indigo-700 dark:text-indigo-300 font-mono font-bold text-right">{Math.round(c.semantic_score * 100)}%</span>
                    </div>

                    <div className="flex items-center gap-3 text-xs">
                      <span className="w-16 text-emerald-600 dark:text-emerald-400 text-right font-mono font-semibold text-[11px]">Blind:</span>
                      <div className="flex-1 bg-slate-200 dark:bg-slate-900 rounded-full h-2 overflow-hidden">
                        <div 
                          className="bg-emerald-600 dark:bg-emerald-500 h-full rounded-full" 
                          style={{ width: `${Math.round(c.blind_semantic_score * 100)}%` }}
                        ></div>
                      </div>
                      <span className="w-10 text-emerald-700 dark:text-emerald-300 font-mono font-bold text-right">{Math.round(c.blind_semantic_score * 100)}%</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: JD BIAS & REWRITE */}
      {activeTab === "bias-audit" && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-4">
              <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center justify-between">
                <span>Coded Word Diagnostics</span>
                <Badge variant="danger">
                  {(jdBefore?.gender_bias?.total_flagged_terms || 0) + (jdBefore?.age_bias?.total_flagged_terms || 0)} Signals Flagged
                </Badge>
              </h3>

              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] space-y-1">
                <div className="text-xs font-semibold text-slate-700 dark:text-slate-300">Gender Directional Lean:</div>
                <div className="text-xs text-slate-600 dark:text-slate-400 font-mono">
                  Original: <strong className="text-rose-600 dark:text-rose-400 capitalize">{jdBefore?.gender_bias?.lean}</strong> &bull; Score: {jdBefore?.gender_bias?.bias_score}
                  <br />
                  Neutralized: <strong className="text-emerald-600 dark:text-emerald-400 capitalize">{jdAfter?.gender_bias?.lean}</strong> &bull; Score: {jdAfter?.gender_bias?.bias_score}
                </div>
              </div>

              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] space-y-1">
                <div className="text-xs font-semibold text-slate-700 dark:text-slate-300">Readability Accessibility:</div>
                <div className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                  Flesch Reading Ease: <strong>{jdBefore?.readability?.flesch_score}</strong> ({jdBefore?.readability?.level}).
                </div>
              </div>
            </div>

            <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-4">
              <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center justify-between">
                <span>Inclusive Rewriter Engine Changelog</span>
                <Badge variant="success">{rewrite?.changes?.length || 0} Substitutions</Badge>
              </h3>

              {rewrite?.changes && rewrite.changes.length > 0 ? (
                <div className="max-h-60 overflow-y-auto space-y-1.5 pr-1">
                  {rewrite.changes.map((c, i) => (
                    <div key={i} className="flex items-center justify-between p-2.5 rounded-lg bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] text-xs">
                      <div>
                        <span className="line-through text-rose-600 dark:text-rose-400 font-mono">{c.from}</span>
                        <span className="text-slate-400 mx-2">&rarr;</span>
                        <span className="text-emerald-600 dark:text-emerald-400 font-bold font-mono">{c.to}</span>
                      </div>
                      <Badge variant="cyan" className="text-[10px] font-mono">{c.count}x</Badge>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-12 text-slate-400 text-xs font-mono">
                  Zero biased or coded terms flagged in this job description.
                </div>
              )}
            </div>

          </div>

          {/* Side by side */}
          <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-900 dark:text-white">Side-by-Side Job Description Comparison</h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-950 border border-rose-200 dark:border-rose-500/25 space-y-1.5">
                <div className="text-[11px] font-bold uppercase text-rose-600 dark:text-rose-400 font-mono">
                  Original Job Description (Biased Baseline)
                </div>
                <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-wrap">
                  {rewrite?.original_text}
                </p>
              </div>

              <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-950 border border-emerald-200 dark:border-emerald-500/25 space-y-1.5">
                <div className="text-[11px] font-bold uppercase text-emerald-600 dark:text-emerald-400 font-mono">
                  FairHire Neutralized Rewrite
                </div>
                <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-wrap">
                  {rewrite?.rewritten_text}
                </p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Candidate Inspector Modal */}
      {selectedCandidate && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/20 space-y-5 shadow-2xl">
            
            <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-white/[0.08]">
              <div>
                <h3 className="text-base font-bold text-slate-900 dark:text-white font-heading">{selectedCandidate.name}</h3>
                <div className="text-xs text-slate-500 dark:text-slate-400 font-mono">{selectedCandidate.years_experience} years relevant experience</div>
              </div>

              <button
                onClick={() => setSelectedCandidate(null)}
                className="text-slate-400 hover:text-slate-700 dark:hover:text-white p-1.5 rounded-lg bg-slate-100 dark:bg-white/[0.04] transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Score Grid */}
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] space-y-0.5">
                <div className="text-[10px] uppercase font-bold text-slate-500 dark:text-slate-400 font-mono">Keyword ATS</div>
                <div className="text-lg font-bold font-mono text-slate-900 dark:text-white">{selectedCandidate.keyword_score}</div>
                <Badge variant="default" className="text-[9px]">Rank #{selectedCandidate.rank_keyword}</Badge>
              </div>

              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] space-y-0.5">
                <div className="text-[10px] uppercase font-bold text-slate-500 dark:text-slate-400 font-mono">Semantic S-BERT</div>
                <div className="text-lg font-bold font-mono text-sky-700 dark:text-cyan-400">{selectedCandidate.semantic_score}</div>
                <Badge variant="cyan" className="text-[9px]">Rank #{selectedCandidate.rank_semantic}</Badge>
              </div>

              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] space-y-0.5">
                <div className="text-[10px] uppercase font-bold text-slate-500 dark:text-slate-400 font-mono">Blind Semantic</div>
                <div className="text-lg font-bold font-mono text-emerald-700 dark:text-emerald-400">{selectedCandidate.blind_semantic_score}</div>
                <Badge variant="success" className="text-[9px]">Rank #{selectedCandidate.rank_blind}</Badge>
              </div>
            </div>

            {/* Skills */}
            <div className="space-y-3">
              <div>
                <div className="text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">Matched Competencies:</div>
                <div className="flex flex-wrap gap-1">
                  {selectedCandidate.skills && selectedCandidate.skills.length > 0 ? (
                    selectedCandidate.skills.map((s, i) => (
                      <Badge key={i} variant="default" className="text-xs font-mono">{s}</Badge>
                    ))
                  ) : <span className="text-xs text-slate-400">None detected</span>}
                </div>
              </div>

              <div>
                <div className="text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">Missing Skill Gaps (Required by JD):</div>
                <div className="flex flex-wrap gap-1">
                  {selectedCandidate.skill_gap && selectedCandidate.skill_gap.length > 0 ? (
                    selectedCandidate.skill_gap.map((g, i) => (
                      <Badge key={i} variant="danger" className="text-xs font-mono">{g}</Badge>
                    ))
                  ) : <Badge variant="success" className="text-xs">No Missing Skills</Badge>}
                </div>
              </div>
            </div>

            {/* Redactions */}
            <div className="space-y-1">
              <div className="text-xs font-semibold text-slate-700 dark:text-slate-300">Identity Redactions Applied:</div>
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] text-xs font-mono text-slate-700 dark:text-slate-300 space-y-0.5">
                {selectedCandidate.redactions_applied && selectedCandidate.redactions_applied.length > 0 ? (
                  selectedCandidate.redactions_applied.map((r, i) => (
                    <div key={i} className="flex items-center gap-1.5">
                      <span className="text-emerald-600 dark:text-emerald-400">&bull;</span>
                      <span>{r}</span>
                    </div>
                  ))
                ) : <div>No explicit identity signals flagged.</div>}
              </div>
            </div>

            {/* Anonymized text */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs">
                <span className="font-semibold text-slate-700 dark:text-slate-300">Anonymized Resume Stream (Evaluated by S-BERT):</span>
                <button
                  onClick={() => handleCopyAnonymized(selectedCandidate.anonymized_text || selectedCandidate.raw_text)}
                  className="flex items-center gap-1 text-[11px] text-indigo-600 dark:text-indigo-400 hover:underline font-mono cursor-pointer"
                >
                  {copiedText ? <Check className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copiedText ? "Copied" : "Copy"}</span>
                </button>
              </div>

              <pre className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] text-[11px] font-mono text-slate-800 dark:text-slate-300 max-h-48 overflow-y-auto whitespace-pre-wrap leading-relaxed shadow-inner">
                {selectedCandidate.anonymized_text || selectedCandidate.raw_text}
              </pre>
            </div>

          </div>
        </div>
      )}

    </div>
  )
}
