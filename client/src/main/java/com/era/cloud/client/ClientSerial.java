package com.era.cloud.client;

import com.era.cloud.common.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import static com.era.cloud.common.CommandMessage.CMD_MSG_FILES_LIST;
import static com.era.cloud.common.CommandMessage.CMD_MSG_FILE_DOWNLOAD;

public class ClientSerial extends JFrame {

    private final int MAX_SIZE = 1024*1024*100;
    private Socket socket;
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;

    private String clientDirectory = "client/clientDir/";
    private JTextField textField;
    private DefaultListModel<String> listOnServerModel = new DefaultListModel<>();
    private DefaultListModel<String> clientListModel;

    private ArrayBlockingQueue<Task> requests = new ArrayBlockingQueue<>(5);
    private boolean isFile = false;  // true - если файл
    private JButton buttonEnt, buttonAUth, button1, button2, button3;
    private String messFromServer = "message";

    private ClientSerial() {
        connect(); // подключение к серверу, создание потоков ввода/вывода
        new Thread(new Operation()).start(); // поток для выполнения задач
        GUI(); // графический интерфейс

    }

    public static void main(String[] args) throws Exception { // главный поток
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientSerial();
            }
        });
    }

    class Operation implements Runnable { //класс для выполнения задач

        @Override
        public void run() {
            while (true){
                try {
                    Task task = requests.take();
                    task.doing();
                } catch (InterruptedException ex) {ex.printStackTrace();}
            }
        }
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream(), 100 * 1024 * 1024);
            System.out.println("Клиент подключился");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//============================
    private void GUI() {
        setTitle("Клиент");
        setSize(500, 400);
        setLocation(600, 100);
        setResizable(false);

        Container c = getContentPane();
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout());
        buttonEnt = new JButton("Войти");
        buttonEnt.addActionListener(new EnterButtonListener());

        buttonAUth = new JButton("Регистрация");
        buttonAUth.addActionListener(new EnterButtonListener());

        p1.add(buttonEnt);
        p1.add(buttonAUth);
        c.add(p1, BorderLayout.NORTH);

        clientListModel = new DefaultListModel<>();
        JList<String> clientList = new JList<>(clientListModel);
        clientList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!clientList.isSelectionEmpty()) { // если "выбор" не пуст
                    textField.setText(clientList.getSelectedValue());
                    isFile = true; // на отправку будет сформирован объект файл
                    button2.setEnabled(false);
                }
            }
        });
        clientList.addFocusListener(new ClientListFocusListener(clientList));

        JList<String> listOnServer = new JList<>(listOnServerModel);
        listOnServer.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!listOnServer.isSelectionEmpty()) {
                    textField.setText(listOnServer.getSelectedValue());
                    isFile = true; // на отправку будет сформирован объект файл
                }
            }
        });
        listOnServer.addFocusListener(new ServerListFocusListener(listOnServer));
        JScrollPane paneL = new JScrollPane(clientList);
        JScrollPane paneR = new JScrollPane(listOnServer);
        JPanel paneForList = new JPanel();
        paneForList.setLayout(new GridLayout(1,2));
        paneForList.add(paneL);
        paneForList.add(paneR);
        c.add(paneForList, BorderLayout.CENTER);
        JPanel p3 = new JPanel();
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        JLabel label = new JLabel(" File: ");
        textField = new JTextField(8);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.isFocusOwner())
                {textField.setText(""); // очистили текстовое поле
                    isFile = false;}  // теперь там не файл
            }
        });
        button1 = new JButton("Отправить");
        button1.setEnabled(false);
        button1.addActionListener(new SendButtonListener());
        button2 = new JButton("Скачать");
        button2.setEnabled(false);
        button2.addActionListener(new DownLoadButtonListener());
        button3 = new JButton("Удалить");
        button3.setEnabled(false);
        p3.add(label);
        p3.add(textField);
        p3.add(button1);
        p3.add(button2);
        p3.add(button3);
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
    //============================== Слушатели ============

    // Слушатель для кнопки "Отправить"
    private class SendButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Отправить")) {
                String sendObj = textField.getText();
                if (!sendObj.equals("")) {
                    if (isFile) {
                        try {
                            requests.put(new SendFileTask(clientDirectory + sendObj, out)); // передача файла на сервер
                            requests.put(new GetFileListCommandTask(out)); // запрос на список файлов
                            updateServerList();  // обновляем список
//                            requests.put(new UpdateServerListTask(in, listOnServerModel));
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } else JOptionPane.showMessageDialog(null, "Выберите файл из списка!");
                }
                textField.setText("");
            }
        }
    }

// Слушатель для "Скачать"
    private class DownLoadButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String obj = textField.getText();
            if (!obj.equals("")) {
                if (isFile) { // если obj элемент списка, т.е название файла
                    CommandMessage com = new CommandMessage(CMD_MSG_FILE_DOWNLOAD, obj); // командана - скачивание файла с сервера
                    try {
                        requests.put(new DownloadFileFromServerCommandTask(com, out)); // задача на отправку команды
                    } catch (InterruptedException ex) {ex.printStackTrace();}
                } else JOptionPane.showMessageDialog(null, "Выберите файл из списка!");
            }
            textField.setText(""); // очистили поле
            try {
                requests.put(new WriteFileToClientDirectoryTask(in, clientDirectory));
                requests.put(new UpdateClientListTask(clientListModel, clientDirectory));

            } catch (InterruptedException ex) {ex.printStackTrace();}
        }
    }
    //===============================================================================================
    //слушатель для "Войти" и "Регистрация" главного окна
    private class EnterButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Войти")) {
                new AuthWindow("Авторизация");
            }
            if (e.getActionCommand().equals("Регистрация")) {
                new AuthWindow("Регистрация");
            }
        }
    }
