# 🎫 IT Helpdesk & Ticket Management System

A web-based IT Helpdesk and Ticket Management System designed to streamline IT support operations, manage technical issues, and track ticket resolution efficiently.

## 📌 Project Overview

The **IT Helpdesk & Ticket Management System** provides a centralized platform where employees can report IT-related issues, administrators can assign tickets to technicians, and technicians can resolve issues while maintaining a complete history of ticket activities.

The system improves communication, accountability, and efficiency in IT support management.

## ✨ Key Features

- 🔐 **User Authentication** – Secure login and role-based access.
- 👨‍💻 **Employee Management** – Manage employee accounts and information.
- 🎫 **Ticket Management** – Create, view, update, and manage support tickets.
- 👥 **Technician Assignment** – Assign tickets to appropriate technicians.
- 🚦 **Priority Management** – Categorize tickets by priority.
- 📊 **Status Tracking** – Track tickets from creation to closure.
- 📝 **Resolution History** – Maintain a history of ticket updates and resolutions.
- 💬 **Ticket Comments** – Enable communication related to support tickets.
- 🔍 **Search and Reports** – Find tickets and review support activity.
- 🛡️ **Role-Based Access Control** – Separate permissions for Employees, Technicians, and Administrators.

## 🔄 System Workflow

1. Employee logs in to the system.
2. Employee raises a support ticket.
3. Administrator reviews and assigns the ticket to a technician.
4. Technician investigates the issue and updates its status.
5. Technician resolves the issue and records the resolution.
6. The ticket is closed, and its history remains available.

**Ticket lifecycle:**  
`Open → Assigned → In Progress → Resolved → Closed`

## 👥 User Roles

| Role | Responsibilities |
|---|---|
| Employee | Raise tickets, view status, add comments |
| Technician | View assigned tickets, update progress, resolve issues |
| Administrator | Manage users, assign tickets, monitor support activities |

## 🛠️ Technology Stack

- **Backend:** Java, Java Servlets
- **Frontend:** HTML, CSS, JavaScript, JSP
- **Database:** Apache Derby
- **Database Connectivity:** JDBC
- **Application Server:** GlassFish 7
- **Development Environment:** NetBeans IDE

## 🗄️ Database Structure

The system uses the following primary database tables:

- `USERS` – Stores user and role information.
- `TICKETS` – Stores ticket details, priorities, assignments, and statuses.
- `TICKET_HISTORY` – Records ticket status changes and activities.
- `TICKET_COMMENTS` – Stores comments associated with tickets.

## ⚙️ Installation and Setup

### Prerequisites

Install the following software before running the project:

- Java Development Kit (JDK)
- NetBeans IDE
- GlassFish Server 7
- Apache Derby Database

### Steps to Run

1. Clone the repository:

   ```bash
   git clone https://github.com/YOUR_USERNAME/IT-Helpdesk-Ticket-Management-System.git
   ```

2. Open NetBeans IDE.
3. Import or open the downloaded project.
4. Configure the GlassFish 7 server.
5. Configure the Apache Derby database connection.
6. Execute the database schema SQL script to create the required tables.
7. Configure the database connection settings in the project.
8. Build and deploy the application to GlassFish.
9. Open the application using the local URL provided by the server.

> **Note:** Update the repository URL and database settings according to your environment. The exact deployment steps may vary depending on your project configuration.

## 📁 Project Structure

```text
IT-Helpdesk-Ticket-Management-System/
├── src/
│   ├── servlets/
│   ├── dao/
│   ├── models/
│   └── utils/
├── web/
│   ├── WEB-INF/
│   ├── css/
│   ├── js/
│   └── *.jsp
├── database/
│   └── schema.sql
└── README.md
```

*The structure above is illustrative. Adjust it to match the actual files and folders in your project.*

## 🔒 Security Considerations

- Store passwords using a suitable password-hashing algorithm such as Argon2id or bcrypt.
- Use prepared statements to help prevent SQL injection.
- Validate user input on the server.
- Enforce role-based authorization on protected operations.
- Never commit database passwords, API keys, or other secrets to GitHub.

## 🎯 Project Objectives

- Simplify IT support ticket creation and management.
- Improve ticket assignment and resolution tracking.
- Maintain transparent communication between employees and IT staff.
- Provide an organized record of support activities.
- Improve the overall efficiency of IT service management.

## 🚀 Future Enhancements

- Email notifications for ticket updates.
- Dashboard analytics and visual reports.
- Service-level agreement (SLA) tracking.
- Ticket attachments and image uploads.
- Advanced filtering and ticket export.
- Automated ticket assignment.

## 🎓 Academic Project

Developed as an academic mini project to demonstrate web application development, database management, user authentication, and IT service workflow automation.

## 👨‍💻 Author

**Suchit Raute**

GitHub: [@SuchitRaute](https://github.com/SuchitRaute)

---

⭐ If you find this project useful, consider giving the repository a star.
