#!/bin/bash

DB_NAME="your_database"
DB_USER="your_username"
LOG_FILE="/var/log/postgres_tables_logs.log"

# Get table names
echo "$(date): Tables in $DB_NAME" | tee -a $LOG_FILE
psql -U $DB_USER -d $DB_NAME -t -c "SELECT tablename FROM pg_tables WHERE schemaname='public';" | tee -a $LOG_FILE

# Get access logs (requires superuser rights)
echo "$(date): Current activity in $DB_NAME" | tee -a $LOG_FILE
psql -U $DB_USER -d $DB_NAME -c "SELECT pid, usename, datname, client_addr, state, query FROM pg_stat_activity;" | tee -a $LOG_FILE
