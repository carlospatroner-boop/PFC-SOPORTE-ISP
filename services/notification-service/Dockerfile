FROM node:20-slim

WORKDIR /app

COPY package.json .
RUN npm install --omit=dev

COPY src ./src

RUN groupadd -r appgroup && useradd -r -g appgroup appuser
USER appuser

EXPOSE 8003

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD node -e "require('http').get('http://localhost:8003/health', (r) => process.exit(r.statusCode === 200 ? 0 : 1)).on('error', () => process.exit(1))"

CMD ["node", "src/index.js"]
