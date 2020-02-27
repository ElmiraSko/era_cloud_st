Добрый день!
Клиент подключается к серверу, проходит аутентификацию, получает список файлов, которые хранятся на сервере, может
отправлять файлы на сервер, или скачивать их оттуда.
При передачи файла на сервер или обратно, если такой файл уже есть, он просто перезаписывается.
Для передачи данных воспользовалась сериализацией. Клиент подключается и запускает отдельный поток для выполнения
задач - new Thread(new Operation()).start(); где выполняется отправка/получение файлов, команд и сообщений.
Но, два метода оставила в основном потоке:
1.  readMessFromServer() - чтение текстового сообщения от сервера
2.  updateServerList() - обновление содержимого в серверном списке (DefaultListModel<String> listOnServerModel).
Думала все взаимодействие осуществлять в этом отдельном потоке new Operation(), но не получается обновить listOnServerModel, т.е.
как передать из другого потока обновленный listOnServerModel в поток gui. Пыталась сделать volatile listOnServerModel, но
это отработало один раз. Поэтому обновление модели оставила в основном потоке Клиента.
Метод readMessFromServer() ждет положительный ответ, когда клиент авторизуется и потом направляет команду для
получения списка файлов с сервера.
Реализовала удаление файлов как на стороне клиента, так и на сервере. Есть возможность регистрировать нового клиента.
У каждого клиента с логином login1 создается своя папка login1 (server/ServerDir/login1).

// ============== Класс клиента ========================
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

import static com.era.cloud.common.CommandMessage.CMD_MSG_FILE_DOWNLOAD;
import static com.era.cloud.common.CommandMessage.CMD_MSG_SERVER_DELETE_FILE;

public class Client extends JFrame {

//    private final int MAX_SIZE = 1024*1024*100; максимальный размер передачи, указан в задаче

    private Socket socket;
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;

    private File[] roots = File.listRoots();
    private File clientFile = roots[0]; // диск С:
    private File parentClientFile = clientFile;
    private String serverSideFileName = ""; // ссылка на имя файла на стороне сервера

    private JTextField textField;
    private JList<File> clientList; // клиентский список
    private DefaultListModel<String> listOnServerModel = new DefaultListModel<>();
    private DefaultListModel<File> clientListModel;

