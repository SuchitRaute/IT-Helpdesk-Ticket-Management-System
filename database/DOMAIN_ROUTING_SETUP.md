# NexDesk — Named Automatic Technician Assignment

The application assigns each new ticket to the active technician account whose `FULL_NAME` matches the mapping in `TicketDAO.java`.

| Ticket category | Required technician account name |
|---|---|
| Software | Rohit Sharma |
| Hardware | Amit Patil |
| Network | Akash Verma |
| Email | Priya Deshmukh |
| Account Access | Sneha Joshi |
| Security | Rahul Kulkarni |

## Important
These are the requested sample names. They must exist in `APP.USERS` with `ROLE='TECHNICIAN'` and `IS_ACTIVE=TRUE`. If the exact matching account is not found, the ticket remains `OPEN`; it is not assigned to a different technician. The project code does not create login accounts or passwords automatically.

## Check technician accounts
Run this in the Derby SQL command tool connected to `HelpdeskDB`:

```sql
SELECT USER_ID, FULL_NAME, EMAIL, ROLE, DEPARTMENT, IS_ACTIVE
FROM APP.USERS
WHERE ROLE = 'TECHNICIAN'
ORDER BY FULL_NAME;
```

If your existing accounts have different names, update them only after confirming each technician's actual email address. Do not change accounts by name alone if you are unsure which account belongs to whom.

Recommended department values: Software, Hardware, Network, Email, Account Access, Security.
