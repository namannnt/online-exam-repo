package com.naman;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.Timer;

class DatabaseConnectionException extends Exception {
    public DatabaseConnectionException(String message) {
        super(message);
    }
}

class QuestionNotFoundException extends Exception {
    public QuestionNotFoundException(String message) {
        super(message);
    }
}

class InvalidAnswerException extends Exception {
    public InvalidAnswerException(String message) {
        super(message);
    }
}

class OnlineTest extends JFrame implements ActionListener {
    JLabel timerLabel, questionLabel; // Separate labels for timer and questions
    JRadioButton jb[] = new JRadioButton[5];
    JButton b1, b2, b3;
    ButtonGroup bg;
    int count = 0, attempted = 0, current = -1;
    long StartTime, EndTime, seconds, minutes, flag = 0;
    int a;
    Timer timer;
    int timeRemaining = 300; // 5 minutes in seconds

    OnlineTest(String s) {
        super(s);
        timerLabel = new JLabel();
        questionLabel = new JLabel();
        add(timerLabel);
        add(questionLabel);
        bg = new ButtonGroup();
        for (int i = 0; i < 5; i++) {
            jb[i] = new JRadioButton();
            add(jb[i]);
            bg.add(jb[i]);
        }
        b1 = new JButton("Start");
        b1.addActionListener(this);
        add(b1);
        welcome();
        b2 = new JButton("Previous");
        b3 = new JButton("Result");
        b2.addActionListener(this);
        b3.addActionListener(this);
        add(b2);
        add(b3);

        // Position the labels
        timerLabel.setBounds(30, 10, 450, 20); // Timer label at the top
        questionLabel.setBounds(30, 40, 550, 20); // Question label below the timer

        if (current != -1) {
            jb[0].setBounds(50, 80, 450, 20);
            jb[1].setBounds(50, 110, 450, 20);
            jb[2].setBounds(50, 140, 450, 20);
            jb[3].setBounds(50, 170, 450, 20);
        }
        b1.setBounds(100, 240, 100, 30);
        b2.setBounds(270, 240, 100, 30);
        b3.setBounds(400, 240, 100, 30);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);
        setLocation(250, 100);
        setVisible(true);
        setSize(600, 350);

