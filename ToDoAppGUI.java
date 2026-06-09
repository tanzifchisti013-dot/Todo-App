import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;

// ───────────────────────── MAIN GUI APP ─────────────────────────
public class ToDoAppGUI extends JFrame {
    private static final String DATA_FILE = "smart_task_data_v2.ser";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ArrayList<User> users = new ArrayList<>();
    private User currentUser;
    private boolean darkMode = true;

    private CardLayout appCards = new CardLayout();
    private JPanel root = new JPanel(appCards);
    private JPanel appPanel, contentPanel, sidebar;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> filterBox, sortBox;
    private JProgressBar progressBar;
    private JLabel totalCard, pendingCard, progressLabel;
    private JLabel doneCard, overdueCard, greetingLabel;
    private String currentPage = "Dashboard";

    // Professional masculine color theme: navy, steel blue, cyan, teal, graphite and amber
    Color bg() { return darkMode ? new Color(5, 11, 24) : new Color(235, 243, 252); }
    Color panel() { return darkMode ? new Color(12, 25, 48) : new Color(255, 255, 255); }
    Color card() { return darkMode ? new Color(18, 38, 70) : new Color(248, 252, 255); }
    Color input() { return darkMode ? new Color(20, 48, 84) : new Color(229, 241, 255); }
    Color text() { return darkMode ? new Color(243, 248, 255) : new Color(10, 27, 50); }
    Color muted() { return darkMode ? new Color(174, 197, 224) : new Color(67, 85, 110); }
    Color line() { return darkMode ? new Color(41, 99, 150) : new Color(173, 205, 236); }
    final Color BLUE = new Color(0, 102, 255), GREEN = new Color(0, 184, 148), RED = new Color(235, 70, 70), AMBER = new Color(245, 170, 35), PURPLE = new Color(72, 85, 255);
    final Color PINK = new Color(0, 180, 216), ORANGE = new Color(255, 140, 42), CYAN = new Color(0, 210, 255), VIOLET = new Color(28, 64, 180);
    Font titleFont = new Font("Segoe UI", Font.BOLD, 25);
    Font hFont = new Font("Segoe UI", Font.BOLD, 18);
    Font bodyFont = new Font("Segoe UI", Font.PLAIN, 14);
    Font smallFont = new Font("Segoe UI", Font.PLAIN, 12);
    Font boldFont = new Font("Segoe UI", Font.BOLD, 14);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ToDoAppGUI());
    }

    public ToDoAppGUI() {
        loadData();
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("Button.disabledText", new Color(160, 190, 220));
        UIManager.put("ComboBox.disabledForeground", new Color(160, 190, 220));
        UIManager.put("Table.gridColor", line());
        UIManager.put("ComboBox.background", input());
        UIManager.put("ComboBox.foreground", text());
        UIManager.put("ComboBox.selectionBackground", BLUE);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", darkMode ? new Color(19, 76, 125) : new Color(207, 230, 255));
        UIManager.put("TableHeader.foreground", darkMode ? Color.WHITE : new Color(5, 45, 105));
        setTitle("Smart Task Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setContentPane(root);
        rebuildAll();
        setVisible(true);
    }

    // ───────────────────────── BASIC HELPERS ─────────────────────────
    private String hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) { return s; }
    }

    private void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) { out.writeObject(users); }
        catch (Exception e) { error("Data save failed: " + e.getMessage()); }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_FILE))) { users = (ArrayList<User>) in.readObject(); }
        catch (Exception ignored) { users = new ArrayList<>(); }
    }

    private void error(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    private void success(String msg) { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
    private boolean validEmail(String email) { return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"); }
    private boolean validDate(String d) {
        if (d == null || d.trim().isEmpty()) return true;
        try { LocalDate.parse(d.trim(), DATE_FORMAT); return true; } catch (Exception e) { return false; }
    }
    private User findUser(String username) {
        for (User u : users) if (u.username.equalsIgnoreCase(username)) return u;
        return null;
    }

    // ───────────────────────── UI COMPONENTS ─────────────────────────
    private JLabel label(String txt, Font f, Color c) { JLabel l = new JLabel(txt); l.setFont(f); l.setForeground(c); return l; }
    private JPanel roundedPanel(Color color, int pad) {
        JPanel p = new JPanel();
        p.setBackground(color);
        p.setBorder(new CompoundBorder(new LineBorder(line(), 1, true), new EmptyBorder(pad, pad, pad, pad)));
        return p;
    }
    private JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.setToolTipText(placeholder);
        f.setFont(bodyFont); f.setBackground(input()); f.setForeground(text()); f.setCaretColor(BLUE);
        f.setBorder(new CompoundBorder(new LineBorder(line(), 1, true), new EmptyBorder(9, 12, 9, 12)));
        return f;
    }
    private JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(bodyFont); f.setBackground(input()); f.setForeground(text()); f.setCaretColor(BLUE);
        f.setBorder(new CompoundBorder(new LineBorder(line(), 1, true), new EmptyBorder(9, 12, 9, 12)));
        return f;
    }
    private JTextArea textArea() {
        JTextArea a = new JTextArea(4, 20);
        a.setFont(bodyFont); a.setLineWrap(true); a.setWrapStyleWord(true);
        a.setBackground(input()); a.setForeground(text()); a.setCaretColor(BLUE);
        a.setBorder(new EmptyBorder(9, 12, 9, 12));
        return a;
    }
    private JButton button(String txt, Color c) {
        JButton b = new JButton(txt) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color right = c.equals(GREEN) ? new Color(0, 210, 255)
                        : c.equals(RED) ? new Color(255, 120, 58)
                        : c.equals(AMBER) ? new Color(255, 198, 72)
                        : c.equals(PURPLE) ? new Color(0, 180, 216)
                        : new Color(0, 180, 216);
                GradientPaint gp = new GradientPaint(0, 0, c, getWidth(), getHeight(), right);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(255, 255, 255, 42));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(boldFont);
        b.setForeground(Color.WHITE);
        b.setBackground(c);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(11, 18, 11, 18));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(new Color(255, 252, 220)); }
            public void mouseExited(MouseEvent e) { b.setForeground(Color.WHITE); }
        });
        return b;
    }

    private JButton ghostButton(String txt) {
        JButton b = new JButton(txt) {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color left = darkMode ? new Color(19, 57, 96) : new Color(226, 240, 255);
                Color right = darkMode ? new Color(11, 91, 122) : new Color(205, 231, 255);
                GradientPaint gp = new GradientPaint(0, 0, left, getWidth(), getHeight(), right);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(darkMode ? new Color(0, 210, 255, 160) : new Color(0, 102, 255, 140));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(boldFont);
        b.setForeground(darkMode ? Color.WHITE : new Color(5, 45, 105));
        b.setBackground(darkMode ? new Color(19, 57, 96) : new Color(226, 240, 255));
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setForeground(darkMode ? new Color(255, 220, 120) : BLUE); }
            public void mouseExited(MouseEvent e) { b.setForeground(darkMode ? Color.WHITE : new Color(5, 45, 105)); }
        });
        return b;
    }
    private JComboBox<String> combo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(bodyFont);
        cb.setBackground(darkMode ? new Color(14, 54, 92) : new Color(230, 244, 255));
        cb.setForeground(darkMode ? Color.WHITE : new Color(8, 48, 88));
        cb.setBorder(new LineBorder(new Color(0, 170, 220), 1, true));
        cb.setFocusable(false);
        cb.setOpaque(true);
        cb.setPreferredSize(new Dimension(150, 38));
        cb.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton("▼");
                b.setBorder(new EmptyBorder(0, 6, 0, 6));
                b.setForeground(Color.WHITE);
                b.setBackground(darkMode ? new Color(0, 108, 170) : new Color(0, 132, 210));
                b.setFocusPainted(false);
                return b;
            }

            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(darkMode ? new Color(14, 54, 92) : new Color(230, 244, 255));
                g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                g2.dispose();
            }
        });
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setFont(bodyFont);
                l.setOpaque(true);
                l.setBackground(isSelected ? new Color(0, 132, 210) : (darkMode ? new Color(14, 54, 92) : new Color(230, 244, 255)));
                l.setForeground(isSelected ? Color.WHITE : (darkMode ? Color.WHITE : new Color(8, 48, 88)));
                l.setBorder(new EmptyBorder(8, 12, 8, 12));
                return l;
            }
        });
        return cb;
    }

    private void rebuildAll() {
        root.removeAll();
        root.setBackground(bg());
        root.add(buildLoginPanel(), "LOGIN");
        root.add(buildRegisterPanel(), "REGISTER");
        if (currentUser != null) root.add(buildAppPanel(), "APP");
        appCards.show(root, currentUser == null ? "LOGIN" : "APP");
        root.revalidate(); root.repaint();
    }

    // ───────────────────────── AUTH SCREENS ─────────────────────────
    private JPanel authWrapper(JPanel formPanel) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(bg());

        JPanel hero = new JPanel(new GridBagLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(15, 23, 42), getWidth(), getHeight(), new Color(14, 116, 144));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 42));
                g2.fillOval(-90, 55, 280, 280);
                g2.setColor(new Color(59, 130, 246, 70));
                g2.fillOval(getWidth() - 160, 25, 220, 220);
                g2.setColor(new Color(16, 185, 129, 45));
                g2.fillOval(getWidth() - 140, getHeight() - 180, 300, 300);
                g2.setColor(new Color(255, 255, 255, 28));
                g2.fillRoundRect(64, getHeight() - 235, getWidth() - 128, 150, 38, 38);
                g2.dispose();
            }
        };
        hero.setPreferredSize(new Dimension(470, 0));
        hero.setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel heroText = new JPanel();
        heroText.setOpaque(false);
        heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        JLabel brand = label("⚡ Smart Task", new Font("Segoe UI", Font.BOLD, 38), Color.WHITE);
        JLabel tagline = label("Focused. Fast. Organized.", new Font("Segoe UI", Font.BOLD, 23), new Color(255, 252, 220));
        JTextArea desc = new JTextArea("A professional task management system with deadlines, priorities, reports, calendar, archive, trash, backup and profile features.");
        desc.setOpaque(false); desc.setEditable(false); desc.setFocusable(false);
        desc.setLineWrap(true); desc.setWrapStyleWord(true);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        desc.setForeground(new Color(224, 231, 255));
        desc.setMaximumSize(new Dimension(360, 100));
        heroText.add(brand); heroText.add(Box.createVerticalStrut(16));
        heroText.add(tagline); heroText.add(Box.createVerticalStrut(14));
        heroText.add(desc); heroText.add(Box.createVerticalStrut(36));
        heroText.add(heroBadge("📊  Professional dashboard"));
        heroText.add(Box.createVerticalStrut(12));
        heroText.add(heroBadge("🔔  Smart reminders"));
        heroText.add(Box.createVerticalStrut(12));
        heroText.add(heroBadge("📅  Calendar, reports & backup"));
        hero.add(heroText);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(bg());
        right.setBorder(new EmptyBorder(30, 30, 30, 30));
        right.add(formPanel);

        page.add(hero, BorderLayout.WEST);
        page.add(right, BorderLayout.CENTER);
        return page;
    }

    private JPanel heroBadge(String textValue) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(360, 44));
        p.setBorder(new CompoundBorder(new LineBorder(new Color(255,255,255,70), 1, true), new EmptyBorder(2, 12, 2, 12)));
        JLabel l = label(textValue, new Font("Segoe UI", Font.BOLD, 14), Color.WHITE);
        p.add(l);
        return p;
    }

    private JPanel modernAuthCard(int width, int height) {
        JPanel cardBox = new JPanel();
        cardBox.setLayout(new BoxLayout(cardBox, BoxLayout.Y_AXIS));
        cardBox.setPreferredSize(new Dimension(width, height));
        cardBox.setBackground(panel());
        cardBox.setBorder(new CompoundBorder(new LineBorder(line(), 1, true), new EmptyBorder(34, 38, 34, 38)));
        return cardBox;
    }

    private void setFieldSize(JComponent c, int width, int height) {
        c.setPreferredSize(new Dimension(width, height));
        c.setMaximumSize(new Dimension(width, height));
    }

    private JPanel authInfoStrip(String textValue) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(darkMode ? new Color(22, 42, 70) : new Color(232, 240, 254));
        p.setMaximumSize(new Dimension(360, 48));
        p.setBorder(new CompoundBorder(new LineBorder(darkMode ? new Color(59, 130, 246) : new Color(147, 197, 253), 1, true), new EmptyBorder(10, 14, 10, 14)));
        JLabel l = label(textValue, smallFont, darkMode ? new Color(219, 234, 254) : new Color(30, 64, 175));
        p.add(l, BorderLayout.CENTER);
        return p;
    }

    private JButton linkButton(String txt) {
        JButton b = new JButton(txt);
        b.setFont(boldFont);
        b.setForeground(BLUE);
        b.setBackground(panel());
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        return b;
    }

    private JPanel buildLoginPanel() {
        JPanel cardBox = modernAuthCard(440, 560);
        JLabel mini = label("ACCOUNT LOGIN", new Font("Segoe UI", Font.BOLD, 12), BLUE);
        mini.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = label("Welcome back", new Font("Segoe UI", Font.BOLD, 31), text());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = label("Sign in to continue your task workspace", smallFont, muted());
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField user = field("Username");
        JPasswordField pass = passwordField();
        setFieldSize(user, 360, 46); setFieldSize(pass, 360, 46);

        JButton login = button("Sign In", BLUE);
        setFieldSize(login, 360, 48); login.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton forgot = linkButton("Forgot password?");
        JButton register = ghostButton("Create New Account");
        setFieldSize(register, 360, 46); register.setAlignmentX(Component.CENTER_ALIGNMENT);

        login.addActionListener(e -> {
            User u = findUser(user.getText().trim());
            if (u != null && u.passwordHash.equals(hash(new String(pass.getPassword())))) {
                currentUser = u; rebuildAll(); refreshAll(); showReminders();
            } else error("Invalid username or password.");
        });
        register.addActionListener(e -> appCards.show(root, "REGISTER"));
        forgot.addActionListener(e -> forgotPasswordDialog());

        cardBox.add(mini); cardBox.add(Box.createVerticalStrut(8));
        cardBox.add(title); cardBox.add(Box.createVerticalStrut(8));
        cardBox.add(sub); cardBox.add(Box.createVerticalStrut(28));
        addFormField(cardBox, "Username", user);
        addFormField(cardBox, "Password", pass);
        JPanel forgotWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotWrap.setOpaque(false); forgotWrap.setMaximumSize(new Dimension(360, 28)); forgotWrap.add(forgot);
        cardBox.add(forgotWrap); cardBox.add(Box.createVerticalStrut(16));
        cardBox.add(login); cardBox.add(Box.createVerticalStrut(18));
        cardBox.add(authInfoStrip("🔒 Your tasks are saved locally on this computer."));
        cardBox.add(Box.createVerticalGlue());
        cardBox.add(register);
        return authWrapper(cardBox);
    }

    private JPanel buildRegisterPanel() {
        JPanel cardBox = modernAuthCard(500, 680);
        JLabel mini = label("NEW USER REGISTRATION", new Font("Segoe UI", Font.BOLD, 12), GREEN);
        mini.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = label("Create your account", new Font("Segoe UI", Font.BOLD, 29), text());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = label("Set up your profile and start managing tasks", smallFont, muted());
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField full = field("Full Name"), email = field("Email"), user = field("Username");
        JPasswordField pass = passwordField(), confirm = passwordField();
        for (JComponent c : new JComponent[]{full,email,user,pass,confirm}) setFieldSize(c, 390, 43);

        JButton create = button("Create Account", GREEN);
        setFieldSize(create, 390, 48); create.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton back = ghostButton("Back to Login");
        setFieldSize(back, 390, 46); back.setAlignmentX(Component.CENTER_ALIGNMENT);

        create.addActionListener(e -> {
            String f = full.getText().trim(), em = email.getText().trim(), un = user.getText().trim();
            String pw = new String(pass.getPassword()), cpw = new String(confirm.getPassword());
            if (f.isEmpty() || em.isEmpty() || un.isEmpty() || pw.isEmpty() || cpw.isEmpty()) { error("Please fill all fields."); return; }
            if (!validEmail(em)) { error("Please enter a valid email address."); return; }
            if (pw.length() < 6) { error("Password must be at least 6 characters."); return; }
            if (!pw.equals(cpw)) { error("Confirm password does not match."); return; }
            if (findUser(un) != null) { error("Username already exists."); return; }
            users.add(new User(f, un, hash(pw), em, hash(""))); saveData(); success("Account created successfully."); appCards.show(root, "LOGIN");
        });
        back.addActionListener(e -> appCards.show(root, "LOGIN"));

        cardBox.add(mini); cardBox.add(Box.createVerticalStrut(6));
        cardBox.add(title); cardBox.add(Box.createVerticalStrut(6));
        cardBox.add(sub); cardBox.add(Box.createVerticalStrut(22));
        addFormField(cardBox, "Full Name", full);
        addFormField(cardBox, "Email Address", email);
        addFormField(cardBox, "Username", user);
        addFormField(cardBox, "Password", pass);
        addFormField(cardBox, "Confirm Password", confirm);
        cardBox.add(Box.createVerticalStrut(6));
        cardBox.add(authInfoStrip("Password must be at least 6 characters."));
        cardBox.add(Box.createVerticalStrut(14));
        cardBox.add(create); cardBox.add(Box.createVerticalStrut(10)); cardBox.add(back);
        return authWrapper(cardBox);
    }

    private void addFormField(JPanel parent, String name, JComponent comp) {
        parent.add(label(name, smallFont, muted())); parent.add(Box.createVerticalStrut(4)); parent.add(comp); parent.add(Box.createVerticalStrut(9));
    }

    private void forgotPasswordDialog() {
        JTextField username = field("Username");
        JTextField email = field("Registered Email");
        JPasswordField newPass = passwordField();
        JPanel p = new JPanel(new GridLayout(0,1,6,6)); p.setBackground(panel());
        p.add(label("Username", smallFont, text())); p.add(username);
        p.add(label("Registered Email", smallFont, text())); p.add(email);
        p.add(label("New Password", smallFont, text())); p.add(newPass);
        int ok = JOptionPane.showConfirmDialog(this, p, "Reset Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            User u = findUser(username.getText().trim());
            String np = new String(newPass.getPassword());
            if (u == null) error("User not found.");
            else if (!u.email.equalsIgnoreCase(email.getText().trim())) error("Email does not match this account.");
            else if (np.length() < 6) error("Password must be at least 6 characters.");
            else { u.passwordHash = hash(np); saveData(); success("Password reset successful."); }
        }
    }

    // ───────────────────────── MAIN APP SCREEN ─────────────────────────
    private JPanel buildAppPanel() {
        appPanel = new JPanel(new BorderLayout()); appPanel.setBackground(bg());
        sidebar = buildSidebar();
        contentPanel = new JPanel(new BorderLayout()); contentPanel.setBackground(bg()); contentPanel.setBorder(new EmptyBorder(18, 18, 18, 18));
        appPanel.add(sidebar, BorderLayout.WEST); appPanel.add(contentPanel, BorderLayout.CENTER);
        showPage(currentPage);
        return appPanel;
    }

    private JPanel buildSidebar() {
        JPanel s = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = darkMode
                    ? new GradientPaint(0, 0, new Color(15, 23, 42), getWidth(), getHeight(), new Color(8, 75, 105))
                    : new GradientPaint(0, 0, new Color(226, 235, 247), getWidth(), getHeight(), new Color(209, 232, 244));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(darkMode ? new Color(255,255,255,28) : new Color(59,130,246,32));
                g2.fillOval(-70, 90, 190, 190);
                g2.setColor(darkMode ? new Color(255,203,82,35) : new Color(34,211,238,35));
                g2.fillOval(120, 430, 190, 190);
                g2.dispose();
            }
        };
        s.setOpaque(false);
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS)); s.setPreferredSize(new Dimension(250, 0));
        s.setBorder(new MatteBorder(0, 0, 0, 1, darkMode ? new Color(59, 130, 246) : new Color(147, 197, 253)));
        JLabel title = label("⚡ Smart Task", new Font("Segoe UI", Font.BOLD, 24), darkMode ? Color.WHITE : new Color(30, 64, 175));
        title.setBorder(new EmptyBorder(26, 22, 20, 20)); s.add(title);
        String[] items = {"Dashboard", "Tasks", "Calendar", "Archive", "Trash", "Reports", "Profile"};
        for (String item : items) s.add(sideBtn(item));
        s.add(Box.createVerticalGlue());
        JButton theme = sideBtn(darkMode ? "☀ Light Mode" : "🌙 Dark Mode"); theme.addActionListener(e -> { darkMode = !darkMode; rebuildAll(); refreshAll(); }); s.add(theme);
        JButton logout = sideBtn("↪ Logout"); logout.addActionListener(e -> { currentUser = null; currentPage = "Dashboard"; rebuildAll(); }); s.add(logout);
        s.add(Box.createVerticalStrut(18));
        return s;
    }

    private JButton sideBtn(String txt) {
        String clean = txt.replace("☀ ", "").replace("🌙 ", "").replace("↪ ", "");
        boolean active = clean.equals(currentPage);
        JButton b = new JButton(txt);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFont(boldFont);
        b.setForeground(active ? Color.WHITE : (darkMode ? new Color(224, 242, 254) : new Color(8, 68, 120)));
        b.setBackground(active ? new Color(0, 153, 230) : new Color(0,0,0,0));
        b.setOpaque(active);
        b.setFocusPainted(false); b.setBorderPainted(false);
        b.setMaximumSize(new Dimension(250, 46)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(12, 24, 12, 16));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (!active) { b.setOpaque(true); b.setBackground(darkMode ? new Color(255,255,255,34) : new Color(255,255,255,160)); } }
            public void mouseExited(MouseEvent e) { if (!active) { b.setOpaque(false); b.setBackground(new Color(0,0,0,0)); } }
        });
        if (!clean.equals("Logout") && !clean.contains("Mode")) b.addActionListener(e -> showPage(clean));
        return b;
    }

    private void showPage(String page) {
        currentPage = page;
        if (contentPanel == null) return;
        contentPanel.removeAll();
        contentPanel.add(buildHeader(page), BorderLayout.NORTH);
        switch (page) {
            case "Tasks": contentPanel.add(buildTasksPage(false, false), BorderLayout.CENTER); break;
            case "Calendar": contentPanel.add(buildCalendarPage(), BorderLayout.CENTER); break;
            case "Archive": contentPanel.add(buildTasksPage(true, false), BorderLayout.CENTER); break;
            case "Trash": contentPanel.add(buildTasksPage(false, true), BorderLayout.CENTER); break;
            case "Reports": contentPanel.add(buildReportsPage(), BorderLayout.CENTER); break;
            case "Profile": contentPanel.add(buildProfilePage(), BorderLayout.CENTER); break;
            default: contentPanel.add(buildDashboardPage(), BorderLayout.CENTER); break;
        }
        if (appPanel != null) { appPanel.remove(sidebar); sidebar = buildSidebar(); appPanel.add(sidebar, BorderLayout.WEST); }
        contentPanel.revalidate(); contentPanel.repaint();
        refreshAll();
    }

    private JPanel buildHeader(String page) {
        JPanel h = new JPanel(new BorderLayout()); h.setBackground(bg()); h.setBorder(new EmptyBorder(0, 0, 14, 0));
        greetingLabel = label(page, titleFont, text());
        JLabel sub = label("Welcome, " + safe(currentUser.fullName) + "  •  " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), smallFont, muted());
        JPanel left = new JPanel(); left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS)); left.setBackground(bg()); left.add(greetingLabel); left.add(Box.createVerticalStrut(3)); left.add(sub);
        JButton add = button("+ Add Task", BLUE); add.addActionListener(e -> openTaskDialog(null));
        JButton backup = ghostButton("Backup"); backup.addActionListener(e -> backupData());
        JButton restore = ghostButton("Restore"); restore.addActionListener(e -> restoreData());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); right.setBackground(bg()); right.add(backup); right.add(restore); right.add(add);
        h.add(left, BorderLayout.WEST); h.add(right, BorderLayout.EAST); return h;
    }

    private String safe(String s) { return s == null || s.trim().isEmpty() ? currentUser.username : s; }

    // ───────────────────────── DASHBOARD ─────────────────────────
    private JPanel buildDashboardPage() {
        JPanel p = new JPanel(new BorderLayout(12, 12)); p.setBackground(bg());
        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12)); cards.setBackground(bg());
        totalCard = statCard("Total Tasks", "0", BLUE); pendingCard = statCard("Pending", "0", AMBER); doneCard = statCard("Completed", "0", GREEN); overdueCard = statCard("Overdue", "0", RED);
        cards.add(totalCard.getParent()); cards.add(pendingCard.getParent()); cards.add(doneCard.getParent()); cards.add(overdueCard.getParent());
        JPanel middle = new JPanel(new GridLayout(1, 2, 12, 12)); middle.setBackground(bg());
        middle.add(buildProgressCard()); middle.add(buildQuickReportCard());
        JPanel top = new JPanel(new BorderLayout(0,12)); top.setBackground(bg()); top.add(cards, BorderLayout.NORTH); top.add(middle, BorderLayout.CENTER);
        p.add(top, BorderLayout.NORTH); p.add(buildTasksPage(false, false), BorderLayout.CENTER); return p;
    }

    private JLabel statCard(String name, String value, Color accent) {
        JPanel c = new JPanel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, card(), getWidth(), getHeight(), new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), darkMode ? 90 : 55));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 7, getHeight(), 20, 20);
                g2.setColor(line());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        c.setOpaque(false);
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBorder(new EmptyBorder(18, 20, 18, 18));
        JLabel n = label(name, smallFont, darkMode ? new Color(210, 225, 245) : muted());
        JLabel v = label(value, new Font("Segoe UI", Font.BOLD, 30), accent);
        c.add(n); c.add(Box.createVerticalStrut(10)); c.add(v); return v;
    }

    private JPanel buildProgressCard() {
        JPanel p = roundedPanel(card(), 18); p.setLayout(new BorderLayout(10,10));
        progressLabel = label("Completion Progress", hFont, text());
        progressBar = new JProgressBar(0,100); progressBar.setStringPainted(true); progressBar.setForeground(GREEN); progressBar.setBackground(darkMode ? new Color(10, 30, 55) : new Color(220, 238, 255)); progressBar.setBorderPainted(false); progressBar.setPreferredSize(new Dimension(0, 34));
        progressBar.setUI(new BasicProgressBarUI() {
            protected Color getSelectionBackground() { return Color.WHITE; }
            protected Color getSelectionForeground() { return Color.WHITE; }
        });
        p.add(progressLabel, BorderLayout.NORTH); p.add(progressBar, BorderLayout.CENTER); return p;
    }

    private JPanel buildQuickReportCard() {
        JPanel p = roundedPanel(card(), 18); p.setLayout(new BorderLayout());
        JTextArea a = new JTextArea(weeklyReportText()); a.setEditable(false); a.setBackground(card()); a.setForeground(text()); a.setFont(bodyFont);
        p.add(label("Weekly Summary", hFont, text()), BorderLayout.NORTH); p.add(a, BorderLayout.CENTER); return p;
    }

    // ───────────────────────── TASKS PAGE/TABLE ─────────────────────────
    private JPanel buildTasksPage(boolean archiveMode, boolean trashMode) {
        JPanel p = new JPanel(new BorderLayout(0, 10)); p.setBackground(bg());
        JPanel toolbar = roundedPanel(darkMode ? new Color(14, 34, 62) : new Color(238, 247, 255), 10); toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchField = field("Search tasks..."); searchField.setPreferredSize(new Dimension(230, 38));
        filterBox = combo(new String[]{"All", "Pinned", "Pending", "In Progress", "Completed", "Cancelled", "Overdue", "Due Today", "HIGH", "MEDIUM", "LOW", "WORK", "PERSONAL", "SHOPPING", "HEALTH", "EDUCATION", "FINANCE", "OTHER"});
        sortBox = combo(new String[]{"Pinned First", "Newest First", "Deadline", "Priority", "Category", "Status"});
        JButton export = ghostButton("Export CSV"); export.addActionListener(e -> exportCSV());
        searchField.getDocument().addDocumentListener(new DocumentListener(){ public void insertUpdate(DocumentEvent e){refreshTable(archiveMode,trashMode);} public void removeUpdate(DocumentEvent e){refreshTable(archiveMode,trashMode);} public void changedUpdate(DocumentEvent e){refreshTable(archiveMode,trashMode);} });
        filterBox.addActionListener(e -> refreshTable(archiveMode, trashMode)); sortBox.addActionListener(e -> refreshTable(archiveMode, trashMode));
        toolbar.add(label("Search", smallFont, darkMode ? new Color(210, 235, 255) : new Color(8, 68, 120))); toolbar.add(searchField); toolbar.add(label("Filter", smallFont, darkMode ? new Color(210, 235, 255) : new Color(8, 68, 120))); toolbar.add(filterBox); toolbar.add(label("Sort", smallFont, darkMode ? new Color(210, 235, 255) : new Color(8, 68, 120))); toolbar.add(sortBox); toolbar.add(export);

        String[] cols = {"ID", "Pin", "Title", "Priority", "Category", "Deadline", "Status", "Tags"};
        tableModel = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        table = new JTable(tableModel); table.setRowHeight(40); table.setFont(bodyFont); table.setBackground(card()); table.setForeground(text()); table.setSelectionBackground(new Color(0, 153, 230)); table.setSelectionForeground(Color.WHITE); table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0, 2));
        table.getTableHeader().setFont(boldFont); table.getTableHeader().setBackground(darkMode ? new Color(17, 80, 130) : new Color(215, 235, 255)); table.getTableHeader().setForeground(darkMode ? Color.WHITE : new Color(8, 68, 120)); table.getTableHeader().setOpaque(true);
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                l.setFont(boldFont);
                l.setOpaque(true);
                l.setBackground(darkMode ? new Color(17, 80, 130) : new Color(215, 235, 255));
                l.setForeground(darkMode ? Color.WHITE : new Color(8, 68, 120));
                l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 1, line()), new EmptyBorder(8, 10, 8, 10)));
                return l;
            }
        });
        int[] w = {45,45,260,90,110,105,120,180}; for (int i=0;i<w.length;i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object val,boolean sel,boolean foc,int row,int col){
                super.getTableCellRendererComponent(t,val,sel,foc,row,col);
                setBorder(new EmptyBorder(0,12,0,12)); setBackground(sel ? new Color(0, 153, 230) : (row%2==0?card():panel())); setForeground(text());
                String v = val == null ? "" : val.toString();
                if (col==3) setForeground(v.equals("HIGH")?RED:v.equals("MEDIUM")?AMBER:GREEN);
                if (col==6) setForeground(v.equals("COMPLETED")?GREEN:v.equals("CANCELLED")?RED:v.equals("IN_PROGRESS")?BLUE:muted());
                if (col==5 && isDateOverdue(v)) setForeground(RED);
                return this;
            }
        });
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(new LineBorder(line(), 1, true)); scroll.getViewport().setBackground(card());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); buttons.setBackground(bg());
        JButton view = ghostButton("View Details"), edit = ghostButton("Edit"), complete = button("Complete", GREEN), progress = button("In Progress", BLUE), pin = ghostButton("Pin/Unpin");
        JButton archive = ghostButton(archiveMode ? "Unarchive" : "Archive"), del = button(trashMode ? "Delete Forever" : "Move to Trash", RED), restore = ghostButton("Restore");
        view.addActionListener(e -> viewSelected()); edit.addActionListener(e -> editSelected()); complete.addActionListener(e -> setStatusSelected(TaskStatus.COMPLETED)); progress.addActionListener(e -> setStatusSelected(TaskStatus.IN_PROGRESS)); pin.addActionListener(e -> pinSelected());
        archive.addActionListener(e -> archiveSelected(!archiveMode)); del.addActionListener(e -> deleteSelected(trashMode)); restore.addActionListener(e -> restoreSelected());
        buttons.add(view); buttons.add(edit); buttons.add(complete); buttons.add(progress); buttons.add(pin); buttons.add(archive); buttons.add(del); if (trashMode) buttons.add(restore);
        p.add(toolbar, BorderLayout.NORTH); p.add(scroll, BorderLayout.CENTER); p.add(buttons, BorderLayout.SOUTH);
        refreshTable(archiveMode, trashMode); return p;
    }

    private boolean isDateOverdue(String v) { try { return !v.isEmpty() && LocalDate.now().isAfter(LocalDate.parse(v)); } catch(Exception e){ return false; } }

    private void refreshTable(boolean archiveMode, boolean trashMode) {
        if (tableModel == null || currentUser == null) return;
        tableModel.setRowCount(0);
        String search = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        String filter = filterBox == null ? "All" : (String) filterBox.getSelectedItem();
        ArrayList<Task> list = new ArrayList<>();
        for (Task t : currentUser.tasks) {
            if (archiveMode != t.archived) continue;
            if (trashMode != t.deleted) continue;
            if (!matchesSearch(t, search) || !matchesFilter(t, filter)) continue;
            list.add(t);
        }
        String sort = sortBox == null ? "Pinned First" : (String) sortBox.getSelectedItem();
        Collections.sort(list, comparatorFor(sort));
        for (Task t : list) tableModel.addRow(new Object[]{t.id, t.pinned ? "📌" : "", t.title, t.priority, t.category, t.deadline, t.status, t.tagsAsText()});
        updateStats();
    }

    private boolean matchesSearch(Task t, String s) {
        if (s.isEmpty()) return true;
        return (t.title+" "+t.description+" "+t.notes+" "+t.tagsAsText()+" "+t.attachmentName).toLowerCase().contains(s);
    }
    private boolean matchesFilter(Task t, String f) {
        if (f == null || f.equals("All")) return true;
        if (f.equals("Pinned")) return t.pinned;
        if (f.equals("Pending")) return t.status == TaskStatus.PENDING;
        if (f.equals("In Progress")) return t.status == TaskStatus.IN_PROGRESS;
        if (f.equals("Completed")) return t.status == TaskStatus.COMPLETED;
        if (f.equals("Cancelled")) return t.status == TaskStatus.CANCELLED;
        if (f.equals("Overdue")) return t.isOverdue();
        if (f.equals("Due Today")) return t.isDueToday();
        try { return t.priority == Priority.valueOf(f) || t.category == Category.valueOf(f); } catch(Exception e) { return true; }
    }
    private Comparator<Task> comparatorFor(String sort) {
        Comparator<Task> pinned = Comparator.comparing((Task t) -> !t.pinned);
        if ("Deadline".equals(sort)) return pinned.thenComparing(t -> t.deadline == null || t.deadline.isEmpty() ? "9999-12-31" : t.deadline);
        if ("Priority".equals(sort)) return pinned.thenComparing(t -> t.priority.ordinal());
        if ("Category".equals(sort)) return pinned.thenComparing(t -> t.category.toString());
        if ("Status".equals(sort)) return pinned.thenComparing(t -> t.status.ordinal());
        if ("Newest First".equals(sort)) return pinned.thenComparing((Task t) -> t.id).reversed();
        return pinned.thenComparing(t -> t.isOverdue() ? 0 : 1).thenComparing(t -> t.priority.ordinal()).thenComparing(t -> t.deadline == null || t.deadline.isEmpty() ? "9999-12-31" : t.deadline);
    }

    private Task selectedTask() {
        if (table == null || table.getSelectedRow() < 0) { error("Please select a task first."); return null; }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        int id = (Integer) tableModel.getValueAt(modelRow, 0);
        for (Task t : currentUser.tasks) if (t.id == id) return t;
        return null;
    }

    private void openTaskDialog(Task edit) {
        JTextField title = field("Title"), deadline = field("yyyy-MM-dd"), tags = field("tags"), attach = field("attachment name");
        JTextArea desc = textArea(), notes = textArea(), subtasks = textArea();
        JComboBox<Priority> pr = new JComboBox<>(Priority.values()); JComboBox<Category> cat = new JComboBox<>(Category.values()); JComboBox<TaskStatus> st = new JComboBox<>(TaskStatus.values()); JCheckBox pinned = new JCheckBox("Pin this task");
        for (JComponent c: new JComponent[]{pr,cat,st,pinned}) { c.setFont(bodyFont); c.setBackground(input()); c.setForeground(text()); }
        if (edit != null) {
            title.setText(edit.title); desc.setText(edit.description); pr.setSelectedItem(edit.priority); cat.setSelectedItem(edit.category); deadline.setText(edit.deadline); notes.setText(edit.notes); st.setSelectedItem(edit.status); tags.setText(edit.tagsAsText()); attach.setText(edit.attachmentName); pinned.setSelected(edit.pinned);
            StringBuilder rawSub = new StringBuilder(); for (SubTask sub: edit.subtasks) rawSub.append(sub.text).append("\n"); subtasks.setText(rawSub.toString());
        } else { deadline.setText(LocalDate.now().plusDays(1).toString()); }
        JPanel form = new JPanel(new GridBagLayout()); form.setBackground(panel()); GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(5,5,5,5); g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
        int r = 0; addRow(form,g,r++,"Title",title); addRow(form,g,r++,"Description",new JScrollPane(desc)); addRow(form,g,r++,"Priority",pr); addRow(form,g,r++,"Category",cat); addRow(form,g,r++,"Deadline",deadline); addRow(form,g,r++,"Status",st); addRow(form,g,r++,"Tags",tags); addRow(form,g,r++,"Subtasks (one per line)",new JScrollPane(subtasks)); addRow(form,g,r++,"Notes",new JScrollPane(notes)); addRow(form,g,r++,"Attachment Name",attach); addRow(form,g,r++,"Pinned",pinned);
        int ok = JOptionPane.showConfirmDialog(this, form, edit==null?"Add New Task":"Edit Task", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) {
            if (title.getText().trim().isEmpty()) { error("Title is required."); return; }
            if (!validDate(deadline.getText().trim())) { error("Date must be in yyyy-MM-dd format."); return; }
            if (edit == null) currentUser.tasks.add(new Task(currentUser.taskIdCounter++, title.getText().trim(), desc.getText().trim(), (Priority)pr.getSelectedItem(), (Category)cat.getSelectedItem(), deadline.getText().trim(), notes.getText().trim(), (TaskStatus)st.getSelectedItem(), tags.getText(), subtasks.getText(), attach.getText().trim(), pinned.isSelected()));
            else {
                edit.title=title.getText().trim(); edit.description=desc.getText().trim(); edit.priority=(Priority)pr.getSelectedItem(); edit.category=(Category)cat.getSelectedItem(); edit.deadline=deadline.getText().trim(); edit.notes=notes.getText().trim(); edit.status=(TaskStatus)st.getSelectedItem(); edit.setTags(tags.getText()); edit.setSubtasks(subtasks.getText()); edit.attachmentName=attach.getText().trim(); edit.pinned=pinned.isSelected();
            }
            saveData(); refreshAll();
        }
    }

    private void addRow(JPanel p, GridBagConstraints g, int r, String name, Component comp) {
        g.gridx=0; g.gridy=r; g.weightx=.25; p.add(label(name, smallFont, text()), g); g.gridx=1; g.weightx=.75; p.add(comp, g);
    }

    private void viewSelected() {
        Task t = selectedTask(); if (t == null) return;
        JTextArea a = new JTextArea(
            "ID: " + t.id + "\nTitle: " + t.title + "\nDescription: " + t.description + "\nPriority: " + t.priority + "\nCategory: " + t.category +
            "\nDeadline: " + t.deadline + "\nStatus: " + t.status + "\nTags: " + t.tagsAsText() + "\nPinned: " + (t.pinned?"Yes":"No") +
            "\nAttachment: " + safeText(t.attachmentName) + "\nCreated At: " + t.createdAt + "\n\nSubtasks:\n" + t.subtasksAsText() + "\n\nNotes:\n" + safeText(t.notes));
        a.setEditable(false); a.setFont(bodyFont); a.setBackground(panel()); a.setForeground(text());
        JOptionPane.showMessageDialog(this, new JScrollPane(a), "Task Details", JOptionPane.INFORMATION_MESSAGE);
    }
    private String safeText(String s){ return s == null || s.trim().isEmpty()?"N/A":s; }
    private void editSelected(){ Task t = selectedTask(); if (t != null) openTaskDialog(t); }
    private void setStatusSelected(TaskStatus st){ Task t = selectedTask(); if(t!=null){ t.status=st; saveData(); refreshAll(); } }
    private void pinSelected(){ Task t = selectedTask(); if(t!=null){ t.pinned=!t.pinned; saveData(); refreshAll(); } }
    private void archiveSelected(boolean archive){ Task t = selectedTask(); if(t!=null){ t.archived=archive; saveData(); refreshAll(); } }
    private void restoreSelected(){ Task t = selectedTask(); if(t!=null){ t.deleted=false; saveData(); refreshAll(); } }
    private void deleteSelected(boolean permanent) {
        Task t = selectedTask(); if (t == null) return;
        int c = JOptionPane.showConfirmDialog(this, permanent?"Delete permanently?":"Move selected task to trash?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c == JOptionPane.YES_OPTION) { if (permanent) currentUser.tasks.remove(t); else t.deleted=true; saveData(); refreshAll(); }
    }

    // ───────────────────────── CALENDAR / REPORTS / PROFILE ─────────────────────────
    private JPanel buildCalendarPage() {
        JPanel p = roundedPanel(card(), 18); p.setLayout(new BorderLayout(10,10));
        JTextArea a = new JTextArea(calendarText()); a.setFont(new Font("Consolas", Font.PLAIN, 14)); a.setEditable(false); a.setBackground(card()); a.setForeground(text());
        p.add(label("Monthly Deadline Calendar", hFont, text()), BorderLayout.NORTH); p.add(new JScrollPane(a), BorderLayout.CENTER); return p;
    }
    private String calendarText() {
        YearMonth ym = YearMonth.now(); StringBuilder sb = new StringBuilder(); sb.append(ym.getMonth()).append(" ").append(ym.getYear()).append("\n"); sb.append("────────────────────────────────────────\n");
        for (int d=1; d<=ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d); ArrayList<String> tasks = new ArrayList<>();
            for (Task t: currentUser.tasks) if (!t.deleted && !t.archived && date.toString().equals(t.deadline)) tasks.add((t.pinned?"📌 ":"") + t.title + " [" + t.status + "]");
            if (!tasks.isEmpty()) sb.append(String.format("%02d %s : %s\n", d, date.getDayOfWeek().toString().substring(0,3), String.join(", ", tasks)));
        }
        return sb.toString();
    }
    private JPanel buildReportsPage() {
        JPanel p = new JPanel(new BorderLayout(0,10)); p.setBackground(bg());
        JTextArea report = new JTextArea(weeklyReportText() + "\n\n" + fullReportText()); report.setFont(bodyFont); report.setBackground(card()); report.setForeground(text()); report.setEditable(false); report.setBorder(new EmptyBorder(14,14,14,14));
        JButton export = button("Export Report TXT", PURPLE); export.addActionListener(e -> exportReport(report.getText()));
        p.add(new JScrollPane(report), BorderLayout.CENTER); p.add(export, BorderLayout.SOUTH); return p;
    }
    private String weeklyReportText() {
        LocalDate start = LocalDate.now().minusDays(6); int total=0, done=0, over=0;
        for (Task t: currentUser==null?new ArrayList<Task>():currentUser.tasks) if(!t.deleted && !t.archived) {
            try { LocalDate c=LocalDate.parse(t.createdAt); if(!c.isBefore(start)){ total++; if(t.isCompleted()) done++; if(t.isOverdue()) over++; } } catch(Exception ignored) {}
        }
        int percent = total==0?0:(done*100/total);
        return "This Week Summary\nTotal Created: " + total + "\nCompleted: " + done + "\nOverdue: " + over + "\nProductivity: " + percent + "%";
    }
    private String fullReportText() {
        int total=0,pending=0,progress=0,done=0,cancel=0,over=0,arch=0,trash=0;
        for(Task t: currentUser.tasks){ if(t.deleted){trash++; continue;} if(t.archived){arch++; continue;} total++; if(t.status==TaskStatus.PENDING)pending++; if(t.status==TaskStatus.IN_PROGRESS)progress++; if(t.status==TaskStatus.COMPLETED)done++; if(t.status==TaskStatus.CANCELLED)cancel++; if(t.isOverdue())over++; }
        return "Overall Task Report\nTotal Active Tasks: " + total + "\nPending: " + pending + "\nIn Progress: " + progress + "\nCompleted: " + done + "\nCancelled: " + cancel + "\nOverdue: " + over + "\nArchived: " + arch + "\nTrash: " + trash;
    }
    private JPanel buildProfilePage() {
        JPanel p = roundedPanel(card(), 20); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JTextField full = field("Full Name"), email = field("Email"); full.setText(currentUser.fullName); email.setText(currentUser.email);
        JPasswordField oldPw = passwordField(), newPw = passwordField();
        JButton update = button("Update Profile", BLUE); JButton pass = button("Change Password", GREEN);
        update.addActionListener(e->{ if(!validEmail(email.getText().trim())){error("Invalid email.");return;} currentUser.fullName=full.getText().trim(); currentUser.email=email.getText().trim(); saveData(); success("Profile updated."); rebuildAll(); });
        pass.addActionListener(e->{ String op=new String(oldPw.getPassword()), np=new String(newPw.getPassword()); if(!currentUser.passwordHash.equals(hash(op))) error("Old password is wrong."); else if(np.length()<6) error("New password must be at least 6 characters."); else {currentUser.passwordHash=hash(np); saveData(); success("Password changed."); oldPw.setText(""); newPw.setText("");} });
        p.add(label("Account Information", hFont, text())); p.add(Box.createVerticalStrut(12)); addFormField(p,"Full Name",full); addFormField(p,"Email",email); p.add(update); p.add(Box.createVerticalStrut(24));
        p.add(label("Change Password", hFont, text())); p.add(Box.createVerticalStrut(10)); addFormField(p,"Old Password",oldPw); addFormField(p,"New Password",newPw); p.add(pass);
        return p;
    }

    // ───────────────────────── EXPORT / BACKUP / STATS ─────────────────────────
    private void refreshAll() { updateStats(); if (tableModel != null) refreshTable(currentPage.equals("Archive"), currentPage.equals("Trash")); }
    private void updateStats() {
        if (currentUser == null) return;
        int total=0,pending=0,done=0,over=0;
        for(Task t: currentUser.tasks) if(!t.deleted && !t.archived){ total++; if(t.status==TaskStatus.PENDING || t.status==TaskStatus.IN_PROGRESS) pending++; if(t.isCompleted()) done++; if(t.isOverdue()) over++; }
        if(totalCard!=null) totalCard.setText(String.valueOf(total)); if(pendingCard!=null) pendingCard.setText(String.valueOf(pending)); if(doneCard!=null) doneCard.setText(String.valueOf(done)); if(overdueCard!=null) overdueCard.setText(String.valueOf(over));
        if(progressBar!=null){ int pct=total==0?0:(done*100/total); progressBar.setValue(pct); progressBar.setString(pct + "% Completed"); }
    }
    private void showReminders() {
        ArrayList<String> due = new ArrayList<>(), over = new ArrayList<>();
        for(Task t: currentUser.tasks) if(!t.deleted && !t.archived){ if(t.isDueToday()) due.add(t.title); if(t.isOverdue()) over.add(t.title); }
        if(!due.isEmpty() || !over.isEmpty()) JOptionPane.showMessageDialog(this, "Due Today: " + due.size() + "\n" + String.join("\n", due) + "\n\nOverdue: " + over.size() + "\n" + String.join("\n", over), "Task Reminder", JOptionPane.INFORMATION_MESSAGE);
    }
    private void exportCSV() {
        JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File("tasks_export.csv"));
        if(fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) try(PrintWriter pw=new PrintWriter(fc.getSelectedFile())){
            pw.println("ID,Title,Priority,Category,Deadline,Status,Tags,Archived,Deleted");
            for(Task t: currentUser.tasks) pw.printf("%d,\"%s\",%s,%s,%s,%s,\"%s\",%s,%s%n",t.id,t.title.replace("\"","\"\""),t.priority,t.category,t.deadline,t.status,t.tagsAsText().replace("\"","\"\""),t.archived,t.deleted);
            success("CSV exported successfully.");
        } catch(Exception e){ error("Export failed: " + e.getMessage()); }
    }
    private void exportReport(String text) {
        JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File("weekly_report.txt"));
        if(fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) try { Files.write(fc.getSelectedFile().toPath(), text.getBytes()); success("Report exported."); } catch(Exception e){ error("Export failed."); }
    }
    private void backupData() {
        saveData(); JFileChooser fc = new JFileChooser(); fc.setSelectedFile(new File("smart_task_backup.ser"));
        if(fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) try { Files.copy(Paths.get(DATA_FILE), fc.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING); success("Backup created."); } catch(Exception e){ error("Backup failed: " + e.getMessage()); }
    }
    private void restoreData() {
        JFileChooser fc = new JFileChooser();
        if(fc.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) try { Files.copy(fc.getSelectedFile().toPath(), Paths.get(DATA_FILE), StandardCopyOption.REPLACE_EXISTING); loadData(); currentUser=null; success("Data restored. Please login again."); rebuildAll(); } catch(Exception e){ error("Restore failed: " + e.getMessage()); }
    }
}
