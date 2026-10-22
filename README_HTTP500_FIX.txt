NexDesk - HTTP 500 database fix

Root cause from the screenshot:
APP.TICKET_HISTORY does not have the ACTION_DESCRIPTION column, but TicketHistoryDAO inserts into it.

Fix:
1. Extract this ZIP. Open the ITHelpdeskTicketSystem folder in NetBeans.
2. In NetBeans, open Services > Databases and connect to jdbc:derby://localhost:1527/HelpdeskDB (user app).
3. Open a SQL command window for HelpdeskDB and run the single statement in database/FIX_TICKET_HISTORY_ACTION_DESCRIPTION.sql.
4. If NetBeans reports the column already exists, do not run it again; refresh/reconnect and continue.
5. Clean and Build the project, then redeploy/restart the app and try creating a ticket again.

The ZIP does not contain your live Derby database, so the schema change must be run against the same HelpdeskDB used by GlassFish.
