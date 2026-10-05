import React, { useState, useEffect } from "react"
import { Search, Download, Trash2, ArrowRight, History, Award, Plus, FileSpreadsheet, AlertCircle } from "lucide-react"
import { Button } from "./ui/button"
import { Card } from "./ui/card"
import { Badge } from "./ui/badge"

export function AuditHistory({ onSelectRun, setActiveView }) {
  const [runs, setRuns] = useState([])
  const [searchQuery, setSearchQuery] = useState("")
  const [isLoading, setIsLoading] = useState(true)

  const fetchHistory = () => {
    setIsLoading(true)
    fetch("/api/history")
      .then(res => res.json())
      .then(data => {
        setRuns(data)
        setIsLoading(false)
      })
      .catch(err => {
        console.error(err)
        setIsLoading(false)
      })
  }

  useEffect(() => {
    fetchHistory()
  }, [])

  const handleDelete = async (id, title) => {
    if (!window.confirm(`Are you sure you want to permanently delete audit record for "${title}" (Run #${id})?`)) {
      return
    }

    try {
      const res = await fetch(`/api/history/${id}`, { method: "DELETE" })
      if (res.ok) {
        setRuns(prev => prev.filter(r => r.id !== id))
      }
    } catch (err) {
      console.error("Delete error:", err)
    }
  }

  const filteredRuns = runs.filter(r => 
    (r.jd_title || "").toLowerCase().includes(searchQuery.toLowerCase()) ||
    String(r.id || "").includes(searchQuery)
  )

  return (
    <div className="max-w-7xl mx-auto py-10 px-4 sm:px-6 lg:px-8 space-y-8 relative z-10">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 pb-6 border-b border-slate-200 dark:border-white/[0.08]">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Badge variant="cyan" dot>Audit Repository</Badge>
            <span className="text-xs text-slate-500 dark:text-slate-400 font-mono">&bull; Persistent SQLite Ledger</span>
          </div>
          <h1 className="font-heading text-2xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Screening Audit Archive
          </h1>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 mt-1 max-w-2xl leading-relaxed">
            Cryptographically timestamped historical log of previous candidate evaluations, fairness indices, and rank drift benchmarks.
          </p>
        </div>

        <Button 
          onClick={() => setActiveView("studio")} 
          variant="default"
          className="flex items-center gap-2 text-xs font-semibold"
        >
          <Plus className="w-4 h-4" />
          <span>Launch New Screening</span>
        </Button>
      </div>

      {/* Main Table Card */}
      <Card className="p-0 overflow-hidden border-slate-200 dark:border-white/[0.08]">
        
        {/* Search Bar & Stats */}
        <div className="p-5 sm:p-6 border-b border-slate-200 dark:border-white/[0.08] flex flex-wrap items-center justify-between gap-4">
          <div className="relative w-full max-w-sm">
            <Search className="w-4 h-4 text-slate-400 dark:text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search by role title or run ID..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-slate-50 dark:bg-[#04060a]/90 border border-slate-200 dark:border-white/10 rounded-xl pl-10 pr-4 py-2 text-xs text-slate-900 dark:text-slate-200 placeholder:text-slate-400 dark:placeholder:text-slate-500 focus:outline-none focus:border-indigo-500 font-sans"
            />
          </div>

          <div className="flex items-center gap-2 text-xs font-mono text-slate-500 dark:text-slate-400">
            <span>Total Verified Audits:</span>
            <strong className="text-slate-900 dark:text-white bg-slate-100 dark:bg-white/[0.05] border border-slate-200 dark:border-white/10 px-2 py-0.5 rounded-lg">{runs.length}</strong>
          </div>
        </div>

        {/* Table Content */}
        {isLoading ? (
          <div className="py-24 text-center text-xs text-slate-500 dark:text-slate-400 font-mono">
            <div className="w-8 h-8 rounded-full border-2 border-indigo-500/20 border-t-indigo-600 animate-spin mx-auto mb-3" />
            Loading persistent audit history...
          </div>
        ) : filteredRuns.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 dark:bg-[#04060a]/60 text-slate-600 dark:text-slate-400 uppercase tracking-wider font-mono text-[11px] border-b border-slate-200 dark:border-white/[0.08]">
                <tr>
                  <th className="py-4 px-4 w-24">Run ID</th>
                  <th className="py-4 px-4">Evaluated Role Title</th>
                  <th className="py-4 px-4">Evaluation Timestamp</th>
                  <th className="py-4 px-4">Candidate Count</th>
                  <th className="py-4 px-4">Fairness Score</th>
                  <th className="py-4 px-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-white/[0.05]">
                {filteredRuns.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/80 dark:hover:bg-white/[0.02] transition-colors">
                    <td className="py-4 px-4 font-mono font-bold text-slate-500 dark:text-slate-400">
                      <span className="px-2 py-1 rounded-md bg-slate-100 dark:bg-white/[0.03] border border-slate-200 dark:border-white/10">
                        #{r.id}
                      </span>
                    </td>

                    <td className="py-4 px-4">
                      <button
                        onClick={() => onSelectRun(r.id)}
                        className="font-bold text-slate-900 dark:text-white hover:text-indigo-600 dark:hover:text-indigo-300 transition-colors text-left cursor-pointer font-heading text-sm"
                      >
                        {r.jd_title}
                      </button>
                    </td>

                    <td className="py-4 px-4 text-slate-500 dark:text-slate-400 font-mono text-[11px]">
                      {r.created_at ? r.created_at.slice(0, 19).replace('T', ' ') : 'N/A'} UTC
                    </td>

                    <td className="py-4 px-4 font-mono">
                      <span className="text-slate-700 dark:text-slate-300 font-bold">{r.candidate_count || 4} candidates</span>
                    </td>

                    <td className="py-4 px-4">
                      {r.fairness_score !== null && r.fairness_score !== undefined ? (
                        <div className="flex items-center gap-2">
                          <span className={`font-mono font-bold text-sm ${
                            r.fairness_score >= 80 ? "text-emerald-600 dark:text-emerald-400" :
                            r.fairness_score >= 65 ? "text-amber-600 dark:text-amber-400" : "text-rose-600 dark:text-rose-400"
                          }`}>
                            {r.fairness_score} / 100
                          </span>
                          <Badge 
                            variant={r.fairness_score >= 80 ? "success" : r.fairness_score >= 65 ? "warning" : "danger"} 
                            className="text-[10px]"
                          >
                            {r.fairness_score >= 80 ? "Optimal" : r.fairness_score >= 65 ? "Moderate" : "Biased"}
                          </Badge>
                        </div>
                      ) : (
                        <span className="text-slate-400 font-mono">&mdash;</span>
                      )}
                    </td>

                    <td className="py-4 px-4 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <a
                          href={`/api/export/${r.id}.csv`}
                          download
                          title="Download Audit CSV"
                          className="p-1.5 rounded-lg bg-slate-100 dark:bg-white/[0.03] hover:bg-slate-200 dark:hover:bg-white/[0.08] text-slate-600 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white border border-slate-200 dark:border-white/10 transition-colors"
                        >
                          <FileSpreadsheet className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
                        </a>

                        <button
                          onClick={() => onSelectRun(r.id)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-50 dark:bg-indigo-500/15 hover:bg-indigo-100 dark:hover:bg-indigo-500/25 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-500/30 text-xs font-semibold transition-all cursor-pointer"
                        >
                          <span>Inspect</span>
                          <ArrowRight className="w-3 h-3" />
                        </button>

                        <button
                          onClick={() => handleDelete(r.id, r.jd_title)}
                          title="Delete Audit Record"
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-500/10 transition-colors cursor-pointer"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="py-24 text-center space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-white/[0.02] border border-slate-200 dark:border-white/10 flex items-center justify-center mx-auto text-slate-400 dark:text-slate-500">
              <History className="w-6 h-6" />
            </div>
            <div className="text-sm font-bold text-slate-900 dark:text-white font-heading">No Screening Records Found</div>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
              {searchQuery ? "No audit runs match your search query." : "Run your first benchmark screening from the Studio to populate this audit ledger."}
            </p>
            <Button onClick={() => setActiveView("studio")} size="sm" variant="default">
              Launch Screening Studio
            </Button>
          </div>
        )}

      </Card>

    </div>
  )
}
