Добрый день!
Сейчас проект рабочий, но надо его улучшать и переписывать.
Внесла следующие правки:
1. в методе для передачи файла по частям:  req.setData(new byte[MAX_SIZE]); прописала один раз;

2. с сервера список файлов передавала в виде массива строк( String[] filesList),
   теперь формируется ArrayList<String> filesList и отправляется клиенту;


// список файлов на сервере
    private void fileListToClient(ChannelHandlerContext ctx) {
        File dir = new File(userDirectory);
        String[] filesArray = dir.list();
        ArrayList<String> files = new ArrayList<>();
        if (filesArray != null){
            files.addAll(Arrays.asList(filesArray));
        }
        ServerListMessage serverList = new ServerListMessage(files);
        ctx.writeAndFlush(serverList);
    }

3. все пыталась решить как правильно обновлять gui клиента (там гонка возникает):
Например, при нажатии кнопки "Отправить" файл улетает на сервер
@Override
public void actionPerformed(ActionEvent e) {
....
requests.put(new SendFileTask(sendObj, out)); // передача файла на сервер
(ответ присылать не должен)
далее  выполняю отправку команды на получение списка и в той же задаче получаю список:

requests.put(new GetFileListCommandTask(out, in, cdl3)); // получение списка файлов с сервера

Но все эти задачи выполняются в отдельном потоке из которого нет доступа в gui.
Т.е. список получен, но gui клиента об этом не знает.
Если тут же после этих задач выполнить updateServerList(); то метод может выполниться раньше,
чем GetFileListCommandTask.
Пака нашла выход в использовании CountDownLatch cdl3 = new CountDownLatch(1); в каждом слушателе.
Проверила, работает. (код клиента в файле myClient.md)

//-------------
Пока ясной, хорошо понятной картины нет, как обновлять интерфес через другой поток.
В интернете читала, как вариант использовать SwingWorker, еще Callable-задачи и Future, на форумах
видела предложения обновлять компоненты ui так:
SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          //  String result = // call a REST API
           // textArea.setText(result);
        }
    });
Продолжаю разбираться.
Дальше хочу переписать клиента на javaFX и Netty, и пробовать передавать данные протоколом.
Понимаю, что textField можно убрать, но пока его не трогала, т.к. все работает.
Не совсем поняла по поводу правки private String rootDirectory = "server/ServerDir/";
как прописать нужную директорию, поэтому, пока оставила как есть.

Спасибо Вам большое за отличные уроки! Все было профессионально и качественно.
С уважением, Эльмира.