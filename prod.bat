@echo off
echo Starting LIMS in PRODUCTION mode...
echo This will build optimized production images.
echo Frontend (Next.js) will be available at http://localhost:3000
echo Backend (Spring Boot) will be available at http://localhost:8080
echo Press Ctrl+C to stop.
echo.
docker-compose -f docker-compose.prod.yml up --build -d
echo Production services started in detached mode.
