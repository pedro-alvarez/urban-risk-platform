export const colors = {
  bg: '#f4f6f8',
  card: '#ffffff',
  ink: '#1f2933',
  muted: '#62707d',
  line: '#dde3e9',
  brand: '#c2185b',
  ok: '#2e7d32',
  warn: '#f9a825',
  danger: '#c62828',
  crit: '#6a1b9a',
  info: '#1565c0',
  water: '#1976d2',
  heat: '#ef6c00',
  serv: '#6d4c41',
};

export const riskColor = {
  BAIXO: colors.ok,
  MEDIO: colors.warn,
  ALTO: colors.danger,
  CRITICO: colors.crit,
};

export const riskLabel = { BAIXO: 'BAIXO', MEDIO: 'MÉDIO', ALTO: 'ALTO', CRITICO: 'CRÍTICO' };

export const typeColor = { ALAGAMENTO: colors.water, CALOR: colors.heat, SERVICOS: colors.serv };

export const confidenceColor = { ALTA: colors.ok, MEDIA: colors.warn, BAIXA: colors.danger };
export const confidenceLabel = { ALTA: 'Alta', MEDIA: 'Média', BAIXA: 'Baixa' };

export const statusLabel = {
  REPORTADO: 'Reportado',
  EM_VALIDACAO: 'Em validação',
  CONFIRMADO: 'Confirmado',
  ALERTA_EMITIDO: 'Alerta emitido',
  EM_MONITORAMENTO: 'Em monitoramento',
  ENCERRADO: 'Encerrado',
  DESCARTADO: 'Descartado',
};
