import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;

class Task implements Serializable {
    private static final long serialVersionUID = 2L;
    int id;
    String title, description, deadline, notes, createdAt, attachmentName;
    Priority priority;
    Category category;
    TaskStatus status;
    boolean pinned, archived, deleted;
    ArrayList<String> tags = new ArrayList<>();
    ArrayList<SubTask> subtasks = new ArrayList<>();

    Task(int id, String title, String description, Priority priority, Category category,
         String deadline, String notes, TaskStatus status, String tagsText, String subTaskText,
         String attachmentName, boolean pinned) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.category = category;
        this.deadline = deadline;
        this.notes = notes;
        this.status = status;
        this.attachmentName = attachmentName;
        this.pinned = pinned;
        this.createdAt = LocalDate.now().toString();
        setTags(tagsText);
        setSubtasks(subTaskText);
    }

    void setTags(String text) {
        tags.clear();
        if (text == null) return;
        for (String t : text.split(",")) {
            t = t.trim();
            if (!t.isEmpty()) tags.add(t);
        }
    }

    void setSubtasks(String text) {
        subtasks.clear();
        if (text == null) return;
        for (String s : text.split("\\n")) {
            s = s.trim();
            if (!s.isEmpty()) subtasks.add(new SubTask(s, false));
        }
    }

    String tagsAsText() { return String.join(", ", tags); }

    String subtasksAsText() {
        StringBuilder sb = new StringBuilder();
        for (SubTask st : subtasks) {
            sb.append(st.done ? "[x] " : "[ ] ").append(st.text).append("\n");
        }
        return sb.toString().trim();
    }

    boolean isCompleted() { return status == TaskStatus.COMPLETED; }

    boolean isOverdue() {
        if (isCompleted() || deleted || archived || deadline == null || deadline.trim().isEmpty()) return false;
        try { return LocalDate.now().isAfter(LocalDate.parse(deadline.trim())); }
        catch (Exception e) { return false; }
    }

    boolean isDueToday() {
        if (isCompleted() || deleted || archived || deadline == null || deadline.trim().isEmpty()) return false;
        try { return LocalDate.now().equals(LocalDate.parse(deadline.trim())); }
        catch (Exception e) { return false; }
    }
}
