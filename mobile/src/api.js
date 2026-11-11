import { Platform } from 'react-native';

/** Endereço padrão do backend. No emulador Android, 10.0.2.2 aponta para o computador. */
export const DEFAULT_BASE_URL = Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';

const TIMEOUT_MS = 4000;

export class ApiError extends Error {
  constructor(message, { status = 0, network = false } = {}) {
    super(message);
    this.status = status;
    this.network = network; // true = servidor inacessível (sem conexão / timeout)
  }
}

/** Faz a requisição com timeout. Falhas de rede viram ApiError com network = true (o app não trava). */
async function request(baseUrl, path, options = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(baseUrl + path, {
      ...options,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...(options.headers || {}) },
      signal: controller.signal,
    });
    let data = null;
    try {
      data = await res.json();
    } catch (e) {
      data = null;
    }
    if (!res.ok) {
      const detail = data && data.details && data.details.length ? ' (' + data.details.join('; ') + ')' : '';
      throw new ApiError(((data && data.message) || 'Erro ' + res.status) + detail, { status: res.status });
    }
    return data;
  } catch (e) {
    if (e instanceof ApiError) {
      throw e;
    }
    throw new ApiError('Servidor inacessível', { network: true });
  } finally {
    clearTimeout(timer);
  }
}

export const api = {
  dashboard: (base) => request(base, '/api/dashboard'),
  events: (base) => request(base, '/api/events'),
  notifications: (base) => request(base, '/api/notifications'),
  sources: (base) => request(base, '/api/sources'),
  transition: (base, id, action, actor) =>
    request(base, '/api/events/' + id + '/transitions', {
      method: 'POST',
      body: JSON.stringify({ action, actor }),
    }),
  communityReport: (base, report) =>
    request(base, '/api/community-reports', { method: 'POST', body: JSON.stringify(report) }),
};
