import java.io.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

// ─── ENUMS ────────────────────────────────────────────────────────────────────

enum Priority { HIGH, MEDIUM, LOW }

enum Category { WORK, PERSONAL, SHOPPING, HEALTH, EDUCATION, FINANCE, OTHER }

enum RecurType { NONE, DAILY, WEEKLY, MONTHLY }

// ─── TASK CLASS ───────────────────────────────────────────────────────────────

class Task implements Serializable {
    static final long serialVersionUID = 1L;

    int id;
    String title;
    String description;
    Priority priority;
    Category category;
    String deadline;        // format: yyyy-MM-dd
    boolean isCompleted;
    String createdAt;
    String completedAt;
    List<String> tags;
    List<String> subtasks;
    List<Boolean> subtaskDone;
    RecurType recurrence;
    int reminderDaysBefore; // 0 = no reminder
    String notes;

    Task(int id, String title, String description, Priority priority,
         Category category, String deadline, RecurType recurrence, int reminderDays) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.category = category;
        this.deadline = deadline;
        this.isCompleted = false;
        this.createdAt = LocalDate.now().toString();
        this.completedAt = null;
        this.tags = new ArrayList<>();
        this.subtasks = new ArrayList<>();
        this.subtaskDone = new ArrayList<>();
        this.recurrence = recurrence;
        this.reminderDaysBefore = reminderDays;
        this.notes = "";
    }

    boolean isOverdue() {
        if (isCompleted || deadline == null || deadline.isEmpty()) return false;
        LocalDate dl = LocalDate.parse(deadline);
        return LocalDate.now().isAfter(dl);
    }

    boolean isDueSoon(int days) {
        if (isCompleted || deadline == null || deadline.isEmpty()) return false;
        LocalDate dl = LocalDate.parse(deadline);
        LocalDate today = LocalDate.now();
        return !today.isAfter(dl) && !today.plusDays(days).isBefore(dl);
    }

    int subtaskProgress() {
        if (subtasks.isEmpty()) return 100;
        long done = subtaskDone.stream().filter(b -> b).count();
        return (int) (done * 100 / subtasks.size());
    }

    void display(boolean compact) {
        String overdue = isOverdue() ? " [!! OVERDUE]" : "";
        String dueSoon = isDueSoon(3) && !isOverdue() ? " [Due Soon]" : "";
        String status = isCompleted ? "✓ Completed" : "○ Pending";
        String priorityIcon = priority == Priority.HIGH ? "🔴" :
                              priority == Priority.MEDIUM ? "🟡" : "🟢";

        System.out.println("\n┌─────────────────────────────────────────────");
        System.out.println("│ [" + id + "] " + priorityIcon + " " + title + overdue + dueSoon);
        System.out.println("│ Status   : " + status);
        System.out.println("│ Priority : " + priority + "  |  Category: " + category);
        System.out.println("│ Deadline : " + (deadline.isEmpty() ? "None" : deadline));
        if (!compact) {
            System.out.println("│ Created  : " + createdAt);
            if (completedAt != null) System.out.println("│ Done At  : " + completedAt);
            System.out.println("│ Recurs   : " + recurrence);
            if (!description.isEmpty())  System.out.println("│ Desc     : " + description);
            if (!notes.isEmpty())        System.out.println("│ Notes    : " + notes);
            if (!tags.isEmpty())         System.out.println("│ Tags     : " + String.join(", ", tags));
            if (!subtasks.isEmpty()) {
                System.out.println("│ Subtasks : [" + subtaskProgress() + "% done]");
                for (int i = 0; i < subtasks.size(); i++) {
                    System.out.println("│   " + (i + 1) + ". [" + (subtaskDone.get(i) ? "✓" : " ") + "] " + subtasks.get(i));
                }
            }
        }
        System.out.println("└─────────────────────────────────────────────");
    }
}

// ─── USER CLASS ───────────────────────────────────────────────────────────────