// Слушатель клиентского списка
    private class ClientListFocusListener extends FocusAdapter {
        JList<String> clientList;

        ClientListFocusListener(JList<String> clientList) {
            this.clientList = clientList;
        }
        @Override
        public void focusGained(FocusEvent e) {
            button2.setEnabled(false);
            button1.setEnabled(true);
        }

        @Override
        public void focusLost(FocusEvent e) {
            clientList.clearSelection();
        }
    }
    // Слушатель серверного списка
    private class ServerListFocusListener extends FocusAdapter {
        JList<String> listOnServer;

        ServerListFocusListener(JList<String> listOnServer) {
            this.listOnServer = listOnServer;
        }
        @Override
        public void focusGained(FocusEvent e) {
            button1.setEnabled(false);
            button2.setEnabled(true);
        }

        @Override
        public void focusLost(FocusEvent e) {
            listOnServer.clearSelection();
        }
    }


// Активация, деактивация кнопок
    private void setEnabledAllButton() {
        buttonEnt.setEnabled(false);
        buttonAUth.setEnabled(false);
        button1.setEnabled(true);
        button2.setEnabled(true);
        button3.setEnabled(true);
    }

//=========================================================================
// вспомогательное окно для авторизации, регистрации нового пользователя
    class AuthWindow extends JFrame{
        private JTextField login, password;

        AuthWindow authWindow; // держит ссылку на фрейм
        AuthWindow(String title){
            super(title);
            JPanel panelButton = new JPanel();
            panelButton.setLayout(new GridLayout(7,1));
            panelButton.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
            JLabel label_login = new JLabel("Введите логин:");
            JLabel label_pass = new JLabel("Введите пароль:");
            login = new JTextField(11);
            password = new JTextField(11);
            JButton btnEnter = new JButton();
            if (title.equals("Авторизация"))
                btnEnter.setText("Войти");
            else
                btnEnter.setText("Зарегистрироваться");
            panelButton.add(label_login);
            panelButton.add(login);
            panelButton.add(label_pass);
            panelButton.add(password);

            panelButton.add(btnEnter);
            btnEnter.addActionListener(new InnerEnterButtonListener());
            add(panelButton);
            setBounds(650, 250 , 400, 200);
            setResizable(false);
            authWindow = this; // ссылается на самого себя
            setVisible(true);
        }

// слушатель кнопки "Войти"/"Зарегистрироваться"
    private class InnerEnterButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String loginText = login.getText();
            String passText = password.getText();
            if (!loginText.equals("") && !passText.equals("")){
                LoginAndPasswordMessage loginAndPass = new LoginAndPasswordMessage(loginText, passText);
                if (e.getActionCommand().equals("Войти")){
                    loginAndPass.setTypeAUTH(); // указали, что для авторизации
                } else {
                    loginAndPass.setTypeREG(); // указали, что для регистрации
                }
                try {
                    requests.put(new AuthCommandTask(out, loginAndPass)); // отправили логин, пароль
                    readMessFromServer(); // дожидаемся ответ сервера
                    authWindow.dispose();
                    if (messFromServer.equals("OK")) {
                        requests.put(new GetFileListCommandTask(out)); // команда на получение списка файлов
                        updateServerList(); // обновляем серверный список
                        try { // обновляем клиентский список
                            requests.put(new UpdateClientListTask(clientListModel, clientDirectory));
                        } catch (InterruptedException ex) {ex.printStackTrace();}
//                      requests.put(new UpdateServerListTask(in, listOnServerModel)); // срабатывает 1 раз ?
                        setEnabledAllButton();
                    } else {
                        if (e.getActionCommand().equals("Войти")){
                            JOptionPane.showMessageDialog(null,
                                    "<html>Указанного пользователя нет в системе,<br> пожалуйста, зарегистрируйтесь!");
                        } else JOptionPane.showMessageDialog(null,
                                "<html>Введенный логин уже занят");
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
            else {
                JOptionPane.showMessageDialog(null, "Введите логин и пароль!",
                        "Внимание", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }
}

    //получение ответа от сервера
    private void readMessFromServer() {
        try {
            Object obj = in.readObject();
            if (obj instanceof SimpleMessage) {
                messFromServer = ((SimpleMessage)obj).getMessage();
                System.out.println(messFromServer);
            }
        } catch (IOException | ClassNotFoundException  ex){ex.printStackTrace();
            System.out.println("При чтении сообщения от сервера во время регистрации");}
    }


    private void updateServerList() {
        listOnServerModel.clear();
        try {
            Object obj = in.readObject();
            if (obj instanceof AbstractMessage) {
                String[] files = ((ServerListMessage) obj).getFilesList();
                if (files != null) {
                    for (String s: files) {
                        listOnServerModel.addElement(s);
                        System.out.println(s);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException ex){ex.printStackTrace();}
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

//=======================++++++


    String getMessFromServer() {
        return messFromServer;
    }

    ArrayBlockingQueue<Task> getRequests() {
        return requests;
    }
}
