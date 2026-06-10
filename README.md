# ✅ Smart Task — ToDo App
A fully featured desktop **Task Management** application built with **Java Swing and AWT**. This isn't just a basic to-do list — it comes with a complete multi-user authentication system, task prioritization, category management, deadline tracking with overdue detection, live search, statistical reporting, and a polished dark-themed GUI.

---

## 📖 About the Project

This project was developed as part of the **Object-Oriented Programming Language (CSE 124)** lab course at **USTC**, Semester July 25 – December 25. It demonstrates how well-structured class design, type-safe enums, event-driven programming, and Java's built-in serialization can combine to produce a polished and practical desktop application — without any external framework dependencies.

What started as a task tracker grew into a full desktop application with secure login/registration, SHA-256 password hashing, multi-category task organization, archive and trash support, a calendar view, productivity reports, and CSV/TXT export.

---

## ✨ Features

### 👤 User System
- Registration with full validation (full name, email format, username, password length, confirm password matching)
- Secure login with SHA-256 hashed password verification
- File-based serialized user storage (`.ser` file)
- Password reset via registered email
- Profile management (update name, email, change password)
- Logout with session state clearing

### 📋 Task Management
- Create tasks with: Title, Description, Priority, Category, Deadline, Status, Tags, Subtasks, Notes, Attachment Name, and Pin
- Edit, complete, delete, archive, and move tasks to trash
- Pin/Unpin tasks for quick access
- Restore tasks from trash
- Mark tasks as **Pending**, **In Progress**, **Completed**, or **Cancelled**
- Bulk operations and overdue reminders on login

### 🎯 Priority & Category Classification
- **Priority levels:** HIGH (🔴), MEDIUM (🟡), LOW (🟢)
- **Categories:** WORK, PERSONAL, SHOPPING, HEALTH, EDUCATION, FINANCE, OTHER

### 🔍 Search, Filter & Sort
- Live search across title, description, notes, tags, and attachment name
- Filter by status, priority, category, overdue, due today, and pinned
- Sort by Pinned First, Newest First, Deadline, Priority, Category, or Status

### 📊 Statistics & Reports
- Dashboard with Total Tasks, Pending, Completed, and Overdue summary cards
- Completion Progress bar
- Weekly productivity summary
- Full task report with status breakdown
- Export reports as `.txt` file

### 📅 Calendar View
- Monthly deadline calendar showing tasks by date

### 💾 Data & Export
- Automatic serialized data persistence after every change
- Export task list to **CSV**
- Backup and Restore data files
- Overdue reminder dialog on login

### 🎨 UI/UX
- Dark / Light mode toggle
- Custom gradient buttons with hover effects
- Color-coded priority and status columns in the task table
- Zebra-stripe table rows and custom cell renderers
- Card-based dashboard layout

---

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java | Core programming language |
| Java Swing & AWT | GUI framework |
| Java Serialization | Data persistence (users & tasks) |
| Java Security (SHA-256) | Password hashing via `MessageDigest` |
| Java Time API | Deadline management and overdue detection |
| Java Streams API | Filtering, sorting, and statistics |
| OOP (Java) | Architecture and design |

---

## 📂 Project Structure

```
Smart_Task_Management_System_Main_Run/
│
├── src/
│   ├── Main.java              # Entry point — launches ToDoAppGUI
│   ├── ToDoAppGUI.java        # Main application class (controller + view)
│   ├── Task.java              # Task data model with isOverdue() and isDueToday()
│   ├── User.java              # User data model with task list ownership
│   ├── SubTask.java           # Subtask model for nested task items
│   ├── Priority.java          # Enum: HIGH, MEDIUM, LOW
│   ├── Category.java          # Enum: WORK, PERSONAL, SHOPPING, HEALTH, EDUCATION, FINANCE, OTHER
│   └── TaskStatus.java        # Enum: PENDING, IN_PROGRESS, COMPLETED, CANCELLED
│
├── bin/                       # Compiled .class files
├── .vscode/                   # VS Code settings
├── run.bat                    # Windows one-click run script
├── run.sh                     # Linux/macOS run script
└── README.txt                 # Quick-start instructions
```

---

## 🚀 How to Run

### Prerequisites
- Java JDK 8 or later (tested on Java SE 17)
- Any OS with a Java Runtime Environment (Windows, macOS, Linux)

### Option 1 — Easy Way (Windows)
Double-click `run.bat` in the project folder.

### Option 2 — Manual Compile and Run

