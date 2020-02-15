package com.era.cloud.client;

import com.era.cloud.common.CloudPackage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;

public class Client extends JFrame {

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    Path path1 = Paths.get("fileFrom/5.png");
    Path path2 = Paths.get("fileFrom/2.txt");

    private CloudPackage pack;
    boolean bb = true;

    JTextField textField;

   public Client() {
       GUI();
       work();
   }

    private void work() {
        try{
            socket = new Socket("localhost", 8078);
            output = new DataOutputStream(socket.getOutputStream());
            input = new DataInputStream(socket.getInputStream());

            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    pack = new CloudPackage(input, output); //

                }
            });
            t.setDaemon(true);
            t.start();
            Thread t2 = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        java.util.Scanner scanner = new java.util.Scanner(System.in);
                        String str = scanner.nextLine();
                        if (!str.trim().isEmpty()) {
//                            try {
//                                pack = new CloudPackage(input, output);
//                                pack.writeCommand(str);
//                            } catch (IOException ex) {}
                        } else {
                            System.out.println("сокет закрыт");
                        }
                    }
                }
            });
            t2.setDaemon(true);
            t2.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
// https://github.com/netty/netty
    private class ButtonAct implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Отправить")) {
                String str = textField.getText();
                if (!str.equals("")) {
//                    pack.writeCommand(str);
                    try {
                        System.out.println(str + " наша строка");
//                        String[] arr = str.split("//");
                        Path p = Paths.get(str);
                        System.out.println(p.toString());
                        pack.sendFile(Paths.get(str));
                    } catch (IOException exp) {exp.printStackTrace();}


                }
//   fileFrom/5.png       fileFrom/5.png

            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client();
            }
        });
    }

    private void GUI() {
        setTitle("Клиент");
        setSize(400, 400);
        setLocation(600, 100);
        setResizable(false);

        Container c = getContentPane();
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout());
        JButton buttonEnt = new JButton("Войти");
        JButton buttonAUth = new JButton("Авторизоваться");

        buttonAUth.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        p1.add(buttonEnt);
        p1.add(buttonAUth);
        c.add(p1, BorderLayout.NORTH);
        JTextArea textArea = new JTextArea(5, 30);
        JScrollPane pane = new JScrollPane(textArea);
        c.add(pane, BorderLayout.CENTER);
        JPanel p3 = new JPanel();
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        JLabel label = new JLabel("File/Mes:");
        textField = new JTextField(15);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.isFocusOwner())
                {textField.setText("");}
            }
        });
        JButton button = new JButton("Отправить");
        button.addActionListener(new ButtonAct());
        p3.add(label);
        p3.add(textField);
        p3.add(button);
        c.add(p3, BorderLayout.SOUTH);
        WindowListener wndCloser = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                  input.close();
                } catch (IOException ex) {ex.printStackTrace();
                    System.out.println("oooo1");}
                try {
                    output.close();
                } catch (IOException ex) {ex.printStackTrace();
                    System.out.println("oooo2");}

                try {
                    socket.close();
                } catch (IOException ex) {ex.printStackTrace();
                    System.out.println("oooo3");}
                System.exit(0);
            }
        };
        addWindowListener(wndCloser);
        setVisible(true);
    }

}


