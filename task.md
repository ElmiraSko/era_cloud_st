Добрый день!
Изменила  класс  Request:
public abstract class Request implements Serializable {}
Создала классы:
- Message extends Request — объект для сообщения
- UploadFile extends Request -  объект для файла
- CommandMessage extends Request — для команды
- ServerListMessage extends Request — использую для упаковки в объект массива файлов клиента, находящихся на сервере для передачи клиенту.


Класс CommandMessage делала по вашему образцу. Не совсем поняла как использовать private Object[] attachment;
Сомневаюсь, на сколько правильно реализовала этот класс, но использовать получается.

public class CommandMessage extends Request {
    public static final int CMD_MSG_AUTH_OK = 4352567;
    public static final int CMD_MSG_FILE_DOWNLOAD = 1292567;
    public static final int CMD_MSG_FILES_LIST = 5642532;
    public static final int CMD_MSG_SERVER_DELETE_FILE = 4113577;
    private int type;

//    private Object[] attachment;

    private String str;
    public CommandMessage(int type) {
        this.type = type;
    }

//    public CommandMessage(int type, Object... attachment) {
//        this.type = type;
//        this.attachment = attachment;
//    }

    public CommandMessage(int type, String str) {
        this.type = type;
        this.str = str;
    }
    public int getType() {
        return type;
    }
    public String getStr() {
        return str;
    }

//public Object[] getAttachment() {
//        return attachment;
//    }

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

На клиенте добавила очередь
private ArrayBlockingQueue<Task> requests = new ArrayBlockingQueue<Task>(5);

private ClientSerial() {
    connect();
    //===============
    new Thread(()->{
        while (true){
            try {
                Task task = requests.take();
                task.doing();
            } catch (InterruptedException ex) {ex.printStackTrace();}
        }
    }).start();
    //============
    GUI();
}
Сама задача формируется и помещается в очередь  requests в слушателе кнопок (например, «Отправить»)
Возник такой вопрос, в новом потоке для обработки задач, задачи должны быть только запросами,
а ответы от сервера я должна получать в самих слушателях, где были сформированы задачи?
Т.е. запросы в отдельном потоке, а ответы в потоке GUI.
Пока не работает аутентификация. Не пойму как подключить базу данных к серверу на Netty.
Просматривала примеры в интернете. Там встретила вариант, где соединение открывали
в классе extends ChannelInboundHandlerAdapter. Правильно ли так?
Еще хотела спросить, файлы клиентов не храним в базе, а храним в папке на сервере?
Получается в БД только логины и пароли.

Сейчас файлы передаются от клиента к серверу и обратно. Если файл большой то передается по частям.



