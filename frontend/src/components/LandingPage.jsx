import React, { useState, useEffect } from "react"
import { 
  ArrowRight, CheckCircle2, XCircle, ShieldCheck, 
  FileText, Users, Cpu, Sparkles, Scale, RefreshCw,
  Search, Check, AlertCircle, ArrowUpRight, BarChart3,
  BookOpen, SlidersHorizontal, EyeOff, FileCheck2
} from "lucide-react"
import { Button } from "./ui/button"
import { Card } from "./ui/card"
import { Badge } from "./ui/badge"

export function LandingPage({ setActiveView, onSelectPreset }) {
  // Interactive demo state
  const [activePreset, setActivePreset] = useState("biased")
  const [scannerText, setScannerText] = useState(
    "We are looking for an aggressive, dominant rockstar developer who is fearless and headstrong enough to lead our backend team single-handedly. Must be a competitive ninja with strong Python and SQL skills."
  )
  const [scanResult, setScanResult] = useState(null)
  const [isScanning, setIsScanning] = useState(false)

  // Real-time API scan
  useEffect(() => {
    const timer = setTimeout(async () => {
      if (!scannerText.trim()) {
        setScanResult(null)
        return
      }
      setIsScanning(true)
      try {
        const res = await fetch("/api/bias/analyze", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ text: scannerText, jd_text: scannerText })
        })
        if (res.ok) {
          const data = await res.json()
          setScanResult(data)
        }
      } catch (err) {
        console.error("Scanner error:", err)
      } finally {
        setIsScanning(false)
      }
    }, 250)

    return () => clearTimeout(timer)
  }, [scannerText])

  const loadPreset = (type) => {
    setActivePreset(type)
    if (type === "biased") {
      setScannerText(
        "We are looking for an aggressive, dominant rockstar developer who is fearless and headstrong enough to lead our backend team single-handedly. Must be a competitive ninja with strong Python and SQL skills."
      )
    } else if (type === "neutral") {
      setScannerText(
        "We are looking for a proactive, skilled professional who is confident and focused enough to guide our backend team. Must be a goal-oriented expert with strong Python and SQL skills."
      )
    } else if (type === "analyst") {
      setScannerText(
        "We need a collaborative, supportive data analyst who is understanding and dependable when mentoring junior teammates. Requires SQL, Python, and Tableau."
      )
    }
  }

  // 1-Click Replace in demo text
  const replaceWord = (word, replacement) => {
    if (!word || !replacement) return
    const regex = new RegExp(`\\b${word}\\b`, "gi")
    setScannerText(prev => prev.replace(regex, replacement))
  }

  return (
    <div className="flex flex-col gap-24 sm:gap-32 py-10 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto">
      
      {/* ========================================================================= */}
      {/* 1. HERO SECTION: CRISP, PROFESSIONAL, PRODUCT-FIRST                       */}
      {/* ========================================================================= */}
      <section className="text-center pt-6 sm:pt-10">
        
        {/* Product Identity Pill */}
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full border border-indigo-200 dark:border-indigo-500/30 bg-indigo-50 dark:bg-indigo-500/10 text-indigo-700 dark:text-indigo-300 text-xs font-semibold mb-6">
          <ShieldCheck className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
          <span>FairHire AI &bull; Bias-Free Resume Screening &amp; Compliance Software</span>
        </div>

        {/* Clear, Unambiguous Headline */}
        <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-slate-900 dark:text-white max-w-4xl mx-auto leading-[1.12] mb-6 font-heading">
          Hire on Pure Skill.<br />
          <span className="text-indigo-600 dark:text-indigo-400">
            Eliminate Bias from Resume Screening.
          </span>
        </h1>

        {/* Crystal-Clear Value Proposition */}
        <p className="text-base sm:text-lg text-slate-600 dark:text-slate-300 max-w-3xl mx-auto mb-10 leading-relaxed font-normal">
          Traditional ATS tools reject qualified talent over missing buzzwords, while unconscious bias creates legal risk under EEOC rules.
          <strong className="text-slate-900 dark:text-white font-semibold"> FairHire AI neutralizes biased job descriptions, redacts candidate identities (names, genders, ages) for blind screening, and matches real competencies using neural semantic AI.</strong>
        </p>

        {/* Action Buttons */}
        <div className="flex flex-wrap items-center justify-center gap-4 mb-14">
          <Button
            size="lg"
            variant="default"
            onClick={() => setActiveView("studio")}
            className="flex items-center gap-2 text-sm px-6 h-12 rounded-xl shadow-md font-semibold"
          >
            <span>Launch Screening Studio</span>
            <ArrowRight className="w-4 h-4" />
          </Button>

          <a
            href="#interactive-demo"
            className="inline-flex items-center gap-2 text-sm px-6 h-12 rounded-xl bg-white hover:bg-slate-100 text-slate-700 border border-slate-300 dark:bg-slate-800 dark:hover:bg-slate-700 dark:text-slate-200 dark:border-slate-700 transition-colors font-semibold shadow-sm"
          >
            <span>See Live Interactive Demo &darr;</span>
          </a>

          <Button
            size="lg"
            variant="outline"
            onClick={() => setActiveView("history")}
            className="flex items-center gap-2 text-sm px-6 h-12 rounded-xl border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300"
          >
            <span>Audit History Ledger</span>
          </Button>
        </div>

        {/* 4 Core Pillars Strip */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 max-w-5xl mx-auto pt-6 border-t border-slate-200 dark:border-white/[0.08] text-left">
          <div className="p-4 rounded-xl bg-white dark:bg-slate-900/60 border border-slate-200 dark:border-white/[0.06] shadow-sm">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-900 dark:text-white mb-1">
              <EyeOff className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
              <span>100% Blind Screening</span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-snug">
              Auto-redacts candidate names, pronouns, graduation years, and addresses.
            </p>
          </div>

          <div className="p-4 rounded-xl bg-white dark:bg-slate-900/60 border border-slate-200 dark:border-white/[0.06] shadow-sm">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-900 dark:text-white mb-1">
              <Cpu className="w-4 h-4 text-sky-600 dark:text-cyan-400" />
              <span>Neural S-BERT Engine</span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-snug">
              Understands conceptual skill equivalencies beyond rigid keyword matching.
            </p>
          </div>

          <div className="p-4 rounded-xl bg-white dark:bg-slate-900/60 border border-slate-200 dark:border-white/[0.06] shadow-sm">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-900 dark:text-white mb-1">
              <Scale className="w-4 h-4 text-amber-600 dark:text-amber-400" />
              <span>EEOC 80% Rule Audit</span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-snug">
              Calculates adverse impact mathematically on every screening run.
            </p>
          </div>

          <div className="p-4 rounded-xl bg-white dark:bg-slate-900/60 border border-slate-200 dark:border-white/[0.06] shadow-sm">
            <div className="flex items-center gap-2 text-xs font-bold text-slate-900 dark:text-white mb-1">
              <FileCheck2 className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
              <span>1-Click JD Neutralizer</span>
            </div>
            <p className="text-[11px] text-slate-500 dark:text-slate-400 leading-snug">
              Detects and rewrites masculine/age-biased words with inclusive alternatives.
            </p>
          </div>
        </div>

      </section>

      {/* ========================================================================= */}
      {/* 2. THE 3-STEP SOLUTION: HOW FAIRHIRE AI WORKS                             */}
      {/* ========================================================================= */}
      <section className="space-y-12">
        <div className="text-center max-w-2xl mx-auto space-y-2">
          <Badge variant="cyan">How It Works</Badge>
          <h2 className="text-2xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight font-heading">
            Three Steps to Bias-Free Hiring
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400">
            FairHire AI provides a seamless end-to-end workflow to protect candidate merit and ensure corporate legal defensibility.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          
          {/* Step 1 */}
          <div className="p-6 rounded-2xl bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-white/[0.08] shadow-sm flex flex-col justify-between space-y-4 hover:border-indigo-500/40 transition-colors">
            <div className="space-y-3">
              <div className="w-10 h-10 rounded-xl bg-indigo-50 dark:bg-indigo-500/10 border border-indigo-200 dark:border-indigo-500/30 flex items-center justify-center font-bold text-indigo-600 dark:text-indigo-400 font-mono text-sm">
                01
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white font-heading">
                Job Description Calibration
              </h3>
              <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
                Paste your job requirements. FairHire automatically scans for <strong>masculine-coded language</strong> (e.g. <em>"rockstar"</em>, <em>"dominant"</em>, <em>"ninja"</em>) that research proves deters qualified women from applying, providing 1-click neutral alternatives.
              </p>
            </div>
            <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950/80 border border-slate-200 dark:border-white/[0.05] text-xs font-mono text-indigo-600 dark:text-indigo-300">
              Output: Balanced, inclusive job specification
            </div>
          </div>

          {/* Step 2 */}
          <div className="p-6 rounded-2xl bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-white/[0.08] shadow-sm flex flex-col justify-between space-y-4 hover:border-emerald-500/40 transition-colors">
            <div className="space-y-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-50 dark:bg-emerald-500/10 border border-emerald-200 dark:border-emerald-500/30 flex items-center justify-center font-bold text-emerald-600 dark:text-emerald-400 font-mono text-sm">
                02
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white font-heading">
                Automated Identity Blind Shield
              </h3>
              <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
                Upload resumes in PDF, DOCX, or text. The engine automatically identifies and <strong>redacts names, gender pronouns, college prestige names, and graduation dates</strong>. Candidates are evaluated purely on verifiable project outcomes and skills.
              </p>
            </div>
            <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950/80 border border-slate-200 dark:border-white/[0.05] text-xs font-mono text-emerald-600 dark:text-emerald-300">
              Output: Zero demographic bias during evaluation
            </div>
          </div>

          {/* Step 3 */}
          <div className="p-6 rounded-2xl bg-white dark:bg-slate-900/80 border border-slate-200 dark:border-white/[0.08] shadow-sm flex flex-col justify-between space-y-4 hover:border-cyan-500/40 transition-colors">
            <div className="space-y-3">
              <div className="w-10 h-10 rounded-xl bg-sky-50 dark:bg-cyan-500/10 border border-sky-200 dark:border-cyan-500/30 flex items-center justify-center font-bold text-sky-600 dark:text-cyan-400 font-mono text-sm">
                03
              </div>
              <h3 className="text-lg font-bold text-slate-900 dark:text-white font-heading">
                Triangulated Audit &amp; Legal Defense
              </h3>
              <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400 leading-relaxed">
                The software runs <strong>Keyword ATS</strong>, <strong>Sentence-BERT Semantic Matching</strong>, and <strong>Blind Screening</strong> simultaneously. It highlights rank shifts, calculates EEOC 80% Rule Adverse Impact, and generates downloadable audit CSVs.
              </p>
            </div>
            <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-950/80 border border-slate-200 dark:border-white/[0.05] text-xs font-mono text-sky-600 dark:text-cyan-300">
              Output: Verifiable EEOC Title VII compliance report
            </div>
          </div>

        </div>
      </section>

      {/* ========================================================================= */}
      {/* 3. BEFORE VS. AFTER COMPARISON TABLE                                      */}
      {/* ========================================================================= */}
      <section className="p-6 sm:p-10 rounded-2xl bg-white dark:bg-slate-900/60 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-8">
        <div className="text-center max-w-2xl mx-auto space-y-2">
          <Badge variant="warning">The Strategic Advantage</Badge>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight font-heading">
            Legacy ATS vs. FairHire AI Platform
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400">
            See why standard ATS tools fail diversity initiatives and how FairHire solves it.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          
          {/* Legacy ATS */}
          <div className="p-6 rounded-xl bg-rose-50/60 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-500/20 space-y-4">
            <div className="flex items-center gap-2 text-rose-700 dark:text-rose-400 font-bold text-base">
              <XCircle className="w-5 h-5 flex-shrink-0" />
              <span>Traditional Hiring Process (Legacy ATS)</span>
            </div>
            <ul className="space-y-3 text-xs sm:text-sm text-slate-700 dark:text-slate-300">
              <li className="flex items-start gap-2">
                <span className="text-rose-500 font-bold">&times;</span>
                <span><strong>Keyword Rigidity:</strong> Rejects qualified talent if resume doesn't match exact phrasing.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-rose-500 font-bold">&times;</span>
                <span><strong>Unconscious Bias:</strong> Recruiters see candidate names, schools, and photos, favoring traditional demographics.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-rose-500 font-bold">&times;</span>
                <span><strong>Alienating JDs:</strong> Job descriptions filled with hyper-masculine words deter female applicants.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-rose-500 font-bold">&times;</span>
                <span><strong>Compliance Exposure:</strong> Zero auditable proof or adverse impact checks if sued for disparate impact.</span>
              </li>
            </ul>
          </div>

          {/* FairHire AI */}
          <div className="p-6 rounded-xl bg-emerald-50/60 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-500/20 space-y-4">
            <div className="flex items-center gap-2 text-emerald-700 dark:text-emerald-400 font-bold text-base">
              <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
              <span>With FairHire AI 2.0</span>
            </div>
            <ul className="space-y-3 text-xs sm:text-sm text-slate-700 dark:text-slate-300">
              <li className="flex items-start gap-2">
                <span className="text-emerald-600 dark:text-emerald-400 font-bold">&bull;</span>
                <span><strong>Semantic AI Matching:</strong> S-BERT understands conceptual skills (e.g. "cloud infrastructure" = "AWS backend").</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-emerald-600 dark:text-emerald-400 font-bold">&bull;</span>
                <span><strong>100% Identity Blinding:</strong> Reviewers assess skills without knowing gender, ethnicity, or graduation year.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-emerald-600 dark:text-emerald-400 font-bold">&bull;</span>
                <span><strong>Inclusive Neutralizer:</strong> Automatically detects and swaps exclusionary words for neutral alternatives.</span>
              </li>
              <li className="flex items-start gap-2">
                <span className="text-emerald-600 dark:text-emerald-400 font-bold">&bull;</span>
                <span><strong>EEOC 80% Rule Defense:</strong> Built-in mathematical audit score and downloadable legal compliance dossiers.</span>
              </li>
            </ul>
          </div>

        </div>
      </section>

      {/* ========================================================================= */}
      {/* 4. LIVE INTERACTIVE DEMO: TEST IT RIGHT ON THE PAGE                      */}
      {/* ========================================================================= */}
      <section id="interactive-demo" className="space-y-6 pt-4">
        <div className="text-center max-w-2xl mx-auto space-y-2">
          <Badge variant="default">Interactive Product Demo</Badge>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight font-heading">
            Test the Job Description Bias Scanner
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400">
            Select a preset or edit the text below. Watch how the real-time AI analyzes demographic coding and offers instant neutral rewrites.
          </p>
        </div>

        {/* Demo Box */}
        <div className="p-6 sm:p-8 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.08] shadow-sm space-y-6">
          
          {/* Preset Buttons */}
          <div className="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-white/[0.08]">
            <div className="text-xs font-semibold text-slate-700 dark:text-slate-300">
              Try Pre-Loaded Example Roles:
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <button
                onClick={() => loadPreset("biased")}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                  activePreset === "biased"
                    ? "bg-rose-100 text-rose-800 border border-rose-300 dark:bg-rose-500/20 dark:text-rose-300 dark:border-rose-500/40"
                    : "bg-slate-100 text-slate-700 hover:text-slate-900 dark:bg-slate-800 dark:text-slate-300 dark:hover:text-white"
                }`}
              >
                1. Biased Tech Lead (9 Flags)
              </button>
              <button
                onClick={() => loadPreset("neutral")}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                  activePreset === "neutral"
                    ? "bg-emerald-100 text-emerald-800 border border-emerald-300 dark:bg-emerald-500/20 dark:text-emerald-300 dark:border-emerald-500/40"
                    : "bg-slate-100 text-slate-700 hover:text-slate-900 dark:bg-slate-800 dark:text-slate-300 dark:hover:text-white"
                }`}
              >
                2. FairHire Neutralized Draft
              </button>
              <button
                onClick={() => loadPreset("analyst")}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
                  activePreset === "analyst"
                    ? "bg-purple-100 text-purple-800 border border-purple-300 dark:bg-purple-500/20 dark:text-purple-300 dark:border-purple-500/40"
                    : "bg-slate-100 text-slate-700 hover:text-slate-900 dark:bg-slate-800 dark:text-slate-300 dark:hover:text-white"
                }`}
              >
                3. Entry-Level Analyst
              </button>
            </div>
          </div>

          {/* Interactive 2-Col Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
            
            {/* Input area */}
            <div className="lg:col-span-7 space-y-3">
              <div className="flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
                <span className="font-semibold text-slate-700 dark:text-slate-300">Job Description Input:</span>
                {isScanning ? (
                  <span className="text-sky-600 dark:text-cyan-400 font-mono text-[11px] flex items-center gap-1">
                    <RefreshCw className="w-3 h-3 animate-spin" />
                    Scanning...
                  </span>
                ) : (
                  <span className="text-emerald-600 dark:text-emerald-400 font-mono text-[11px]">&bull; AI Scanner Ready</span>
                )}
              </div>

              <textarea
                rows={5}
                value={scannerText}
                onChange={(e) => {
                  setScannerText(e.target.value)
                  setActivePreset("custom")
                }}
                placeholder="Type or paste a job description here..."
                className="w-full bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/10 rounded-xl p-4 text-sm text-slate-900 dark:text-slate-200 focus:outline-none focus:border-indigo-500 font-sans leading-relaxed resize-none shadow-inner"
              />

              {/* 1-Click Replacement Pills */}
              {scanResult && scanResult.flagged_terms && scanResult.flagged_terms.length > 0 && (
                <div className="p-3.5 rounded-xl bg-slate-50 dark:bg-slate-950/80 border border-slate-200 dark:border-white/[0.06] space-y-2">
                  <div className="text-xs font-semibold text-slate-700 dark:text-slate-300 flex items-center justify-between">
                    <span>Click Any Flagged Word to Neutralize In-Place:</span>
                    <span className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">1-Click Swap</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {scanResult.flagged_terms.map((t, i) => (
                      <button
                        key={i}
                        type="button"
                        onClick={() => replaceWord(t.word, t.neutral_alternative)}
                        className="px-2.5 py-1 rounded-lg bg-white dark:bg-slate-900 hover:bg-emerald-50 dark:hover:bg-emerald-950 border border-slate-200 dark:border-white/10 hover:border-emerald-400 text-xs text-slate-700 dark:text-slate-300 hover:text-emerald-700 dark:hover:text-emerald-300 font-mono transition-all flex items-center gap-1 cursor-pointer shadow-sm"
                        title={`Replace "${t.word}" with "${t.neutral_alternative}"`}
                      >
                        <span className="line-through text-rose-600 dark:text-rose-400">{t.word}</span>
                        <span>&rarr;</span>
                        <span className="text-emerald-600 dark:text-emerald-400 font-bold">{t.neutral_alternative}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Live Telemetry Card */}
            <div className="lg:col-span-5 p-5 rounded-xl bg-slate-50 dark:bg-slate-950 border border-slate-200 dark:border-white/[0.08] space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-white/[0.06]">
                <span className="text-xs font-bold text-slate-900 dark:text-white uppercase tracking-wider font-mono">
                  Live Scanner Diagnostics
                </span>
                <Badge variant={String(scanResult?.gender_lean || scanResult?.lean || scanResult?.gender_bias?.lean || "neutral").toLowerCase().includes("masculine") ? "danger" : String(scanResult?.gender_lean || scanResult?.lean || scanResult?.gender_bias?.lean || "neutral").toLowerCase().includes("feminine") ? "purple" : "success"}>
                  {scanResult ? `${String(scanResult?.gender_lean || scanResult?.lean || scanResult?.gender_bias?.lean || "neutral").toUpperCase()} LEAN` : "ANALYZING"}
                </Badge>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.05] shadow-sm">
                  <span className="text-slate-500 dark:text-slate-400 block text-[10px] uppercase font-mono">Flagged Terms</span>
                  <strong className="text-slate-900 dark:text-white text-base font-mono">
                    {scanResult?.flagged_terms ? scanResult.flagged_terms.length : 0} detected
                  </strong>
                </div>

                <div className="p-3 rounded-lg bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/[0.05] shadow-sm">
                  <span className="text-slate-500 dark:text-slate-400 block text-[10px] uppercase font-mono">Readability Score</span>
                  <strong className="text-sky-600 dark:text-cyan-400 text-base font-mono">
                    {scanResult?.readability?.level ? scanResult.readability.level : "Grade 10"}
                  </strong>
                </div>
              </div>

              {/* Skills detected */}
              <div>
                <span className="text-[11px] font-bold text-slate-500 dark:text-slate-400 uppercase font-mono block mb-1.5">
                  Extracted Skills from Text:
                </span>
                <div className="flex flex-wrap gap-1">
                  {scanResult?.skills && scanResult.skills.length > 0 ? (
                    scanResult.skills.map((s, idx) => (
                      <Badge key={idx} variant="default" className="text-[10px] font-mono">{s}</Badge>
                    ))
                  ) : (
                    <span className="text-xs text-slate-400 font-mono">No technical skills detected yet...</span>
                  )}
                </div>
              </div>

              {/* Action Button */}
              <Button
                variant="default"
                className="w-full h-10 rounded-xl text-xs font-semibold flex items-center justify-center gap-2"
                onClick={() => {
                  sessionStorage.setItem("fairhire_custom_jd_text", scannerText)
                  setActiveView("studio")
                }}
              >
                <span>Screen Candidates with this Role</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            </div>

          </div>

        </div>
      </section>

      {/* ========================================================================= */}
      {/* 5. ACADEMIC GROUNDING & LEGAL DEFENSE                                     */}
      {/* ========================================================================= */}
      <section className="space-y-6">
        <div className="text-center max-w-2xl mx-auto space-y-2">
          <Badge variant="purple">Legal &amp; Scientific Grounding</Badge>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white tracking-tight font-heading">
            Built on Empirical Behavioral Economics
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-400">
            Every metric and blind screening mechanism is grounded in published peer-reviewed research and EEOC regulatory standards.
          </p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="p-5 space-y-2.5">
            <BookOpen className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
            <div className="font-bold text-sm text-slate-900 dark:text-white font-heading">Gaucher et al. (2011)</div>
            <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
              <em>Journal of Personality and Social Psychology</em>: Proved masculine wording in job ads deters female applications.
            </p>
            <Badge variant="default" className="text-[10px]">Lexical Bias Basis</Badge>
          </Card>

          <Card className="p-5 space-y-2.5">
            <BookOpen className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
            <div className="font-bold text-sm text-slate-900 dark:text-white font-heading">Åslund &amp; Skans (2012)</div>
            <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
              <em>Journal of Labor Economics</em>: Demonstrated blind recruitment significantly boosts minority and female callback rates.
            </p>
            <Badge variant="success" className="text-[10px]">Blind Shield Basis</Badge>
          </Card>

          <Card className="p-5 space-y-2.5">
            <BookOpen className="w-5 h-5 text-sky-600 dark:text-cyan-400" />
            <div className="font-bold text-sm text-slate-900 dark:text-white font-heading">Bertrand &amp; Mullainathan (2004)</div>
            <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
              <em>American Economic Review</em>: Showed resumes with African-American names require 50% more applications for an interview.
            </p>
            <Badge variant="cyan" className="text-[10px]">Audit Model</Badge>
          </Card>

          <Card className="p-5 space-y-2.5">
            <BookOpen className="w-5 h-5 text-amber-600 dark:text-amber-400" />
            <div className="font-bold text-sm text-slate-900 dark:text-white font-heading">EEOC 80% Rule (29 C.F.R.)</div>
            <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
              Federal Four-Fifths rule to mathematically identify adverse impact in hiring selection procedures.
            </p>
            <Badge variant="warning" className="text-[10px]">Compliance Metric</Badge>
          </Card>
        </div>
      </section>

      {/* ========================================================================= */}
      {/* 6. CALL TO ACTION                                                         */}
      {/* ========================================================================= */}
      <section className="p-8 sm:p-12 rounded-2xl bg-gradient-to-r from-indigo-50 via-slate-50 to-slate-100 dark:from-indigo-950/60 dark:via-slate-900 dark:to-slate-900 border border-slate-200 dark:border-white/10 text-center space-y-6 shadow-sm">
        <div className="max-w-2xl mx-auto space-y-3">
          <h2 className="text-2xl sm:text-4xl font-extrabold text-slate-900 dark:text-white tracking-tight font-heading">
            Start Screening Candidates with FairHire AI Today
          </h2>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
            Upload your resumes and paste your job description to run a 3-engine benchmark with full identity blinding in under 3 seconds.
          </p>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-4">
          <Button
            size="lg"
            variant="default"
            onClick={() => setActiveView("studio")}
            className="flex items-center gap-2 text-sm px-7 h-12 rounded-xl font-semibold shadow-md"
          >
            <span>Open Screening Studio</span>
            <ArrowRight className="w-4 h-4" />
          </Button>
          <Button
            size="lg"
            variant="secondary"
            onClick={() => setActiveView("history")}
            className="flex items-center gap-2 text-sm px-6 h-12 rounded-xl"
          >
            <span>View Past Audits</span>
          </Button>
        </div>
      </section>

    </div>
  )
}
