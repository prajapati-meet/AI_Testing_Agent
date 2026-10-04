import React, { useState } from 'react';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [statusMessage, setStatusMessage] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    setStatusMessage(`Placeholder login submitted for ${email}`);
  };

  return (
    <div className="page-container form-wrapper">
      <h2>Login to AI Test Agent</h2>
      <form onSubmit={handleSubmit} className="auth-form">
        <label htmlFor="login-email">Email</label>
        <input
          id="login-email"
          type="email"
          placeholder="engineer@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <label htmlFor="login-password">Password</label>
        <input
          id="login-password"
          type="password"
          placeholder="Enter password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        <button type="submit">Login</button>
      </form>
      {statusMessage && <p className="status-msg">{statusMessage}</p>}
    </div>
  );
}

export default Login;
