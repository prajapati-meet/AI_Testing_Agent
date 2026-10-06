import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

const Dashboard = () => {
  const navigate = useNavigate();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/login');
      return;
    }

    const fetchProfile = async () => {
      try {
        const response = await api.get('/api/auth/me');
        setProfile(response.data);
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to fetch profile');
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    window.location.href = '/login';
  };

  if (loading) {
    return <div className="loading">Loading dashboard...</div>;
  }

  if (error) {
    return (
      <div className="page-container" style={{ padding: '2rem' }}>
        <div className="error-msg">{error}</div>
        <button onClick={handleLogout} className="logout-btn" style={{ marginTop: '1rem' }}>Logout</button>
      </div>
    );
  }

  return (
    <div className="page-container" style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1>Dashboard</h1>
        <button onClick={handleLogout} className="logout-btn">Logout</button>
      </div>

      {profile && (
        <div className="user-info">
          <h2>Welcome, {profile.name}!</h2>
          <p>Email: {profile.email}</p>
          <p>Role: {profile.role}</p>
        </div>
      )}

      <div className="card-grid">
        <div className="card">
          <h3>Exploration</h3>
          <p>Analyze and explore test subjects.</p>
        </div>
        <div className="card">
          <h3>Test/AI</h3>
          <p>Generate and manage test cases using AI.</p>
        </div>
        <div className="card">
          <h3>Execution</h3>
          <p>Run tests and view execution reports.</p>
        </div>
        <div className="card">
          <h3>Bug Analysis</h3>
          <p>Review and analyze reported bugs.</p>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
