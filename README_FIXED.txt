NexDesk – updated database-backed project
==========================================

Changes:
- Removed literal Markdown fences from web source files (these caused visible ```html text).
- Dashboard remains in its original NexDesk styling and uses DashboardServlet.
- All Tickets loads live ticket records from PortalDataServlet.
- Employee Directory and technician workload load users/tickets from Derby.
- Reports are calculated from real ticket records; sample numbers/charts were removed.
- Ticket Details displays a selected ticket from the database instead of a hardcoded demo.
- Create Ticket redirects to the live ticket list after a successful insert.
- Password hash schema length updated to VARCHAR(200) for PBKDF2.

Run:
1. Back up your current project folder.
2. Extract this ZIP.
3. Open the extracted ITHelpdeskTicketSystem project in NetBeans.
4. Ensure Derby Network Server is running and DBConnection points to HelpdeskDB.
5. Check database/EXISTING_DATABASE_NOTES.txt. Do not rerun schema.sql against existing tables.
6. In NetBeans, right-click project > Clean and Build, then Run.
7. Log in with an existing account. Create a new ticket; it should appear in All Tickets and the dashboard.
8. Employee and technician directory endpoints require an ADMIN session.

Important:
- Existing sample rows already stored in your Derby database, if any, are not deleted automatically. The UI no longer hardcodes sample rows; it displays whatever is actually in the database.
- If the current USERS table uses PASSWORD_HASH VARCHAR(64), apply the single ALTER statement in database/EXISTING_DATABASE_NOTES.txt only after checking the current type.
