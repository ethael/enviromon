#!/bin/bash

echo "Creating virtual environment..."
python3 -m venv venv
source venv/bin/activate

echo "Installing dependencies..."
pip install flask mariadb

echo "Setting up directories..."
mkdir -p apk

echo "Initializing version file..."
echo "101" > apk/.version

echo "Creating database and tables..."
DB_USER="enviromon"
DB_PASSWORD="enviromon"
DB_NAME="enviromon"
mariadb -u root -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
mariadb -u root -e "GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASSWORD';"
mariadb -u $DB_USER -p$DB_PASSWORD $DB_NAME < ./init.sql

echo "Installation complete! You can now start your Python script with:"
echo "./venv/bin/python3 server.py"

