import React, { useMemo, useState } from 'react';
import { Alert, FlatList, Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from 'react-native';
import { api } from './api';
import { Button, Card, ConfidenceBar, EventRow, Pill, RiskPill, StatusPill, TypePill } from './components';
import { colors, statusLabel } from './theme';

export const NEIGHBORHOODS = ['Ribeira', 'Sol', 'Central', 'Vila Nova', 'Jardim das Flores', 'Porto Seco'];
const TYPES = [
  { key: 'ALAGAMENTO', label: 'Alagamento' },
  { key: 'CALOR', label: 'Ilha de calor' },
  { key: 'SERVICOS', label: 'Serviço essencial' },
];

function Chip({ label, selected, onPress }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ selected }}
      onPress={onPress}
      style={[s.chip, selected && s.chipOn]}
    >
      <Text style={[s.chipText, selected && { color: '#fff' }]}>{label}</Text>
    </Pressable>
  );
}

// ---------------------------------------------------------------- Painel
export function DashboardScreen({ dashboard, events, online, onOpen, onGoToEvents }) {
  const top = events.slice(0, 3);
  const k = dashboard;
  return (
    <ScrollView contentContainerStyle={s.pad}>
      <Text style={s.h1}>Painel de acompanhamento</Text>
      {!k ? (
        <Text style={s.muted}>{online ? 'Carregando dados…' : 'Sem dados ainda. Conecte-se ao servidor.'}</Text>
      ) : (
        <View style={s.kpis}>
          <Kpi value={k.alertsIssued} label="Alertas emitidos" />
          <Kpi value={k.awaitingValidation} label="Aguardando validação" />
          <Kpi value={`${k.averageConfidence}%`} label="Confiança média" />
          <Kpi value={`${k.sourcesOnline}/${k.sourcesTotal}`} label="Fontes online" />
        </View>
      )}
      <Text style={s.h2}>Alertas prioritários</Text>
      <Card>
        {top.length === 0 ? <Text style={s.muted}>Nenhum evento ativo.</Text> : top.map((e) => <EventRow key={e.id} event={e} onPress={onOpen} />)}
        <Button title="Ver todos os eventos" secondary onPress={onGoToEvents} />
      </Card>
    </ScrollView>
  );
}

function Kpi({ value, label }) {
  return (
    <View style={s.kpi}>
      <Text style={s.kpiValue}>{value}</Text>
      <Text style={s.kpiLabel}>{label}</Text>
    </View>
  );
}

// ---------------------------------------------------------------- Eventos
export function EventsScreen({ events, onOpen }) {
  const [type, setType] = useState(null);
  const list = useMemo(() => events.filter((e) => !type || e.type === type), [events, type]);
  return (
    <View style={{ flex: 1 }}>
      <View style={s.chips}>
        <Chip label="Todos" selected={!type} onPress={() => setType(null)} />
        {TYPES.map((t) => (
          <Chip key={t.key} label={t.label} selected={type === t.key} onPress={() => setType(t.key)} />
        ))}
      </View>
      <FlatList
        data={list}
        keyExtractor={(e) => String(e.id)}
        contentContainerStyle={s.pad}
        ListHeaderComponent={<Text style={s.muted}>Ordenados por prioridade ({list.length})</Text>}
        ListEmptyComponent={<Text style={s.muted}>Nenhum evento.</Text>}
        renderItem={({ item }) => <EventRow event={item} onPress={onOpen} />}
      />
    </View>
  );
}

// ---------------------------------------------------------------- Detalhe
const ACTIONS_BY_STATUS = {
  REPORTADO: ['VALIDAR', 'DESCARTAR'],
  EM_VALIDACAO: ['CONFIRMAR', 'DESCARTAR'],
  CONFIRMADO: ['ALERTAR', 'MONITORAR'],
  ALERTA_EMITIDO: ['MONITORAR'],
  EM_MONITORAMENTO: ['ALERTAR', 'ENCERRAR'],
  ENCERRADO: [],
  DESCARTADO: [],
};
const ACTION_LABEL = {
  VALIDAR: 'Iniciar validação',
  CONFIRMAR: 'Confirmar evento',
  ALERTAR: 'Emitir alerta',
  MONITORAR: 'Monitorar',
  ENCERRAR: 'Encerrar',
  DESCARTAR: 'Descartar',
};

