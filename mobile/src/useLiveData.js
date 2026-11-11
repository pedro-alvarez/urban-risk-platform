import { useCallback, useEffect, useRef, useState } from 'react';
import { Vibration } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { api } from './api';

const POLL_MS = 5000; // tempo de atualização
const CACHE_KEY = 'urb.cache.v1';

/**
 * Mantém os dados do app atualizados em tempo (quase) real, consultando a API a cada 5 s.
 *  - Se o servidor cair: marca online = false, MANTÉM os últimos dados exibidos e segue tentando.
 *  - Quando voltar: marca recovered = true por alguns segundos e atualiza a tela sozinho.
 *  - Novos alertas (notificações com id maior que o último visto) disparam `liveAlert`.
 */
export function useLiveData(baseUrl) {
  const [state, setState] = useState({
    events: [],
    dashboard: null,
    sources: [],
    notifications: [],
    loaded: false,
  });
  const [online, setOnline] = useState(true);
  const [failures, setFailures] = useState(0);
  const [lastUpdate, setLastUpdate] = useState(null);
  const [recovered, setRecovered] = useState(false);
  const [liveAlert, setLiveAlert] = useState(null);

  const lastNotificationId = useRef(null);
  const wasOffline = useRef(false);
  const busy = useRef(false);
  const alive = useRef(true);

  // abre o app já com os últimos dados salvos (útil sem conexão)
  useEffect(() => {
    AsyncStorage.getItem(CACHE_KEY)
      .then((raw) => {
        if (raw && alive.current) {
          const cached = JSON.parse(raw);
          setState((s) => (s.loaded ? s : { ...cached, loaded: false }));
          if (cached.savedAt) {
            setLastUpdate(new Date(cached.savedAt));
          }
        }
      })
      .catch(() => {});
    return () => {
      alive.current = false;
    };
  }, []);

  const refresh = useCallback(async () => {
    if (busy.current) {
      return;
    }
    busy.current = true;
    try {
      const [dashboard, events, notifications, sources] = await Promise.all([
        api.dashboard(baseUrl),
        api.events(baseUrl),
        api.notifications(baseUrl),
        api.sources(baseUrl),
      ]);
      if (!alive.current) {
        return;
      }
      // alertas novos desde a última atualização
      const maxId = notifications.reduce((m, n) => Math.max(m, n.id), 0);
      if (lastNotificationId.current !== null) {
        const fresh = notifications
          .filter((n) => n.id > lastNotificationId.current && n.kind === 'ALERTA')
          .sort((a, b) => a.id - b.id);
        if (fresh.length) {
          setLiveAlert(fresh[fresh.length - 1]);
          Vibration.vibrate([0, 300, 150, 300]);
        }
      }
      lastNotificationId.current = Math.max(maxId, lastNotificationId.current || 0);

      const next = { events, dashboard, sources, notifications, loaded: true };
      setState(next);
      setLastUpdate(new Date());
      AsyncStorage.setItem(CACHE_KEY, JSON.stringify({ ...next, savedAt: Date.now() })).catch(() => {});

      if (wasOffline.current) {
        wasOffline.current = false;
        setRecovered(true);
        Vibration.vibrate(120);
        setTimeout(() => alive.current && setRecovered(false), 5000);
      }
      setFailures(0);
      setOnline(true);
    } catch (e) {
      if (!alive.current) {
        return;
      }
      wasOffline.current = true;
      setOnline(false);
      setFailures((f) => f + 1);
    } finally {
      busy.current = false;
    }
  }, [baseUrl]);

  useEffect(() => {
    lastNotificationId.current = null; // ao trocar de servidor, não dispara alertas antigos
    refresh();
    const timer = setInterval(refresh, POLL_MS);
    return () => clearInterval(timer);
  }, [refresh]);

  return {
    ...state,
    online,
    failures,
    lastUpdate,
    recovered,
    liveAlert,
    dismissAlert: () => setLiveAlert(null),
    refresh,
  };
}
