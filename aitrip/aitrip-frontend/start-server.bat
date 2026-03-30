@echo off
echo Starting AiTrip Frontend Server...
echo Open browser: http://localhost:5500
python -m http.server 5500 --directory "%~dp0"
pause