    private ArrayBlockingQueue<Task> requests = new ArrayBlockingQueue<>(5);
    private boolean isFile = false;  // true - если файл
    private JButton backButton, buttonEnt, buttonAUth, button1, button2, button3;
    private String messFromServer = "message";

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
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
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
                        try {
                            requests.put(new SendFileTask(sendObj, out)); // передача файла на сервер
                            requests.put(new GetFileListCommandTask(out)); // запрос на список файлов
                            updateServerList();  // обновли серверный список
//                            requests.put(new UpdateServerListTask(in, listOnServerModel));
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    } else JOptionPane.showMessageDialog(null, "Выберите файл из списка!");
                } else JOptionPane.showMessageDialog(null, "Файл не выбран!");
                textField.setText("");
            }
        }
    }

    // Слушатель для "Скачать"
    private class DownLoadButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (!serverSideFileName.equals("")) {
                System.out.println("\n " + serverSideFileName + " файл для скачивания");
                CommandMessage com = new CommandMessage(CMD_MSG_FILE_DOWNLOAD, serverSideFileName); // командана на скачивание файла с сервера
                try {
                    requests.put(new DownloadFileFromServerCommandTask(com, out)); // отправка команды
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                textField.setText(""); // очистили поле
                serverSideFileName = ""; // очистили ссылку
                try {
                    requests.put(new WriteFileToClientDirectoryTask(in, parentClientFile)); // записали файл
                    requests.put(new UpdateClientListTask(clientListModel, parentClientFile)); // обновили список
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
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
                    try { // обновим клиентский список
                        requests.put(new UpdateClientListTask(clientListModel, parentClientFile));
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    if (bb) System.out.println(deleteFile + " файл удален.");
                    else System.out.println("Произошла ошибка при удалении файла.");
                }
            } else {
                if (!serverSideFileName.equals("")) { // если выбран элемент из серверного списка
                    CommandMessage deleteCommand = new CommandMessage(CMD_MSG_SERVER_DELETE_FILE, serverSideFileName);
                    try {
                        requests.put(new DeleteFileOnServerTask(out, deleteCommand)); // отправка команды
                        requests.put(new GetFileListCommandTask(out)); // запрос серверного списка
                    } catch (InterruptedException ex) {ex.printStackTrace();}
                    updateServerList();    // ожидаем и обновляем серверный список
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
                        System.out.println(f);
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

    //-----------------------------------------------------------------
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
                        requests.put(new AuthCommandTask(out, loginAndPass)); // отправили логин, пароль
                        readMessFromServer(); // дожидаемся ответ сервера
                        authWindow.dispose();
                        if (messFromServer.equals("OK")) {
                            // обновляем клиентский список
                            requests.put(new UpdateClientListTask(clientListModel, clientFile));
                            requests.put(new GetFileListCommandTask(out)); // команда на получение серверного списка
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

    //============= Два метода вызываются в gui: =========================
    // получение ответа от сервера
    private void readMessFromServer() {
        try {
            Object obj = in.readObject();
            if (obj instanceof SimpleMessage) {
                messFromServer = ((SimpleMessage) obj).getMessage();
                System.out.println(messFromServer);
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
            System.out.println("Сообщение от сервера");
        }
    }

    // обновление серверного списка
    private void updateServerList() {
        listOnServerModel.clear();
        try {
            Object obj = in.readObject();
            if (obj instanceof AbstractMessage) {
                String[] files = ((ServerListMessage) obj).getFilesList();
                if (files != null) {
                    for (String s : files) {
                        listOnServerModel.addElement(s);
                        System.out.println(s);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
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
}
//================= task по удалению файла: class DeleteFileOnServerTask ====== в модуле клиента ======================

import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;

public class DeleteFileOnServerTask implements Task { // отправка команды на удаление, выполняется в отдельном потоке

    private ObjectEncoderOutputStream out; // выходной поток
    private CommandMessage deleteCommand; // объект-команда

    public DeleteFileOnServerTask(ObjectEncoderOutputStream out, CommandMessage deleteCommand) {
        this.out = out;
        this.deleteCommand = deleteCommand;
    }

    @Override
    public void doing() {
        try {
            out.writeObject(deleteCommand);
        } catch (IOException e) {e.printStackTrace();}
    }
}
//------------------------------------------------
package com.era.cloud.client;

import javax.swing.*;
import java.io.File;
import java.io.FileFilter;

public class UpdateClientListTask implements Task {

    private DefaultListModel<File> clientListModel; // модель клиентского списка
    private File clientFile;  // путь к файлу

    UpdateClientListTask(DefaultListModel<File> clientListModel, File clientFile) {
        this.clientListModel = clientListModel;
        this.clientFile = clientFile;
    }

    @Override
    public void doing() {
        clientListModel.clear();
        File[] filesAndDirectory = clientFile.listFiles(new MyFilter());
        if (filesAndDirectory != null) {
            for (File s : filesAndDirectory){
                clientListModel.addElement(s);
            System.out.println(s);}
        }
        else System.out.println("Возникла проблема при обновлении файлов!");
    }

    private class MyFilter implements FileFilter {

        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden();
        }
    }
}
//--------------------------------------------------------------
package com.era.cloud.client;

import com.era.cloud.common.UploadFile;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteFileToClientDirectoryTask implements Task {
    private ObjectDecoderInputStream in;
    private File clientDirectory;


    WriteFileToClientDirectoryTask(ObjectDecoderInputStream in, File clientDirectory) {
        this.in = in;
        this.clientDirectory = clientDirectory;
    }

    @Override
    public void doing() {
        try {
            Object object = in.readObject();
            if (object instanceof UploadFile) { // если файл
                UploadFile file = (UploadFile) object;
                writeFileInDir(file);
            }
        } catch (IOException | ClassNotFoundException ex) {ex.printStackTrace();}
    }

    // запись файла на диск клиента
    private void writeFileInDir(UploadFile file) {
        boolean append = true;
        String fileName = file.getName();
        String filePath = clientDirectory + "/" + fileName;
        if (file.getPartNumber() == 1) { // если файл состоит из одной части, то не дописываем
            append = false;
        }
        try {
            File writeFile = new File(filePath);
            if (!writeFile.exists()) {
                writeFile.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(writeFile, append);
            out.write(file.getData()); // записали в файл
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("В методе writeFileInDir возникли проблемы.");}
        System.out.println("От сервера: получен файл " + fileName);
    }

}
И еще есть GetFileListCommandTask, DownloadFileFromServerCommandTask
//================= Сервер:  ===========================
package com.era.cloud.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CloudServer {
    private ConnectDB connectDB;


    private CloudServer() {
        connectDB = new ConnectDB();
    }

    private void run() throws Exception {

        EventLoopGroup mainGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(mainGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(
                                    new ObjectDecoder(1024 * 1024 * 100, ClassResolvers.cacheDisabled(null)),
                                    new ObjectEncoder(),
                                    new MainHandler(connectDB)
                            );
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
//                    .childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, 1024 * 1024 * 20)
                    .childOption(ChannelOption.TCP_NODELAY, true);
            ChannelFuture future = b.bind(8189).sync();
            future.channel().closeFuture().sync();


        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            connectDB.close();
        }
    }

    public static void main(String[] args) throws Exception {
        new CloudServer().run();
    }
}

//=========== Handler =======================
package com.era.cloud.server;

import com.era.cloud.common.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;

import java.io.*;
import java.sql.Connection;


public class MainHandler extends ChannelInboundHandlerAdapter {

    private ConnectDB conDB;
    private String loginUser;
    private final int MAX_SIZE = 1024*1024*100;
    private String rootDirectory = "server/ServerDir/";
    private String userDirectory;

   public MainHandler(ConnectDB conDB) {
       super();
       this.conDB = conDB;
   }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            if(msg instanceof CommandMessage) { // если полученный объект - команда
                CommandMessage command = (CommandMessage) msg;
                if (command.is_AUTH_OK()) { // если это команда аутентификации
                    Object[] objects = command.getAttachment();
                    if (objects[0] instanceof LoginAndPasswordMessage) {
                        LoginAndPasswordMessage log_pass = (LoginAndPasswordMessage)objects[0];
                        String log = log_pass.getLogin();
                        String pass = log_pass.getPassword();

                        if (log_pass.typeIsAUTH()){ // если авторизация
                            loginUser = conDB.authorize(log, pass); // есть ли в базе такой пользователь
                            if (loginUser != null) {
                                SimpleMessage mess = new SimpleMessage("OK");
                                ctx.writeAndFlush(mess);
                                userDirectory = rootDirectory + loginUser + "/";
                                System.out.println(userDirectory);
                            }
                            else {SimpleMessage mess = new SimpleMessage("NO");
                                ctx.writeAndFlush(mess);}
                        } else
                            if (log_pass.typeIsREG()) { // если регистрация, при регистрации сразу создаем папку
                                if (conDB.authorize(log, pass) == null) {   // если такого логина и пароля еще нет в базе, то
                                conDB.writeNewUser(log, pass); //добавляем нового пользователя
                                    SimpleMessage mess = new SimpleMessage("OK"); //отправляем OK
                                    ctx.writeAndFlush(mess);
                                    userDirectory = rootDirectory + log; // так как логины не повторяются, то содаем папку
                                    System.out.println(userDirectory);
                                    File dir = new File(userDirectory);
                                    boolean created = dir.mkdir(); // создали новый коталог
                                    if(created){userDirectory += "/";}
                                } else {
                                    SimpleMessage mess = new SimpleMessage("NO"); //отправляем NO
                                    ctx.writeAndFlush(mess);
                                }
                            }
                    }
                }
                if (command.is_FILES_LIST()) // если команда на формирование списка файлов
                    fileListToClient(ctx);
                if (command.is_FILE_DOWNLOAD()) { // команда на скачивание файла
                    String fileName = command.getFileName();
                    sendFileToClient(fileName, ctx);
                }
                if (command.is_SERVER_DELETE_FILE()) { // если команда на удаление файла
                    String fileName = command.getFileName();
                    deleteFile(fileName);
                }
            } else
                if (msg instanceof UploadFile) { // если получен файл от клиента
                    UploadFile file = (UploadFile) msg;
                    writingFileToStorage(file);
            } else
                if (msg instanceof SimpleMessage) { // если сообщение
                    String mes = ((SimpleMessage) msg).getMessage();
                    System.out.println("От клиента: " + mes);
                    // ответ сервера
                    SimpleMessage mess = new SimpleMessage("Сервер получил сообщение: " + mes);
                    ctx.writeAndFlush(mess);
            }

        }  finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

// список файлов на сервере
    private void fileListToClient(ChannelHandlerContext ctx) {
        File dir = new File(userDirectory);
        ServerListMessage serList = new ServerListMessage(dir.list());
        ctx.writeAndFlush(serList);
    }

    // запись файла на диск
    private void writingFileToStorage(UploadFile file) {
        boolean append = true;
        String fileName = file.getName();
        String filePath = userDirectory + fileName;
        if (file.getPartNumber() == 1) { // если файл состоит из одной части, то не дописываем
            append = false;
        }
        try {
            File writeFile = new File(filePath);
            System.out.println(writeFile);
            FileOutputStream out = new FileOutputStream(writeFile, append);
            out.write(file.getData()); // записали в файл
            out.close();
        } catch (IOException ex) {ex.printStackTrace();}
        System.out.println("От клиента: получен файл " + fileName);
    }

    // передача файла клиенту
    private void sendFileToClient(String fileName, ChannelHandlerContext ctx){
        File file = new File(userDirectory + fileName);
        int len = (int)file.length();
        UploadFile req = new UploadFile(file);
        int partNumber = 1;
        req.setPartNumber(partNumber);
        try {
            FileInputStream inp = new FileInputStream(file);
            BufferedInputStream buff = new BufferedInputStream(inp, MAX_SIZE);
            if (len <= MAX_SIZE) {
                req.setData(new byte[len]);
                buff.read(req.getData());
                ctx.writeAndFlush(req);
            } else { // если длина файла превышает ограничение на передаваемый объем
                while (len > 0) {  // пока все не считали
                    req.setPartNumber(partNumber); // устанавливаем номер посылки
                    req.setData(new byte[MAX_SIZE]); // обновляем массив для данных
                    if ((buff.available()) > 0) {
                        buff.read(req.getData()); // записываем в массив данные
                    }
                    len = len - MAX_SIZE; // уменьшаем длину файла
                    partNumber++;
                    ctx.writeAndFlush(req); // отправляем посылку
                }
            }
            buff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }        System.out.println();
        System.out.println("Сервер отправил файл: " + fileName);
    }

    private void deleteFile(String fileName){
        File file = new File(userDirectory + fileName);
        if (file.exists()) {
           boolean del = file.delete();
           if (del) System.out.println("Файл удален.");
           else System.out.println("Ошибка при удалении файла.");
        }
    }
}
//=================================== Класс ConnectDB для работы с БД ===========
package com.era.cloud.server;

import java.sql.*;

public class ConnectDB {
    private Connection connection = null;
    private Statement state;
    private final String DB_URL = "jdbc:sqlite:server/DB/DataBase.db";
    private final String DB_Driver = "org.sqlite.JDBC";
    //-----------------------------------------------------------------------------------------------------------------

    ConnectDB() {
        try {
            Class.forName(DB_Driver);
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Все отлично! Мы подключились к БД");
        } catch (ClassNotFoundException ex) {
            System.out.println("Не удалось соединиться с базой: " + ex.getMessage());
        } catch (Exception e) {
            System.out.println("Что-то не так, надо разобраться! " + e.getMessage());
        }

    }
// авторизация, проверяем, есть ли юзер в базе
    String authorize(String login, String pass) {
        String login_user = null;
        String query = "SELECT login, password FROM users WHERE login = '" + login + "'" + " AND password = '" + pass + "'";
        try {
            state = connection.createStatement();
            ResultSet res = state.executeQuery(query);
            if (res.next()) {
                login_user = res.getString("login");
                System.out.println("Пользователь есть в базе");
            }
            else {
                System.out.println("Пользователя нет в базе");
            }
        } catch (SQLException ex) {ex.printStackTrace();}
        return login_user;
    }

    // добавление нового пользователя
    void writeNewUser(String login, String pass) {
        String query = "INSERT INTO users (login, password) VALUES ('" + login + "', '" + pass + "');";
        try {
            state = connection.createStatement();
            state.executeUpdate(query);
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println("Проблемы с регистрацией");
        }
    }

    // метод для закрытия connection
    void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println("Не закрыли" + e.getMessage());
        }
    }

    // метод для создания таблицы auth в бд DataBase.
    public void createTable(){
        try{
            String query = String.format("CREATE TABLE users (id integer primary key autoincrement, login varchar not null,  password varchar not null, unique (login))");
//            String q = "CREATE TABLE users (id integer primary key autoincrement, login varchar not null,  password varchar not null, unique (login)";
            Statement state = connection.createStatement();
            state.executeUpdate(query);
        }catch (SQLException ex){
            System.out.println("Проблемы при создании таблицы users");
        }
    }

    public void all() {
        String queryALL = "SELECT * FROM users";
        try {
            state = connection.createStatement();
            ResultSet res = state.executeQuery(queryALL);
            System.out.println(res.next() + " есть ли пользователи в базе");
            while (res.next()) {
                String log_ = res.getString("login");
                String pass_ = res.getString("password");
                System.out.println(log_ + ", " + pass_);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}

//============= классы из модуля common =======================================================
package com.era.cloud.common;
import java.io.Serializable;
public abstract class AbstractMessage implements Serializable {
}
//---------------------------------------------
package com.era.cloud.common;
public class CommandMessage extends AbstractMessage {
    public static final int CMD_MSG_AUTH_OK = 4352567;
    public static final int CMD_MSG_FILE_DOWNLOAD = 1292567;
    public static final int CMD_MSG_FILES_LIST = 5642532;
    public static final int CMD_MSG_SERVER_DELETE_FILE = 4113577;

    private int type;
    private Object[] attachment;
    private String fileName;

// клнструкторы
    public CommandMessage(int type) {
        this.type = type;
    }

    public CommandMessage(int type, Object... attachment) {
        this.type = type;
        this.attachment = attachment;
    }

    public CommandMessage(int type, String fileName) {
        this.type = type;
        this.fileName = fileName;
    }

    public int getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public Object[] getAttachment() {
        return attachment;
    }

    public boolean is_AUTH_OK(){
        return type == CMD_MSG_AUTH_OK;
    }
    public boolean is_FILE_DOWNLOAD(){
        return type == CMD_MSG_FILE_DOWNLOAD;
    }
    public boolean is_FILES_LIST(){
        return type == CMD_MSG_FILES_LIST;
    }
    public boolean is_SERVER_DELETE_FILE(){
        return type == CMD_MSG_SERVER_DELETE_FILE;
    }
}
//-----------------------------------------
package com.era.cloud.common;

public class LoginAndPasswordMessage extends AbstractMessage {
    private final int AUTH = 259436787; // для авторизации
    private final int REG = 733277879; // для регистрации

    private String login;
    private String password;
    private int typeMess; // тип

    public LoginAndPasswordMessage(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public int getTypeMess() {
        return typeMess;
    }
// методы для задания типа
    public void setTypeAUTH() {
        this.typeMess = AUTH;
    }
    public void setTypeREG() {
        this.typeMess = REG;
    }

    public boolean typeIsAUTH() {
        return typeMess == AUTH;
    }

    public boolean typeIsREG() {
        return typeMess == REG;
    }
}
//-----------------------------------------
package com.era.cloud.common;
public class ServerListMessage extends AbstractMessage {
    private String[] filesList;

    public ServerListMessage(String[] list) {
        filesList = list;
    }

    public String[] getFilesList() {
        return filesList;
    }
}
//----------------------------------------------
package com.era.cloud.common;

public class SimpleMessage extends AbstractMessage {
    private String message;
    public SimpleMessage(String mess) {
        message = mess;
    }

    public String getMessage() {
        return message;
    }
}
//----------------------------------------
package com.era.cloud.common;

import java.io.File;
import java.io.Serializable;

public class UploadFile extends AbstractMessage {

    private String name;
    private byte[] data;
    private int size; // удалить

    private int partNumber; // номер части

    public UploadFile(File file) {
        name = file.getName();
        size = (int)file.length();
    }

    public int getPartNumber() {
        return partNumber;
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
    }
}
//============================================================================================================