**Step 1: Compile all source files**
```bash
javac -d bin src/Main.java src/Task.java src/User.java src/SubTask.java src/Priority.java src/Category.java src/TaskStatus.java src/ToDoAppGUI.java
```

**Step 2: Run the application**
```bash
java -cp bin Main
```

### Option 3 — Using an IDE
Import the `src/` folder into **VS Code**, **IntelliJ IDEA**, or **Eclipse** and run `Main.java` directly.

---

## ⌨️ How to Use

1. **Register** — Create an account with your full name, email, username, and password
2. **Login** — Sign in with your credentials (SHA-256 secured)
3. **Add Tasks** — Click `+ Add Task` and fill in the details
4. **Manage Tasks** — Use the action buttons: View Details, Edit, Complete, In Progress, Pin/Unpin, Archive, Move to Trash
5. **Search & Filter** — Use the live search bar and filter/sort dropdowns
6. **Check Reports** — Visit the Reports page for productivity statistics
7. **Calendar** — See this month's deadlines at a glance
8. **Export** — Export your tasks as CSV or the report as TXT

---

## 🏗️ OOP Design

The project applies core Object-Oriented Programming principles throughout:

**Encapsulation** — `Task` and `User` classes group related data with their methods. `isOverdue()` and `isDueToday()` encapsulate date comparison logic. Passwords are stored only as SHA-256 hashes.

**Abstraction** — `ToDoAppGUI` exposes clearly named action methods (`showAddTaskDialog()`, `markSelectedDone()`, `deleteSelectedTask()`) without exposing internal table model or file I/O complexity.

**Enum-Based Type Safety** — `Priority`, `Category`, and `TaskStatus` enums replace error-prone string constants, enabling type-safe filtering, exhaustive switch matching, and direct use in `JComboBox`.

**Event-Driven Programming** — `ActionListener` drives all CRUD button operations. `DocumentListener` powers live search — updating the task table on every keystroke. `MouseListener` adds hover effects to buttons.

**Separation of Concerns** — `Task` and `User` handle data modeling; `Priority`, `Category`, and `TaskStatus` handle type safety; `ToDoAppGUI` handles all presentation and interaction logic.

---

## 📊 UML Class Overview

| Class | Role |
|---|---|
| `Priority` | Enum — task urgency: HIGH, MEDIUM, LOW |
| `Category` | Enum — task domain: WORK, PERSONAL, SHOPPING, etc. |
| `TaskStatus` | Enum — task state: PENDING, IN_PROGRESS, COMPLETED, CANCELLED |
| `SubTask` | Serializable — nested checklist item inside a task |
| `Task` | Serializable — core data model with overdue/dueToday logic |
| `User` | Serializable — user account, owns an `ArrayList<Task>` |
| `ToDoAppGUI` | Extends `JFrame` — main controller, all panels and CRUD logic |

---

## ⚠️ Known Limitations

- Data is stored locally in a `.ser` binary file — no online sync
- No password salt added before SHA-256 hashing (vulnerable to rainbow table attacks if file is exposed)
- `showAddTaskDialog()` and `showEditTaskDialog()` share duplicated structure (DRY violation)
- File I/O runs on the Swing Event Dispatch Thread — may cause brief freezes on large datasets
- No column sorting by clicking table headers
- Unused `tags` UI in some views

---

## 🔮 Possible Future Improvements

- Replace serialized file storage with **SQLite** or **MySQL** database
- Add **password salt** or adopt **BCrypt/Argon2** for stronger security
- Consolidate add/edit dialogs into a single reusable `TaskDialog` class
- Implement **TableRowSorter** for clickable column sorting
- **System Tray** deadline notifications
- **Export to PDF** in addition to CSV/TXT
- **Cloud sync / REST API** backend for mobile cross-device access
- **Recurring tasks** (daily, weekly, monthly)
- **Dark mode** preference persistence across sessions

---

## 👨‍💻 Project Info

| | |
|---|---|
| **Student Name** | Tanzif Mozumder Chisti |
| **Student ID** | 0022520005101002 |
| **Batch** | CSE-45th |
| **Course Code** | CSE 124 |
| **Course Name** | Object Oriented Programming Language |
| **Course Teacher** | Debabrata Mallick (Lecturer) |
| **Institution** | University of Science and Technology Chittagong (USTC) |
| **Semester** | July 25 – December 25 |
| **Submission Date** | 06/05/2026 |

---

> *This project was built for academic and learning purposes as part of the OOP Lab Final Project.*
