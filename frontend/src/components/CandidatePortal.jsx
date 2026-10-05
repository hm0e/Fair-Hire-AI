import React, { useState, useEffect } from "react"
import { UserCheck, FileUp, Sparkles, CheckCircle2, AlertCircle, ArrowRight, BookOpen, Briefcase } from "lucide-react"

export function CandidatePortal() {
  const [jobs, setJobs] = useState([])
  const [selectedJobId, setSelectedJobId] = useState("")
  const [candidateName, setCandidateName] = useState("")
  const [candidateEmail, setCandidateEmail] = useState("")
  const [resumeText, setResumeText] = useState("")
  const [uploadFile, setUploadFile] = useState(null)
  const [evaluating, setEvaluating] = useState(false)
  const [matchResult, setMatchResult] = useState(null)
  const [errorMsg, setErrorMsg] = useState(null)

  useEffect(() => {
    fetch("/api/jobs")
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          setJobs(data)
          if (data.length > 0) setSelectedJobId(data[0].id)
        }
      })
      .catch(err => console.error(err))
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!selectedJobId) {
      setErrorMsg("Please select a target job position.")
      return
    }
    setEvaluating(true)
    setErrorMsg(null)
    setMatchResult(null)

    try {
      // 1. Upload/parse resume
      let parseRes
      if (uploadFile) {
        const formData = new FormData()
        formData.append("file", uploadFile)
        if (candidateName) formData.append("name", candidateName)
        if (candidateEmail) formData.append("email", candidateEmail)
        parseRes = await fetch("/api/resumes", {
          method: "POST",
          body: formData
        })
      } else if (resumeText.trim()) {
        parseRes = await fetch("/api/resumes", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            raw_text: resumeText,
            name: candidateName || "Job Applicant",
            email: candidateEmail || "applicant@example.com"
          })
        })
      } else {
        setErrorMsg("Please provide a resume document or paste your resume text.")
        setEvaluating(false)
        return
      }

      if (!parseRes.ok) {
        const err = await parseRes.json()
        setErrorMsg(err.error || "Failed to parse resume.")
        setEvaluating(false)
        return
      }

      const parseData = await parseRes.json()
      const resume = parseData.resume

      // 2. Run matching for the chosen job to calculate scorecard
      await fetch(`/api/matching/${selectedJobId}`, { method: "POST" })
      const resultsRes = await fetch(`/api/matching/${selectedJobId}/results`)
      const resultsData = await resultsRes.json()

      const found = resultsData.results?.find(r => r.resume_id === resume.id)
      if (found) {
        setMatchResult(found)
      } else if (resultsData.results?.length > 0) {
        setMatchResult(resultsData.results[0])
      }
    } catch (err) {
      setErrorMsg("Network error connecting to evaluation service.")
    } finally {
      setEvaluating(false)
    }
  }

  const selectedJobObj = jobs.find(j => j.id === Number(selectedJobId))

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">
      {/* Header */}
      <div className="text-center max-w-2xl mx-auto space-y-2">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-300 text-xs font-semibold">
          <UserCheck className="w-3.5 h-3.5" />
          Candidate Job Seeker Portal
        </div>
        <h1 className="text-3xl font-bold font-heading text-slate-900 dark:text-white">
          Check Your Job Fit & Fair ATS Feedback
        </h1>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          Upload your resume to receive an unbiased, transparent match score, see skills you matched, and discover areas to improve.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-12 gap-8 items-start">
        {/* Left Form */}
        <div className="md:col-span-7 bg-white dark:bg-slate-900/80 p-6 rounded-2xl border border-slate-200 dark:border-white/10 shadow-sm space-y-5">
          <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <FileUp className="w-4 h-4 text-indigo-500" />
            Submit Profile for Review
          </h2>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Target Job Position
              </label>
              <select
                value={selectedJobId}
                onChange={(e) => setSelectedJobId(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
              >
                {jobs.map(j => (
                  <option key={j.id} value={j.id}>
                    {j.title} (ID #{j.id})
                  </option>
                ))}
              </select>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                  Full Name
                </label>
                <input
                  type="text"
                  placeholder="e.g. Maya Sharma"
                  value={candidateName}
                  onChange={(e) => setCandidateName(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 text-xs text-slate-900 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                  Email Address
                </label>
                <input
                  type="email"
                  placeholder="e.g. maya@example.com"
                  value={candidateEmail}
                  onChange={(e) => setCandidateEmail(e.target.value)}
                  className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 text-xs text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                Resume File (.pdf, .docx, .txt)
              </label>
              <input
                type="file"
                accept=".pdf,.docx,.txt"
                onChange={(e) => setUploadFile(e.target.files[0] || null)}
                className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-indigo-50 file:text-indigo-700 hover:file:bg-indigo-100"
              />
            </div>

            <div>
              <label className="block text-xs font-medium text-slate-600 dark:text-slate-400 mb-1">
                Or Paste Resume Text
              </label>
              <textarea
                rows="4"
                placeholder="Paste key experience, skills, projects, and education..."
                value={resumeText}
                onChange={(e) => setResumeText(e.target.value)}
                className="w-full px-3 py-2 rounded-lg border border-slate-200 dark:border-white/10 bg-white dark:bg-slate-900 text-xs text-slate-900 dark:text-white"
              ></textarea>
            </div>

            {errorMsg && (
              <div className="p-3 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-600 dark:text-rose-400">
                {errorMsg}
              </div>
            )}

            <button
              type="submit"
              disabled={evaluating}
              className="w-full py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold shadow-sm transition-colors cursor-pointer"
            >
              {evaluating ? "Evaluating Fit Against Job..." : "Evaluate Match & Get Feedback"}
            </button>
          </form>
        </div>

        {/* Right Result Card */}
        <div className="md:col-span-5 space-y-4">
          {matchResult ? (
            <div className="bg-white dark:bg-slate-900/80 p-6 rounded-2xl border border-slate-200 dark:border-white/10 shadow-sm space-y-5 animate-in fade-in duration-300">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-white/10">
                <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Feedback Scorecard
                </span>
                <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-indigo-50 dark:bg-indigo-950/50 text-indigo-600 dark:text-indigo-400">
                  Rank #{matchResult.normal_rank || 1}
                </span>
              </div>

              <div className="text-center py-2 space-y-1">
                <div className="text-4xl font-extrabold font-heading text-indigo-600 dark:text-indigo-400">
                  {(matchResult.final_score * 100).toFixed(0)}%
                </div>
                <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                  Overall Composite Match Score
                </div>
                <p className="text-[11px] text-slate-500">
                  Based on keyword ATS skills and S-BERT semantic similarity.
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3 text-center">
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-white/[0.02] border border-slate-200 dark:border-white/10">
                  <div className="text-lg font-bold text-amber-500">
                    {(matchResult.keyword_score * 100).toFixed(0)}%
                  </div>
                  <div className="text-[10px] text-slate-500 uppercase font-semibold">Keyword ATS</div>
                </div>
                <div className="p-3 rounded-xl bg-slate-50 dark:bg-white/[0.02] border border-slate-200 dark:border-white/10">
                  <div className="text-lg font-bold text-indigo-500">
                    {(matchResult.semantic_score * 100).toFixed(0)}%
                  </div>
                  <div className="text-[10px] text-slate-500 uppercase font-semibold">Semantic Fit</div>
                </div>
              </div>

              {/* Matched Skills */}
              <div className="space-y-2">
                <div className="flex items-center gap-1.5 text-xs font-bold text-emerald-600 dark:text-emerald-400">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  Matched Skills ({matchResult.matched_skills?.length || 0})
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {matchResult.matched_skills?.map((sk, i) => (
                    <span key={i} className="px-2 py-0.5 rounded-md bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 text-xs font-medium">
                      {sk}
                    </span>
                  ))}
                  {(!matchResult.matched_skills || matchResult.matched_skills.length === 0) && (
                    <span className="text-xs text-slate-400">None detected</span>
                  )}
                </div>
              </div>

              {/* Missing Skills */}
              <div className="space-y-2">
                <div className="flex items-center gap-1.5 text-xs font-bold text-rose-600 dark:text-rose-400">
                  <AlertCircle className="w-3.5 h-3.5" />
                  Skill Gaps to Address ({matchResult.missing_skills?.length || 0})
                </div>
                <div className="flex flex-wrap gap-1.5">
                  {matchResult.missing_skills?.map((sk, i) => (
                    <span key={i} className="px-2 py-0.5 rounded-md bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 text-xs font-medium">
                      {sk}
                    </span>
                  ))}
                </div>
                <p className="text-[11px] text-slate-500 mt-2 leading-relaxed">
                  Adding projects or certifications covering these missing skills will significantly increase your ATS matching rank.
                </p>
              </div>
            </div>
          ) : (
            <div className="bg-slate-50 dark:bg-white/[0.02] p-6 rounded-2xl border border-dashed border-slate-300 dark:border-white/10 text-center space-y-3 py-12">
              <Sparkles className="w-8 h-8 text-indigo-400 mx-auto" />
              <h3 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                Transparent Evaluation Scorecard
              </h3>
              <p className="text-xs text-slate-500 max-w-xs mx-auto">
                Submit your resume on the left to see your match score, skill breakdown, and personalized improvement tips.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
