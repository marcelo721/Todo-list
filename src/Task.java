import java.io.Serializable;

public class Task implements Serializable {
    private String description;
    private boolean completed;
    private String horario;

    public Task(String description, String horario) {
        this.description = description;
        this.horario = horario;
        this.completed = false;
    }

    public String getDescription() {
        return description;
    }

    public String getHorario() {
        return horario;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    @Override
    public String toString() {
        return (completed ? "(x) " : "( ) ") + "" + horario + "  " + description;
    }
}
