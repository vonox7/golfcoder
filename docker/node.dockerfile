# Build on debian (trixie) as we need a lot of native libraries for tree-sitter (e.g. Python itself)
FROM node:22-trixie
WORKDIR /app
COPY . .
RUN npm install

EXPOSE 8031
CMD ["node", "tree-sitter-server.js"]