class User implements Serializable {
    static final long serialVersionUID = 1L;
    String username;
    String passwordHash;
    String email;
    ArrayList<Task> tasks = new ArrayList<>();
    int taskIdCounter = 1;

    User(String username, String passwordHash, String email) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
    }
}

// ─── MAIN APP ─────────────────────────────────────────────────────────────────

public class ToDoApp {

    static Scanner sc = new Scanner(System.in);
    static ArrayList<User> users = new ArrayList<>();
    static User currentUser = null;
    static final String FILE = "todoapp_data.ser";
    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── UTILS ────────────────────────────────────────────────────────────────

    static String hashPassword(String pw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(pw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return pw; }
    }

    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try { return Integer.parseInt(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  ✗ Please enter a valid number."); }
        }
    }

    static String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    static <T extends Enum<T>> T readEnum(String prompt, Class<T> enumClass) {
        while (true) {
            System.out.print(prompt);
            try { return Enum.valueOf(enumClass, sc.nextLine().toUpperCase().trim()); }
            catch (Exception e) { System.out.println("  ✗ Invalid option. Try again."); }
        }
    }

    static String readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd, or blank to skip): ");
            String s = sc.nextLine().trim();
            if (s.isEmpty()) return "";
            try { LocalDate.parse(s, FMT); return s; }
            catch (Exception e) { System.out.println("  ✗ Invalid date format. Use yyyy-MM-dd."); }
        }
    }

    // ── DATA PERSISTENCE ─────────────────────────────────────────────────────

    static void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(users);
        } catch (Exception e) { System.out.println("  ✗ Error saving data!"); }
    }

    @SuppressWarnings("unchecked")
    static void loadData() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
            users = (ArrayList<User>) in.readObject();
        } catch (Exception e) { System.out.println("  ℹ️ No previous data. Starting fresh."); }
    }

    // ── AUTH ──────────────────────────────────────────────────────────────────

    static void register() {
        System.out.println("\n══════ REGISTER ══════");
        String u = readLine("Username   : ");
        if (u.isEmpty()) { System.out.println("  ✗ Username cannot be empty."); return; }
        for (User user : users) {
            if (user.username.equalsIgnoreCase(u)) {
                System.out.println("  ✗ Username already taken."); return;
            }
        }
        String p = readLine("Password   : ");
        if (p.length() < 4) { System.out.println("  ✗ Password must be at least 4 characters."); return; }
        String p2 = readLine("Confirm PW : ");
        if (!p.equals(p2)) { System.out.println("  ✗ Passwords do not match."); return; }
        String email = readLine("Email      : ");

        users.add(new User(u, hashPassword(p), email));
        saveData();
        System.out.println("  ✓ Registration successful! You can now log in.\n");
    }

    static boolean login() {
        System.out.println("\n══════ LOGIN ══════");
        String u = readLine("Username : ");
        String p = readLine("Password : ");
        String hash = hashPassword(p);
        for (User user : users) {
            if (user.username.equalsIgnoreCase(u) && user.passwordHash.equals(hash)) {
                currentUser = user;
                System.out.println("  ✓ Welcome back, " + user.username + "!\n");
                checkReminders();
                return true;
            }
        }
        System.out.println("  ✗ Invalid username or password.\n");
        return false;
    }

    static void changePassword() {
        System.out.println("\n══════ CHANGE PASSWORD ══════");
        String old = readLine("Current password : ");
        if (!currentUser.passwordHash.equals(hashPassword(old))) {
            System.out.println("  ✗ Current password is wrong."); return;
        }
        String nw = readLine("New password     : ");
        if (nw.length() < 4) { System.out.println("  ✗ Must be at least 4 characters."); return; }
        String nw2 = readLine("Confirm new PW   : ");
        if (!nw.equals(nw2)) { System.out.println("  ✗ Passwords do not match."); return; }
        currentUser.passwordHash = hashPassword(nw);
        saveData();
        System.out.println("  ✓ Password changed successfully.");
    }

    // ── REMINDERS ────────────────────────────────────────────────────────────

    static void checkReminders() {
        List<Task> alerts = new ArrayList<>();
        for (Task t : currentUser.tasks) {
            if (!t.isCompleted && t.reminderDaysBefore > 0 && !t.deadline.isEmpty()) {
                if (t.isDueSoon(t.reminderDaysBefore)) alerts.add(t);
            }
        }
        if (!alerts.isEmpty()) {
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║         ⏰  REMINDERS FOR YOU            ║");
            System.out.println("╠══════════════════════════════════════════╣");
            for (Task t : alerts)
                System.out.println("║ 🔔 [" + t.id + "] " + t.title + " due on " + t.deadline);
            System.out.println("╚══════════════════════════════════════════╝\n");
        }

        List<Task> overdue = new ArrayList<>();
        for (Task t : currentUser.tasks)
            if (t.isOverdue()) overdue.add(t);
        if (!overdue.isEmpty()) {
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║         ⚠️  OVERDUE TASKS                ║");
            System.out.println("╠══════════════════════════════════════════╣");
            for (Task t : overdue)
                System.out.println("║ ❗ [" + t.id + "] " + t.title + " was due " + t.deadline);
            System.out.println("╚══════════════════════════════════════════╝\n");
        }
    }

    // ── ADD TASK ─────────────────────────────────────────────────────────────

    static void addTask() {
        System.out.println("\n══════ ADD TASK ══════");
        String title = readLine("Title       : ");
        if (title.isEmpty()) { System.out.println("  ✗ Title is required."); return; }
        String desc  = readLine("Description : ");

        System.out.println("  Priority options: HIGH / MEDIUM / LOW");
        Priority priority = readEnum("Priority    : ", Priority.class);

        System.out.println("  Categories: WORK / PERSONAL / SHOPPING / HEALTH / EDUCATION / FINANCE / OTHER");
        Category category = readEnum("Category    : ", Category.class);

        String deadline = readDate("Deadline    ");

        System.out.println("  Recurrence: NONE / DAILY / WEEKLY / MONTHLY");
        RecurType recur = readEnum("Recurrence  : ", RecurType.class);

        int reminder = 0;
        String remStr = readLine("Reminder (days before deadline, 0 = none): ");
        try { reminder = Integer.parseInt(remStr); } catch (Exception e) { reminder = 0; }

        String tagsRaw = readLine("Tags (comma-separated, or blank): ");

        Task task = new Task(currentUser.taskIdCounter++, title, desc, priority, category, deadline, recur, reminder);

        if (!tagsRaw.isEmpty()) {
            for (String tag : tagsRaw.split(","))
                task.tags.add(tag.trim().toLowerCase());
        }

        // Subtasks
        String addSub = readLine("Add subtasks? (y/n): ");
        if (addSub.equalsIgnoreCase("y")) {
            System.out.println("  Enter subtasks one by one. Type 'done' when finished.");
            while (true) {
                String sub = readLine("  Subtask: ");
                if (sub.equalsIgnoreCase("done") || sub.isEmpty()) break;
                task.subtasks.add(sub);
                task.subtaskDone.add(false);
            }
        }

        String notes = readLine("Notes (optional): ");
        task.notes = notes;

        currentUser.tasks.add(task);
        saveData();
        System.out.println("  ✓ Task added successfully! [ID: " + task.id + "]");
    }

    // ── VIEW TASKS ───────────────────────────────────────────────────────────

    static void viewTasks() {
        if (currentUser.tasks.isEmpty()) {
            System.out.println("\n  ℹ️ No tasks found."); return;
        }
        System.out.println("\n══════ VIEW OPTIONS ══════");
        System.out.println("  1. All Tasks");
        System.out.println("  2. Pending Only");
        System.out.println("  3. Completed Only");
        System.out.println("  4. Overdue Only");
        System.out.println("  5. By Priority");
        System.out.println("  6. By Category");
        System.out.println("  7. Search by Keyword / Tag");
        System.out.println("  8. Due this week");
        int choice = readInt("Choose: ");

        List<Task> list = new ArrayList<>(currentUser.tasks);
        switch (choice) {
            case 1 -> {}
            case 2 -> list.removeIf(t -> t.isCompleted);
            case 3 -> list.removeIf(t -> !t.isCompleted);
            case 4 -> list.removeIf(t -> !t.isOverdue());
            case 5 -> {
                System.out.println("  Priority: HIGH / MEDIUM / LOW");
                Priority p = readEnum("Filter: ", Priority.class);
                list.removeIf(t -> t.priority != p);
            }
            case 6 -> {
                System.out.println("  Categories: WORK / PERSONAL / SHOPPING / HEALTH / EDUCATION / FINANCE / OTHER");
                Category c = readEnum("Filter: ", Category.class);
                list.removeIf(t -> t.category != c);
            }
            case 7 -> {
                String kw = readLine("Keyword or tag: ").toLowerCase();
                list.removeIf(t ->
                    !t.title.toLowerCase().contains(kw) &&
                    !t.description.toLowerCase().contains(kw) &&
                    !t.tags.contains(kw));
            }
            case 8 -> list.removeIf(t -> !t.isDueSoon(7));
            default -> System.out.println("  Invalid choice.");
        }

        // Sort by priority then deadline
        list.sort(Comparator
            .comparingInt((Task t) -> t.priority.ordinal())
            .thenComparing(t -> t.deadline.isEmpty() ? "9999" : t.deadline));

        if (list.isEmpty()) { System.out.println("\n  ℹ️ No tasks match."); return; }

        boolean compact = list.size() > 5;
        System.out.println("\n  Showing " + list.size() + " task(s):");
        for (Task t : list) t.display(compact);
    }

    // ── MARK COMPLETE ────────────────────────────────────────────────────────

    static void markComplete() {
        System.out.println("\n══════ MARK COMPLETE ══════");
        int id = readInt("Task ID: ");
        for (Task t : currentUser.tasks) {
            if (t.id == id) {
                if (t.isCompleted) { System.out.println("  ℹ️ Already completed."); return; }
                // Handle subtasks
                if (!t.subtasks.isEmpty() && t.subtaskProgress() < 100) {
                    System.out.println("  ⚠️ This task has unfinished subtasks (" + t.subtaskProgress() + "% done).");
                    String confirm = readLine("  Mark as complete anyway? (y/n): ");
                    if (!confirm.equalsIgnoreCase("y")) return;
                }
                t.isCompleted = true;
                t.completedAt = LocalDate.now().toString();
                // Handle recurrence: create next occurrence
                if (t.recurrence != RecurType.NONE && !t.deadline.isEmpty()) {
                    LocalDate next = LocalDate.parse(t.deadline);
                    switch (t.recurrence) {
                        case DAILY   -> next = next.plusDays(1);
                        case WEEKLY  -> next = next.plusWeeks(1);
                        case MONTHLY -> next = next.plusMonths(1);
                    }
                    Task recurring = new Task(currentUser.taskIdCounter++, t.title,
                        t.description, t.priority, t.category, next.toString(),
                        t.recurrence, t.reminderDaysBefore);
                    recurring.tags.addAll(t.tags);
                    recurring.notes = t.notes;
                    currentUser.tasks.add(recurring);
                    System.out.println("  🔁 Next recurrence created for " + next + " [ID: " + recurring.id + "]");
                }
                saveData();
                System.out.println("  ✓ Task marked as completed!");
                return;
            }
        }
        System.out.println("  ✗ Task not found.");
    }

    // ── MARK SUBTASK ─────────────────────────────────────────────────────────

    static void manageSubtasks() {
        System.out.println("\n══════ MANAGE SUBTASKS ══════");
        int id = readInt("Task ID: ");
        for (Task t : currentUser.tasks) {
            if (t.id == id) {
                if (t.subtasks.isEmpty()) { System.out.println("  ℹ️ No subtasks for this task."); return; }
                System.out.println("  Subtasks:");
                for (int i = 0; i < t.subtasks.size(); i++)
                    System.out.println("    " + (i + 1) + ". [" + (t.subtaskDone.get(i) ? "✓" : " ") + "] " + t.subtasks.get(i));
                int sub = readInt("  Toggle subtask number (0 to cancel): ");
                if (sub < 1 || sub > t.subtasks.size()) return;
                boolean cur = t.subtaskDone.get(sub - 1);
                t.subtaskDone.set(sub - 1, !cur);
                saveData();
                System.out.println("  ✓ Subtask " + (cur ? "un-marked." : "marked done!"));
                return;
            }
        }
        System.out.println("  ✗ Task not found.");
    }

    // ── EDIT TASK ────────────────────────────────────────────────────────────

    static void editTask() {
        System.out.println("\n══════ EDIT TASK ══════");
        int id = readInt("Task ID to edit: ");
        for (Task t : currentUser.tasks) {
            if (t.id == id) {
                System.out.println("  Leave blank to keep current value.");
                String newTitle = readLine("New Title [" + t.title + "]: ");
                if (!newTitle.isEmpty()) t.title = newTitle;

                String newDesc = readLine("New Description [" + t.description + "]: ");
                if (!newDesc.isEmpty()) t.description = newDesc;

                String newDL = readLine("New Deadline [" + t.deadline + "] (yyyy-MM-dd or blank): ");
                if (!newDL.isEmpty()) {
                    try { LocalDate.parse(newDL, FMT); t.deadline = newDL; }
                    catch (Exception e) { System.out.println("  ✗ Invalid date, keeping original."); }
                }

                String newNotes = readLine("New Notes [" + t.notes + "]: ");
                if (!newNotes.isEmpty()) t.notes = newNotes;

                String newTags = readLine("New Tags (comma-separated) [" + String.join(", ", t.tags) + "]: ");
                if (!newTags.isEmpty()) {
                    t.tags.clear();
                    for (String tag : newTags.split(",")) t.tags.add(tag.trim().toLowerCase());
                }

                saveData();
                System.out.println("  ✓ Task updated!");
                return;
            }
        }
        System.out.println("  ✗ Task not found.");
    }

    // ── DELETE TASK ──────────────────────────────────────────────────────────

    static void deleteTask() {
        System.out.println("\n══════ DELETE TASK ══════");
        int id = readInt("Task ID to delete: ");
        Iterator<Task> it = currentUser.tasks.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.id == id) {
                String confirm = readLine("  Delete \"" + t.title + "\"? (y/n): ");
                if (!confirm.equalsIgnoreCase("y")) { System.out.println("  Cancelled."); return; }
                it.remove();
                saveData();
                System.out.println("  ✓ Task deleted.");
                return;
            }
        }
        System.out.println("  ✗ Task not found.");
    }

    // ── CLEAR COMPLETED ──────────────────────────────────────────────────────

    static void clearCompleted() {
        long count = currentUser.tasks.stream().filter(t -> t.isCompleted).count();
        if (count == 0) { System.out.println("  ℹ️ No completed tasks to clear."); return; }
        String confirm = readLine("  Clear all " + count + " completed task(s)? (y/n): ");
        if (!confirm.equalsIgnoreCase("y")) { System.out.println("  Cancelled."); return; }
        currentUser.tasks.removeIf(t -> t.isCompleted);
        saveData();
        System.out.println("  ✓ Cleared " + count + " completed task(s).");
    }

    // ── STATISTICS ───────────────────────────────────────────────────────────

    static void showStats() {
        List<Task> tasks = currentUser.tasks;
        long total     = tasks.size();
        long completed = tasks.stream().filter(t -> t.isCompleted).count();
        long pending   = total - completed;
        long overdue   = tasks.stream().filter(Task::isOverdue).count();
        long high      = tasks.stream().filter(t -> t.priority == Priority.HIGH && !t.isCompleted).count();
        long dueSoon   = tasks.stream().filter(t -> t.isDueSoon(3)).count();

        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║           📊  YOUR STATISTICS            ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.printf( "║  Total Tasks    : %-24d║%n", total);
        System.out.printf( "║  Completed      : %-24d║%n", completed);
        System.out.printf( "║  Pending        : %-24d║%n", pending);
        System.out.printf( "║  Overdue        : %-24d║%n", overdue);
        System.out.printf( "║  High Priority  : %-24d║%n", high);
        System.out.printf( "║  Due in 3 days  : %-24d║%n", dueSoon);
        if (total > 0)
        System.out.printf( "║  Completion     : %-23s ║%n", (completed * 100 / total) + "%");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  By Category:                            ║");
        for (Category c : Category.values()) {
            long cnt = tasks.stream().filter(t -> t.category == c).count();
            if (cnt > 0) System.out.printf("║  %-12s : %-26d║%n", c, cnt);
        }
        System.out.println("╚══════════════════════════════════════════╝");
    }

    // ── EXPORT ───────────────────────────────────────────────────────────────

    static void exportToCSV() {
        String filename = "tasks_" + currentUser.username + "_" + LocalDate.now() + ".csv";
        try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
            pw.println("ID,Title,Description,Priority,Category,Deadline,Status,Created,Tags,Progress");
            for (Task t : currentUser.tasks) {
                pw.printf("%d,\"%s\",\"%s\",%s,%s,%s,%s,%s,\"%s\",%d%%%n",
                    t.id, t.title, t.description, t.priority, t.category,
                    t.deadline, t.isCompleted ? "Completed" : "Pending",
                    t.createdAt, String.join("|", t.tags), t.subtaskProgress());
            }
            System.out.println("  ✓ Exported to " + filename);
        } catch (Exception e) {
            System.out.println("  ✗ Export failed: " + e.getMessage());
        }
    }

    // ── MENUS ─────────────────────────────────────────────────────────────────

    static void menu() {
        while (true) {
            System.out.println("\n╔══════════════════════════════════════════╗");
            System.out.println("║      📋  TO-DO APP — " + currentUser.username);
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  TASKS                                   ║");
            System.out.println("║   1. Add Task                            ║");
            System.out.println("║   2. View / Search Tasks                 ║");
            System.out.println("║   3. Edit Task                           ║");
            System.out.println("║   4. Mark Task Complete                  ║");
            System.out.println("║   5. Manage Subtasks                     ║");
            System.out.println("║   6. Delete Task                         ║");
            System.out.println("║   7. Clear All Completed                 ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  ACCOUNT                                 ║");
            System.out.println("║   8. Statistics                          ║");
            System.out.println("║   9. Export to CSV                       ║");
            System.out.println("║  10. Change Password                     ║");
            System.out.println("║  11. Check Reminders                     ║");
            System.out.println("║   0. Logout                              ║");
            System.out.println("╚══════════════════════════════════════════╝");

            int choice = readInt("Choose: ");
            switch (choice) {
                case  1 -> addTask();
                case  2 -> viewTasks();
                case  3 -> editTask();
                case  4 -> markComplete();
                case  5 -> manageSubtasks();
                case  6 -> deleteTask();
                case  7 -> clearCompleted();
                case  8 -> showStats();
                case  9 -> exportToCSV();
                case 10 -> changePassword();
                case 11 -> checkReminders();
                case  0 -> { currentUser = null; System.out.println("  Logged out. Goodbye!"); return; }
                default -> System.out.println("  ✗ Invalid choice.");
            }
        }
    }

    // ── MAIN ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        loadData();
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║      ✅  WELCOME TO TO-DO APP            ║");
        System.out.println("╚══════════════════════════════════════════╝");

        while (true) {
            System.out.println("\n  1. Register");
            System.out.println("  2. Login");
            System.out.println("  3. Exit");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1 -> register();
                case 2 -> { if (login()) menu(); }
                case 3 -> { saveData(); System.out.println("\n  Goodbye! ✌️"); return; }
                default -> System.out.println("  ✗ Invalid choice.");
            }
        }
    }
}