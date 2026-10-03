/**
 * @format
 */

import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import { Text } from 'react-native';
import App from '../App';
import Watch from '../specs/NativeXiaomiWatch';

jest.mock('react-native-safe-area-context', () =>
  require('react-native-safe-area-context/jest/mock').default,
);

jest.mock('../specs/NativeXiaomiWatch', () => ({
  __esModule: true,
  default: {
    check: jest.fn(() =>
      Promise.resolve(
        JSON.stringify({
          time: 0,
          returned: [{ pkg: 'com.android.chrome', label: 'Chrome' }],
          unsuspended: [],
          manual: [],
          newSystem: [],
          watchedRemoved: 104,
          watchedSuspended: 5,
          configured: true,
        }),
      ),
    ),
    acknowledgeNew: jest.fn(),
    lastBackgroundCheck: jest.fn(() => 0),
  },
}));

async function renderText() {
  let tree: ReactTestRenderer.ReactTestRenderer | undefined;
  await ReactTestRenderer.act(async () => {
    tree = ReactTestRenderer.create(<App />);
  });
  return tree!.root
    .findAllByType(Text)
    .map(t => [t.props.children].flat().join(''))
    .join('\n');
}

test('mostra le app tornate', async () => {
  const text = await renderText();
  expect(text).toContain('1 app da ricontrollare');
  expect(text).toContain('com.android.chrome');
});

test('senza lista chiede di lanciare invia-lista.sh', async () => {
  (Watch.check as jest.Mock).mockResolvedValueOnce(
    JSON.stringify({
      time: 0,
      returned: [],
      unsuspended: [],
      manual: [],
      newSystem: [],
      watchedRemoved: 0,
      watchedSuspended: 0,
      configured: false,
    }),
  );
  const text = await renderText();
  expect(text).toContain('Lista non ancora caricata');
  expect(text).toContain('./invia-lista.sh');
  expect(text).not.toContain('Tutto pulito');
});
