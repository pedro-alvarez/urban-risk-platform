import React, { useEffect, useState } from 'react';
import { Platform, Pressable, StatusBar, StyleSheet, Text, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { DEFAULT_BASE_URL } from './src/api';
import { useLiveData } from './src/useLiveData';
import { colors } from './src/theme';
import {
  DashboardScreen,
  EventDetail,
  EventsScreen,
  NotificationsScreen,
  ReportScreen,
  SettingsScreen,
} from './src/screens';

const TABS = [
  { key: 'painel', label: 'Painel', icon: '▦' },
  { key: 'eventos', label: 'Eventos', icon: '▲' },
  { key: 'notificacoes', label: 'Alertas', icon: '✉' },
  { key: 'registrar', label: 'Relatar', icon: '＋' },
  { key: 'ajustes', label: 'Ajustes', icon: '⚙' },
];

const topInset = Platform.OS === 'ios' ? 54 : (StatusBar.currentHeight || 24) + 6;

export default function App() {
  const [baseUrl, setBaseUrl] = useState(DEFAULT_BASE_URL);
  const [persona, setPersona] = useState('agente');
  const [tab, setTab] = useState('painel');
  const [selectedId, setSelectedId] = useState(null);

  // restaura preferências salvas
  useEffect(() => {
    AsyncStorage.multiGet(['urb.baseUrl', 'urb.persona'])
      .then((pairs) => {
        const saved = Object.fromEntries(pairs);
        if (saved['urb.baseUrl']) setBaseUrl(saved['urb.baseUrl']);
        if (saved['urb.persona']) setPersona(saved['urb.persona']);
      })
      .catch(() => {});
  }, []);

  const live = useLiveData(baseUrl);
  const selected = selectedId ? live.events.find((e) => e.id === selectedId) : null;

  const changeUrl = (url) => {
    setBaseUrl(url);
    AsyncStorage.setItem('urb.baseUrl', url).catch(() => {});
  };
  const changePersona = (p) => {
    setPersona(p);
    AsyncStorage.setItem('urb.persona', p).catch(() => {});
  };
  const openEvent = (e) => setSelectedId(e.id);

  const hhmm = live.lastUpdate
    ? live.lastUpdate.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '—';

  return (
    <View style={[styles.root, { paddingTop: topInset }]}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.header}>
        <Text style={styles.title}>Urban Risk Intelligence</Text>
        <View style={[styles.dot, { backgroundColor: live.online ? colors.ok : colors.danger }]} />
        <Text style={styles.status}>{live.online ? `online · ${hhmm}` : 'offline'}</Text>
      </View>

      {!live.online ? (
        <View style={[styles.banner, { backgroundColor: colors.danger }]} accessibilityRole="alert">
          <Text style={styles.bannerText}>
            ⚠ Sem conexão com o servidor. Exibindo dados de {hhmm}. Tentando reconectar… (tentativa {live.failures})
          </Text>
        </View>
      ) : null}
      {live.recovered ? (
        <View style={[styles.banner, { backgroundColor: colors.ok }]} accessibilityRole="alert">
          <Text style={styles.bannerText}>✔ Conexão restabelecida. Dados atualizados.</Text>
        </View>
      ) : null}
      {live.liveAlert ? (
        <Pressable
          style={[styles.banner, { backgroundColor: colors.crit }]}
          onPress={() => {
            const ev = live.events.find((e) => e.code === live.liveAlert.eventCode);
            live.dismissAlert();
            if (ev) setSelectedId(ev.id);
          }}
          accessibilityRole="alert"
        >
          <Text style={styles.bannerText}>🔔 {live.liveAlert.message} (toque para abrir)</Text>
        </Pressable>
      ) : null}

      <View style={{ flex: 1 }}>
        {tab === 'painel' && (
          <DashboardScreen dashboard={live.dashboard} events={live.events} online={live.online} onOpen={openEvent} onGoToEvents={() => setTab('eventos')} />
        )}
        {tab === 'eventos' && <EventsScreen events={live.events} onOpen={openEvent} />}
        {tab === 'notificacoes' && <NotificationsScreen notifications={live.notifications} events={live.events} onOpen={openEvent} />}
        {tab === 'registrar' && (
          <ReportScreen
            baseUrl={baseUrl}
            online={live.online}
            onSent={(ev) => {
              live.refresh();
              setTab('eventos');
            }}
          />
        )}
        {tab === 'ajustes' && (
          <SettingsScreen
            baseUrl={baseUrl}
            onChangeUrl={changeUrl}
            persona={persona}
            onChangePersona={changePersona}
            online={live.online}
            failures={live.failures}
            lastUpdate={live.lastUpdate}
            sources={live.sources}
          />
        )}
        <EventDetail
          event={selected}
          persona={persona}
          baseUrl={baseUrl}
          online={live.online}
          onClose={() => setSelectedId(null)}
          onChanged={live.refresh}
        />
      </View>

      <View style={styles.tabs}>
        {TABS.map((t) => (
          <Pressable
            key={t.key}
            accessibilityRole="tab"
            accessibilityState={{ selected: tab === t.key }}
            style={styles.tab}
            onPress={() => {
              setTab(t.key);
              setSelectedId(null);
            }}
          >
            <Text style={[styles.tabIcon, tab === t.key && { color: colors.brand }]}>{t.icon}</Text>
            <Text style={[styles.tabLabel, tab === t.key && { color: colors.brand, fontWeight: '800' }]}>{t.label}</Text>
          </Pressable>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  header: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingBottom: 8 },
  title: { flex: 1, fontSize: 16, fontWeight: '800', color: colors.ink },
  dot: { width: 10, height: 10, borderRadius: 5, marginRight: 6 },
  status: { fontSize: 12, color: colors.muted },
  banner: { paddingVertical: 8, paddingHorizontal: 14 },
  bannerText: { color: '#fff', fontWeight: '700', fontSize: 13 },
  tabs: { flexDirection: 'row', borderTopColor: colors.line, borderTopWidth: 1, backgroundColor: colors.card, paddingBottom: Platform.OS === 'ios' ? 18 : 6 },
  tab: { flex: 1, alignItems: 'center', paddingVertical: 8 },
  tabIcon: { fontSize: 18, color: colors.muted },
  tabLabel: { fontSize: 11, color: colors.muted },
});
