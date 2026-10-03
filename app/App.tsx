import { useCallback, useEffect, useState } from 'react';
import {
  PermissionsAndroid,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native';
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from 'react-native-safe-area-context';
import Watch from './specs/NativeXiaomiWatch';

type AppEntry = { pkg: string; label: string };

type CheckResult = {
  time: number;
  returned: AppEntry[];
  unsuspended: AppEntry[];
  manual: AppEntry[];
  newSystem: AppEntry[];
  watchedRemoved: number;
  watchedSuspended: number;
  configured: boolean;
};

const light = {
  bg: '#f6f6f4',
  card: '#ffffff',
  text: '#1c1c1a',
  muted: '#6b6b66',
  border: '#e4e4e0',
  ok: '#1f7a4d',
  okBg: '#e3f3ea',
  bad: '#b3261e',
  badBg: '#fbe9e7',
  warn: '#8a5a00',
  warnBg: '#fdf1d8',
  accent: '#1c1c1a',
  onAccent: '#ffffff',
};

const dark: typeof light = {
  bg: '#121211',
  card: '#1d1d1b',
  text: '#ededea',
  muted: '#9a9a94',
  border: '#2e2e2b',
  ok: '#6fd3a0',
  okBg: '#173326',
  bad: '#ff8a80',
  badBg: '#3a1c1a',
  warn: '#f2c46b',
  warnBg: '#3a2e14',
  accent: '#ededea',
  onAccent: '#121211',
};

type Palette = typeof light;

function formatTime(ms: number) {
  if (!ms) return 'mai';
  const d = new Date(ms);
  return d.toLocaleString('it-IT', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function App() {
  const isDark = useColorScheme() === 'dark';
  return (
    <SafeAreaProvider>
      <StatusBar barStyle={isDark ? 'light-content' : 'dark-content'} />
      <Screen c={isDark ? dark : light} />
    </SafeAreaProvider>
  );
}

function Screen({ c }: { c: Palette }) {
  const insets = useSafeAreaInsets();
  const [result, setResult] = useState<CheckResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [lastBg, setLastBg] = useState(0);

  const runCheck = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setResult(JSON.parse(await Watch.check()));
      setLastBg(Watch.lastBackgroundCheck());
    } catch (e) {
      setError(String(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (Platform.OS === 'android' && Platform.Version >= 33) {
      PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
      );
    }
    runCheck();
  }, [runCheck]);

  const acknowledge = (pkgs: string[]) => {
    Watch.acknowledgeNew(pkgs);
    runCheck();
  };

  const problems = result
    ? result.returned.length +
      result.unsuspended.length +
      result.newSystem.length
    : 0;

  return (
    <ScrollView
      style={{ backgroundColor: c.bg }}
      contentContainerStyle={[
        styles.content,
        { paddingTop: insets.top + 16, paddingBottom: insets.bottom + 24 },
      ]}
      refreshControl={
        <RefreshControl refreshing={loading} onRefresh={runCheck} />
      }
    >
      <Text style={[styles.title, { color: c.text }]}>Debloat Watch</Text>
      <Text style={[styles.subtitle, { color: c.muted }]}>
        Ti avvisa se le app che hai tolto tornano sul telefono.
      </Text>

      {error && (
        <Banner c={c} tone="bad" title="Controllo non riuscito" body={error} />
      )}

      {result && !error && !result.configured && (
        <Banner
          c={c}
          tone="warn"
          title="Lista non ancora caricata"
          body="Collega il telefono al PC e lancia ./invia-lista.sh: così l'app sa quali app tenere d'occhio."
        />
      )}

      {result && !error && result.configured && (
        <Banner
          c={c}
          tone={problems === 0 ? 'ok' : 'bad'}
          title={
            problems === 0
              ? 'Tutto pulito'
              : problems === 1
              ? '1 app da ricontrollare'
              : `${problems} app da ricontrollare`
          }
          body={
            problems === 0
              ? `${result.watchedRemoved} app tolte e ${result.watchedSuspended} bloccate, tutto come previsto.`
              : 'Collega il telefono al PC e lancia ./controlla.sh --ripara.'
          }
        />
      )}

      {result && (
        <>
          <Section
            c={c}
            tone="bad"
            title="Tornate"
            hint="Erano state tolte e ora sono di nuovo installate."
            items={result.returned}
          />
          <Section
            c={c}
            tone="bad"
            title="Non più bloccate"
            hint="Erano sospese o disattivate e ora possono di nuovo partire."
            items={result.unsuspended}
          />
          <Section
            c={c}
            tone="warn"
            title="Nuove di sistema"
            hint="Non c'erano all'inizio: probabilmente arrivate con un aggiornamento. Valutale con ./controlla.sh, poi segnale come viste."
            items={result.newSystem}
            onAcknowledge={pkg => acknowledge([pkg])}
            onAcknowledgeAll={() =>
              acknowledge(result.newSystem.map(a => a.pkg))
            }
          />
          <Section
            c={c}
            tone="neutral"
            title="Da disinstallare a mano"
            hint="Xiaomi non le fa togliere via ADB: tieni premuta l'icona e scegli Disinstalla."
            items={result.manual}
          />
        </>
      )}

      <Pressable
        onPress={runCheck}
        disabled={loading}
        style={({ pressed }) => [
          styles.button,
          { backgroundColor: c.accent, opacity: pressed || loading ? 0.6 : 1 },
        ]}
      >
        <Text style={[styles.buttonText, { color: c.onAccent }]}>
          {loading ? 'Controllo…' : 'Controlla ora'}
        </Text>
      </Pressable>

      <Text style={[styles.footer, { color: c.muted }]}>
        Controllo automatico dopo ogni riavvio e ogni 12 ore.{'\n'}
        Ultimo controllo: {formatTime(result?.time ?? 0)} · in background:{' '}
        {formatTime(lastBg)}
      </Text>
    </ScrollView>
  );
}

type Tone = 'ok' | 'bad' | 'warn' | 'neutral';

function toneColors(c: Palette, tone: Tone) {
  switch (tone) {
    case 'ok':
      return { fg: c.ok, bg: c.okBg };
    case 'bad':
      return { fg: c.bad, bg: c.badBg };
    case 'warn':
      return { fg: c.warn, bg: c.warnBg };
    default:
      return { fg: c.muted, bg: c.card };
  }
}

function Banner({
  c,
  tone,
  title,
  body,
}: {
  c: Palette;
  tone: Tone;
  title: string;
  body: string;
}) {
  const t = toneColors(c, tone);
  return (
    <View style={[styles.banner, { backgroundColor: t.bg }]}>
      <Text style={[styles.bannerTitle, { color: t.fg }]}>{title}</Text>
      <Text style={[styles.bannerBody, { color: c.text }]}>{body}</Text>
    </View>
  );
}

function Section({
  c,
  tone,
  title,
  hint,
  items,
  onAcknowledge,
  onAcknowledgeAll,
}: {
  c: Palette;
  tone: Tone;
  title: string;
  hint: string;
  items: AppEntry[];
  onAcknowledge?: (pkg: string) => void;
  onAcknowledgeAll?: () => void;
}) {
  if (items.length === 0) return null;
  const t = toneColors(c, tone);
  return (
    <View
      style={[styles.card, { backgroundColor: c.card, borderColor: c.border }]}
    >
      <View style={styles.cardHeader}>
        <Text style={[styles.cardTitle, { color: t.fg }]}>
          {title} · {items.length}
        </Text>
        {onAcknowledgeAll && items.length > 1 && (
          <Pressable onPress={onAcknowledgeAll} hitSlop={8}>
            <Text style={[styles.link, { color: c.text }]}>Viste tutte</Text>
          </Pressable>
        )}
      </View>
      <Text style={[styles.hint, { color: c.muted }]}>{hint}</Text>
      {items.map(a => (
        <View key={a.pkg} style={[styles.row, { borderTopColor: c.border }]}>
          <View style={styles.rowText}>
            <Text style={[styles.label, { color: c.text }]}>{a.label}</Text>
            <Text style={[styles.pkg, { color: c.muted }]}>{a.pkg}</Text>
          </View>
          {onAcknowledge && (
            <Pressable onPress={() => onAcknowledge(a.pkg)} hitSlop={8}>
              <Text style={[styles.link, { color: c.text }]}>Vista</Text>
            </Pressable>
          )}
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  content: { paddingHorizontal: 16, gap: 12 },
  title: { fontSize: 28, fontWeight: '700' },
  subtitle: { fontSize: 15, marginBottom: 4 },
  banner: { borderRadius: 14, padding: 16, gap: 4 },
  bannerTitle: { fontSize: 20, fontWeight: '700' },
  bannerBody: { fontSize: 14, lineHeight: 20 },
  card: { borderRadius: 14, borderWidth: 1, padding: 16 },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  cardTitle: { fontSize: 16, fontWeight: '700' },
  hint: { fontSize: 13, lineHeight: 18, marginTop: 4, marginBottom: 8 },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    borderTopWidth: StyleSheet.hairlineWidth,
    paddingVertical: 10,
    gap: 12,
  },
  rowText: { flex: 1 },
  label: { fontSize: 15, fontWeight: '600' },
  pkg: { fontSize: 12, marginTop: 2, fontFamily: 'monospace' },
  link: { fontSize: 14, fontWeight: '600', textDecorationLine: 'underline' },
  button: {
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
    marginTop: 4,
  },
  buttonText: { fontSize: 16, fontWeight: '700' },
  footer: { fontSize: 12, lineHeight: 18, textAlign: 'center' },
});

export default App;
