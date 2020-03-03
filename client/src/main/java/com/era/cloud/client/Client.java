package com.era.cloud.client;


import com.era.cloud.common.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

import java.util.ArrayList;
import java.util.concurrent.*;

import static com.era.cloud.common.CommandMessage.CMD_MSG_FILE_DOWNLOAD;
import static com.era.cloud.common.CommandMessage.CMD_MSG_SERVER_DELETE_FILE;

public class Client extends JFrame {

    private Socket socket;
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;

    private File[] roots = File.listRoots();
    private File clientFile = roots[0]; // диск С:
    private File parentClientFile = clientFile;
    private String serverSideFileName = ""; // пустая строка

    private JTextField textField;
    private JList<File> clientList; // клиентский список
    private DefaultListModel<String> listOnServerModel = new DefaultListModel<>();
    private DefaultListModel<File> clientListModel;

    private ArrayBlockingQueue<Task> requests = new ArrayBlockingQueue<>(10);
    private boolean isFile = false;  // true - если файл
    private JButton backButton, buttonEnt, buttonAUth, button1, button2, button3;

    private volatile String messFromServer = "message";
    private volatile ArrayList<String> fileList;

    private Client() {
        connect(); // подключение к серверу, создание потоков ввода/вывода
        new Thread(new Operation()).start(); // поток для выполнения задач
        GUI(); // графический интерфейс

    }

