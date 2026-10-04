import axios from 'axios';

const API_GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_GATEWAY_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

export default api;