        // Initialize the timer
        timer = new Timer(1000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                timeRemaining--;
                updateTimerLabel();
                if (timeRemaining <= 0) {
                    timer.stop();
                    endExam();
                }
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/qa", "root", "Naman2006yo");
            Statement stmt = con.createStatement();
            if (e.getSource() == b1 && current == 9) {
                adduserans();
                JOptionPane.showMessageDialog(this, "No more questions. Please go back or see result.");
            } else if (e.getSource() == b1) {
                if (current == -1) {
                    StartTime = System.currentTimeMillis();
                    b1.setText("Next");
                    timer.start(); // Start the timer when the exam begins
                } else {
                    adduserans();
                }
                current++;
                setnext();
            } else if (e.getSource() == b2 && current == 0) {
                adduserans();
                JOptionPane.showMessageDialog(this, "No previous questions.");
            } else if (e.getSource() == b2) {
                current--;
                adduserans();
                setnext();
            } else if (e.getActionCommand().equals("Result")) {
                EndTime = System.currentTimeMillis();
                EndTime -= StartTime;
                EndTime /= 1000;
                if (EndTime >= 60) {
                    seconds = EndTime % 60;
                    EndTime /= 60;
                    flag = 1;
                    if (EndTime >= 60) {
                        flag = 2;
                        minutes = EndTime % 60;
                        EndTime /= 60;
                    }
                }
                current++;
                check();
                String timeMsg;
                if (flag == 0) {
                    timeMsg = "Time taken: " + EndTime + " seconds";
                } else if (flag == 1) {
                    timeMsg = "Time taken: " + EndTime + " minutes " + seconds + " seconds";
                } else {
                    timeMsg = "Time taken: " + EndTime + " hours " + minutes + " minutes " + seconds + " seconds";
                }
                a = JOptionPane.showConfirmDialog(this, "Attempted: " + attempted + "/10\n" + timeMsg +
                        "\nScore: " + count + "/10\nPercentage: " + (count * 10) + "%\nView answer key?");
                if (a == JOptionPane.YES_OPTION) {
                    showAnswerKey();
                } else {
                    stmt.executeUpdate("DELETE FROM stuua");
                    stmt.executeUpdate("DELETE FROM stuqao");
                    System.exit(0);
                }
            }
            con.close();
        } catch (java.sql.SQLException ex) {
            try {
                throw new DatabaseConnectionException("Failed to connect to the database: " + ex.getMessage());
            } catch (DatabaseConnectionException ex1) {
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    void updateTimerLabel() {
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timerLabel.setText(String.format("Time Remaining: %02d:%02d", minutes, seconds)); // Update timerLabel
    }

    void endExam() {
        JOptionPane.showMessageDialog(this, "Time's up! The exam has ended.");
        EndTime = System.currentTimeMillis();
        EndTime -= StartTime;
        EndTime /= 1000;
        check();
        JOptionPane.showMessageDialog(this, "Score: " + count + "/10\nPercentage: " + (count * 10) + "%");
        System.exit(0);
    }

    void welcome() {
        timerLabel.setText("Welcome to the Online Exam. Click Start to begin."); // Update timerLabel
    }

    void setnext() throws DatabaseConnectionException {
        jb[4].setSelected(true);
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/qa", "root", "Naman2006yo");
            Statement stmt = con.createStatement();
            String sql = "SELECT * FROM stuqao WHERE qno=" + (current + 1);
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                questionLabel.setText("Q." + (current + 1) + " " + rs.getString("question")); // Update questionLabel
                jb[0].setText(rs.getString("option1"));
                jb[1].setText(rs.getString("option2"));
                jb[2].setText(rs.getString("option3"));
                jb[3].setText(rs.getString("option4"));
            } else {
                throw new QuestionNotFoundException("Question not found for question number: " + (current + 1));
            }
            for (int i = 0, y = 80; i < 4; i++, y += 30) {
                jb[i].setBounds(50, y, 500, 20);
            }
            con.close();
        } catch (java.sql.SQLException ex) {
            throw new DatabaseConnectionException("Failed to connect to the database: " + ex.getMessage());
        } catch (QuestionNotFoundException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading question: " + e.getMessage());
        }
    }

    void adduserans() throws DatabaseConnectionException {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/qa", "root", "Naman2006yo");
            Statement stmt = con.createStatement();
            boolean answerSelected = false;
            for (int i = 0; i < 4; i++) {
                if (jb[i].isSelected()) {
                    String sql = "UPDATE stuua SET userans='" + jb[i].getText() + "' WHERE qno=" + (current + 1);
                    stmt.executeUpdate(sql);
                    answerSelected = true;
                    break;
                }
            }
            if (!answerSelected) {
                throw new InvalidAnswerException("No answer selected for question number: " + (current + 1));
            }
            con.close();
        } catch (java.sql.SQLException ex) {
            throw new DatabaseConnectionException("Failed to connect to the database: " + ex.getMessage());
        } catch (InvalidAnswerException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (Exception e) {
            System.out.println("adduserans: " + e);
        }
    }

