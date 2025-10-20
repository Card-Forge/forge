git pull
set NUM_THREADS=%1
if "%NUM_THREADS%"=="" (
    set NUM_THREADS=8
)
mvn -U -T %NUM_THREADS% -DskipTests clean package
