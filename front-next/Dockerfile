# Stage 1: Build Stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package manager files
COPY package.json package-lock.json* ./
# Install dependencies
RUN npm ci

# Copy the rest of the application source code
COPY . .

# Build the Next.js application
# Use a build argument for the API URL
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
RUN npm run build

# Stage 2: Production/Runner Stage
FROM node:20-alpine
WORKDIR /app

# Set environment to production
ENV NODE_ENV=production

# Copy built assets from the build stage
# This mimics the logic from the existing GitHub Actions workflow
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/public ./public
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/ecosystem.config.js ./ecosystem.config.js


EXPOSE 3000

# The standalone output includes a server.js file
CMD ["node", "server.js"]
