import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import {
  colors,
  confidenceColor,
  confidenceLabel,
  riskColor,
  riskLabel,
  statusLabel,
  typeColor,
} from './theme';

export function Pill({ text, color, dark }) {
  return (
    <View style={[styles.pill, { backgroundColor: color }]}>
      <Text style={[styles.pillText, dark && { color: '#222' }]}>{text}</Text>
    </View>
  );
}

export function RiskPill({ risk }) {
  return <Pill text={riskLabel[risk] || risk} color={riskColor[risk]} dark={risk === 'MEDIO'} />;
}

export function TypePill({ type, label }) {
  return <Pill text={label} color={typeColor[type]} />;
}

export function StatusPill({ status }) {
  return <Pill text={statusLabel[status] || status} color="#546e7a" />;
}

export function ConfidenceBar({ value, level }) {
  return (
    <View style={styles.confRow} accessible accessibilityLabel={`Confiança ${value}%, ${confidenceLabel[level]}`}>
      <View style={styles.meter}>
        <View style={[styles.meterFill, { width: `${value}%`, backgroundColor: confidenceColor[level] }]} />
      </View>
      <Text style={styles.confText}>
        {value}% · {confidenceLabel[level]}
      </Text>
    </View>
  );
}

export function Card({ children, style }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function Button({ title, onPress, secondary, disabled }) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.btn,
        secondary && styles.btnSecondary,
        disabled && { opacity: 0.4 },
        pressed && { opacity: 0.75 },
      ]}
    >
      <Text style={[styles.btnText, secondary && { color: colors.ink }]}>{title}</Text>
    </Pressable>
  );
}

export function EventRow({ event, onPress }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${event.title}, ${event.neighborhood}, risco ${riskLabel[event.risk]}`}
      onPress={() => onPress(event)}
      style={({ pressed }) => [styles.row, pressed && { backgroundColor: '#eef1f4' }]}
    >
      <View style={{ flex: 1 }}>
        <Text style={styles.rowTitle}>{event.title}</Text>
        <Text style={styles.meta}>
          {event.code} · {event.neighborhood}
          {event.vulnerableArea ? ' ★' : ''}
        </Text>
        <View style={styles.pills}>
          <TypePill type={event.type} label={event.typeLabel} />
          <RiskPill risk={event.risk} />
          <StatusPill status={event.status} />
        </View>
        <Text style={styles.meta}>
          Origem: {event.origin} · {event.evidenceCount} evidência(s) · prioridade {event.priority}
          {event.trend && event.trend.direction === 'SUBINDO' ? ' · ▲ subindo' : ''}
        </Text>
        <ConfidenceBar value={event.confidence} level={event.confidenceLevel} />
      </View>
    </Pressable>
  );
}

export const styles = StyleSheet.create({
  pill: { paddingHorizontal: 9, paddingVertical: 2, borderRadius: 12, marginRight: 6, marginTop: 4 },
  pillText: { color: '#fff', fontSize: 12, fontWeight: '700' },
  pills: { flexDirection: 'row', flexWrap: 'wrap', marginTop: 2 },
  confRow: { flexDirection: 'row', alignItems: 'center', marginTop: 6 },
  meter: { flex: 1, height: 8, backgroundColor: colors.line, borderRadius: 6, overflow: 'hidden' },
  meterFill: { height: '100%' },
  confText: { marginLeft: 8, fontSize: 12.5, fontWeight: '700', color: colors.ink, minWidth: 92 },
  card: {
    backgroundColor: colors.card,
    borderColor: colors.line,
    borderWidth: 1,
    borderRadius: 12,
    padding: 14,
    marginBottom: 12,
  },
  btn: { backgroundColor: colors.brand, borderRadius: 8, paddingVertical: 11, paddingHorizontal: 14, marginRight: 8, marginBottom: 8 },
  btnSecondary: { backgroundColor: 'transparent', borderColor: colors.line, borderWidth: 1 },
  btnText: { color: '#fff', fontWeight: '700', fontSize: 14 },
  row: { paddingVertical: 12, paddingHorizontal: 4, borderBottomColor: colors.line, borderBottomWidth: 1, flexDirection: 'row' },
  rowTitle: { fontSize: 15.5, fontWeight: '700', color: colors.ink },
  meta: { fontSize: 12.5, color: colors.muted, marginTop: 2 },
});
