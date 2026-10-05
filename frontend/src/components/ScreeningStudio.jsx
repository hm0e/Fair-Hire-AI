import React, { useState, useEffect } from "react"
import { 
  FileText, Users, UploadCloud, ArrowRight, Sparkles, 
  Check, AlertCircle, File, Trash2, Cpu, Sliders, ShieldCheck,
  Zap, RefreshCw, CheckCircle2, ShieldAlert
} from "lucide-react"
import { Button } from "./ui/button"
import { Card } from "./ui/card"
import { Badge } from "./ui/badge"

export function ScreeningStudio({ onScreeningComplete, prefilledSampleIndex }) {
  const [sampleJDs, setSampleJDs] = useState([])
  const [selectedPresetIndex, setSelectedPresetIndex] = useState(null)
  const [jdTitle, setJdTitle] = useState("")
  const [jdText, setJdText] = useState("")
  const [useSampleResumes, setUseSampleResumes] = useState(true)
  const [uploadedFiles, setUploadedFiles] = useState([])
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [processingStep, setProcessingStep] = useState("")
  const [processingProgress, setProcessingProgress] = useState(15)
  const [errorMsg, setErrorMsg] = useState("")
  
  // Real-time JD analysis
  const [liveAnalysis, setLiveAnalysis] = useState(null)

  // Fetch sample JDs on mount
  useEffect(() => {
    fetch("/api/sample-jds")
      .then(res => res.json())
      .then(data => {
        setSampleJDs(data)
        if (prefilledSampleIndex !== null && data[prefilledSampleIndex]) {
          selectPreset(data, prefilledSampleIndex)
        } else {
          const savedCustom = sessionStorage.getItem("fairhire_custom_jd_text")
          if (savedCustom) {
            setJdTitle("Custom Sourced Role")
            setJdText(savedCustom)
            sessionStorage.removeItem("fairhire_custom_jd_text")
          }
        }
      })
      .catch(err => console.error("Failed to fetch sample JDs:", err))
  }, [prefilledSampleIndex])

  // Live analysis of JD text with 500ms debounce and AbortController
  useEffect(() => {
    if (!jdText.trim()) {
      setLiveAnalysis(null)
      return
    }

    const controller = new AbortController()
    const timer = setTimeout(async () => {
      try {
        const res = await fetch("/api/analyze-jd", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ jd_text: jdText }),
          signal: controller.signal
        })
        if (res.ok) {
          const data = await res.json()
          setLiveAnalysis(data)
        }
      } catch (err) {
        if (err.name !== "AbortError") {
          console.error("Live analysis error:", err)
        }
      }
    }, 500)

    return () => {
      clearTimeout(timer)
      controller.abort()
    }
  }, [jdText])

  const selectPreset = (list, idx) => {
    setSelectedPresetIndex(idx)
    const target = list[idx]
    if (target) {
      setJdTitle(target.title)
      setJdText(target.text)
    }
  }

  // 1-Click word neutralizer with regex escaping
  const replaceFlaggedWord = (originalWord, replacement) => {
    if (!originalWord || !replacement) return
    const escaped = originalWord.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
    const regex = new RegExp(`\\b${escaped}\\b`, "gi")
    setJdText(prev => prev.replace(regex, replacement))
  }

  const handleFileUpload = (e) => {
    if (e.target.files) {
      const filesArr = Array.from(e.target.files)
      setUploadedFiles(prev => [...prev, ...filesArr])
    }
  }

  const removeFile = (idx) => {
    setUploadedFiles(prev => prev.filter((_, i) => i !== idx))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setErrorMsg("")

    if (!jdText.trim()) {
      setErrorMsg("Please provide a valid Job Description.")
      return
    }
    if (!useSampleResumes && uploadedFiles.length === 0) {
      setErrorMsg("Please select the bundled candidate resumes or upload at least one candidate file.")
      return
    }

    setIsSubmitting(true)
    setProcessingProgress(20)
    setProcessingStep("Ingesting candidate corpus & resolving file buffers...")

    const formData = new FormData()
    formData.append("jd_title", jdTitle || "Untitled Role")
    formData.append("jd_text", jdText)
    formData.append("use_sample_resumes", useSampleResumes ? "true" : "false")

    uploadedFiles.forEach(file => {
      formData.append("resume_files", file)
    })

    const steps = [
      { step: "Ingesting candidate corpus & resolving file buffers...", progress: 25 },
      { step: "Executing spaCy NLP & stripping demographic PII tokens...", progress: 50 },
      { step: "Generating 384-dimensional Sentence-BERT embeddings...", progress: 75 },
      { step: "Auditing EEOC Four-Fifths Adverse Impact & Spearman Rank Correlation...", progress: 95 }
    ]
    let stepIndex = 0
    const stepInterval = setInterval(() => {
      stepIndex = (stepIndex + 1) % steps.length
      setProcessingStep(steps[stepIndex].step)
      setProcessingProgress(steps[stepIndex].progress)
    }, 1100)

    try {
      const res = await fetch("/api/run-comparison", {
        method: "POST",
        body: formData,
      })
      const data = await res.json()
      clearInterval(stepInterval)

      if (!res.ok) {
        setErrorMsg(data.error || "Failed to execute comparison.")
        setIsSubmitting(false)
        return
      }

      setProcessingProgress(100)
      setProcessingStep("Audit Dossier generated! Loading results...")
      setTimeout(() => {
        if (onScreeningComplete) {
          onScreeningComplete(data.run_id)
        }
      }, 400)
    } catch (err) {
      clearInterval(stepInterval)
      console.error(err)
      setErrorMsg("Network error communicating with the REST API backend.")
      setIsSubmitting(false)
    }
  }

  const wordCount = jdText.trim() ? jdText.trim().split(/\s+/).length : 0

  return (
    <div className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8 space-y-8">
      
      {/* Studio Header */}
      <div className="pb-6 border-b border-slate-200 dark:border-white/[0.08] flex flex-col md:flex-row md:items-end justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1.5">
            <Badge variant="default">Recruitment Workbench</Badge>
            <span className="text-xs text-slate-500 dark:text-slate-400 font-mono">&bull; 3-Engine Calibration</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight font-heading">
            Candidate Screening Studio
          </h1>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 mt-1 max-w-2xl leading-relaxed">
            Configure job requirements, select candidate resumes, and trigger simultaneous benchmarking across Keyword ATS, Neural S-BERT, and Blind Screening.
          </p>
        </div>

        <div className="flex items-center gap-2 text-xs font-mono text-slate-500 dark:text-slate-400">
          <span>Engine Model:</span>
          <span className="text-indigo-600 dark:text-cyan-400 font-bold bg-slate-100 dark:bg-white/[0.04] px-2 py-0.5 rounded border border-slate-200 dark:border-white/10">
            all-MiniLM-L6-v2
          </span>
        </div>
      </div>

      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-50 dark:bg-rose-500/10 border border-rose-200 dark:border-rose-500/30 flex items-center gap-3 text-rose-700 dark:text-rose-300 text-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-8">
        
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* ========================================================================= */}
          {/* LEFT COLUMN: Job Specification (7 Cols)                                   */}
          {/* ========================================================================= */}
          <div className="lg:col-span-7 space-y-6">
            <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-5">
              
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-white/[0.06]">
                <div className="flex items-center gap-2.5">
                  <div className="w-7 h-7 rounded-lg bg-indigo-50 dark:bg-indigo-500/15 flex items-center justify-center text-indigo-600 dark:text-indigo-400 font-bold text-xs">
                    1
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white font-heading">Job Description Specification</h3>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">Target role requirements and required technical duties</p>
                  </div>
                </div>

                <Badge variant={selectedPresetIndex !== null ? "cyan" : "outline"} className="text-[10px]">
                  {selectedPresetIndex !== null ? "Preset Loaded" : "Custom Draft"}
                </Badge>
              </div>

              {/* Pre-calibrated Sample Roles */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 block">
                  Load Pre-Calibrated Benchmark Roles (Optional):
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  {sampleJDs.map((jd, idx) => (
                    <div
                      key={idx}
                      onClick={() => selectPreset(sampleJDs, idx)}
                      className={`p-3 rounded-xl border text-left cursor-pointer transition-colors ${
                        selectedPresetIndex === idx
                          ? "bg-indigo-50 border-indigo-400 text-indigo-950 dark:bg-indigo-600/15 dark:border-indigo-500 dark:text-white shadow-sm"
                          : "bg-slate-50 border-slate-200 hover:border-slate-300 text-slate-700 dark:bg-slate-950/70 dark:border-white/[0.08] dark:hover:border-slate-600 dark:text-slate-300"
                      }`}
                    >
                      <div className="text-xs font-bold truncate mb-0.5">
                        {jd.title}
                      </div>
                      <div className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-2 leading-snug">
                        {jd.text}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Role Title */}
              <div className="space-y-1.5">
                <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 block">
                  Role Title / Identifier:
                </label>
                <input
                  type="text"
                  required
                  value={jdTitle}
                  onChange={(e) => setJdTitle(e.target.value)}
                  placeholder="e.g. Senior Backend Python Developer"
                  className="w-full bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/10 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 dark:text-slate-200 focus:outline-none focus:border-indigo-500 font-sans shadow-inner"
                />
              </div>

              {/* Job Requirements Text */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <label className="font-semibold text-slate-700 dark:text-slate-300">
                    Job Requirements Body:
                  </label>
                  <div className="flex items-center gap-2 font-mono text-[11px]">
                    <span className="text-slate-500 dark:text-slate-400">{wordCount} words</span>
                    {(() => {
                      const lean = liveAnalysis?.gender_lean || liveAnalysis?.lean || liveAnalysis?.gender_bias?.lean;
                      if (!lean) return null;
                      return (
                        <Badge 
                          variant={String(lean).toLowerCase().includes("masculine") ? "danger" : String(lean).toLowerCase().includes("feminine") ? "purple" : "success"}
                          className="text-[10px]"
                        >
                          {String(lean).toUpperCase()} LEAN
                        </Badge>
                      );
                    })()}
                  </div>
                </div>

                <textarea
                  required
                  rows={6}
                  value={jdText}
                  onChange={(e) => {
                    setJdText(e.target.value)
                    setSelectedPresetIndex(null)
                  }}
                  placeholder="Paste the full job requirements or duties here..."
                  className="w-full bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/10 rounded-xl p-3.5 text-xs text-slate-900 dark:text-slate-200 focus:outline-none focus:border-indigo-500 font-sans leading-relaxed resize-none shadow-inner"
                />
              </div>

              {/* In-place Word Neutralizer */}
              {liveAnalysis && liveAnalysis.flagged_terms && liveAnalysis.flagged_terms.length > 0 && (
                <div className="p-3.5 rounded-xl bg-amber-50/60 dark:bg-slate-950 border border-amber-200 dark:border-amber-500/25 space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-semibold text-amber-800 dark:text-amber-300 flex items-center gap-1.5">
                      <ShieldAlert className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400" />
                      1-Click Word Neutralizer ({liveAnalysis.flagged_terms.length} flagged):
                    </span>
                    <span className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">Click word to swap</span>
                  </div>

                  <div className="flex flex-wrap gap-1.5">
                    {liveAnalysis.flagged_terms.map((t, i) => (
                      <button
                        key={i}
                        type="button"
                        onClick={() => replaceFlaggedWord(t.word, t.neutral_alternative)}
                        className="px-2.5 py-1 rounded-lg bg-white dark:bg-slate-900 hover:bg-emerald-50 dark:hover:bg-emerald-950 border border-slate-200 dark:border-white/10 hover:border-emerald-400 text-xs text-slate-700 dark:text-slate-300 hover:text-emerald-700 dark:hover:text-emerald-300 font-mono transition-all flex items-center gap-1 cursor-pointer shadow-sm"
                        title={`Replace '${t.word}' with '${t.neutral_alternative}'`}
                      >
                        <span className="line-through text-rose-600 dark:text-rose-400">{t.word}</span>
                        <span>&rarr;</span>
                        <span className="text-emerald-600 dark:text-emerald-400 font-bold">{t.neutral_alternative}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {/* Detected Skills */}
              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950/60 border border-slate-200 dark:border-white/[0.05] space-y-1.5">
                <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400 font-mono">
                  <span>Detected Technical Skills:</span>
                  <span>{liveAnalysis?.readability?.level ? `Readability: ${liveAnalysis.readability.level}` : "Parsing..."}</span>
                </div>
                <div className="flex flex-wrap gap-1">
                  {liveAnalysis?.skills && liveAnalysis.skills.length > 0 ? (
                    liveAnalysis.skills.map((s, i) => (
                      <Badge key={i} variant="default" className="text-[10px] font-mono">{s}</Badge>
                    ))
                  ) : (
                    <span className="text-[11px] text-slate-400 font-mono">Skills detected in real time...</span>
                  )}
                </div>
              </div>

            </div>
          </div>

          {/* ========================================================================= */}
          {/* RIGHT COLUMN: Candidate Pool (5 Cols)                                     */}
          {/* ========================================================================= */}
          <div className="lg:col-span-5 space-y-6">
            <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-5">
              
              <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-white/[0.06]">
                <div className="flex items-center gap-2.5">
                  <div className="w-7 h-7 rounded-lg bg-emerald-50 dark:bg-emerald-500/15 flex items-center justify-center text-emerald-600 dark:text-emerald-400 font-bold text-xs">
                    2
                  </div>
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 dark:text-white font-heading">Candidate Talent Pool</h3>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400">Roster to evaluate across the 3 engines</p>
                  </div>
                </div>

                <Badge variant="success" className="text-[10px]">
                  {useSampleResumes ? 4 + uploadedFiles.length : uploadedFiles.length} Profiles Ready
                </Badge>
              </div>

              {/* Bundled Candidates Card */}
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] space-y-2.5">
                <label className="flex items-start gap-2.5 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={useSampleResumes}
                    onChange={(e) => setUseSampleResumes(e.target.checked)}
                    className="mt-0.5 w-4 h-4 rounded text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                  />
                  <div>
                    <div className="text-xs font-bold text-slate-900 dark:text-white">Include Curated Benchmark Roster (4 Profiles)</div>
                    <div className="text-[11px] text-slate-500 dark:text-slate-400 leading-snug">
                      Diverse test profiles with varied tech stacks and demographic signals to benchmark rank shifts.
                    </div>
                  </div>
                </label>

                {useSampleResumes && (
                  <div className="grid grid-cols-2 gap-2 pt-2 border-t border-slate-200 dark:border-white/[0.05]">
                    <div className="p-2 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-transparent text-xs shadow-xs">
                      <div className="font-semibold text-slate-800 dark:text-slate-200">Aarav Sharma</div>
                      <div className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">2y &bull; Python, SQL</div>
                    </div>
                    <div className="p-2 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-transparent text-xs shadow-xs">
                      <div className="font-semibold text-slate-800 dark:text-slate-200">Priya Patel</div>
                      <div className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">3y &bull; Django, REST</div>
                    </div>
                    <div className="p-2 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-transparent text-xs shadow-xs">
                      <div className="font-semibold text-slate-800 dark:text-slate-200">Rohan Joshi</div>
                      <div className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">1y &bull; Java, Spring</div>
                    </div>
                    <div className="p-2 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-transparent text-xs shadow-xs">
                      <div className="font-semibold text-slate-800 dark:text-slate-200">Ananya Deshmukh</div>
                      <div className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">4y &bull; Python, ML, AWS</div>
                    </div>
                  </div>
                )}
              </div>

              {/* Upload Additional Resumes */}
              <div className="space-y-2">
                <label className="text-xs font-semibold text-slate-700 dark:text-slate-300 block">
                  Upload Additional Resumes (PDF, DOCX, TXT):
                </label>

                <label className="border border-dashed border-slate-300 dark:border-white/20 hover:border-indigo-500 dark:hover:border-indigo-500 rounded-xl p-5 flex flex-col items-center justify-center gap-1.5 cursor-pointer bg-slate-50/50 hover:bg-slate-50 dark:bg-slate-950/50 dark:hover:bg-slate-950 transition-colors">
                  <UploadCloud className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                  <span className="text-xs font-semibold text-slate-900 dark:text-white">Click to browse or drop files</span>
                  <span className="text-[10px] text-slate-500 font-mono">Supports standard resume formats</span>
                  <input
                    type="file"
                    multiple
                    accept=".pdf,.docx,.txt"
                    onChange={handleFileUpload}
                    className="hidden"
                  />
                </label>

                {uploadedFiles.length > 0 && (
                  <div className="space-y-1 max-h-32 overflow-y-auto pr-1">
                    {uploadedFiles.map((file, idx) => (
                      <div
                        key={idx}
                        className="flex items-center justify-between p-2 rounded-lg bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] text-xs"
                      >
                        <div className="flex items-center gap-2 truncate">
                          <File className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400 flex-shrink-0" />
                          <span className="truncate text-slate-700 dark:text-slate-200">{file.name}</span>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeFile(idx)}
                          className="text-slate-400 hover:text-rose-500 p-1 cursor-pointer"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Engine Checklist */}
              <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.06] space-y-2">
                <span className="text-[11px] font-bold uppercase text-slate-500 dark:text-slate-400 font-mono block">
                  Engines Executing in Parallel:
                </span>
                <div className="space-y-1.5 text-xs text-slate-700 dark:text-slate-300">
                  <div className="flex items-center justify-between">
                    <span>1. Keyword ATS (Baseline)</span>
                    <Badge variant="warning" className="text-[9px]">Active</Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>2. S-BERT Semantic Matcher</span>
                    <Badge variant="cyan" className="text-[9px]">Active</Badge>
                  </div>
                  <div className="flex items-center justify-between">
                    <span>3. Identity-Blind AI Shield</span>
                    <Badge variant="success" className="text-[9px]">Active</Badge>
                  </div>
                </div>
              </div>

              {/* Submit CTA */}
              <Button
                type="submit"
                variant="default"
                size="lg"
                disabled={isSubmitting}
                className="w-full h-12 rounded-xl text-sm font-semibold shadow-md flex items-center justify-center gap-2"
              >
                <span>Run Triple-Engine Evaluation</span>
                <ArrowRight className="w-4 h-4" />
              </Button>

            </div>
          </div>

        </div>

      </form>

      {/* Clean Enterprise Progress Modal */}
      {isSubmitting && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-xs p-4">
          <div className="max-w-md w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/20 p-6 rounded-2xl text-center space-y-4 shadow-2xl">
            <div className="w-12 h-12 rounded-xl bg-indigo-600 flex items-center justify-center mx-auto text-white">
              <Cpu className="w-6 h-6 animate-pulse" />
            </div>

            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white font-heading">
                Executing Evaluation Pipeline
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 min-h-[32px] px-2 font-mono">
                {processingStep}
              </p>
            </div>

            <div className="w-full bg-slate-100 dark:bg-slate-950 rounded-full h-2 overflow-hidden border border-slate-200 dark:border-white/10">
              <div 
                className="h-full bg-indigo-600 rounded-full transition-all duration-700"
                style={{ width: `${processingProgress}%` }}
              />
            </div>

            <div className="flex items-center justify-between text-xs text-slate-500 font-mono">
              <span>Fairness Engine</span>
              <span>{processingProgress}%</span>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}
