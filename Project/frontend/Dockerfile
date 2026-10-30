# ============================================================
# Stage 1: Build Application
# ============================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy sources and build production bundle
COPY . .
RUN npm run build

# ============================================================
# Stage 2: Production Runtime with Nginx
# ============================================================
FROM nginx:alpine

# Copy Nginx server configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy production static assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]

