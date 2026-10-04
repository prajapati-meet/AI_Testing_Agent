import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import Register from './pages/Register';

function App() {
  return (
    <Router>
      <div className="app-shell">
        <header className="navbar">
          <div className="brand">AI Test Agent</div>
          <nav className="nav-links">
            <Link to="/">Dashboard</Link>
            <Link to="/login">Login</Link>
            <Link to="/register">Register</Link>
          </nav>
        </header>
        <main className="main-content">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
