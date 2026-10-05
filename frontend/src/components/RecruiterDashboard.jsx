import React, { useState, useEffect } from "react"
import {
  Briefcase,
  Sliders,
  ShieldAlert,
  Sparkles,
  EyeOff,
  BarChart3,
  Download,
  Plus,
  RefreshCw,
  FileUp,
  X
} from "lucide-react"

export function RecruiterDashboard({ onOpenLegacyStudio }) {
  const [jobs, setJobs] = useState([])
  const [selectedJobId, setSelectedJobId] = useState(null)
  const [selectedJob, setSelectedJob] = useState(null)
  const [resumes, setResumes] = useState([])
  const [matchResults, setMatchResults] = useState([])
  const [blindData, setBlindData] = useState(null)
  const [biasReport, setBiasReport] = useState(null)
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState("ranking") // ranking | blind | bias | rewrite | upload

  // Configurable ATS Scoring Weights
  const [keywordWeight, setKeywordWeight] = useState(30) // 30% keyword, 70% semantic

  // Job Creation Modal State
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [newTitle, setNewTitle] = useState("")
  const [newDescription, setNewDescription] = useState("")
  const [newRequirements, setNewRequirements] = useState("")
  const [creatingJob, setCreatingJob] = useState(false)

  // Resume Upload State
  const [uploadFile, setUploadFile] = useState(null)
  const [candidateName, setCandidateName] = useState("")
  const [candidateEmail, setCandidateEmail] = useState("")
  const [uploadText, setUploadText] = useState("")
  const [uploading, setUploading] = useState(false)
  const [uploadSuccess, setUploadSuccess] = useState(null)

  // Rewrite state
  const [rewriteData, setRewriteData] = useState(null)
  const [rewriting, setRewriting] = useState(false)

  const fetchJobs = async () => {
    try {
      const res = await fetch("/api/jobs")
      const data = await res.json()
      if (Array.isArray(data) && data.length > 0) {
        setJobs(data)
        if (!selectedJobId) {
          setSelectedJobId(data[0].id)
        }
      }
    } catch (err) {
      console.error("Failed to load jobs:", err)
    }
  }

  const fetchResumes = async () => {
    try {
      const res = await fetch("/api/resumes")
      const data = await res.json()
      if (Array.isArray(data)) {
        setResumes(data)
      }
    } catch (err) {
      console.error("Failed to load resumes:", err)
    }
  }

  const loadJobDetails = async (jobId) => {
    setLoading(true)
    try {
      // 1. Job details
      const jobRes = await fetch(`/api/jobs/${jobId}`)
      const jobData = await jobRes.json()
      setSelectedJob(jobData)

      // 2. Match results
      const matchRes = await fetch(`/api/matching/${jobId}/results`)
      const matchData = await matchRes.json()
      setMatchResults(matchData?.results || [])

      // 3. Blind screening comparison
      const blindRes = await fetch(`/api/blind-screening/${jobId}`)
      const bData = await blindRes.json()
      setBlindData(bData)

      // 4. Bias report
      const biasRes = await fetch(`/api/bias/${jobId}`)
      const biData = await biasRes.json()
      setBiasReport(biData)

      // 5. Pre-generate rewrite preview
      if (jobData?.description) {
        const rwRes = await fetch("/api/bias/rewrite", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ text: `${jobData.title}\n${jobData.description}\n${jobData.requirements || ""}` })
        })
        const rwData = await rwRes.json()
        setRewriteData(rwData)
      }
    } catch (err) {
      console.error("Failed to fetch job details:", err)
    } finally {
      setLoading(false)
    }
  }

  // Fetch Jobs on Mount
  useEffect(() => {
    fetchJobs()
    fetchResumes()
  }, [])

  // Fetch Details for Selected Job
  useEffect(() => {
    if (!selectedJobId) return
    loadJobDetails(selectedJobId)
  }, [selectedJobId])

  // Run/Re-calculate Matching with Weights
  const handleRecalculateWeights = async () => {
    if (!selectedJobId) return
    setLoading(true)
    try {
      const kw = keywordWeight / 100
      const sem = (100 - keywordWeight) / 100
      const res = await fetch(`/api/matching/${selectedJobId}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          keyword_weight: kw,
          semantic_weight: sem
        })
      })
      const data = await res.json()
      setMatchResults(data.results || [])

      // Refresh blind comparison
      const blindRes = await fetch(`/api/blind-screening/${selectedJobId}`)
      const bData = await blindRes.json()
      setBlindData(bData)
    } catch (err) {
      console.error("Failed to recalculate matching:", err)
    } finally {
      setLoading(false)
    }
  }

  // Create Job
  const handleCreateJob = async (e) => {
    e.preventDefault()
    if (!newTitle.trim() || !newDescription.trim()) return
    setCreatingJob(true)
    try {
      const res = await fetch("/api/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: newTitle,
          description: newDescription,
          requirements: newRequirements
        })
      })
      const data = await res.json()
      if (data.job) {
        setJobs(prev => [data.job, ...prev])
        setSelectedJobId(data.job.id)
        setShowCreateModal(false)
        setNewTitle("")
        setNewDescription("")
        setNewRequirements("")
      }
    } catch (err) {
      console.error("Error creating job:", err)
    } finally {
      setCreatingJob(false)
    }
  }

  // Upload Resume
  const handleUploadResume = async (e) => {
    e.preventDefault()
    setUploading(true)
    setUploadSuccess(null)
    try {
      let res
      if (uploadFile) {
        const formData = new FormData()
        formData.append("file", uploadFile)
        if (candidateName) formData.append("name", candidateName)
        if (candidateEmail) formData.append("email", candidateEmail)
        res = await fetch("/api/resumes", {
          method: "POST",
          body: formData
        })
      } else if (uploadText.trim()) {
        res = await fetch("/api/resumes", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            raw_text: uploadText,
            name: candidateName || "New Candidate",
            email: candidateEmail || "candidate@example.com"
          })
        })
      }

      if (res && res.ok) {
        setUploadSuccess("Resume successfully parsed and indexed into ATS pool!")
        setUploadFile(null)
        setUploadText("")
        setCandidateName("")
        setCandidateEmail("")
        fetchResumes()
        if (selectedJobId) {
          loadJobDetails(selectedJobId)
        }
      } else {
        const errData = await res.json()
        setUploadSuccess(`Error: ${errData.error || "Failed to upload"}`)
      }
    } catch (err) {
      console.error("Error uploading resume:", err)
      setUploadSuccess("Network error during upload.")
    } finally {
      setUploading(false)
    }
  }

  // Apply Inclusive Rewrite to current job
  const handleApplyRewrite = async () => {
    if (!selectedJobId || !rewriteData?.rewritten_text) return
    setRewriting(true)
    try {
      const res = await fetch(`/api/jobs/${selectedJobId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          description: rewriteData.rewritten_text
        })
      })
      if (res.ok) {
        await loadJobDetails(selectedJobId)
        setActiveTab("bias")
      }
    } catch (err) {
      console.error("Failed to apply rewrite:", err)
    } finally {
      setRewriting(false)
    }
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Top Header & Quick Actions */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-slate-200 dark:border-white/10">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 dark:bg-indigo-950/50 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 text-xs font-semibold mb-2">
            <Briefcase className="w-3.5 h-3.5" />
            Recruiter Workspace • Spring Boot & AI Architecture
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold font-heading text-slate-900 dark:text-white">
            Recruitment & Screening Studio
          </h1>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            Manage job descriptions, upload candidate resumes, configure ATS weights, and compare normal vs. blind screenings.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowCreateModal(true)}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm transition-colors cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Post New Job Description
          </button>
          <a
            href="/api/reports/export"
            download
            className="inline-flex items-center gap-2 px-3 py-2 rounded-xl border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-800 text-xs font-semibold shadow-sm transition-colors"
          >
            <Download className="w-4 h-4 text-emerald-500" />
            Export Audit CSV
          </a>
        </div>
      </div>

      {/* Main Grid: Job Selector Sidebar + Workspace */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        
        {/* Left Column: Job Description Selector */}
        <div className="lg:col-span-1 space-y-4">
          <div className="p-4 rounded-2xl bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-white/10 shadow-sm space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Active Job Postings ({jobs.length})
              </span>
              <button
                onClick={fetchJobs}
                className="p-1 rounded text-slate-400 hover:text-slate-600 dark:hover:text-white"
                title="Refresh jobs"
              >
                <RefreshCw className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
              {jobs.map(job => {
                const isSelected = job.id === selectedJobId
                return (
                  <button
                    key={job.id}
                    onClick={() => setSelectedJobId(job.id)}
                    className={`w-full text-left p-3 rounded-xl border transition-all cursor-pointer ${
                      isSelected
                        ? "bg-indigo-50 dark:bg-indigo-950/40 border-indigo-500 text-indigo-950 dark:text-indigo-100 shadow-sm"
                        : "bg-slate-50 dark:bg-white/[0.02] border-slate-200 dark:border-white/[0.06] text-slate-700 dark:text-slate-300 hover:border-slate-300 dark:hover:border-white/20"
                    }`}
                  >
                    <div className="font-semibold text-xs leading-snug line-clamp-2">
                      {job.title}
                    </div>
                    <div className="flex items-center gap-2 mt-2 text-[10px] text-slate-500 dark:text-slate-400">
                      <span>ID #{job.id}</span>
                      <span>•</span>
                      <span>{job.skills?.length || 0} skills</span>
                    </div>
                  </button>
                )
              })}
            </div>
          </div>

          {/* Configurable ATS Scoring Engine Weights */}
          <div className="p-4 rounded-2xl bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-white/10 shadow-sm space-y-4">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-900 dark:text-white">
              <Sliders className="w-4 h-4 text-indigo-500" />
              ATS Scoring Weights
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-relaxed">
              Dynamically balance Keyword ATS overlap vs. Semantic S-BERT neural embeddings.
            </p>

            <div className="space-y-3">
              <div className="flex justify-between text-xs font-semibold">
                <span className="text-amber-600 dark:text-amber-400">Keyword ATS: {keywordWeight}%</span>
                <span className="text-indigo-600 dark:text-indigo-400">Semantic: {100 - keywordWeight}%</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                step="5"
                value={keywordWeight}
                onChange={(e) => setKeywordWeight(Number(e.target.value))}
                className="w-full h-1.5 bg-slate-200 dark:bg-slate-700 rounded-lg appearance-none cursor-pointer accent-indigo-600"
              />
              <button
                onClick={handleRecalculateWeights}
                disabled={loading}
                className="w-full py-1.5 rounded-lg bg-slate-900 hover:bg-slate-800 dark:bg-white dark:hover:bg-slate-100 text-white dark:text-slate-900 text-xs font-semibold transition-colors cursor-pointer"
              >
                Apply & Re-rank
              </button>
            </div>
          </div>
        </div>

        {/* Right Columns: Active Job & Work Tabs */}
        <div className="lg:col-span-3 space-y-6">
          {selectedJob && (
            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-white/10 shadow-sm">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-100 dark:border-white/[0.06]">
                <div>
                  <h2 className="text-xl font-bold font-heading text-slate-900 dark:text-white">
                    {selectedJob.title}
                  </h2>
                  <div className="flex flex-wrap items-center gap-2 mt-1.5">
                    <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 dark:bg-white/[0.06] text-slate-600 dark:text-slate-300">
                      Job #{selectedJob.id}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-300">
                      {selectedJob.skills?.length || 0} Required Skills
                    </span>
                    {biasReport && biasReport.bias_level && (
                      <span className={`px-2 py-0.5 rounded text-[11px] font-medium ${
                        (biasReport.bias_score || 0) > 0.05
                          ? "bg-rose-50 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400"
                          : "bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400"
                      }`}>
                        Bias Level: {String(biasReport.bias_level).toUpperCase()} ({biasReport.bias_score ?? 0})
                      </span>
                    )}
                  </div>
                </div>

                {blindData && (
                  <div className="flex items-center gap-3 px-3 py-2 rounded-xl bg-slate-50 dark:bg-white/[0.02] border border-slate-200 dark:border-white/[0.08]">
                    <div className="text-center">
                      <div className="text-[10px] text-slate-500 uppercase tracking-wider font-semibold">Fairness Index</div>
                      <div className="text-base font-bold text-indigo-600 dark:text-indigo-400">
                        {blindData.fairness_index} / 100
                      </div>
                    </div>
                    <div className="w-[1px] h-6 bg-slate-200 dark:bg-white/10"></div>
                    <div className="text-center">
                      <div className="text-[10px] text-slate-500 uppercase tracking-wider font-semibold">Rank Stability (ρ)</div>
                      <div className="text-base font-bold text-slate-800 dark:text-slate-200">
                        {blindData.rank_correlation}
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Navigation Tabs */}
              <div className="flex items-center gap-2 overflow-x-auto pt-4 text-xs font-semibold border-b border-slate-100 dark:border-white/[0.06] pb-2">
                <button
                  onClick={() => setActiveTab("ranking")}
                  className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center gap-1.5 ${
                    activeTab === "ranking"
                      ? "bg-indigo-600 text-white shadow-sm"
                      : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
                  }`}
                >
                  <BarChart3 className="w-3.5 h-3.5" />
                  Candidate Rankings ({matchResults.length})
                </button>

                <button
                  onClick={() => setActiveTab("blind")}
                  className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center gap-1.5 ${
                    activeTab === "blind"
                      ? "bg-indigo-600 text-white shadow-sm"
                      : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
                  }`}
                >
                  <EyeOff className="w-3.5 h-3.5" />
                  Blind Screening Comparison
                </button>

                <button
                  onClick={() => setActiveTab("bias")}
                  className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center gap-1.5 ${
                    activeTab === "bias"
                      ? "bg-indigo-600 text-white shadow-sm"
                      : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
                  }`}
                >
                  <ShieldAlert className="w-3.5 h-3.5" />
                  Bias Analysis Report
                </button>

                <button
                  onClick={() => setActiveTab("rewrite")}
                  className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center gap-1.5 ${
                    activeTab === "rewrite"
                      ? "bg-indigo-600 text-white shadow-sm"
                      : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
                  }`}
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  Inclusive Rewrite
                </button>

                <button
                  onClick={() => setActiveTab("upload")}
                  className={`px-3 py-1.5 rounded-lg transition-colors cursor-pointer flex items-center gap-1.5 ${
                    activeTab === "upload"
                      ? "bg-indigo-600 text-white shadow-sm"
                      : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white"
                  }`}
                >
                  <FileUp className="w-3.5 h-3.5" />
                  Upload Resumes ({resumes.length})
                </button>
              </div>

              {/* TAB 1: CANDIDATE RANKINGS */}
              {activeTab === "ranking" && (
                <div className="pt-4 space-y-4">
                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      <thead>
                        <tr className="border-b border-slate-200 dark:border-white/10 text-slate-400 dark:text-slate-500 font-semibold uppercase tracking-wider text-[10px]">
                          <th className="py-2.5 px-3">Normal Rank</th>
                          <th className="py-2.5 px-3">Candidate</th>
                          <th className="py-2.5 px-3">Keyword Score</th>
                          <th className="py-2.5 px-3">Semantic Score</th>
                          <th className="py-2.5 px-3">Final Score</th>
                          <th className="py-2.5 px-3">Matched Skills</th>
                          <th className="py-2.5 px-3">Skill Gap</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 dark:divide-white/[0.04]">
                        {matchResults.map((cand, idx) => (
                          <tr key={cand.id || idx} className="hover:bg-slate-50/50 dark:hover:bg-white/[0.01]">
                            <td className="py-3 px-3">
                              <span className="inline-flex items-center justify-center w-6 h-6 rounded-full font-bold bg-slate-100 dark:bg-white/[0.08] text-slate-800 dark:text-slate-200">
                                #{cand.normal_rank || idx + 1}
                              </span>
                            </td>
                            <td className="py-3 px-3">
                              <div className="font-semibold text-slate-900 dark:text-white">
                                {cand.candidate_name}
                              </div>
                              <div className="text-[10px] text-slate-500">
                                {cand.years_experience} yrs exp • {cand.candidate_email || "No email"}
                              </div>
                            </td>
                            <td className="py-3 px-3 font-mono font-medium text-amber-600 dark:text-amber-400">
                              {(cand.keyword_score * 100).toFixed(1)}%
                            </td>
                            <td className="py-3 px-3 font-mono font-medium text-indigo-600 dark:text-indigo-400">
                              {(cand.semantic_score * 100).toFixed(1)}%
                            </td>
                            <td className="py-3 px-3">
                              <div className="font-mono font-bold text-slate-900 dark:text-white">
                                {(cand.final_score * 100).toFixed(1)}%
                              </div>
                              <div className="w-16 h-1 bg-slate-100 dark:bg-white/10 rounded-full overflow-hidden mt-1">
                                <div
                                  className="h-full bg-indigo-600 rounded-full"
                                  style={{ width: `${Math.min(100, cand.final_score * 100)}%` }}
                                ></div>
                              </div>
                            </td>
                            <td className="py-3 px-3 max-w-[180px]">
                              <div className="flex flex-wrap gap-1">
                                {cand.matched_skills?.slice(0, 3).map((sk, i) => (
                                  <span key={i} className="px-1.5 py-0.5 rounded bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 text-[10px]">
                                    {sk}
                                  </span>
                                ))}
                                {cand.matched_skills?.length > 3 && (
                                  <span className="text-[10px] text-slate-400">+{cand.matched_skills.length - 3}</span>
                                )}
                              </div>
                            </td>
                            <td className="py-3 px-3 max-w-[180px]">
                              <div className="flex flex-wrap gap-1">
                                {cand.missing_skills?.slice(0, 2).map((sk, i) => (
                                  <span key={i} className="px-1.5 py-0.5 rounded bg-slate-100 dark:bg-white/[0.06] text-slate-500 text-[10px]">
                                    {sk}
                                  </span>
                                ))}
                                {cand.missing_skills?.length > 2 && (
                                  <span className="text-[10px] text-slate-400">+{cand.missing_skills.length - 2}</span>
                                )}
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* TAB 2: BLIND SCREENING COMPARISON */}
              {activeTab === "blind" && blindData && (
                <div className="pt-4 space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 rounded-xl bg-slate-50 dark:bg-white/[0.02] border border-slate-200 dark:border-white/10">
                    <div>
                      <div className="text-[11px] text-slate-500">Spearman Rank Correlation</div>
                      <div className="text-xl font-bold text-slate-900 dark:text-white mt-0.5">
                        ρ = {blindData.rank_correlation}
                      </div>
                      <p className="text-[10px] text-slate-400 mt-1">1.0 = identical; lower indicates blind identity changed rank.</p>
                    </div>
                    <div>
                      <div className="text-[11px] text-slate-500">Average Rank Shift</div>
                      <div className="text-xl font-bold text-amber-500 mt-0.5">
                        {blindData.avg_rank_shift} positions
                      </div>
                      <p className="text-[10px] text-slate-400 mt-1">{blindData.candidates_with_shift} of {blindData.total_candidates} candidates shifted</p>
                    </div>
                    <div>
                      <div className="text-[11px] text-slate-500">Fairness Index</div>
                      <div className="text-xl font-bold text-emerald-500 mt-0.5">
                        {blindData.fairness_index} / 100
                      </div>
                      <p className="text-[10px] text-slate-400 mt-1">Combines stability, bias reduction & readability.</p>
                    </div>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-left text-xs">
                      <thead>
                        <tr className="border-b border-slate-200 dark:border-white/10 text-slate-400 text-[10px] uppercase font-semibold">
                          <th className="py-2.5 px-3">Candidate</th>
                          <th className="py-2.5 px-3">Normal Rank</th>
                          <th className="py-2.5 px-3">Blind Rank (PII Stripped)</th>
                          <th className="py-2.5 px-3">Rank Delta / Shift</th>
                          <th className="py-2.5 px-3">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100 dark:divide-white/[0.04]">
                        {blindData.candidates.map((cand, idx) => {
                          const shift = cand.rank_shift || 0
                          const improved = cand.blind_rank < cand.normal_rank
                          return (
                            <tr key={idx} className="hover:bg-slate-50/50 dark:hover:bg-white/[0.01]">
                              <td className="py-3 px-3 font-semibold text-slate-900 dark:text-white">
                                {cand.candidate_name}
                              </td>
                              <td className="py-3 px-3 font-mono font-medium">
                                #{cand.normal_rank}
                              </td>
                              <td className="py-3 px-3 font-mono font-medium text-indigo-600 dark:text-indigo-400">
                                #{cand.blind_rank}
                              </td>
                              <td className="py-3 px-3 font-mono font-bold">
                                {shift === 0 ? (
                                  <span className="text-slate-400">0 (Unchanged)</span>
                                ) : improved ? (
                                  <span className="text-emerald-500 font-bold">+{shift} positions (Gained)</span>
                                ) : (
                                  <span className="text-rose-500 font-bold">-{shift} positions (Lowered)</span>
                                )}
                              </td>
                              <td className="py-3 px-3">
                                {shift === 0 ? (
                                  <span className="px-2 py-0.5 rounded text-[10px] bg-slate-100 dark:bg-white/[0.06] text-slate-600 dark:text-slate-300">
                                    Neutral
                                  </span>
                                ) : (
                                  <span className="px-2 py-0.5 rounded text-[10px] bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400">
                                    Identity-Sensitive
                                  </span>
                                )}
                              </td>
                            </tr>
                          )
                        })}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}

              {/* TAB 3: BIAS ANALYSIS REPORT */}
              {activeTab === "bias" && biasReport && (
                <div className="pt-4 space-y-4">
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 rounded-xl bg-slate-50 dark:bg-white/[0.02] border border-slate-200 dark:border-white/10">
                    <div>
                      <div className="text-[11px] text-slate-500">Overall Bias Score</div>
                      <div className="text-xl font-bold text-slate-900 dark:text-white mt-0.5">
                        {biasReport.bias_score}
                      </div>
                      <p className="text-[10px] text-slate-400 mt-1">Calculated via Gaucher et al. gendered word dictionary.</p>
                    </div>
                    <div>
                      <div className="text-[11px] text-slate-500">Readability (Flesch Score)</div>
                      <div className="text-xl font-bold text-indigo-500 mt-0.5">
                        {biasReport.readability?.flesch_score || 60} / 100
                      </div>
                      <p className="text-[10px] text-slate-400 mt-1">{biasReport.readability?.reading_ease || "Standard English"}</p>
                    </div>
                    <div>
                      <div className="text-[11px] text-slate-500">Flagged Exclusions</div>
                      <div className="text-xl font-bold text-rose-500 mt-0.5">
                        {biasReport.flagged_words?.length || 0} Terms
                      </div>
                      <p className="text-[10px] text-slate-400 mt-1">Potentially alienating to underrepresented groups.</p>
                    </div>
                  </div>

                  <div className="space-y-2">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                      Flagged Phrases in Job Description
                    </h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {biasReport.flagged_words?.map((item, idx) => (
                        <div key={idx} className="p-3 rounded-xl border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 flex items-start justify-between">
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-xs text-rose-600 dark:text-rose-400">
                                "{item.word}"
                              </span>
                              <span className="px-1.5 py-0.2 rounded text-[10px] font-mono bg-rose-50 dark:bg-rose-950/40 text-rose-500">
                                {item.category}
                              </span>
                            </div>
                            <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">
                              Suggestions: <span className="text-emerald-600 dark:text-emerald-400 font-semibold">{item.suggestions?.join(", ")}</span>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}

              {/* TAB 4: INCLUSIVE REWRITE */}
              {activeTab === "rewrite" && (
                <div className="pt-4 space-y-4">
                  {rewriteData ? (
                    <div className="space-y-4">
                      <div className="flex items-center justify-between p-3 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-800">
                        <div>
                          <span className="text-xs font-bold text-indigo-900 dark:text-indigo-200">
                            Inclusive Rewrite Ready ({rewriteData.replacements?.length || 0} neutral terms swapped)
                          </span>
                          <p className="text-[11px] text-indigo-700 dark:text-indigo-300 mt-0.5">
                            Bias Score reduced from {rewriteData.bias_score_before} to {rewriteData.bias_score_after} (Reduction: {rewriteData.bias_reduction})
                          </p>
                        </div>
                        <button
                          onClick={handleApplyRewrite}
                          disabled={rewriting}
                          className="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm transition-colors cursor-pointer"
                        >
                          {rewriting ? "Applying..." : "Apply to Job Description"}
                        </button>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="p-4 rounded-xl border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-2">
                          <div className="text-xs font-bold text-slate-500 uppercase">Original Job Description</div>
                          <div className="text-xs text-slate-700 dark:text-slate-300 whitespace-pre-wrap leading-relaxed">
                            {rewriteData.original_text}
                          </div>
                        </div>
                        <div className="p-4 rounded-xl border border-emerald-200 dark:border-emerald-800/60 bg-emerald-50/30 dark:bg-emerald-950/10 space-y-2">
                          <div className="text-xs font-bold text-emerald-600 dark:text-emerald-400 uppercase">Neutral & Inclusive Rewrite</div>
                          <div className="text-xs text-slate-800 dark:text-slate-200 whitespace-pre-wrap leading-relaxed">
                            {rewriteData.rewritten_text}
                          </div>
                        </div>
                      </div>
                    </div>
                  ) : (
                    <div className="text-center py-8 text-xs text-slate-500">
                      Generating inclusive rewrite suggestions...
                    </div>
                  )}
                </div>
              )}

              {/* TAB 5: UPLOAD RESUMES */}
              {activeTab === "upload" && (
                <div className="pt-4 space-y-6">
                  <form onSubmit={handleUploadResume} className="p-5 rounded-xl border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-4">
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                      Upload Candidate Resume (PDF, DOCX, TXT)
                    </h3>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                          Candidate Name (Optional)
                        </label>
                        <input
                          type="text"
                          placeholder="e.g. John Doe"
                          value={candidateName}
                          onChange={(e) => setCandidateName(e.target.value)}
                          className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 text-xs"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                          Candidate Email (Optional)
                        </label>
                        <input
                          type="email"
                          placeholder="e.g. john@example.com"
                          value={candidateEmail}
                          onChange={(e) => setCandidateEmail(e.target.value)}
                          className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 text-xs"
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                        Upload Document
                      </label>
                      <input
                        type="file"
                        accept=".pdf,.docx,.txt"
                        onChange={(e) => setUploadFile(e.target.files[0] || null)}
                        className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
                      />
                    </div>

                    <div className="relative flex py-1 items-center">
                      <div className="flex-grow border-t border-slate-200 dark:border-white/10"></div>
                      <span className="flex-shrink mx-4 text-[10px] text-slate-400 uppercase">Or Paste Plain Resume Text</span>
                      <div className="flex-grow border-t border-slate-200 dark:border-white/10"></div>
                    </div>

                    <div>
                      <textarea
                        rows="4"
                        placeholder="Paste resume content here..."
                        value={uploadText}
                        onChange={(e) => setUploadText(e.target.value)}
                        className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 text-xs"
                      ></textarea>
                    </div>

                    <div className="flex items-center justify-between">
                      {uploadSuccess && (
                        <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">
                          {uploadSuccess}
                        </span>
                      )}
                      <button
                        type="submit"
                        disabled={uploading}
                        className="ml-auto px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm transition-colors cursor-pointer"
                      >
                        {uploading ? "Parsing & Anonymizing..." : "Parse & Save to Candidate Pool"}
                      </button>
                    </div>
                  </form>

                  {/* List of existing resumes */}
                  <div className="space-y-3">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-500">
                      Indexed Candidate Pool ({resumes.length})
                    </h4>
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-3">
                      {resumes.map(r => (
                        <div key={r.id} className="p-3 rounded-xl border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 space-y-1.5">
                          <div className="font-semibold text-xs text-slate-900 dark:text-white">
                            {r.candidate_name}
                          </div>
                          <div className="text-[10px] text-slate-500">
                            {r.years_experience} yrs exp • {r.skills?.length || 0} skills found
                          </div>
                          <div className="flex flex-wrap gap-1 mt-1">
                            {r.skills?.slice(0, 3).map((sk, i) => (
                              <span key={i} className="px-1.5 py-0.5 rounded bg-slate-100 dark:bg-white/[0.06] text-[9px] text-slate-600 dark:text-slate-300">
                                {sk}
                              </span>
                            ))}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* CREATE JOB MODAL */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-xl rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-100 dark:border-white/10 pb-3">
              <h3 className="font-bold text-base text-slate-900 dark:text-white">
                Post New Job Description
              </h3>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-white"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateJob} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Job Title
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Senior Backend Engineer (Python/Cloud)"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Job Description & Roles
                </label>
                <textarea
                  required
                  rows="4"
                  placeholder="Paste or write the job description..."
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
                ></textarea>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Technical Requirements & Skills
                </label>
                <textarea
                  rows="2"
                  placeholder="e.g. Python, SQL, REST APIs, Docker, AWS, Team collaboration..."
                  value={newRequirements}
                  onChange={(e) => setNewRequirements(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
                ></textarea>
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 rounded-lg border border-slate-200 dark:border-white/10 text-xs font-semibold text-slate-600 dark:text-slate-300"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={creatingJob}
                  className="px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm"
                >
                  {creatingJob ? "Extracting Skills & Creating..." : "Save Job Description"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
