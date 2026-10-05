import React, { useState, useEffect } from "react"
import { Navbar } from "./components/Navbar"
import { LandingPage } from "./components/LandingPage"
import { ScreeningStudio } from "./components/ScreeningStudio"
import { ResultsDashboard } from "./components/ResultsDashboard"
import { AuditHistory } from "./components/AuditHistory"
import { RecruiterDashboard } from "./components/RecruiterDashboard"
import { CandidatePortal } from "./components/CandidatePortal"
import { ResearchAdminPortal } from "./components/ResearchAdminPortal"
import { SystemArchitectureViewer } from "./components/SystemArchitectureViewer"
import { Footer } from "./components/Footer"

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }
  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }
  componentDidCatch(error, errorInfo) {
    console.error("ErrorBoundary caught an error:", error, errorInfo)
  }
  render() {
    if (this.state.hasError) {
      return (
        <div className="max-w-xl mx-auto py-24 px-4 text-center space-y-4">
          <div className="w-12 h-12 rounded-xl bg-rose-500/10 text-rose-500 flex items-center justify-center mx-auto text-xl font-bold">
            !
          </div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-white">Something went wrong</h2>
          <p className="text-sm text-slate-600 dark:text-slate-400">
            {this.state.error?.message || "An unexpected rendering error occurred."}
          </p>
          <button
            onClick={() => {
              this.setState({ hasError: false, error: null })
              window.location.reload()
            }}
            className="px-4 py-2 rounded-xl bg-indigo-600 text-white text-xs font-semibold hover:bg-indigo-500 transition-colors shadow-sm"
          >
            Reload Application
          </button>
        </div>
      )
    }
    return this.props.children
  }
}

export function App() {
  const [activeView, setActiveView] = useState("recruiter") // Default to Recruiter Workspace from architecture
  const [currentRunId, setCurrentRunId] = useState(null)
  const [prefilledSampleIndex, setPrefilledSampleIndex] = useState(null)

  // Light / Dark Theme State Management
  const [theme, setTheme] = useState(() => {
    return localStorage.getItem("fairhire_theme") || "light"
  })

  useEffect(() => {
    const root = document.documentElement
    if (theme === "dark") {
      root.classList.add("dark")
    } else {
      root.classList.remove("dark")
    }
    localStorage.setItem("fairhire_theme", theme)
  }, [theme])

  const toggleTheme = () => {
    setTheme(prev => prev === "dark" ? "light" : "dark")
  }

  // Scroll to top on view change
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "smooth" })
  }, [activeView])

  const handleScreeningComplete = (runId) => {
    setCurrentRunId(runId)
    setActiveView("results")
  }

  const handleSelectRun = (runId) => {
    setCurrentRunId(runId)
    setActiveView("results")
  }

  const handleSelectPreset = (idx) => {
    setPrefilledSampleIndex(idx)
    setActiveView("studio")
  }

  const handleNewScreening = () => {
    setPrefilledSampleIndex(null)
    setActiveView("studio")
  }

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 dark:bg-[#080c14] text-slate-800 dark:text-slate-100 transition-colors duration-200">
      <Navbar 
        activeView={activeView} 
        setActiveView={setActiveView}
        onNewScreening={handleNewScreening}
        theme={theme}
        toggleTheme={toggleTheme}
      />

      <main className="flex-1 relative">
        <ErrorBoundary>
          {activeView === "recruiter" && (
            <RecruiterDashboard
              onOpenLegacyStudio={() => setActiveView("studio")}
            />
          )}

          {activeView === "candidate" && (
            <CandidatePortal />
          )}

          {activeView === "admin" && (
            <ResearchAdminPortal />
          )}

          {activeView === "architecture" && (
            <SystemArchitectureViewer />
          )}

          {activeView === "landing" && (
            <LandingPage 
              setActiveView={setActiveView}
              onSelectPreset={handleSelectPreset}
            />
          )}

          {activeView === "studio" && (
            <ScreeningStudio
              onScreeningComplete={handleScreeningComplete}
              prefilledSampleIndex={prefilledSampleIndex}
            />
          )}

          {activeView === "results" && (
            <ResultsDashboard
              runId={currentRunId}
              setActiveView={setActiveView}
            />
          )}

          {activeView === "history" && (
            <AuditHistory
              onSelectRun={handleSelectRun}
              setActiveView={setActiveView}
            />
          )}
        </ErrorBoundary>
      </main>

      <Footer setActiveView={setActiveView} />
    </div>
  )
}

export default App
