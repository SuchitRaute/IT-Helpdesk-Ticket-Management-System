-- Review the current technician accounts before making any changes.
SELECT USER_ID, FULL_NAME, EMAIL, ROLE, DEPARTMENT, IS_ACTIVE
FROM APP.USERS
WHERE ROLE = 'TECHNICIAN'
ORDER BY FULL_NAME;

-- EXAMPLE ONLY: replace the email with the verified email for the actual account.
-- UPDATE APP.USERS SET FULL_NAME = 'Rohit Sharma', DEPARTMENT = 'Software', IS_ACTIVE = TRUE
-- WHERE EMAIL = 'replace-with-verified-email@example.com' AND ROLE = 'TECHNICIAN';
-- Repeat for each technician only after verifying their email address.
