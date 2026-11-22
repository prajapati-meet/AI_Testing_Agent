import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link, useNavigate } from 'react-router-dom';
import { DEMO_CONFIG } from './config';

function HomePage() {
  return (
    <div className="page">
      <h1>Demo Target E-Commerce Store</h1>
      <p>This standalone web application is the target system under test for the AI Test Agent.</p>
      <div className="links-box">
        <Link to="/login" className="btn">Go to Login</Link>
        <Link to="/products" className="btn secondary">Browse Products</Link>
      </div>
    </div>
  );
}

function LoginPage({ onLoginSuccess }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = (e) => {
    e.preventDefault();
    setError('');

    if (email === DEMO_CONFIG.DEMO_EMAIL && password === DEMO_CONFIG.DEMO_PASSWORD) {
      if (onLoginSuccess) onLoginSuccess();
      navigate('/dashboard');
    } else {
      // REAL BUG (Defect #DEF-AUTH-401):
      // When invalid credentials are submitted, the application fails to display
      // the error banner (#login-error) due to a missing state update (Silent Failure).
      // The user receives zero visual feedback explaining why login was rejected.
      console.warn('[AUTH] Login rejected: Invalid credentials provided for ' + email);
    }
  };

  return (
    <div className="page form-card">
      <h2>Customer Login</h2>
      <p className="hint">Demo Credentials: <code>test@example.com</code> / <code>password123</code></p>
      <form onSubmit={handleLogin} className="login-form">
        <label htmlFor="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="test@example.com"
          required
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="password123"
          required
        />

        <button id="login-submit" type="submit" className="btn">Sign In</button>
      </form>
      {error && <div id="login-error" className="error-banner">{error}</div>}
    </div>
  );
}

function DashboardPage({ onLogout }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    if (onLogout) onLogout();
    navigate('/login');
  };

  return (
    <div id="dashboard-container" className="page dashboard-container">
      <h2>Customer Account Dashboard</h2>
      <p>Welcome back, <strong>{DEMO_CONFIG.DEMO_EMAIL}</strong>! Login succeeded.</p>
      <div style={{ marginTop: '1.5rem' }}>
        <button id="logout-button" className="btn secondary" onClick={handleLogout}>
          Sign Out / Logout
        </button>
      </div>
    </div>
  );
}

function ProductsPage() {
  return (
    <div className="page">
      <h2>Products Catalog (Placeholder)</h2>
      <ul>
        <li>Wireless Mechanical Keyboard — $89.00</li>
        <li>Ergonomic Developer Mouse — $59.00</li>
        <li>USB-C 4K Monitor Hub — $45.00</li>
      </ul>
    </div>
  );
}

function CartPage() {
  return (
    <div className="page">
      <h2>Shopping Cart (Placeholder)</h2>
      <p>Your cart currently contains 1 placeholder item.</p>
      <Link to="/checkout" className="btn">Proceed to Checkout</Link>
    </div>
  );
}

function CheckoutPage() {
  return (
    <div className="page">
      <h2>Checkout (Placeholder)</h2>
      <p>Order summary and payment placeholder for automated testing.</p>
    </div>
  );
}

function AppContent() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const navigate = useNavigate();

  const handleLogout = () => {
    setIsLoggedIn(false);
    navigate('/login');
  };

  return (
    <div className="target-app">
      <header className="header">
        <div className="logo">DemoShop (Target App :3000)</div>
        <nav className="nav">
          <Link to="/">Home</Link>
          {!isLoggedIn ? (
            <Link to="/login">Login</Link>
          ) : (
            <button
              id="header-logout-btn"
              onClick={handleLogout}
              style={{
                background: 'none',
                border: 'none',
                color: '#fff',
                cursor: 'pointer',
                font: 'inherit',
                padding: '0 0.5rem',
                textDecoration: 'underline'
              }}
            >
              Logout
            </button>
          )}
          <Link to="/dashboard">Dashboard</Link>
          <Link to="/products">Products</Link>
          <Link to="/cart">Cart</Link>
          <Link to="/checkout">Checkout</Link>
        </nav>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage onLoginSuccess={() => setIsLoggedIn(true)} />} />
          <Route path="/dashboard" element={<DashboardPage onLogout={() => setIsLoggedIn(false)} />} />
          <Route path="/products" element={<ProductsPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <Router>
      <AppContent />
    </Router>
  );
}

export default App;
