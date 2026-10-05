import React, { useState } from "react"
import {
  Layers,
  Cpu,
  Database,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  Users,
  Code,
  FileCode,
  Network
} from "lucide-react"

export function SystemArchitectureViewer() {
  const [activeSection, setActiveSection] = useState("1")

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="text-center max-w-3xl mx-auto space-y-2">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 dark:bg-indigo-950/50 border border-indigo-200 dark:border-indigo-800 text-indigo-700 dark:text-indigo-300 text-xs font-semibold">
          <Network className="w-3.5 h-3.5" />
          FairHire AI • System Design Specification
        </div>
        <h1 className="text-3xl font-bold font-heading text-slate-900 dark:text-white">
          Detailed System Design & Architecture
        </h1>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          AI-Powered Fair Recruitment & Resume Screening Framework implementation reference.
        </p>
      </div>

      {/* 5-Section Nav Buttons */}
      <div className="flex flex-wrap items-center justify-center gap-2 p-1.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 shadow-sm max-w-4xl mx-auto">
        {[
          { id: "1", title: "1. Overall Architecture", icon: Layers },
          { id: "2", title: "2. Detailed Workflow", icon: ArrowRight },
          { id: "3", title: "3. Use Case Diagram", icon: Users },
          { id: "4", title: "4. Database ER Diagram", icon: Database },
          { id: "5", title: "5. API Architecture", icon: Code },
        ].map(tab => {
          const Icon = tab.icon
          const isActive = activeSection === tab.id
          return (
            <button
              key={tab.id}
              onClick={() => setActiveSection(tab.id)}
              className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                isActive
                  ? "bg-indigo-600 text-white shadow-sm"
                  : "text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-white/[0.04]"
              }`}
            >
              <Icon className="w-3.5 h-3.5" />
              {tab.title}
            </button>
          )
        })}
      </div>

      {/* SECTION 1: OVERALL SYSTEM ARCHITECTURE */}
      {activeSection === "1" && (
        <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 shadow-sm space-y-6">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Layers className="w-5 h-5 text-indigo-500" />
            1. Overall System Architecture
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-stretch">
            {/* Frontend */}
            <div className="p-5 rounded-2xl bg-indigo-50/50 dark:bg-indigo-950/20 border border-indigo-200 dark:border-indigo-800/60 flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-center gap-2 text-sm font-bold text-indigo-900 dark:text-indigo-200 mb-3">
                  <div className="w-3 h-3 rounded-full bg-indigo-500"></div>
                  Frontend (React)
                </div>
                <ul className="text-xs space-y-2 text-indigo-800/80 dark:text-indigo-300">
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-indigo-500" /> Recruiter Dashboard</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-indigo-500" /> Job Description Upload</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-indigo-500" /> Resume Upload</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-indigo-500" /> Candidate Ranking</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-indigo-500" /> Bias Analysis Report</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-indigo-500" /> Inclusive Rewrite Suggestions</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-indigo-500" /> Blind Screening Comparison</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-indigo-500" /> Charts & Analytics</li>
                </ul>
              </div>
              <div className="text-[10px] font-mono text-center p-2 rounded bg-indigo-100/60 dark:bg-indigo-900/40 text-indigo-800 dark:text-indigo-200">
                HTTPS / REST JSON
              </div>
            </div>

            {/* Backend */}
            <div className="p-5 rounded-2xl bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800/60 flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-center gap-2 text-sm font-bold text-emerald-900 dark:text-emerald-200 mb-3">
                  <div className="w-3 h-3 rounded-full bg-emerald-500"></div>
                  Backend (Spring Boot / API Core)
                </div>
                <ul className="text-xs space-y-2 text-emerald-800/80 dark:text-emerald-300">
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-emerald-500" /> REST Controllers</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-emerald-500" /> Business Services</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-emerald-500" /> ATS Scoring Engine</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-emerald-500" /> Experiment Manager</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-emerald-500" /> Authentication & Authorization</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-emerald-500" /> File Management</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-emerald-500" /> Database Access (JPA / ORM)</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-emerald-500" /> AI Service Client</li>
                </ul>
              </div>
              <div className="text-[10px] font-mono text-center p-2 rounded bg-emerald-100/60 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-200">
                JPA / JDBC Database Access • REST JSON to AI
              </div>
            </div>

            {/* AI / NLP Service */}
            <div className="p-5 rounded-2xl bg-purple-50/50 dark:bg-purple-950/20 border border-purple-200 dark:border-purple-800/60 flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-center gap-2 text-sm font-bold text-purple-900 dark:text-purple-200 mb-3">
                  <div className="w-3 h-3 rounded-full bg-purple-500"></div>
                  AI/NLP Service (Python)
                </div>
                <ul className="text-xs space-y-2 text-purple-800/80 dark:text-purple-300">
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-purple-500" /> Resume Parser</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-purple-500" /> Skill Extraction</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-purple-500" /> Job Description Analyzer</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-purple-500" /> Semantic Matching (Embeddings)</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-purple-500" /> Bias Detection</li>
                  <li className="flex items-center gap-1.5"><CheckCircle2 className="w-3 h-3 text-purple-500" /> Inclusive Rewrite Generation</li>
                </ul>
              </div>
              <div className="text-[10px] font-mono text-center p-2 rounded bg-purple-100/60 dark:bg-purple-900/40 text-purple-800 dark:text-purple-200">
                spaCy • Sentence Transformers • HuggingFace
              </div>
            </div>
          </div>
        </div>
      )}

      {/* SECTION 2: DETAILED WORKFLOW */}
      {activeSection === "2" && (
        <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 shadow-sm space-y-6">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <ArrowRight className="w-5 h-5 text-indigo-500" />
            2. Detailed System Workflow (9 Steps)
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {[
              { num: "1", title: "User Login", desc: "Recruiter login/register -> Access dashboard" },
              { num: "2", title: "Job Description Upload", desc: "Recruiter uploads JD -> System stores JD in DB -> JD text sent to Python AI" },
              { num: "3", title: "JD Analysis (Python)", desc: "Extract required skills -> Detect potentially exclusionary language -> Generate inclusive rewrite suggestions -> Return to backend" },
              { num: "4", title: "Resume Upload", desc: "Candidates upload resume (PDF/DOCX) -> System stores file in DB -> Send resume to Python AI for parsing" },
              { num: "5", title: "Resume Parsing (Python)", desc: "Extract text from resume -> Extract skills, education, experience -> Remove personal information (for blind screening) -> Return structured data" },
              { num: "6", title: "ATS Scoring Engine", desc: "Keyword matching score -> Semantic matching score (embeddings) -> Combine scores (configurable weights) -> Candidate ranking" },
              { num: "7", title: "Blind Screening", desc: "Create anonymized resumes (remove name, gender, location) -> Run matching again -> Compare normal vs blind rankings" },
              { num: "8", title: "Results & Reports", desc: "Display candidate rankings -> Show skill match details -> Show bias report -> Show rewrite suggestions -> Show blind comparison" },
              { num: "9", title: "Research & Evaluation", desc: "Compute Precision, Recall, F1-score, Accuracy, Response Time -> Store experiment results -> Generate reports for research paper" }
            ].map(step => (
              <div key={step.num} className="p-4 rounded-xl border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-2">
                <div className="flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-indigo-600 text-white font-bold text-xs flex items-center justify-center">
                    {step.num}
                  </span>
                  <h3 className="font-bold text-xs text-slate-900 dark:text-white">{step.title}</h3>
                </div>
                <p className="text-[11px] text-slate-600 dark:text-slate-400 leading-relaxed">
                  {step.desc}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* SECTION 3: USE CASE DIAGRAM */}
      {activeSection === "3" && (
        <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 shadow-sm space-y-6">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Users className="w-5 h-5 text-indigo-500" />
            3. Use Case Diagram Actors & Permissions
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-5 rounded-xl border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-3">
              <div className="font-bold text-sm text-indigo-600 dark:text-indigo-400">Recruiter (Employer)</div>
              <ul className="text-xs space-y-1.5 text-slate-600 dark:text-slate-400">
                <li>• Login / Register</li>
                <li>• Upload Job Description</li>
                <li>• View JD Analysis & Bias Report</li>
                <li>• View Inclusive Rewrite Suggestions</li>
                <li>• View Candidate Rankings</li>
                <li>• Compare Normal vs Blind Screening</li>
                <li>• View Evaluation Reports</li>
                <li>• Manage Account</li>
              </ul>
            </div>

            <div className="p-5 rounded-xl border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-3">
              <div className="font-bold text-sm text-emerald-600 dark:text-emerald-400">Candidate (Job Seeker)</div>
              <ul className="text-xs space-y-1.5 text-slate-600 dark:text-slate-400">
                <li>• Register / Login</li>
                <li>• Upload Resume (PDF)</li>
                <li>• View Match Score and Feedback</li>
              </ul>
            </div>

            <div className="p-5 rounded-xl border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-3">
              <div className="font-bold text-sm text-purple-600 dark:text-purple-400">Admin (Research Team)</div>
              <ul className="text-xs space-y-1.5 text-slate-600 dark:text-slate-400">
                <li>• Manage Users</li>
                <li>• Manage Dataset</li>
                <li>• View Experiment Results</li>
              </ul>
            </div>
          </div>
        </div>
      )}

      {/* SECTION 4: DATABASE ER DIAGRAM */}
      {activeSection === "4" && (
        <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 shadow-sm space-y-6">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Database className="w-5 h-5 text-indigo-500" />
            4. Database ER Diagram (Simplified)
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3 text-xs">
            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">users</div>
              <div className="text-[10px] text-slate-500">PK: id</div>
              <div className="text-[10px] text-slate-500">name, email, password, role, created_at</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">jobs</div>
              <div className="text-[10px] text-slate-500">PK: id, FK: created_by</div>
              <div className="text-[10px] text-slate-500">title, description, requirements, created_at</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">bias_reports</div>
              <div className="text-[10px] text-slate-500">PK: id, FK: job_id</div>
              <div className="text-[10px] text-slate-500">phrase, category, severity, suggestion, created_at</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">candidates</div>
              <div className="text-[10px] text-slate-500">PK: id</div>
              <div className="text-[10px] text-slate-500">name, email, created_at</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">resumes</div>
              <div className="text-[10px] text-slate-500">PK: id, FK: candidate_id</div>
              <div className="text-[10px] text-slate-500">file_path, raw_text, anonymized_text, created_at</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">skills</div>
              <div className="text-[10px] text-slate-500">PK: id</div>
              <div className="text-[10px] text-slate-500">skill_name, category</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">job_skills</div>
              <div className="text-[10px] text-slate-500">PK: id, FK: job_id, FK: skill_id</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">resume_skills</div>
              <div className="text-[10px] text-slate-500">PK: id, FK: resume_id, FK: skill_id</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">matches</div>
              <div className="text-[10px] text-slate-500">PK: id, FK: job_id, FK: resume_id</div>
              <div className="text-[10px] text-slate-500">keyword_score, semantic_score, final_score, normal_rank, blind_rank, created_at</div>
            </div>

            <div className="p-3 rounded-lg border border-slate-200 dark:border-white/10 bg-slate-50 dark:bg-white/[0.02] space-y-1">
              <div className="font-bold text-indigo-600">experiments</div>
              <div className="text-[10px] text-slate-500">PK: id</div>
              <div className="text-[10px] text-slate-500">type, dataset_name, metric_name, metric_value, created_at</div>
            </div>
          </div>
        </div>
      )}

      {/* SECTION 5: API ARCHITECTURE */}
      {activeSection === "5" && (
        <div className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/10 shadow-sm space-y-6">
          <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Code className="w-5 h-5 text-indigo-500" />
            5. Complete API Architecture (18 Endpoints)
          </h2>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-200 dark:border-white/10 text-slate-400 text-[10px] uppercase font-semibold">
                  <th className="py-2.5 px-3">Module</th>
                  <th className="py-2.5 px-3">Method</th>
                  <th className="py-2.5 px-3">Endpoint</th>
                  <th className="py-2.5 px-3">Description</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-white/[0.04] font-mono">
                <tr><td className="py-2 px-3 font-sans font-semibold">Authentication</td><td className="text-emerald-500">POST</td><td>/api/auth/register</td><td className="font-sans text-slate-500">Register user (recruiter/candidate)</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Authentication</td><td className="text-emerald-500">POST</td><td>/api/auth/login</td><td className="font-sans text-slate-500">Login and get JWT token</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Authentication</td><td className="text-indigo-500">GET</td><td>/api/auth/me</td><td className="font-sans text-slate-500">Get current user details</td></tr>
                
                <tr><td className="py-2 px-3 font-sans font-semibold">Job Management</td><td className="text-emerald-500">POST</td><td>/api/jobs</td><td className="font-sans text-slate-500">Create new job description</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Job Management</td><td className="text-indigo-500">GET</td><td>/api/jobs</td><td className="font-sans text-slate-500">Get all job descriptions</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Job Management</td><td className="text-indigo-500">GET</td><td>/api/jobs/&#123;id&#125;</td><td className="font-sans text-slate-500">Get job details</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Job Management</td><td className="text-amber-500">PUT</td><td>/api/jobs/&#123;id&#125;</td><td className="font-sans text-slate-500">Update job description</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Job Management</td><td className="text-rose-500">DELETE</td><td>/api/jobs/&#123;id&#125;</td><td className="font-sans text-slate-500">Delete job description</td></tr>

                <tr><td className="py-2 px-3 font-sans font-semibold">Resume Management</td><td className="text-emerald-500">POST</td><td>/api/resumes</td><td className="font-sans text-slate-500">Upload resume (PDF)</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Resume Management</td><td className="text-indigo-500">GET</td><td>/api/resumes</td><td className="font-sans text-slate-500">Get all resumes</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Resume Management</td><td className="text-indigo-500">GET</td><td>/api/resumes/&#123;id&#125;</td><td className="font-sans text-slate-500">Get resume details</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Resume Management</td><td className="text-rose-500">DELETE</td><td>/api/resumes/&#123;id&#125;</td><td className="font-sans text-slate-500">Delete resume</td></tr>

                <tr><td className="py-2 px-3 font-sans font-semibold">Matching & Ranking</td><td className="text-emerald-500">POST</td><td>/api/matching/&#123;jobId&#125;</td><td className="font-sans text-slate-500">Run matching for a job</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Matching & Ranking</td><td className="text-indigo-500">GET</td><td>/api/matching/&#123;jobId&#125;/results</td><td className="font-sans text-slate-500">Get candidate rankings</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Matching & Ranking</td><td className="text-indigo-500">GET</td><td>/api/matching/&#123;id&#125;</td><td className="font-sans text-slate-500">Get detailed match result</td></tr>

                <tr><td className="py-2 px-3 font-sans font-semibold">Bias Detection</td><td className="text-emerald-500">POST</td><td>/api/bias/analyze</td><td className="font-sans text-slate-500">Analyze JD for bias (called internally)</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Bias Detection</td><td className="text-indigo-500">GET</td><td>/api/bias/&#123;jobId&#125;</td><td className="font-sans text-slate-500">Get bias analysis report</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Bias Detection</td><td className="text-emerald-500">POST</td><td>/api/bias/rewrite</td><td className="font-sans text-slate-500">Get inclusive rewrite suggestions</td></tr>

                <tr><td className="py-2 px-3 font-sans font-semibold">Blind Screening</td><td className="text-emerald-500">POST</td><td>/api/blind-screening/&#123;jobId&#125;</td><td className="font-sans text-slate-500">Run blind screening</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Blind Screening</td><td className="text-indigo-500">GET</td><td>/api/blind-screening/&#123;jobId&#125;</td><td className="font-sans text-slate-500">Get blind vs normal ranking comparison</td></tr>

                <tr><td className="py-2 px-3 font-sans font-semibold">Evaluation & Reports</td><td className="text-indigo-500">GET</td><td>/api/reports/metrics</td><td className="font-sans text-slate-500">Get evaluation metrics</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Evaluation & Reports</td><td className="text-indigo-500">GET</td><td>/api/reports/experiments</td><td className="font-sans text-slate-500">Get experiment results</td></tr>
                <tr><td className="py-2 px-3 font-sans font-semibold">Evaluation & Reports</td><td className="text-indigo-500">GET</td><td>/api/reports/export</td><td className="font-sans text-slate-500">Export results (CSV/PDF)</td></tr>
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
