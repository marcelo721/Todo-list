import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class TodoListApp extends JFrame {
    private final Map<String, List<Task>> tasksByDay;
    private final String[] days = {"Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};
    private final Map<String, DefaultListModel<Task>> listModels = new HashMap<>();
    private final File saveFile = new File("tasks.ser");
    private final LocalDate currentDate = LocalDate.now();
    private final int currentDayIndex;

    public TodoListApp() {
        setTitle("To-Do List Semanal");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Encontra o índice do dia atual (0=Domingo, 1=Segunda, etc.)
        DayOfWeek currentDayOfWeek = currentDate.getDayOfWeek();
        this.currentDayIndex = currentDayOfWeek.getValue() % 7; // Ajuste para nosso array que começa com Domingo

        tasksByDay = loadTasks();
        checkWeeklyReset();

        JTabbedPane tabbedPane = new JTabbedPane();

        for (int i = 0; i < days.length; i++) {
            final int dayIndex = i;
            String day = days[i];
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            DefaultListModel<Task> model = new DefaultListModel<>();
            tasksByDay.get(day).forEach(model::addElement);
            listModels.put(day, model);

            JList<Task> taskList = new JList<>(model);
            taskList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            taskList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    Task task = (Task) value;
                    label.setText(task.toString());

                    // Verifica se é um dia passado e a task não foi completada
                    boolean isPastDay = dayIndex < currentDayIndex;
                    boolean isToday = dayIndex == currentDayIndex;

                    if (task.isCompleted()) {
                        label.setForeground(new Color(0, 128, 0)); // Verde para completadas
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    } else if (isPastDay) {
                        label.setForeground(Color.RED); // Vermelho para tasks não feitas em dias passados
                        label.setFont(label.getFont().deriveFont(Font.PLAIN));
                    } else if (isToday) {
                        label.setForeground(Color.BLACK); // Preto para tasks de hoje
                        label.setFont(label.getFont().deriveFont(Font.PLAIN));
                    } else {
                        label.setForeground(Color.BLUE); // Azul para tasks futuras
                        label.setFont(label.getFont().deriveFont(Font.PLAIN));
                    }

                    return label;
                }
            });

            taskList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Verifica se é um dia passado
                        if (dayIndex < currentDayIndex) {
                            JOptionPane.showMessageDialog(panel, "Não é possível marcar tasks de dias passados!", "Aviso", JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        Task selected = taskList.getSelectedValue();
                        if (selected != null) {
                            selected.setCompleted(!selected.isCompleted());
                            taskList.repaint();
                            saveTasks();
                        }
                    }
                }
            });

            // Add popup menu for delete option
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem deleteItem = new JMenuItem("Excluir");
            deleteItem.addActionListener(e -> {
                Task selected = taskList.getSelectedValue();
                if (selected != null) {
                    model.removeElement(selected);
                    tasksByDay.get(day).remove(selected);
                    saveTasks();
                }
            });
            popupMenu.add(deleteItem);

            taskList.setComponentPopupMenu(popupMenu);
            taskList.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        int index = taskList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            taskList.setSelectedIndex(index);
                            popupMenu.show(taskList, e.getX(), e.getY());
                        }
                    }
                }

                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        int index = taskList.locationToIndex(e.getPoint());
                        if (index >= 0) {
                            taskList.setSelectedIndex(index);
                            popupMenu.show(taskList, e.getX(), e.getY());
                        }
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(taskList);

            JTextField descField = new JTextField();
            JTextField timeField = new JTextField();
            timeField.setPreferredSize(new Dimension(80, 25));
            timeField.setToolTipText("Horário (ex: 14:30 - 12:20)");

            JButton addButton = new JButton("Adicionar");
            addButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            addButton.addActionListener(e -> {
                String desc = descField.getText().trim();
                String horario = timeField.getText().trim();

                if (desc.isEmpty() || horario.isEmpty()) {
                    JOptionPane.showMessageDialog(panel, "Preencha todos os campos.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (!horario.matches("\\d{2}:\\d{2}-\\d{2}:\\d{2}")) {
                    JOptionPane.showMessageDialog(panel, "Horário inválido. Use o formato HH:mm.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Task task = new Task(desc, horario);
                model.addElement(task);
                tasksByDay.get(day).add(task);
                descField.setText("");
                timeField.setText("");
                saveTasks();
            });

            JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
            JPanel inputFields = new JPanel(new BorderLayout(5, 5));
            inputFields.add(timeField, BorderLayout.WEST);
            inputFields.add(descField, BorderLayout.CENTER);
            inputPanel.add(inputFields, BorderLayout.CENTER);
            inputPanel.add(addButton, BorderLayout.EAST);

            panel.add(scrollPane, BorderLayout.CENTER);
            panel.add(inputPanel, BorderLayout.SOUTH);

            tabbedPane.addTab(day, panel);
        }

        add(tabbedPane);
        setVisible(true);
    }

    private void checkWeeklyReset() {
        if (currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            // Check if we already reset this week
            File resetFile = new File("reset_" + currentDate.toString() + ".marker");
            if (!resetFile.exists()) {
                // Reset all tasks
                for (String day : days) {
                    List<Task> tasks = tasksByDay.get(day);
                    for (Task task : tasks) {
                        task.setCompleted(false);
                    }
                    DefaultListModel<Task> model = listModels.get(day);
                    if (model != null) {
                        for (int i = 0; i < model.size(); i++) {
                            Task task = model.get(i);
                            task.setCompleted(false);
                        }
                    }
                }
                saveTasks();

                // Create marker file
                try {
                    resetFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Delete old marker files
                File[] oldMarkers = new File(".").listFiles((dir, name) -> name.startsWith("reset_") && name.endsWith(".marker"));
                if (oldMarkers != null) {
                    for (File marker : oldMarkers) {
                        if (!marker.getName().equals(resetFile.getName())) {
                            marker.delete();
                        }
                    }
                }
            }
        }
    }

    private Map<String, List<Task>> loadTasks() {
        Map<String, List<Task>> loaded = null;

        if (saveFile.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(saveFile))) {
                loaded = (Map<String, List<Task>>) in.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (loaded == null) {
            loaded = new HashMap<>();
        }
        for (String day : days) {
            loaded.putIfAbsent(day, new ArrayList<>());
        }

        return loaded;
    }

    private void saveTasks() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(saveFile))) {
            out.writeObject(tasksByDay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TodoListApp());
    }
}