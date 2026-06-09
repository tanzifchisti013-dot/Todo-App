import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;

class User implements Serializable {
    private static final long serialVersionUID = 2L;
    String fullName, username, passwordHash, email, securityAnswerHash;
    String createdAt = LocalDate.now().toString();
    ArrayList<Task> tasks = new ArrayList<>();
    int taskIdCounter = 1;

    User(String fullName, String username, String passwordHash, String email, String answerHash) {
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.securityAnswerHash = answerHash;
    }
}
