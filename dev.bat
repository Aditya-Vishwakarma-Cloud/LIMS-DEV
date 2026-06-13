@echo off
echo Starting LIMS in DEVELOPMENT mode with hot-reloading...
echo Frontend (Next.js) will be available at http://localhost:3000
echo Backend (Spring Boot) will be available at http://localhost:8080
echo Press Ctrl+C to stop.
echo.
docker-compose -f docker-compose.yml up
