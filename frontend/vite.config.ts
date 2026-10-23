import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Запросы анализа и истории уходят на бэкенд
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // Проверка живости бэкенда
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