export function EventDetail({ event, persona, baseUrl, online, onClose, onChanged }) {
  const [busy, setBusy] = useState(false);
  if (!event) {
    return null;
  }
  const act = async (action) => {
    if (!online) {
      Alert.alert('Sem conexão', 'Não é possível alterar o evento sem conexão com o servidor.');
      return;
    }
    setBusy(true);
    try {
      await api.transition(baseUrl, event.id, action, 'agente-app');
      onChanged();
    } catch (e) {
      Alert.alert('Não foi possível concluir', e.message);
    } finally {
      setBusy(false);
    }
  };
  const t = event.trend;
  return (
    <View style={s.overlay}>
      <ScrollView contentContainerStyle={s.pad}>
        <Button title="← Voltar" secondary onPress={onClose} />
        <Text style={s.h1}>{event.title}</Text>
        <Text style={s.muted}>
          {event.code} · {event.neighborhood}
          {event.vulnerableArea ? ' · área vulnerável ★' : ''}
        </Text>
        <View style={s.pillsRow}>
          <TypePill type={event.type} label={event.typeLabel} />
          <RiskPill risk={event.risk} />
          <StatusPill status={event.status} />
          <Pill text={`Prioridade ${event.priority}`} color="#546e7a" />
        </View>
        <Card>
          <Text style={s.h2}>Descrição</Text>
          <Text style={s.body}>{event.description}</Text>
          <Text style={s.h2}>Origem e confiança</Text>
          <ConfidenceBar value={event.confidence} level={event.confidenceLevel} />
          {event.staleSource ? <Text style={s.warn}>⚠ Fonte principal sem atualização: confiança reduzida (-20).</Text> : null}
          {event.confidenceLevel === 'BAIXA' ? (
            <Text style={s.warn}>Confiança baixa: exige validação antes de emitir alerta.</Text>
          ) : null}
          {event.evidences.map((v) => (
            <Text key={v.sourceName} style={s.body}>
              • {v.sourceName} ({v.originLabel}){v.measurement != null ? ` – ${v.measurement}` : ''}
              {v.readings > 1 ? ` · ${v.readings} leituras` : ''}
            </Text>
          ))}
          {t && t.direction !== 'ESTAVEL' ? (
            <Text style={s.body}>
              Tendência: {t.direction === 'SUBINDO' ? '▲ subindo' : '▼ caindo'} ({t.slopePerMinute}/min)
              {t.minutesToLimit != null ? ` · limite em ≈ ${t.minutesToLimit} min` : ''}
            </Text>
          ) : null}
        </Card>
        <Card>
          <Text style={s.h2}>Ciclo de vida</Text>
          {event.history.map((h, i) => (
            <Text key={i} style={s.body}>
              {new Date(h.at).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })} –{' '}
              {statusLabel[h.status]} ({h.actor})
            </Text>
          ))}
        </Card>
        {persona === 'agente' ? (
          <Card>
            <Text style={s.h2}>Ações do agente</Text>
            <View style={s.pillsRow}>
              {(ACTIONS_BY_STATUS[event.status] || []).map((a) => (
                <Button key={a} title={ACTION_LABEL[a]} disabled={busy} onPress={() => act(a)} secondary={a === 'DESCARTAR' || a === 'MONITORAR' || a === 'ENCERRAR'} />
              ))}
              {(ACTIONS_BY_STATUS[event.status] || []).length === 0 ? <Text style={s.muted}>Evento finalizado.</Text> : null}
            </View>
          </Card>
        ) : (
          <Text style={s.muted}>Apenas agentes podem alterar o ciclo de vida do evento (troque o perfil em Ajustes).</Text>
        )}
      </ScrollView>
    </View>
  );
}