    void check() {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/qa", "root", "Naman2006yo");
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM stuua");
            while (rs.next()) {
                String userAns = rs.getString("userans");
                String correctAns = rs.getString("correctans");
                if (userAns != null && !userAns.isEmpty()) {
                    attempted++;
                    if (userAns.equals(correctAns)) {
                        count++;
                    }
                }
            }
            con.close();
        } catch (Exception e) {
            System.out.println("check: " + e);
        }
    }

    void showAnswerKey() {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/qa", "root", "Naman2006yo");
            Statement stmt = con.createStatement();
            StringBuilder answerKey = new StringBuilder("Answer Key:\nQ.No\tYour Answer\tCorrect Answer\n");
            ResultSet rs = stmt.executeQuery("SELECT * FROM stuua ORDER BY qno");
            while (rs.next()) {
                int qno = rs.getInt("qno");
                String userAns = rs.getString("userans");
                String correctAns = rs.getString("correctans");
                answerKey.append(qno).append("\t")
                        .append(userAns.isEmpty() ? "Unanswered" : userAns).append("\t\t")
                        .append(correctAns).append("\n");
            }
            JOptionPane.showMessageDialog(this, answerKey.toString());
            stmt.executeUpdate("DELETE FROM stuua");
            stmt.executeUpdate("DELETE FROM stuqao");
            System.exit(0);
            con.close();
        } catch (Exception e) {
            System.out.println("showAnswerKey: " + e);
        }
    }

    static void initializeDatabase() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/", "root", "Naman2006yo");
            Statement stmt = con.createStatement();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS qa");
            con.close();

            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/qa", "root", "Naman2006yo");
            stmt = con.createStatement();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS qao (qno INT PRIMARY KEY, question VARCHAR(255), option1 VARCHAR(255), option2 VARCHAR(255), option3 VARCHAR(255), option4 VARCHAR(255))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ua (qno INT PRIMARY KEY, userans VARCHAR(255), correctans VARCHAR(255))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS stuqao (qno INT PRIMARY KEY, question VARCHAR(255), option1 VARCHAR(255), option2 VARCHAR(255), option3 VARCHAR(255), option4 VARCHAR(255))");
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS stuua (qno INT PRIMARY KEY, userans VARCHAR(255), correctans VARCHAR(255))");
            con.close();
        } catch (Exception e) {
            System.out.println("initializeDatabase: " + e);
        }
    }

    static void populateQuestions() {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/qa", "root", "Naman2006yo");
            Statement stmt = con.createStatement();
            stmt.executeUpdate("INSERT IGNORE INTO qao VALUES " +
                    "(1, 'What language is this exam written in?', 'C', 'C++', 'Java', 'Python'), " +
                    "(2, 'Which keyword defines a class in Java?', 'class', 'interface', 'object', 'type'), " +
                    "(3, 'Parent class of all Java classes?', 'Object', 'Main', 'Super', 'Parent'), " +
                    "(4, 'Java loop structure?', 'for', 'loop', 'repeat', 'cycle'), " +
                    "(5, 'Default value of int?', '0', '1', 'null', 'undefined'), " +
                    "(6, 'Data type for true/false?', 'boolean', 'int', 'Boolean', 'bool'), " +
                    "(7, 'Size of char in Java?', '2 bytes', '1 byte', '4 bytes', '8 bytes'), " +
                    "(8, 'Collection allowing duplicates?', 'List', 'Set', 'Map', 'Queue'), " +
                    "(9, 'What is Java?', 'Programming language', 'Coffee', 'Island', 'All'), " +
                    "(10, 'Method called on start?', 'main', 'init', 'start', 'run')");
            stmt.executeUpdate("INSERT IGNORE INTO ua VALUES " +
                    "(1, '', 'Java'), (2, '', 'class'), (3, '', 'Object'), (4, '', 'for'), " +
                    "(5, '', '0'), (6, '', 'boolean'), (7, '', '2 bytes'), (8, '', 'List'), " +
                    "(9, '', 'All'), (10, '', 'main')");
            con.close();
        } catch (Exception e) {
            System.out.println("populateQuestions: " + e);
        }
    }

    static void pickRandomQuestions() {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/qa", "root", "Naman2006yo");
            Statement stmt = con.createStatement();
            stmt.executeUpdate("DELETE FROM stuqao");
            stmt.executeUpdate("DELETE FROM stuua");
            int[] selected = new int[11];
            int count = 0;
            while (count < 10) {
                int rand = 1 + (int) (Math.random() * 10);
                if (selected[rand] == 0) {
                    selected[rand] = 1;
                    count++;
                }
            }
            count = 0;
            for (int i = 1; i <= 10; i++) {
                if (selected[i] == 1) {
                    count++;
                    stmt.executeUpdate("INSERT INTO stuqao SELECT " + count + ", question, option1, option2, option3, option4 FROM qao WHERE qno=" + i);
                    stmt.executeUpdate("INSERT INTO stuua SELECT " + count + ", '', correctans FROM ua WHERE qno=" + i);
                }
            }
            con.close();
        } catch (Exception e) {
            System.out.println("pickRandomQuestions: " + e);
        }
    }

    public static void main(String s[]) {
        initializeDatabase();
        populateQuestions();
        pickRandomQuestions();
        new OnlineTest("Online Exam System");
    }
}