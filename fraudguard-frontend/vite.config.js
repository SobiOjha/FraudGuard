/* global process */
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/bff": {
        target: process.env.BFF_URL || "http://localhost:3000",
        changeOrigin: true,
      },
    },
  },
})