// ---------------------------------------------------------------- Notificações
export function NotificationsScreen({ notifications, events, onOpen }) {
  return (
    <FlatList
      data={notifications}
      keyExtractor={(n) => String(n.id)}
      contentContainerStyle={s.pad}
      ListHeaderComponent={<Text style={s.h1}>Central de notificações</Text>}
      ListEmptyComponent={<Text style={s.muted}>Nenhuma notificação.</Text>}
      renderItem={({ item }) => {
        const ev = events.find((e) => e.code === item.eventCode);
        return (
          <Pressable disabled={!ev} onPress={() => ev && onOpen(ev)} style={[s.notif, item.kind === 'ALERTA' && { borderLeftColor: colors.danger }]}>
            <Text style={s.body}>{item.message}</Text>
            <Text style={s.meta}>
              {item.eventCode} · {new Date(item.createdAt).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
              {ev ? ' · toque para abrir' : ''}
            </Text>
          </Pressable>
        );
      }}
    />
  );
}

// ---------------------------------------------------------------- Registro comunitário
export function ReportScreen({ baseUrl, online, onSent }) {
  const [type, setType] = useState('ALAGAMENTO');
  const [neighborhood, setNeighborhood] = useState('Ribeira');
  const [severity, setSeverity] = useState('MEDIO');
  const [description, setDescription] = useState('');
  const [consent, setConsent] = useState(false);
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    if (description.trim().length < 10) {
      Alert.alert('Descrição curta', 'Descreva a ocorrência com pelo menos 10 caracteres.');
      return;
    }
    if (!consent) {
      Alert.alert('Consentimento necessário', 'Concorde com o tratamento dos dados para enviar o relato (LGPD).');
      return;
    }
    if (!online) {
      Alert.alert('Sem conexão', 'Não foi possível enviar agora. Tente novamente quando a conexão voltar.');
      return;
    }
    setBusy(true);
    try {
      const ev = await api.communityReport(baseUrl, { type, neighborhood, severity, description: description.trim(), hasPhoto: false, consent });
      setDescription('');
      setConsent(false);
      Alert.alert('Relato enviado', `Registrado como ${ev.code}. Confiança inicial ${ev.confidence}% (será validado).`);
      onSent(ev);
    } catch (e) {
      Alert.alert('Não foi possível enviar', e.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <ScrollView contentContainerStyle={s.pad} keyboardShouldPersistTaps="handled">
      <Text style={s.h1}>Registrar ocorrência</Text>
      <Text style={s.muted}>Seu relato é anônimo para outros usuários e entra como “Reportado”.</Text>
      <Text style={s.label}>Tipo</Text>
      <View style={s.chipsWrap}>
        {TYPES.map((t) => (
          <Chip key={t.key} label={t.label} selected={type === t.key} onPress={() => setType(t.key)} />
        ))}
      </View>
      <Text style={s.label}>Bairro</Text>
      <View style={s.chipsWrap}>
        {NEIGHBORHOODS.map((n) => (
          <Chip key={n} label={n} selected={neighborhood === n} onPress={() => setNeighborhood(n)} />
        ))}
      </View>
      <Text style={s.label}>Gravidade percebida</Text>
      <View style={s.chipsWrap}>
        {[['BAIXO', 'Baixa'], ['MEDIO', 'Média'], ['ALTO', 'Alta']].map(([k, l]) => (
          <Chip key={k} label={l} selected={severity === k} onPress={() => setSeverity(k)} />
        ))}
      </View>
      <Text style={s.label}>Descrição</Text>
      <TextInput
        style={s.input}
        multiline
        value={description}
        onChangeText={setDescription}
        placeholder="Ex.: água acumulada na rua, na altura do joelho."
        accessibilityLabel="Descrição da ocorrência"
        maxLength={500}
      />
      <View style={s.switchRow}>
        <Switch value={consent} onValueChange={setConsent} />
        <Text style={[s.body, { flex: 1, marginLeft: 8 }]}>
          Concordo com o tratamento do meu relato e da localização aproximada (bairro) para fins de proteção civil (LGPD).
        </Text>
      </View>
      <Button title={busy ? 'Enviando…' : 'Enviar relato'} disabled={busy} onPress={submit} />
    </ScrollView>
  );
}

// ---------------------------------------------------------------- Ajustes
export function SettingsScreen({ baseUrl, onChangeUrl, persona, onChangePersona, online, failures, lastUpdate, sources }) {
  const [url, setUrl] = useState(baseUrl);
  return (
    <ScrollView contentContainerStyle={s.pad} keyboardShouldPersistTaps="handled">
      <Text style={s.h1}>Ajustes</Text>
      <Card>
        <Text style={s.h2}>Conexão</Text>
        <Text style={s.body}>
          Estado: {online ? '🟢 conectado' : `🔴 sem conexão (${failures} tentativa(s))`}
          {lastUpdate ? `\nÚltima atualização: ${lastUpdate.toLocaleTimeString('pt-BR')}` : ''}
        </Text>
        <Text style={s.label}>Endereço do servidor</Text>
        <TextInput style={s.inputLine} value={url} onChangeText={setUrl} autoCapitalize="none" autoCorrect={false} keyboardType="url" />
        <Text style={s.muted}>No celular, use o IP do computador na mesma rede Wi-Fi (ex.: http://192.168.0.10:8080).</Text>
        <View style={{ marginTop: 8 }}>
          <Button title="Salvar endereço" onPress={() => onChangeUrl(url.trim().replace(/\/+$/, ''))} />
        </View>
      </Card>
      <Card>
        <Text style={s.h2}>Perfil de uso</Text>
        <View style={s.chipsWrap}>
          <Chip label="Morador" selected={persona === 'morador'} onPress={() => onChangePersona('morador')} />
          <Chip label="Agente da Defesa Civil" selected={persona === 'agente'} onPress={() => onChangePersona('agente')} />
        </View>
      </Card>
      <Card>
        <Text style={s.h2}>Saúde das fontes</Text>
        {sources.length === 0 ? <Text style={s.muted}>Sem dados.</Text> : null}
        {sources.map((f) => (
          <Text key={f.name} style={s.body}>
            {f.state === 'ONLINE' ? '🟢' : f.state === 'DEGRADADA' ? '🟡' : '🔴'} {f.name} · {f.originLabel} ·{' '}
            {f.periodSeconds === 0 ? 'sob demanda' : `atualizada há ${f.secondsSinceUpdate}s`}
          </Text>
        ))}
      </Card>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  pad: { padding: 16, paddingBottom: 40 },
  h1: { fontSize: 22, fontWeight: '800', color: colors.ink, marginBottom: 6 },
  h2: { fontSize: 16, fontWeight: '700', color: colors.ink, marginTop: 8, marginBottom: 6 },
  body: { fontSize: 14, color: colors.ink, marginTop: 3, lineHeight: 20 },
  muted: { fontSize: 13, color: colors.muted, marginBottom: 8 },
  meta: { fontSize: 12, color: colors.muted, marginTop: 4 },
  warn: { fontSize: 13, color: colors.danger, marginTop: 6, fontWeight: '600' },
  label: { fontSize: 13, fontWeight: '700', color: colors.ink, marginTop: 14, marginBottom: 6 },
  kpis: { flexDirection: 'row', flexWrap: 'wrap', marginBottom: 6 },
  kpi: {
    width: '48%', backgroundColor: colors.card, borderColor: colors.line, borderWidth: 1,
    borderRadius: 12, padding: 12, marginRight: '2%', marginBottom: 10,
  },
  kpiValue: { fontSize: 26, fontWeight: '800', color: colors.ink },
  kpiLabel: { fontSize: 12.5, color: colors.muted },
  chips: { flexDirection: 'row', flexWrap: 'wrap', padding: 12, paddingBottom: 0 },
  chipsWrap: { flexDirection: 'row', flexWrap: 'wrap' },
  chip: { borderColor: colors.line, borderWidth: 1, borderRadius: 18, paddingVertical: 7, paddingHorizontal: 12, marginRight: 8, marginBottom: 8, backgroundColor: colors.card },
  chipOn: { backgroundColor: colors.brand, borderColor: colors.brand },
  chipText: { color: colors.ink, fontWeight: '600', fontSize: 13 },
  input: { borderColor: colors.line, borderWidth: 1, borderRadius: 8, padding: 10, minHeight: 90, textAlignVertical: 'top', backgroundColor: colors.card, color: colors.ink },
  inputLine: { borderColor: colors.line, borderWidth: 1, borderRadius: 8, padding: 10, backgroundColor: colors.card, color: colors.ink },
  switchRow: { flexDirection: 'row', alignItems: 'center', marginVertical: 14 },
  pillsRow: { flexDirection: 'row', flexWrap: 'wrap', marginVertical: 8 },
  notif: { backgroundColor: colors.card, borderColor: colors.line, borderWidth: 1, borderLeftWidth: 5, borderLeftColor: colors.info, borderRadius: 10, padding: 12, marginBottom: 10 },
  overlay: { ...StyleSheet.absoluteFillObject, backgroundColor: colors.bg, zIndex: 10 },
});
