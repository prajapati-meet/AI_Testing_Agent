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

function LoginPage({ bugMode }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = (e) => {
    e.preventDefault();
    setError('');

    if (bugMode) {
      setError('Login failed: Authentication service returned an unexpected error (BUG_MODE=true).');
      return;
    }

    if (email === DEMO_CONFIG.DEMO_EMAIL && password === DEMO_CONFIG.DEMO_PASSWORD) {
      navigate('/dashboard');
    } else {
      setError('Invalid email or password. Use test@example.com / password123.');
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

function DashboardPage() {
  return (
    <div className="page">
      <h2>Customer Account Dashboard</h2>
      <p>Welcome back, <strong>{DEMO_CONFIG.DEMO_EMAIL}</strong>! Login succeeded.</p>
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

function App() {
  const [bugMode, setBugMode] = useState(DEMO_CONFIG.BUG_MODE);

  return (
    <Router>
      <div className="target-app">
        <header className="header">
          <div className="logo">DemoShop (Target App :3000)</div>
          <nav className="nav">
            <Link to="/">Home</Link>
            <Link to="/login">Login</Link>
            <Link to="/dashboard">Dashboard</Link>
            <Link to="/products">Products</Link>
            <Link to="/cart">Cart</Link>
            <Link to="/checkout">Checkout</Link>
          </nav>
          <button
            type="button"
            className={`bug-toggle ${bugMode ? 'active' : ''}`}
            onClick={() => setBugMode((prev) => !prev)}
          >
            BUG_MODE={String(bugMode)}
          </button>
        </header>
        <main className="container">
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage bugMode={bugMode} />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/products" element={<ProductsPage />} />
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
