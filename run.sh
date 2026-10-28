#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$DIR/evil-tech-backend"

usage() {
    echo "Usage: ./run.sh [command]"
    echo ""
    echo "Commands:"
    echo "  start     Start the backend server (port 8080)"
    echo "  build     Build the backend JAR (skips tests)"
    echo "  test      Run test suite"
    echo "  demo      Run the end-to-end Python demo script"
    echo "  clean-db  Wipe local H2 demo database to reset state"
    echo "  ui        Print info on opening the browser storefront"
    echo ""
}

case "${1:-}" in
    start)
        echo "--> Starting EvilTech backend..."
        cd "$BACKEND_DIR"
        if [ ! -f "target/evil-tech-backend-1.0.0.jar" ]; then
            echo "--> JAR not found, building first..."
            ./mvnw -B package -DskipTests
        fi
        echo "--> Launching http://127.0.0.1:8080 (UI at http://127.0.0.1:8080/index.html)"
        java -jar target/evil-tech-backend-1.0.0.jar
        ;;
    build)
        echo "--> Building evil-tech-backend JAR..."
        cd "$BACKEND_DIR"
        ./mvnw -B clean package -DskipTests
        echo "--> Build finished: $BACKEND_DIR/target/evil-tech-backend-1.0.0.jar"
        ;;
    test)
        echo "--> Running test suite..."
        cd "$BACKEND_DIR"
        ./mvnw test
        ;;
    demo)
        echo "--> Running Python demo script..."
        python3 "$BACKEND_DIR/scripts/demo.py" "${2:-http://127.0.0.1:8080}"
        ;;
    clean-db)
        echo "--> Cleaning up local demo database..."
        rm -rf "$BACKEND_DIR/data" "$DIR/data"
        echo "--> Database reset. Next startup will create a fresh database."
        ;;
    ui)
        echo "EvilTech Storefront UI is hosted directly by the backend:"
        echo "  http://127.0.0.1:8080/index.html"
        echo ""
        echo "Parody storefront alternate page:"
        echo "  http://127.0.0.1:8080/ketto.html"
        ;;
    *)
        usage
        exit 1
        ;;
esac