    public static void main(String[] args) throws Exception { // главный поток
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new Client();
            }
        });
    }

    class Operation implements Runnable { //класс для выполнения задач

        @Override
        public void run() {
            while (true) {
                try {
                    Task task = requests.take();
                    task.doing();
                    if (task instanceof AuthCommandTask) {
                        messFromServer = ((AuthCommandTask) task).serverAnswer();
                    }
                    if (task instanceof GetFileListCommandTask) {
                        fileList = ((GetFileListCommandTask) task).getFiles();
                    }
//                    if (task instanceof WriteFileToClientDirectoryTask) {
//                        updateClientList();
//                    }
                    System.out.println("Выполнена задача: " + task.getClass());
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 8189);
            out = new ObjectEncoderOutputStream(socket.getOutputStream(), 100 * 1024 * 1024);
            in = new ObjectDecoderInputStream(socket.getInputStream(), 100 * 1024 * 1024);
            System.out.println("Клиент подключился");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //============================
    private void GUI() {
        setTitle("Клиент");
        setSize(700, 500);
        setLocation(600, 100);
        setResizable(false);

        Container c = getContentPane();
        JPanel p1 = new JPanel();
        p1.setLayout(new FlowLayout());

        backButton = new JButton("Назад");
        backButton.addActionListener(new BackButtonListener());
        backButton.setEnabled(false);

        buttonEnt = new JButton("Войти");
        buttonEnt.addActionListener(new EnterButtonListener());

        buttonAUth = new JButton("Регистрация");
        buttonAUth.addActionListener(new EnterButtonListener());

        p1.add(backButton);
        p1.add(buttonEnt);
        p1.add(buttonAUth);
        c.add(p1, BorderLayout.NORTH);
        clientListModel = new DefaultListModel<>();

        clientList = new JList<>(clientListModel);
        clientList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!clientList.isSelectionEmpty()) { // если "выбор" не пуст
                    File fileOrDirectory = clientList.getSelectedValue(); // получаем выбранный элемент
                    if (fileOrDirectory.isFile()) { // если это файл, то
                        textField.setText(fileOrDirectory.toString()); // выводим его в текстовом поле
                        isFile = true; // на отправку будет сформирован объект файл
                    } else {
                        textField.setText("");
                    }
                    button2.setEnabled(false);
                }
            }
        });
        clientList.addFocusListener(new ClientListFocusListener(clientList));
        clientList.addMouseListener(new clientListMouseListener());


        JList<String> listOnServer = new JList<>(listOnServerModel);
        listOnServer.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!listOnServer.isSelectionEmpty()) {
                    serverSideFileName = listOnServer.getSelectedValue(); // ссылка на имя скачиваемого файла
                }
            }
        });
        listOnServer.addFocusListener(new ServerListFocusListener(listOnServer));


        JScrollPane paneL = new JScrollPane(clientList);
        JScrollPane paneR = new JScrollPane(listOnServer);
        JPanel paneForList = new JPanel();
        paneForList.setLayout(new GridLayout(1, 2));
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
                if (textField.isFocusOwner()) {
                    textField.setText(""); // очистили текстовое поле
                    serverSideFileName = ""; // очистили выбор клиентского файла
                    isFile = false;
                }  // теперь там не файл
            }
        });

        button1 = new JButton("Отправить");
        button1.setEnabled(false);
        button1.addActionListener(new SendButtonListener());
        button2 = new JButton("Скачать");
        button2.setEnabled(false);
        button2.addActionListener(new DownLoadButtonListener());
        button3 = new JButton("Удалить");
        button3.addActionListener(new DeleteButtonListener());
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
                        CountDownLatch cdl3 = new CountDownLatch(1);
                        try {
                            requests.put(new SendFileTask(sendObj, out)); // передача файла на сервер
                            requests.put(new GetFileListCommandTask(out, in, cdl3)); // получение списка файлов с сервера
                            cdl3.await();
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        updateServerList();
                    } else JOptionPane.showMessageDialog(null, "Выберите файл из списка!");
                } else JOptionPane.showMessageDialog(null, "Файл не выбран!");
                textField.setText("");
            }
        }
    }

    // Слушатель для "Скачать"
    private class DownLoadButtonListener implements ActionListener {
        CountDownLatch countD = new CountDownLatch(1);
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!serverSideFileName.equals("")) {
                System.out.println("\n " + serverSideFileName + " файл для скачивания");
                CommandMessage com = new CommandMessage(CMD_MSG_FILE_DOWNLOAD, serverSideFileName); // командана на скачивание файла с сервера
                try {
                    requests.put(new DownloadFileFromServerCommandTask(com, out)); // отправка команды
                    requests.put(new WriteFileToClientDirectoryTask(in, parentClientFile, countD)); // записали файл
                    countD.await();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                textField.setText(""); // очистили поле
                serverSideFileName = ""; // очистили ссылку
                updateClientList();
            } else JOptionPane.showMessageDialog(null, "Файл для скачивания не выбран.");
        }
    }

    // Слушатель для кнопки "Удалить"
    private class DeleteButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isFile && !textField.getText().equals("")) { // если текстовое поле содержит имя файла
                File deleteFile = new File(textField.getText());     // создаем объект файла
                if (deleteFile.exists()) { // если удаляемый файл существует
                    boolean bb = deleteFile.delete();
                    if (bb) {
                        System.out.println(deleteFile + " файл удален.");
                        updateClientList();
                    }
                    else System.out.println("Произошла ошибка при удалении файла.");
                }
            } else {
                if (!serverSideFileName.equals("")) { // если выбран элемент из серверного списка
                    CountDownLatch cdl4 = new CountDownLatch(1);
                    CommandMessage deleteCommand = new CommandMessage(CMD_MSG_SERVER_DELETE_FILE, serverSideFileName);
                    try {
                        requests.put(new DeleteFileOnServerTask(out, deleteCommand)); // отправка команды
                        requests.put(new GetFileListCommandTask(out, in, cdl4)); // запрос серверного списка
                        cdl4.await();
                    } catch (InterruptedException ex) {ex.printStackTrace();}
                    updateServerList();
                }
                else JOptionPane.showMessageDialog(null, "Файл не выбран.");
            }
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


    // Слушатель для кнопки "Назад"
    private class BackButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            textField.setText(""); // очищаем текстовое поле, чтоб не возникли проблемы
            File parentFile = parentClientFile.getParentFile(); // хотим получить родительскую папку
            if (parentFile != null) { // если можно подняться на уровень выше, то
                parentClientFile = parentFile; // сохраняем ссылку на родителя
                File[] files = parentFile.listFiles(pathname -> !pathname.isHidden()); // получаем список содержимого
                clientListModel.clear();
                if (files != null) {
                    for (File f : files) {
                        clientListModel.addElement(f);
//                        System.out.println(f);
                    }
                } else System.out.println("Мы в корневой папке");

            }
        }
    }

    //  Слушатель для двойного клика по элементу клиентского списка
    private class clientListMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent evt) {
            if (evt.getClickCount() == 2) {
                File selectFile = clientList.getSelectedValue();
                parentClientFile = selectFile; // запомнили выбор
                if (parentClientFile.isFile()) { // если это файл
                    textField.setText(parentClientFile.toString()); // выводим на текстовое поле
                    isFile = true; // сигналим, что это файл и его можно отправить и ничего не делаем
                } else { // если выбранный элемент не файл
                    File[] files = selectFile.listFiles(); // получаем содержимое папки
                    clientListModel.clear();
                    if (files != null) { // если папка была не пустой
                        for (File f : files) {
                            clientListModel.addElement(f);
                            System.out.println(f);
                        }
                    } else System.out.println("В папке пусто.");
                }
            }
        }
    }


    // Слушатель фокуса клиентского списка
    private class ClientListFocusListener extends FocusAdapter {
        JList<File> clientList;

        ClientListFocusListener(JList<File> clientList) {
            this.clientList = clientList;
        }

        @Override
        public void focusGained(FocusEvent e) { // при получении фокуса
            button2.setEnabled(false);
            button1.setEnabled(true);
        }

        @Override
        public void focusLost(FocusEvent e) {
            clientList.clearSelection();
        }
    }

    // Слушатель фокуса серверного списка
    private class ServerListFocusListener extends FocusAdapter {
        JList<String> listOnServer;

        ServerListFocusListener(JList<String> listOnServer) {
            this.listOnServer = listOnServer;
        }

        @Override
        public void focusGained(FocusEvent e) {
            textField.setText("");
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
        backButton.setEnabled(true);
    }
    // обновление клиентского списка
    private void updateClientList() {
        clientListModel.clear();
        File[] filesAndDirectory = parentClientFile.listFiles(pathname -> !pathname.isHidden());
        if (filesAndDirectory != null) {
            for (File s : filesAndDirectory) {
                clientListModel.addElement(s);
            }
        } else System.out.println("Возникла проблема при обновлении файлов!");
    }

    // обновление серверного списка
    private void updateServerList() {
        listOnServerModel.clear();
        for(String ss: fileList){
            listOnServerModel.addElement(ss);
        }
    }
    //=========================================================================
// вспомогательное окно для авторизации, регистрации нового пользователя
    class AuthWindow extends JFrame {
        private JTextField login, password;

        AuthWindow authWindow; // держит ссылку на фрейм

        AuthWindow(String title) {
            super(title);
            JPanel panelButton = new JPanel();
            panelButton.setLayout(new GridLayout(7, 1));
            panelButton.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            JLabel label_login = new JLabel("Введите логин:");
            JLabel label_pass = new JLabel("Введите пароль:");
            JLabel label_space = new JLabel("   ");
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
            panelButton.add(label_space);
            panelButton.add(btnEnter);
            btnEnter.addActionListener(new InnerEnterButtonListener());
            add(panelButton);
            setBounds(650, 250, 400, 200);
            setResizable(false);
            authWindow = this; // ссылается на самого себя
            setVisible(true);
        }

        // слушатель кнопки "Войти"/"Зарегистрироваться"
        private class InnerEnterButtonListener implements ActionListener {
            CountDownLatch cdl = new CountDownLatch(1);
            @Override
            public void actionPerformed(ActionEvent e) {
                String loginText = login.getText();
                String passText = password.getText();
                if (!loginText.equals("") && !passText.equals("")) {
                    LoginAndPasswordMessage loginAndPass = new LoginAndPasswordMessage(loginText, passText);
                    if (e.getActionCommand().equals("Войти")) {
                        loginAndPass.setTypeAUTH(); // указали, что для авторизации
                    } else {
                        loginAndPass.setTypeREG(); // указали, что для регистрации
                    }
                    try {
                        AuthCommandTask task = new AuthCommandTask(out, in, loginAndPass, cdl);
                        requests.put(task); // отправили логин, пароль
                        cdl.await();
                        authWindow.dispose();
                        if (messFromServer.equals("OK")){
                            CountDownLatch cdl2 = new CountDownLatch(1);
                            updateClientList();
                            try {
                                requests.put(new GetFileListCommandTask(out, in, cdl2)); // команда на получение серверного списка
                            } catch (InterruptedException ex) {ex.printStackTrace();}
                            cdl2.await();
                            updateServerList(); // обновляем серверный список
                            setEnabledAllButton(); // активируем кнопки
                        } else {
                            if (e.getActionCommand().equals("Войти")) {
                                JOptionPane.showMessageDialog(null,
                                        "<html>Указанного пользователя нет в системе,<br> пожалуйста, зарегистрируйтесь!");
                            } else JOptionPane.showMessageDialog(null,
                                    "<html>Введенный логин уже занят");
                        }
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Введите логин и пароль!",
                            "Внимание", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    // закрытие потоков
    private void close() {
        try {
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Проблема с закрытием входного потока");
        }
        try {
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Проблема с закрытием выходного потока");
        }

        try {
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Проблема с закрытием сокета");
        }

    }






    //Два метода вызываются в gui:
    // получение ответа от сервера
    private void readMessFromServer() {
        try {
            Object message = in.readObject();
            if (message instanceof SimpleMessage) {
                messFromServer = ((SimpleMessage) message).getMessage();
                System.out.println(messFromServer);
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            System.out.println("Сообщение от сервера");
        }
    }
}
