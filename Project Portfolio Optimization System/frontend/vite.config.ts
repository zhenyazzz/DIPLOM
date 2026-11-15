import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // Same-origin /api → gateway (refresh cookie path=/api/v1/auth)
      "/api": {
        target: "http://localhost:8090",
        changeOrigin: true,
      },
    },
  },
});
