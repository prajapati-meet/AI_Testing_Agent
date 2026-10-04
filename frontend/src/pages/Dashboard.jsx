import React from 'react';

function Dashboard() {
  return (
    <div className="page-container">
      <h1>AI Test Agent: Autonomous AI-Based Web Application Testing and Intelligent Bug Reporting</h1>
      <p className="subtitle">
        Base Project Dashboard — Ready for autonomous web exploration, AI test generation, Playwright execution, and LLM bug reporting.
      </p>

      <div className="card-grid">
        <div className="card">
          <h3>1. Exploration Service</h3>
          <p>Crawls target web applications via Playwright to extract structured page elements.</p>
          <span className="badge">Port 8082</span>
        </div>
        <div className="card">
          <h3>2. Test / AI Service</h3>
          <p>Generates structured JSON test cases using LangGraph and LLM reasoning.</p>
          <span className="badge">Port 8083</span>
        </div>
        <div className="card">
          <h3>3. Execution Service</h3>
          <p>Executes test steps deterministically against the browser via Playwright.</p>
          <span className="badge">Port 8084</span>
        </div>
        <div className="card">
          <h3>4. Bug Analysis Service</h3>
          <p>Analyzes failed executions using an LLM to produce actionable bug reports.</p>
          <span className="badge">Port 8085</span>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
