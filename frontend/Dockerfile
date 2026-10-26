# Production runtime image for Angular Bus Microservice UI
# Optimized for CI/CD: Copies pre-compiled production bundles from Jenkins workspace
FROM nginx:alpine

# Copy pre-compiled Angular browser distribution
COPY dist/frontend/browser /usr/share/nginx/html

# Copy custom Nginx configuration with reverse proxy for /api/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
