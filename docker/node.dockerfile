# Build on debian (trixie) as we need a lot of native libraries for tree-sitter (e.g. Python itself)
FROM node:22-trixie as build
WORKDIR /app
COPY . .
RUN npm install

# Add wget for healthchecks
FROM busybox AS busybox

# Build minimal runtime image
FROM gcr.io/distroless/nodejs22-debian13
COPY --from=build /app/tree-sitter-server.js /tree-sitter-server.js
COPY --from=build /app/node_modules /node_modules
COPY --from=busybox /bin/wget /usr/bin/wget
EXPOSE 8031
CMD ["tree-sitter-server.js"]