package com.era.cloud.client;

import com.era.cloud.common.Request;
import com.era.cloud.common.UploadFile;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.Socket;

import java.util.concurrent.CountDownLatch;

public class ClientSerial extends JFrame {

    private Socket socket;
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;

    private String rootDir = "client/clientDir/";
    private JTextField textField;
    private DefaultListModel<String> listOnServerModel = new DefaultListModel<>();

    private boolean isFile = false;  // true - если файл

    private ClientSerial() {
        CountDownLatch cdl = new CountDownLatch(1);
        connect();
        GUI();
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientSerial();
            }
        });
    }

        private void connect() {
//        CountDownLatch cdl = new CountDownLatch(1); // ?
            try {
                socket = new Socket("localhost", 8189);
                out = new ObjectEncoderOutputStream(socket.getOutputStream());
                in = new ObjectDecoderInputStream(socket.getInputStream(), 100 * 1024 * 1024);
                System.out.println("Клиент подключился");
                getFileListFromServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
// Слушатель для кнопки Отправить
    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Отправить")) {
                String sendObj = textField.getText();
                if (!sendObj.equals("")) {
                    if (isFile) {
                        writeFile(sendObj);
                        readMessFromServer();
                        getFileListFromServer();
                    }
                    else {
                        writeMess(sendObj);
                        readMessFromServer();
                    }
                }
            }
        }
    }

    private void GUI() {
        setTitle("Клиент");
        setSize(500, 400);
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

        DefaultListModel<String> clientListModel = new DefaultListModel<>();
            File dir = new File(rootDir);
            String[] files = dir.list();
            if (files != null) {
                for (String s : files)
                    clientListModel.addElement(s);
            }
        JList<String> clientList = new JList<>(clientListModel);
        clientList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (clientList.getSelectedIndex() >= 0) {
                    textField.setText(clientList.getSelectedValue());
                    isFile = true; // на отправку будет сформирован объект файл
                }
            }
        });
        JList<String> listOnServer = new JList<>(listOnServerModel);
        JScrollPane paneL = new JScrollPane(clientList);
        JScrollPane paneR = new JScrollPane(listOnServer);
        JPanel paneForList = new JPanel();
        paneForList.setLayout(new GridLayout(1,2));
        paneForList.add(paneL);
        paneForList.add(paneR);
        c.add(paneForList, BorderLayout.CENTER);
        JPanel p3 = new JPanel();
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        JLabel label = new JLabel("File/Mes:");
        textField = new JTextField(15);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.isFocusOwner())
                {textField.setText(""); // очистили текстовое поле
                    isFile = false;}  // теперь там не файл
            }
        });
        JButton button = new JButton("Отправить");
        button.addActionListener(new SendButtonListener());
        p3.add(label);
        p3.add(textField);
        p3.add(button);
        c.add(p3, BorderLayout.SOUTH);
        WindowListener wndCloser = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
                System.exit(0);
            }
        };
        addWindowListener(wndCloser);
        setVisible(true);
    }

//получение списока файлов находящихся на сервере
    private void getFileListFromServer() {
        String commandGetList = "List!";
        Request mess =  new Request(); // объект для отправки
        mess.setCommand(commandGetList);
        try {
            out.writeObject(mess);
            out.flush();
            System.out.println("Клиент отправил команду: " + commandGetList);
        } catch (IOException ex) {ex.printStackTrace();}

        listOnServerModel.clear();
        try {
            Object obj = in.readObject();
            if (obj instanceof Request) {
                if (((Request) obj).isMessage()) {
                    String files = ((Request) obj).getMessage();
                    System.out.println(files);
                    String[] filesArr = files.split("\n");
                    for (String s: filesArr) {
                        listOnServerModel.addElement(s);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException ex){ex.printStackTrace();}
    }

    //получение ответа от сервера
    private void readMessFromServer() {
        boolean isRead = false;
        while (!isRead) { // пока ответ не получен
            try {
                Object obj = in.readObject();
                if (obj instanceof Request) {
                    if (((Request) obj).isMessage()) {
                        System.out.println(((Request) obj).getMessage());
                        isRead = true;
                    }
                }
            } catch (IOException | ClassNotFoundException  ex){ex.printStackTrace();}

        }
    }
    // передача файла
    private void writeFile(String fileName){
        Request mess = new Request();
        File file = new File(rootDir + fileName);
        try {
            mess.setFile(file);
            out.writeObject(mess);
            out.flush();
        } catch (IOException ex) {ex.printStackTrace();}
        System.out.println();
        System.out.println("Клиент отправил файл: " + fileName);
    }

    // передача сообщения
    private void writeMess(String message) {
        Request mess =  new Request();
        mess.setMessage(message);
        try {
            out.writeObject(mess);
            out.flush();
            System.out.println("Клиент отправил сообщение: " + message);
        } catch (IOException ex) {ex.printStackTrace();}
    }
    // закрытие потоков
    private void close(){
        try {
            in.close();
        } catch (IOException ex) {ex.printStackTrace();
            System.out.println("Проблема с закрытием входного потока");}
        try {
            out.close();
        } catch (IOException ex) {ex.printStackTrace();
            System.out.println("Проблема с закрытием выходного потока");}

        try {
            socket.close();
        } catch (IOException ex) {ex.printStackTrace();
            System.out.println("Проблема с закрытием сокета");}

    }
}
