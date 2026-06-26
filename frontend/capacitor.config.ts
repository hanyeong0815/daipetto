import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.daipetto.app',
  appName: 'Daipetto',
  webDir: 'dist',
  server: {
    // Capacitor 5+のデフォルトはhttps://localhostだがAPIがHTTPのためhttpに変更
    androidScheme: 'http',
    cleartext: true,
  },
  android: {
    buildOptions: {
      keystorePath: undefined,
      keystoreAlias: undefined,
    },
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 0,
    },
  },
}

export default config
