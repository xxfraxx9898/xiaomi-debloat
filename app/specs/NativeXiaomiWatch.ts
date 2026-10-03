import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  /** Esegue il controllo e restituisce il risultato come JSON (vedi CheckResult in App.tsx). */
  check(): Promise<string>;
  /** Segna dei pacchetti nuovi come "visti": non verranno più segnalati. */
  acknowledgeNew(packages: ReadonlyArray<string>): void;
  /** Ora dell'ultimo controllo in background (ms), 0 se mai. */
  lastBackgroundCheck(): number;
}

export default TurboModuleRegistry.getEnforcing<Spec>('NativeXiaomiWatch');
