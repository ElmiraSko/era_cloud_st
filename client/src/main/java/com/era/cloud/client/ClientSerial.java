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
    private final int MAX_SIZE = 1024;
    private Socket socket;
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;

    private String rootDir = "client/clientDir/";
    private JTextField textField;
    private DefaultListModel<String> listOnServerModel = new DefaultListModel<>();
    private DefaultListModel<String> clientListModel;

    private ArrayBlockingQueue<Task> requests = new ArrayBlockingQueue<Task>(5);
    private boolean isFile = false;  // true - если файл

    private ClientSerial() {
        connect();
        //=====================================
        new Thread(()->{
            while (true){
                try {
                    Task task = requests.take();
                    task.doing();
                } catch (InterruptedException ex) {ex.printStackTrace();}
            }
        }).start();
        //=======================================
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
            try {
                socket = new Socket("localhost", 8189);
                out = new ObjectEncoderOutputStream(socket.getOutputStream());
                in = new ObjectDecoderInputStream(socket.getInputStream(), 100 * 1024 * 1024);
                System.out.println("Клиент подключился");

                getFileListFromServer(); // отправка команды на получение списка
                updateServerList(); // ответ и обновление
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
                        Task task = new SendFileTask(rootDir + sendObj, in, out, listOnServerModel);
                        try {
                            requests.put(task); // передача файла на сервер + запрос на список файлов
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                        updateServerList(); // ожидаем и обновляем список
                    }
                }
                textField.setText("");
            }
        }
    }

    private class PullButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String obj = textField.getText();
            if (!obj.equals("")) {
                CommandMessage com = new CommandMessage(CMD_MSG_FILE_DOWNLOAD, obj);
                try { // отправляем запрос на скачивание файла
                    out.writeObject(com);
                    out.flush();
                } catch (IOException ex) {ex.printStackTrace();}
            }
            textField.setText(""); // очистили поле
            try {
                Object object = in.readObject();
                if (object instanceof UploadFile) { // если файл
                    UploadFile file = (UploadFile) object;
                    writeFileInDir(file);
                }
                updateClientList();
            } catch (IOException | ClassNotFoundException ex) {ex.printStackTrace();}

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

        clientListModel = new DefaultListModel<>();
        updateClientList();
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
        listOnServer.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (listOnServer.getSelectedIndex() >= 0) {
                    textField.setText(listOnServer.getSelectedValue());
                    isFile = true; // на отправку будет сформирован объект файл
                }
            }
        });
        JScrollPane paneL = new JScrollPane(clientList);
        JScrollPane paneR = new JScrollPane(listOnServer);
        JPanel paneForList = new JPanel();
        paneForList.setLayout(new GridLayout(1,2));
        paneForList.add(paneL);
        paneForList.add(paneR);
        c.add(paneForList, BorderLayout.CENTER);
        JPanel p3 = new JPanel();
        p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS));
        JLabel label = new JLabel("File:");
        textField = new JTextField(8);
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
        JButton button2 = new JButton("Получить");
        button2.addActionListener(new PullButtonListener());
        JButton button3 = new JButton("Удалить");
        p3.add(label);
        p3.add(textField);
        p3.add(button);
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



    //получение ответа от сервера
    private void readMessFromServer() {
        try {
            Object obj = in.readObject();
            if (obj instanceof Message) {
                System.out.println(((Message)obj).getMessage());
            }
        } catch (IOException | ClassNotFoundException  ex){ex.printStackTrace();}
    }
    // передача файла
    private void writeFile(String fileName){
        File file = new File(rootDir + fileName);
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
                out.writeObject(req);
                out.flush();
            } else {
                while (len > 0) {
                    req.setPartNumber(partNumber);
                    req.setData(new byte[MAX_SIZE]);
                    if ((buff.available()) > 0) {
                        buff.read(req.getData());
                    }
                    len = len - MAX_SIZE;
                    partNumber++;
                    out.writeObject(req);
                    out.flush();
                }
            }
            buff.close();
        } catch (IOException e) {
            e.printStackTrace();
        }        System.out.println();
        System.out.println("Клиент отправил файл: " + fileName);
    }

    // передача сообщения
    private void writeMess(String message) {
        Message mess =  new Message(message);
        try {
            out.writeObject(mess);
            out.flush();
            System.out.println("Клиент отправил сообщение: " + message);
        } catch (IOException ex) {ex.printStackTrace();}
    }
    // запись файла на диск
    private void writeFileInDir(UploadFile file) {
        boolean append = true;
        String fileName = file.getName();
        String filePath = rootDir + fileName;
        if (file.getPartNumber() == 1) { // если файл состоит из одной части, то не дописываем
            append = false;
        }
        try {
            File writeFile = new File(filePath);
            FileOutputStream out = new FileOutputStream(writeFile, append);
            out.write(file.getData()); // записали в файл
            out.close();
        } catch (IOException ex) {ex.printStackTrace();}
        System.out.println("От сервера: получен файл " + fileName);
    }

    // запрос на получение списка файлов находящихся на сервере
    private void getFileListFromServer() {
        CommandMessage message = new CommandMessage(CMD_MSG_FILES_LIST);
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException ex) {ex.printStackTrace();}
    }

    //обновить клиентский список
    private void updateClientList() {
        clientListModel.clear();
        File dir = new File(rootDir);
        String[] files = dir.list();
        if (files != null) {
            for (String s : files)
                clientListModel.addElement(s);
        }
    }

    private void updateServerList() {
        listOnServerModel.clear();
        try {
            Object obj = in.readObject();
            if (obj instanceof Request ) {
                String[] files = ((ServerListMessage) obj).getFilesList();
                for (String s: files) {
                    listOnServerModel.addElement(s);
                    System.out.println(s);
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
}
