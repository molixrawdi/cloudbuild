import psycopg2
import logging

# --- Setup logging ---
logging.basicConfig(
    filename="postgres_access.log",
    level=logging.INFO,
    format="%(asctime)s - %(levelname)s - %(message)s"
)

def get_tables_and_logs(dbname, user, password, host="localhost", port=5432):
    try:
        conn = psycopg2.connect(
            dbname=dbname, user=user, password=password, host=host, port=port
        )
        cur = conn.cursor()

        # 1. Get table names from public schema
        cur.execute("""
            SELECT tablename 
            FROM pg_tables 
            WHERE schemaname='public';
        """)
        tables = [row[0] for row in cur.fetchall()]
        logging.info(f"Tables in {dbname}: {tables}")
        print("Tables:", tables)

        # 2. Get PostgreSQL activity logs (current connections/queries)
        cur.execute("""
            SELECT pid, usename, datname, client_addr, state, query 
            FROM pg_stat_activity;
        """)
        logs = cur.fetchall()
        for log in logs:
            logging.info(f"Log Entry: {log}")
        print("Access logs written to postgres_access.log")

        cur.close()
        conn.close()

    except Exception as e:
        logging.error(f"Error: {e}")
        print("❌ Error connecting to PostgreSQL:", e)


if __name__ == "__main__":
    get_tables_and_logs(
        dbname="your_database",
        user="your_username",
        password="your_password",
        host="localhost",
        port=5432
    )
