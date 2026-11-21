import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const Dashboard = () => {
  const navigate = useNavigate();

  // Authentication & Profile State
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(true);

  // Active Tab: 'all' | 'exploration' | 'tests' | 'executions' | 'bugs'
  const [activeTab, setActiveTab] = useState('all');

  // Global Alerts
  const [alert, setAlert] = useState(null); // { type: 'success' | 'error' | 'info', message: string }

  // ---------------------------------------------------------------------------
  // Panel 1: Application Exploration State
  // ---------------------------------------------------------------------------
  const [targetUrl, setTargetUrl] = useState('http://localhost:3000');
  const [exploration, setExploration] = useState(null);
  const [explorationStatus, setExplorationStatus] = useState('IDLE'); // IDLE, PENDING, RUNNING, COMPLETED, FAILED
  const [isStartingExploration, setIsStartingExploration] = useState(false);
  const [discoveredPages, setDiscoveredPages] = useState([]);
  const explorationPollingRef = useRef(null);

  // ---------------------------------------------------------------------------
  // Panel 2: AI Test Cases State
  // ---------------------------------------------------------------------------
  const [testCases, setTestCases] = useState([]);
  const [isGeneratingTests, setIsGeneratingTests] = useState(false);
  const [expandedTestIds, setExpandedTestIds] = useState(new Set());

  // ---------------------------------------------------------------------------
  // Panel 3: Test Runner & Executions State
  // ---------------------------------------------------------------------------
  const [executions, setExecutions] = useState([]);
  const [selectedTestCaseId, setSelectedTestCaseId] = useState('');
  const [executionTargetUrl, setExecutionTargetUrl] = useState('http://localhost:3000');
  const [isRunningTest, setIsRunningTest] = useState(false);
  const [selectedExecution, setSelectedExecution] = useState(null);
  const [screenshotUrl, setScreenshotUrl] = useState(null);
  const [loadingScreenshot, setLoadingScreenshot] = useState(false);
  const [screenshotError, setScreenshotError] = useState(null);
  const executionPollingRef = useRef(null);

  // ---------------------------------------------------------------------------
  // Panel 4: AI Bug Reports State
  // ---------------------------------------------------------------------------
  const [bugs, setBugs] = useState([]);
  const [analyzingExecutionId, setAnalyzingExecutionId] = useState(null);
  const [selectedBug, setSelectedBug] = useState(null);

  // ===========================================================================
  // Initial Data Fetching
  // ===========================================================================
  const fetchProfile = useCallback(async () => {
    try {
      const response = await api.get('/api/auth/me');
      setProfile(response.data);
    } catch (err) {
      setAlert({
        type: 'error',
        message: err.response?.data?.message || 'Failed to authenticate user.'
      });
    } finally {
      setProfileLoading(false);
    }
  }, []);

  const fetchTestCases = useCallback(async () => {
    try {
      const response = await api.get('/api/tests');
      if (Array.isArray(response.data)) {
        setTestCases(response.data);
        if (response.data.length > 0 && !selectedTestCaseId) {
          setSelectedTestCaseId(String(response.data[0].id));
        }
      }
    } catch (err) {
      console.error('Failed to load test cases:', err);
    }
  }, [selectedTestCaseId]);

  const fetchExecutions = useCallback(async () => {
    try {
      const response = await api.get('/api/executions');
      if (Array.isArray(response.data)) {
        setExecutions(response.data);
      }
    } catch (err) {
      console.error('Failed to load executions:', err);
    }
  }, []);

  const fetchBugs = useCallback(async () => {
    try {
      const response = await api.get('/api/bugs');
      if (Array.isArray(response.data)) {
        setBugs(response.data);
      }
    } catch (err) {
      console.error('Failed to load bug reports:', err);
    }
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    fetchProfile();
    fetchTestCases();
    fetchExecutions();
    fetchBugs();
  }, [navigate, fetchProfile, fetchTestCases, fetchExecutions, fetchBugs]);

  // ===========================================================================
  // Panel 1: Exploration Handlers & 3s Polling
  // ===========================================================================
  const fetchDiscoveredPages = useCallback(async (explorationId) => {
    try {
      const response = await api.get(`/api/exploration/${explorationId}/pages`);
      if (Array.isArray(response.data)) {
        setDiscoveredPages(response.data);
      }
    } catch (err) {
      console.error('Failed to fetch discovered pages:', err);
    }
  }, []);

  const stopExplorationPolling = useCallback(() => {
    if (explorationPollingRef.current) {
      clearInterval(explorationPollingRef.current);
      explorationPollingRef.current = null;
    }
  }, []);

  const pollExplorationStatus = useCallback(
    (explorationId) => {
      stopExplorationPolling();

      explorationPollingRef.current = setInterval(async () => {
        try {
          const response = await api.get(`/api/exploration/${explorationId}`);
          const expData = response.data;
          setExploration(expData);
          setExplorationStatus(expData.status);

          if (expData.status === 'COMPLETED') {
            stopExplorationPolling();
            fetchDiscoveredPages(explorationId);
            setAlert({
              type: 'success',
              message: `Exploration #${explorationId} completed successfully! Discovered pages loaded.`
            });
          } else if (expData.status === 'FAILED') {
            stopExplorationPolling();
            setAlert({
              type: 'error',
              message: `Exploration #${explorationId} failed: ${expData.message || 'Crawl error encountered.'}`
            });
          }
        } catch (err) {
          console.error('Polling exploration status error:', err);
        }
      }, 3000);
    },
    [stopExplorationPolling, fetchDiscoveredPages]
  );

  useEffect(() => {
    return () => {
      stopExplorationPolling();
    };
  }, [stopExplorationPolling]);

  const handleStartExploration = async (e) => {
    e.preventDefault();
    if (!targetUrl.trim()) return;

    setIsStartingExploration(true);
    setDiscoveredPages([]);
    setExploration(null);
    setExplorationStatus('PENDING');

    try {
      const response = await api.post('/api/exploration/start', {
        targetUrl: targetUrl.trim()
      });
      const expData = response.data;
      setExploration(expData);
      setExplorationStatus(expData.status || 'PENDING');
      setAlert({
        type: 'info',
        message: `Exploration started for ${targetUrl.trim()}. Polling for page discovery...`
      });

      // Start automatic 3-second polling
      pollExplorationStatus(expData.id);
    } catch (err) {
      setExplorationStatus('FAILED');
      setAlert({
        type: 'error',
        message: err.response?.data?.message || 'Failed to trigger exploration crawl.'
      });
    } finally {
      setIsStartingExploration(false);
    }
  };

  // ===========================================================================
  // Panel 2: AI Test Generation Handlers
  // ===========================================================================
  const handleGenerateTests = async () => {
    setIsGeneratingTests(true);
    try {
      const response = await api.post('/api/tests/generate', {
        explorationId: exploration?.id || null,
        targetUrl: targetUrl.trim() || 'http://localhost:3000'
      });

      const newTests = Array.isArray(response.data) ? response.data : [response.data];
      setTestCases((prev) => {
        const existingIds = new Set(prev.map((t) => t.id));
        const filteredNew = newTests.filter((t) => !existingIds.has(t.id));
        return [...filteredNew, ...prev];
      });

      if (newTests.length > 0 && newTests[0].id) {
        setSelectedTestCaseId(String(newTests[0].id));
      }

      setAlert({
        type: 'success',
        message: `Successfully generated ${newTests.length} AI test case(s)!`
      });
    } catch (err) {
      setAlert({
        type: 'error',
        message: err.response?.data?.message || 'Failed to generate AI tests.'
      });
    } finally {
      setIsGeneratingTests(false);
    }
  };

  const toggleTestSteps = (testId) => {
    setExpandedTestIds((prev) => {
      const next = new Set(prev);
      if (next.has(testId)) {
        next.delete(testId);
      } else {
        next.add(testId);
      }
      return next;
    });
  };

  // Helper to parse steps JSON string safely
  const parseSteps = (stepsRaw) => {
    if (!stepsRaw) return [];
    if (Array.isArray(stepsRaw)) return stepsRaw;
    if (typeof stepsRaw === 'string') {
      try {
        const parsed = JSON.parse(stepsRaw);
        return Array.isArray(parsed) ? parsed : [];
      } catch {
        return [];
      }
    }
    return [];
  };

  // ===========================================================================
  // Panel 3: Test Runner & Executions Handlers
  // ===========================================================================
  const handleTriggerExecution = async (customTestCaseId = null) => {
    const testIdToRun = customTestCaseId || selectedTestCaseId;
    if (!testIdToRun) {
      setAlert({
        type: 'error',
        message: 'Please select a Test Case to execute.'
      });
      return;
    }

    setIsRunningTest(true);
    try {
      const response = await api.post('/api/executions', {
        testCaseId: Number(testIdToRun),
        targetUrl: executionTargetUrl.trim() || 'http://localhost:3000'
      });

      const executionData = response.data;
      setExecutions((prev) => [executionData, ...prev.filter((item) => item.id !== executionData.id)]);

      setAlert({
        type: 'info',
        message: `Triggered execution #${executionData.id} for Test Case #${testIdToRun}.`
      });

      // Refresh executions list
      fetchExecutions();
    } catch (err) {
      setAlert({
        type: 'error',
        message: err.response?.data?.message || 'Failed to trigger test execution.'
      });
    } finally {
      setIsRunningTest(false);
    }
  };

  // Execution polling when an execution is RUNNING or PENDING
  useEffect(() => {
    const hasActiveExecution = executions.some(
      (e) => e.status === 'RUNNING' || e.status === 'PENDING'
    );

    if (hasActiveExecution) {
      executionPollingRef.current = setInterval(() => {
        fetchExecutions();
      }, 3000);
    } else {
      if (executionPollingRef.current) {
        clearInterval(executionPollingRef.current);
        executionPollingRef.current = null;
      }
    }

    return () => {
      if (executionPollingRef.current) {
        clearInterval(executionPollingRef.current);
      }
    };
  }, [executions, fetchExecutions]);

  // Open Execution Details Modal & Fetch Authenticated Screenshot
  const handleOpenExecutionDetails = (exec) => {
    setSelectedExecution(exec);
    setScreenshotUrl(null);
    setScreenshotError(null);
    setLoadingScreenshot(true);

    api
      .get(`/api/executions/${exec.id}/screenshot`, { responseType: 'blob' })
      .then((res) => {
        const url = URL.createObjectURL(res.data);
        setScreenshotUrl(url);
      })
      .catch(() => {
        setScreenshotError('No screenshot recorded or failed to fetch screenshot.');
      })
      .finally(() => {
        setLoadingScreenshot(false);
      });
  };

  const handleCloseExecutionModal = () => {
    if (screenshotUrl) {
      URL.revokeObjectURL(screenshotUrl);
    }
    setSelectedExecution(null);
    setScreenshotUrl(null);
    setScreenshotError(null);
  };

  // ===========================================================================
  // Panel 4: AI Bug Reports Handlers
  // ===========================================================================
  const handleAnalyzeBug = async (executionId) => {
    if (!executionId) return;
    setAnalyzingExecutionId(executionId);

    try {
      const response = await api.post(`/api/bugs/analyze/${executionId}`);
      const bugData = response.data;

      setBugs((prev) => [bugData, ...prev.filter((b) => b.id !== bugData.id)]);
      setAlert({
        type: 'success',
        message: `AI Bug Analysis complete for execution #${executionId}: "${bugData.title}"`
      });

      // Auto open bug details modal to show insights
      setSelectedBug(bugData);
    } catch (err) {
      setAlert({
        type: 'error',
        message: err.response?.data?.message || `Failed to analyze bug for execution #${executionId}.`
      });
    } finally {
      setAnalyzingExecutionId(null);
    }
  };

  // ===========================================================================
  // Helper Formatters
  // ===========================================================================
  const handleLogout = () => {
    localStorage.removeItem('token');
    window.location.href = '/login';
  };

  const safeJsonArrayCount = (raw) => {
    if (!raw) return 0;
    if (Array.isArray(raw)) return raw.length;
    if (typeof raw === 'string') {
      try {
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed.length : 0;
      } catch {
        return 0;
      }
    }
    return 0;
  };

  const renderStatusPill = (status) => {
    const s = (status || 'IDLE').toUpperCase();
    let pillClass = 'status-pill idle';
    let icon = null;

    if (s === 'RUNNING') {
      pillClass = 'status-pill running';
      icon = <span className="spinner spinner-sm"></span>;
    } else if (s === 'PENDING') {
      pillClass = 'status-pill pending';
      icon = <span className="pulse-dot"></span>;
    } else if (s === 'COMPLETED' || s === 'PASSED') {
      pillClass = 'status-pill completed';
      icon = <span>✓</span>;
    } else if (s === 'FAILED' || s === 'ERROR') {
      pillClass = 'status-pill failed';
      icon = <span>✕</span>;
    } else if (s === 'TIMEOUT') {
      pillClass = 'status-pill timeout';
      icon = <span>⏱</span>;
    }

    return (
      <span className={pillClass}>
        {icon}
        {s}
      </span>
    );
  };

  const renderPriorityBadge = (priority) => {
    const p = (priority || 'LOW').toUpperCase();
    let badgeClass = 'badge-priority badge-low';
    if (p === 'CRITICAL') badgeClass = 'badge-priority badge-critical';
    else if (p === 'HIGH') badgeClass = 'badge-priority badge-high';
    else if (p === 'MEDIUM') badgeClass = 'badge-priority badge-medium';

    return <span className={badgeClass}>{p}</span>;
  };

  const renderSeverityBadge = (severity) => {
    const s = (severity || 'LOW').toUpperCase();
    let badgeClass = 'badge-severity badge-low';
    if (s === 'CRITICAL') badgeClass = 'badge-severity badge-critical';
    else if (s === 'HIGH') badgeClass = 'badge-severity badge-high';
    else if (s === 'MEDIUM') badgeClass = 'badge-severity badge-medium';

    return <span className={badgeClass}>{s}</span>;
  };

  const renderActionBadge = (action) => {
    const a = (action || '').toUpperCase();
    let badgeClass = 'badge-action';
    if (a === 'FILL') badgeClass = 'badge-action badge-action-fill';
    else if (a === 'CLICK') badgeClass = 'badge-action badge-action-click';
    else if (a.startsWith('ASSERT')) badgeClass = 'badge-action badge-action-assert';
    else if (a === 'NAVIGATE') badgeClass = 'badge-action badge-action-navigate';
    else if (a === 'WAIT') badgeClass = 'badge-action badge-action-wait';

    return <span className={badgeClass}>{a}</span>;
  };

  if (profileLoading) {
    return (
      <div className="loading">
        <span className="spinner spinner-lg"></span>
        <p style={{ marginTop: '1rem' }}>Loading Testing Workbench...</p>
      </div>
    );
  }

  return (
    <div className="page-container">
      {/* Top Header & User Info */}
      <div className="workbench-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h1>AI Testing Workbench</h1>
            <p className="subtitle">
              Autonomous end-to-end web testing, exploration, execution, and root-cause analysis
            </p>
          </div>
          <button onClick={handleLogout} className="logout-btn">
            Logout
          </button>
        </div>

        {profile && (
          <div className="user-info">
            <div>
              <h2>Welcome back, {profile.name}!</h2>
              <div className="user-info-details">
                <span><strong>Email:</strong> {profile.email}</span>
                <span><strong>Role:</strong> {profile.role}</span>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  fetchTestCases();
                  fetchExecutions();
                  fetchBugs();
                }}
                title="Refresh workbench data"
              >
                ↻ Refresh All
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Global Alert Notification */}
      {alert && (
        <div className={`alert-banner ${alert.type}`}>
          <span>{alert.message}</span>
          <button className="alert-close" onClick={() => setAlert(null)} title="Dismiss">
            ✕
          </button>
        </div>
      )}

      {/* Workbench Tab Navigation */}
      <div className="workbench-tabs">
        <button
          className={`workbench-tab ${activeTab === 'all' ? 'active' : ''}`}
          onClick={() => setActiveTab('all')}
        >
          All Panels
        </button>
        <button
          className={`workbench-tab ${activeTab === 'exploration' ? 'active' : ''}`}
          onClick={() => setActiveTab('exploration')}
        >
          1. Exploration
          <span className="tab-badge">{discoveredPages.length}</span>
        </button>
        <button
          className={`workbench-tab ${activeTab === 'tests' ? 'active' : ''}`}
          onClick={() => setActiveTab('tests')}
        >
          2. AI Test Cases
          <span className="tab-badge">{testCases.length}</span>
        </button>
        <button
          className={`workbench-tab ${activeTab === 'executions' ? 'active' : ''}`}
          onClick={() => setActiveTab('executions')}
        >
          3. Test Runner
          <span className="tab-badge">{executions.length}</span>
        </button>
        <button
          className={`workbench-tab ${activeTab === 'bugs' ? 'active' : ''}`}
          onClick={() => setActiveTab('bugs')}
        >
          4. AI Bug Reports
          <span className="tab-badge">{bugs.length}</span>
        </button>
      </div>

      {/* Main 4-Panel Workbench Grid */}
      <div className={activeTab === 'all' ? 'workbench-grid' : ''}>
        {/* ================================================================= */}
        {/* PANEL 1: Application Exploration                                  */}
        {/* ================================================================= */}
        {(activeTab === 'all' || activeTab === 'exploration') && (
          <div className="workbench-panel">
            <div className="panel-header">
              <div className="panel-title-area">
                <span className="panel-number">1</span>
                <div>
                  <h2>Application Exploration</h2>
                  <p className="panel-desc">Autonomous crawler maps DOM elements, forms, and links</p>
                </div>
              </div>
              <div className="panel-actions">
                {renderStatusPill(explorationStatus)}
              </div>
            </div>

            <div className="panel-body">
              {/* Target URL Input & Start Exploration Button */}
              <form onSubmit={handleStartExploration} className="panel-control-bar">
                <input
                  type="url"
                  className="input-field input-flex"
                  placeholder="Target URL (e.g. http://localhost:3000)"
                  value={targetUrl}
                  onChange={(e) => setTargetUrl(e.target.value)}
                  required
                />
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={isStartingExploration || explorationStatus === 'RUNNING' || explorationStatus === 'PENDING'}
                >
                  {isStartingExploration ? (
                    <>
                      <span className="spinner spinner-sm"></span>
                      Starting...
                    </>
                  ) : explorationStatus === 'RUNNING' || explorationStatus === 'PENDING' ? (
                    <>
                      <span className="spinner spinner-sm"></span>
                      Crawling...
                    </>
                  ) : (
                    'Start Exploration'
                  )}
                </button>
              </form>

              {/* Exploration Metadata Banner */}
              {exploration && (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.825rem', color: '#64748b', background: '#f8fafc', padding: '0.6rem 0.85rem', borderRadius: '6px' }}>
                  <span><strong>Exploration ID:</strong> #{exploration.id}</span>
                  <span><strong>Target:</strong> {exploration.targetUrl}</span>
                  {explorationStatus === 'RUNNING' && <span>Auto-polling every 3s...</span>}
                </div>
              )}

              {/* Discovered Pages Table */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 style={{ fontSize: '0.95rem', fontWeight: 600, color: '#334155' }}>
                  Discovered Pages ({discoveredPages.length})
                </h3>
                {exploration?.id && (
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => fetchDiscoveredPages(exploration.id)}
                  >
                    Refresh Pages
                  </button>
                )}
              </div>

              {discoveredPages.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon">🌐</div>
                  <p className="empty-state-text">
                    {explorationStatus === 'RUNNING' || explorationStatus === 'PENDING'
                      ? 'Exploration in progress... Discovered pages will appear here.'
                      : 'No pages discovered yet. Enter target URL and click "Start Exploration".'}
                  </p>
                </div>
              ) : (
                <div className="table-wrapper">
                  <table className="workbench-table">
                    <thead>
                      <tr>
                        <th>Page URL</th>
                        <th>Title</th>
                        <th style={{ textAlign: 'center' }}>Buttons</th>
                        <th style={{ textAlign: 'center' }}>Inputs</th>
                        <th style={{ textAlign: 'center' }}>Links</th>
                        <th style={{ textAlign: 'center' }}>Forms</th>
                      </tr>
                    </thead>
                    <tbody>
                      {discoveredPages.map((page) => (
                        <tr key={page.id || page.pageUrl}>
                          <td style={{ fontWeight: 600, color: '#2563eb', maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={page.pageUrl}>
                            {page.pageUrl}
                          </td>
                          <td style={{ maxWidth: '160px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={page.pageTitle || 'N/A'}>
                            {page.pageTitle || 'Untitled'}
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            <span className="table-stat-badge">
                              {safeJsonArrayCount(page.buttons)}
                            </span>
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            <span className="table-stat-badge">
                              {safeJsonArrayCount(page.inputs)}
                            </span>
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            <span className="table-stat-badge">
                              {safeJsonArrayCount(page.discoveredLinks)}
                            </span>
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            <span className="table-stat-badge">
                              {safeJsonArrayCount(page.forms)}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* PANEL 2: AI Test Cases                                            */}
        {/* ================================================================= */}
        {(activeTab === 'all' || activeTab === 'tests') && (
          <div className="workbench-panel">
            <div className="panel-header">
              <div className="panel-title-area">
                <span className="panel-number">2</span>
                <div>
                  <h2>AI Test Cases</h2>
                  <p className="panel-desc">LLM-synthesized test scenarios with verified step actions</p>
                </div>
              </div>
              <div className="panel-actions">
                <button
                  className="btn btn-ai"
                  onClick={handleGenerateTests}
                  disabled={isGeneratingTests}
                >
                  {isGeneratingTests ? (
                    <>
                      <span className="spinner spinner-sm"></span>
                      Generating...
                    </>
                  ) : (
                    '✨ Generate AI Tests'
                  )}
                </button>
              </div>
            </div>

            <div className="panel-body">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 style={{ fontSize: '0.95rem', fontWeight: 600, color: '#334155' }}>
                  Generated Test Cases ({testCases.length})
                </h3>
                <button className="btn btn-secondary btn-sm" onClick={fetchTestCases}>
                  Refresh
                </button>
              </div>

              {testCases.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon">📋</div>
                  <p className="empty-state-text">
                    No test cases available yet. Click "Generate AI Tests" to synthesize tests from discovered pages.
                  </p>
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                  {testCases.map((tc) => {
                    const steps = parseSteps(tc.steps);
                    const isExpanded = expandedTestIds.has(tc.id);

                    return (
                      <div key={tc.id} className="test-card">
                        <div className="test-card-header">
                          <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                              <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#64748b' }}>
                                #{tc.id}
                              </span>
                              <span className="test-card-title">{tc.name}</span>
                            </div>
                            <p className="test-card-desc">{tc.description}</p>
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            {renderPriorityBadge(tc.priority)}
                            <button
                              className="btn btn-primary btn-sm"
                              onClick={() => handleTriggerExecution(tc.id)}
                              disabled={isRunningTest}
                              title="Run this specific test"
                            >
                              Run
                            </button>
                          </div>
                        </div>

                        {tc.expectedResult && (
                          <div className="test-card-expected">
                            <strong>Expected:</strong> {tc.expectedResult}
                          </div>
                        )}

                        {/* Expandable Step-by-Step Actions */}
                        {steps.length > 0 ? (
                          <div>
                            <button
                              className="steps-toggle-btn"
                              onClick={() => toggleTestSteps(tc.id)}
                            >
                              <span>{isExpanded ? '▼' : '►'}</span>
                              <span>
                                {isExpanded ? 'Hide Steps' : `View Steps (${steps.length} actions)`}
                              </span>
                            </button>

                            {isExpanded && (
                              <div className="steps-list">
                                {steps.map((st, idx) => (
                                  <div key={st.stepOrder || idx} className="step-item">
                                    <span className="step-order">#{st.stepOrder || idx + 1}</span>
                                    {renderActionBadge(st.action)}
                                    {st.target && <span className="step-target">{st.target}</span>}
                                    {st.value && <span className="step-value">"{st.value}"</span>}
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        ) : (
                          <span style={{ fontSize: '0.8rem', color: '#94a3b8' }}>
                            No discrete steps defined
                          </span>
                        )}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* PANEL 3: Test Runner & Executions                                 */}
        {/* ================================================================= */}
        {(activeTab === 'all' || activeTab === 'executions') && (
          <div className="workbench-panel">
            <div className="panel-header">
              <div className="panel-title-area">
                <span className="panel-number">3</span>
                <div>
                  <h2>Test Runner & Executions</h2>
                  <p className="panel-desc">Playwright execution engine with live status and screenshots</p>
                </div>
              </div>
              <div className="panel-actions">
                <button className="btn btn-secondary btn-sm" onClick={fetchExecutions}>
                  Refresh
                </button>
              </div>
            </div>

            <div className="panel-body">
              {/* Trigger Execution Control Bar */}
              <div className="panel-control-bar">
                <select
                  className="input-field"
                  value={selectedTestCaseId}
                  onChange={(e) => setSelectedTestCaseId(e.target.value)}
                  style={{ minWidth: '180px' }}
                >
                  <option value="" disabled>Select Test Case</option>
                  {testCases.map((tc) => (
                    <option key={tc.id} value={tc.id}>
                      #{tc.id} - {tc.name}
                    </option>
                  ))}
                  {testCases.length === 0 && <option value="1">#1 - Verify Login Workflow</option>}
                </select>

                <input
                  type="url"
                  className="input-field input-flex"
                  placeholder="Target URL (http://localhost:3000)"
                  value={executionTargetUrl}
                  onChange={(e) => setExecutionTargetUrl(e.target.value)}
                />

                <button
                  className="btn btn-primary"
                  onClick={() => handleTriggerExecution()}
                  disabled={isRunningTest}
                >
                  {isRunningTest ? (
                    <>
                      <span className="spinner spinner-sm"></span>
                      Running...
                    </>
                  ) : (
                    '▶ Run Test'
                  )}
                </button>
              </div>

              {/* Executions Table */}
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 style={{ fontSize: '0.95rem', fontWeight: 600, color: '#334155' }}>
                  Execution History ({executions.length})
                </h3>
              </div>

              {executions.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon">🚀</div>
                  <p className="empty-state-text">
                    No executions yet. Select a test case and click "Run Test" to execute with Playwright.
                  </p>
                </div>
              ) : (
                <div className="table-wrapper">
                  <table className="workbench-table">
                    <thead>
                      <tr>
                        <th>Execution ID</th>
                        <th>Test Case ID</th>
                        <th>Status</th>
                        <th>Started At</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {executions.map((exec) => {
                        const isFailed = exec.status === 'FAILED' || exec.status === 'ERROR';
                        const isAnalyzing = analyzingExecutionId === exec.id;

                        return (
                          <tr key={exec.id}>
                            <td style={{ fontWeight: 700, color: '#0f172a' }}>
                              #{exec.id}
                            </td>
                            <td>
                              <span style={{ fontWeight: 600, color: '#475569' }}>
                                #{exec.testCaseId || '1'}
                              </span>
                            </td>
                            <td>{renderStatusPill(exec.status)}</td>
                            <td style={{ fontSize: '0.8rem', color: '#64748b' }}>
                              {exec.startedAt ? new Date(exec.startedAt).toLocaleTimeString() : 'N/A'}
                            </td>
                            <td className="actions-cell">
                              <button
                                className="btn btn-secondary btn-sm"
                                onClick={() => handleOpenExecutionDetails(exec)}
                              >
                                View Details
                              </button>

                              {isFailed && (
                                <button
                                  className="btn btn-ai btn-sm"
                                  onClick={() => handleAnalyzeBug(exec.id)}
                                  disabled={isAnalyzing}
                                  title="Analyze failure cause using LLM"
                                >
                                  {isAnalyzing ? (
                                    <>
                                      <span className="spinner spinner-sm"></span>
                                      Analyzing...
                                    </>
                                  ) : (
                                    '🧠 Analyze Bug with AI'
                                  )}
                                </button>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ================================================================= */}
        {/* PANEL 4: AI Bug Reports                                           */}
        {/* ================================================================= */}
        {(activeTab === 'all' || activeTab === 'bugs') && (
          <div className="workbench-panel">
            <div className="panel-header">
              <div className="panel-title-area">
                <span className="panel-number">4</span>
                <div>
                  <h2>AI Bug Reports</h2>
                  <p className="panel-desc">Automated root cause diagnostics and actionable reproduction steps</p>
                </div>
              </div>
              <div className="panel-actions">
                <button className="btn btn-secondary btn-sm" onClick={fetchBugs}>
                  Refresh
                </button>
              </div>
            </div>

            <div className="panel-body">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <h3 style={{ fontSize: '0.95rem', fontWeight: 600, color: '#334155' }}>
                  Reported Defects ({bugs.length})
                </h3>
              </div>

              {bugs.length === 0 ? (
                <div className="empty-state">
                  <div className="empty-state-icon">🐞</div>
                  <p className="empty-state-text">
                    No bug reports found. Run a test case, and on failure click "Analyze Bug with AI" to generate a diagnostic report.
                  </p>
                </div>
              ) : (
                <div className="table-wrapper">
                  <table className="workbench-table">
                    <thead>
                      <tr>
                        <th>Bug ID</th>
                        <th>Title</th>
                        <th>Execution ID</th>
                        <th>Severity</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {bugs.map((b) => (
                        <tr key={b.id}>
                          <td style={{ fontWeight: 700, color: '#0f172a' }}>
                            #{b.id}
                          </td>
                          <td style={{ fontWeight: 600, maxWidth: '240px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={b.title}>
                            {b.title}
                          </td>
                          <td style={{ color: '#64748b', fontWeight: 600 }}>
                            #{b.executionId}
                          </td>
                          <td>{renderSeverityBadge(b.severity)}</td>
                          <td className="actions-cell">
                            <button
                              className="btn btn-secondary btn-sm"
                              onClick={() => setSelectedBug(b)}
                            >
                              View Report
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* =================================================================== */}
      {/* MODAL 1: Execution Details Modal (Error + Authenticated Screenshot)   */}
      {/* =================================================================== */}
      {selectedExecution && (
        <div className="modal-backdrop" onClick={handleCloseExecutionModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <span>Execution Details #{selectedExecution.id}</span>
                {renderStatusPill(selectedExecution.status)}
              </div>
              <button
                className="modal-close-btn"
                onClick={handleCloseExecutionModal}
                title="Close modal"
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              {/* Metadata Grid */}
              <div className="modal-meta-grid">
                <div className="modal-meta-item">
                  <span className="modal-meta-label">Test Case ID</span>
                  <span className="modal-meta-value">#{selectedExecution.testCaseId || '1'}</span>
                </div>
                <div className="modal-meta-item">
                  <span className="modal-meta-label">Target URL</span>
                  <span className="modal-meta-value">{selectedExecution.targetUrl || 'N/A'}</span>
                </div>
                <div className="modal-meta-item">
                  <span className="modal-meta-label">Started At</span>
                  <span className="modal-meta-value">
                    {selectedExecution.startedAt ? new Date(selectedExecution.startedAt).toLocaleString() : 'N/A'}
                  </span>
                </div>
                <div className="modal-meta-item">
                  <span className="modal-meta-label">Completed At</span>
                  <span className="modal-meta-value">
                    {selectedExecution.completedAt ? new Date(selectedExecution.completedAt).toLocaleString() : 'Running / Pending'}
                  </span>
                </div>
              </div>

              {/* Error Message Section */}
              {selectedExecution.errorMessage && (
                <div>
                  <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: '#b91c1c', marginBottom: '0.4rem' }}>
                    Error Message / Failure Trace:
                  </h4>
                  <div className="error-banner">
                    {selectedExecution.errorMessage}
                  </div>
                </div>
              )}

              {/* Screenshot Section */}
              <div className="screenshot-section">
                <h4>Execution Screenshot:</h4>
                <div className="screenshot-box">
                  {loadingScreenshot ? (
                    <div style={{ textAlign: 'center', color: '#94a3b8' }}>
                      <span className="spinner spinner-lg"></span>
                      <p style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>Loading screenshot...</p>
                    </div>
                  ) : screenshotUrl ? (
                    <img src={screenshotUrl} alt={`Execution #${selectedExecution.id} screenshot`} />
                  ) : (
                    <div className="screenshot-placeholder">
                      {screenshotError || 'No screenshot recorded for this execution.'}
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="modal-footer">
              {(selectedExecution.status === 'FAILED' || selectedExecution.status === 'ERROR') && (
                <button
                  className="btn btn-ai"
                  onClick={() => {
                    handleAnalyzeBug(selectedExecution.id);
                  }}
                  disabled={analyzingExecutionId === selectedExecution.id}
                >
                  {analyzingExecutionId === selectedExecution.id ? (
                    <>
                      <span className="spinner spinner-sm"></span>
                      Analyzing...
                    </>
                  ) : (
                    '🧠 Analyze Bug with AI'
                  )}
                </button>
              )}
              <button className="btn btn-secondary" onClick={handleCloseExecutionModal}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* =================================================================== */}
      {/* MODAL 2: Bug Report Details Modal                                    */}
      {/* =================================================================== */}
      {selectedBug && (
        <div className="modal-backdrop" onClick={() => setSelectedBug(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">
                <span>Bug Report #{selectedBug.id}</span>
                {renderSeverityBadge(selectedBug.severity)}
              </div>
              <button
                className="modal-close-btn"
                onClick={() => setSelectedBug(null)}
                title="Close modal"
              >
                ✕
              </button>
            </div>

            <div className="modal-body">
              <h3 style={{ fontSize: '1.15rem', color: '#0f172a', fontWeight: 700 }}>
                {selectedBug.title}
              </h3>

              {/* Metadata */}
              <div className="modal-meta-grid">
                <div className="modal-meta-item">
                  <span className="modal-meta-label">Associated Execution</span>
                  <span className="modal-meta-value">#{selectedBug.executionId}</span>
                </div>
                <div className="modal-meta-item">
                  <span className="modal-meta-label">Suspected Area</span>
                  <span className="modal-meta-value">{selectedBug.possibleArea || 'UI / Logic'}</span>
                </div>
                <div className="modal-meta-item">
                  <span className="modal-meta-label">Created At</span>
                  <span className="modal-meta-value">
                    {selectedBug.createdAt ? new Date(selectedBug.createdAt).toLocaleString() : 'N/A'}
                  </span>
                </div>
              </div>

              {/* Description / Summary */}
              <div>
                <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: '#334155', marginBottom: '0.35rem' }}>
                  Description & Diagnostic Summary
                </h4>
                <div style={{ background: '#f8fafc', padding: '0.85rem 1rem', borderRadius: '6px', border: '1px solid #e2e8f0', fontSize: '0.9rem', color: '#1e293b', lineHeight: 1.5 }}>
                  {selectedBug.summary || 'No detailed summary provided.'}
                </div>
              </div>

              {/* Expected vs Actual (Reproduction Steps) */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1rem' }}>
                <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: '6px', padding: '0.85rem' }}>
                  <h5 style={{ color: '#15803d', fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.35rem' }}>
                    ✓ Expected Result
                  </h5>
                  <p style={{ fontSize: '0.875rem', color: '#166534' }}>
                    {selectedBug.expectedResult || 'Expected operation to succeed without errors.'}
                  </p>
                </div>
                <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '6px', padding: '0.85rem' }}>
                  <h5 style={{ color: '#b91c1c', fontSize: '0.85rem', fontWeight: 700, marginBottom: '0.35rem' }}>
                    ✕ Actual Result (Reproduction Evidence)
                  </h5>
                  <p style={{ fontSize: '0.875rem', color: '#991b1b' }}>
                    {selectedBug.actualResult || 'Operation triggered an unexpected failure.'}
                  </p>
                </div>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn btn-secondary" onClick={() => setSelectedBug(null)}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
