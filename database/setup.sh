#!/bin/bash
# Run this script on the PostgreSQL EC2 instance to install and initialize the database.
# Usage: bash setup.sh

set -e  # exit immediately if any command fails

echo "=== Installing PostgreSQL ==="
sudo apt update
sudo apt install -y postgresql postgresql-contrib

echo "=== Starting PostgreSQL ==="
sudo systemctl start postgresql
sudo systemctl enable postgresql

echo "=== Creating database and user ==="
sudo -u postgres psql <<EOF
CREATE DATABASE chatflow;
CREATE USER chatflow WITH PASSWORD 'chatflow';
GRANT ALL PRIVILEGES ON DATABASE chatflow TO chatflow;
EOF

echo "=== Running schema ==="
sudo -u postgres psql -d chatflow -f "$(dirname "$0")/schema.sql"

echo ""
echo "=== Setup complete ==="
echo "JDBC URL: jdbc:postgresql://localhost:5432/chatflow"
echo "User:     chatflow"
echo "Password: chatflow"
