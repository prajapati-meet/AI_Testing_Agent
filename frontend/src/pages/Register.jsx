import React, { useState } from 'react';

function Register() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [statusMessage, setStatusMessage] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    setStatusMessage(`Placeholder registration submitted for ${name} (${email})`);
  };

  return (
    <div className="page-container form-wrapper">
      <h2>Register Account</h2>
      <form onSubmit={handleSubmit} className="auth-form">
        <label htmlFor="register-name">Full Name</label>
        <input
          id="register-name"
          type="text"
          placeholder="QA Developer"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />

        <label htmlFor="register-email">Email</label>
        <input
          id="register-email"
          type="email"
          placeholder="engineer@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <label htmlFor="register-password">Password</label>
        <input
          id="register-password"
          type="password"
          placeholder="Create password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        <button type="submit">Register</button>
      </form>
      {statusMessage && <p className="status-msg">{statusMessage}</p>}
    </div>
  );
}

export default Register;
