import java.io.Serializable;

class SubTask implements Serializable {
    private static final long serialVersionUID = 1L;
    String text;
    boolean done;

    SubTask(String text, boolean done) {
        this.text = text;
        this.done = done;
    }
}
