package cn.erika;

import cn.erika.handler.ClientHandler;
import cn.erika.handler.ServerHandler;
import cn.erika.plugins.io.ConfigReader;
import cn.erika.plugins.io.GeneralInput;
import cn.erika.plugins.io.KeyboardReader;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;

public class Main {
    // 日志记录
    private static Logger log = Logger.getLogger(Main.class);
    // 通过键盘输入
    private static GeneralInput input = KeyboardReader.getInstance();

    private static String listenAddr;
    private static int listenPort;
    private static String serverAddr;
    private static int serverPort;

    static {
        try {
            listenAddr = ConfigReader.get("listen_addr");
            listenPort = Integer.parseInt(ConfigReader.get("listen_port"));
            serverAddr = ConfigReader.get("server_addr");
            serverPort = Integer.parseInt(ConfigReader.get("server_port"));
        } catch (NumberFormatException e) {
            log.error("配置中有错误", e);
        }
    }

    public static void main(String[] args) throws IOException, SQLException, NoSuchAlgorithmException {
        System.out.println("Hello world");

        // 检测输入命令 s: 服务器 c: 客户端 e: 退出
        if (args.length > 0) {
            if ("s".equalsIgnoreCase(args[0])) {
                server();
            } else if ("c".equalsIgnoreCase(args[0])) {
                client();
            } else {
                log.info("s : 以服务器的方式启动\n" +
                        "c : 以客户端的方式启动\n" +
                        "e : 退出\n");
            }
        } else {
            String tip = "s : 以服务器的方式启动\n" +
                    "c : 以客户端的方式启动\n" +
                    "e : 退出\n";

            String line;
            while ((line = input.read(tip)) != null && !"e".equalsIgnoreCase(line)) {
                String command = null;
                try {
                    command = line.split("\\s")[0];
                } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                    command = line;
                }
                try {
                    String attr = null;
                    switch (command) {
                        case "s":
                            server();
                            break;
                        case "c":
                            client();
                            break;
                        default:
                            log.warn("命令无效");
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    log.warn("命令无效");
                }
                log.info("返回主界面");
            }
        }
    }

    private static void server() throws IOException {
        // 新建对象
        ServerHandler server = new ServerHandler();
        server.setCacheSize(Integer.parseInt(ConfigReader.get("cache_size")));
        server.setCharset(ConfigReader.charset());
        // 尝试启动
        try {
            server.listen(new InetSocketAddress(listenAddr, listenPort));
            // 命令解析部分
            String line;
            while ((line = input.read()) != null && !"EXIT".equalsIgnoreCase(line)) {
                String command = null;
                try {
                    command = line.split("#")[0];
                } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                    command = line;
                }
                String dest;
                String msg;
                try {
                    switch (command) {
                        case "show":
                            server.display();
                            break;
                        case "send":
                            dest = line.substring(command.length() + 1).split(":")[0];
                            msg = line.substring(command.length() + dest.length() + 2);
                            server.send(dest, msg);
                            break;
                        case "file":
                            dest = line.substring(command.length() + 1).split(":")[0];
                            String filename = line.substring(command.length() + dest.length() + 2);
                            server.sendFile(dest, new File(filename));
                            break;
                        case "kill":
                            String client = line.substring(5);
                            server.close(client);
                            break;
                        case "encrypt":
                            dest = line.split("#")[1];
                            server.encrypt(dest);
                            break;
                        default:
                            log.warn("命令无效: " + line);
                            serverTip();
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    log.warn("命令无效: " + line);
                    serverTip();
                }
            }
            // 循环结束关闭服务器
            server.close();
            log.info("关闭服务器");
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private static void client() {
        // 新建对象
        ClientHandler client = null;
        try {
            client = new ClientHandler();
            client.setCacheSize(Integer.parseInt(ConfigReader.get("cache_size")));
            client.setCharset(ConfigReader.charset());
            // 尝试启动
            client.connect(new InetSocketAddress(serverAddr, serverPort));
            //命令解析部分
            String line;
            while ((line = input.read()) != null && !"EXIT".equalsIgnoreCase(line) && !client.isClosed()) {
                String command = null;
                try {
                    command = line.split("#")[0];
                } catch (StringIndexOutOfBoundsException | ArrayIndexOutOfBoundsException e) {
                    command = line;
                }
                String msg = null;
                try {
                    switch (command) {
                        case "send":
                            msg = line.substring(command.length() + 1);
                            client.send(msg);
                            break;
                        case "encrypt":
                            client.encrypt();
                            break;
                        case "file":
                            msg = line.substring(command.length() + 1);
                            if (msg.split("#").length > 1) {
                                String file = msg.substring(0, msg.lastIndexOf("#"));
                                String filename = msg.substring(msg.lastIndexOf("#") + 1, msg.length());
                                client.sendFile(new File(file), filename);
                            } else {
                                client.sendFile(new File(msg));
                            }
                            break;
                        case "reg":
                            msg = line.substring(command.length() + 1);
                            client.registry(msg);
                            break;
                        case "talk":
                            msg = line.substring(command.length() + 1);
                            client.talk(msg);
                            break;
                        case "find":
                            client.find();
                            break;
                        default:
                            log.warn("命令无效: " + line);
                            clientTip();
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    log.warn("命令无效: " + line);
                    clientTip();
                }
            }
            // 循环结束关闭客户端
            client.close();
        } catch (IOException e) {
            log.warn("与服务器断开连接");
            log.debug(e);
            if (client != null) {
                client.close();
            }
        }
    }

    private static void clientTip() {
        System.out.println("\n" +
                "send 发送消息给服务器\n" +
                "talk 发送消息给指定的连接\n" +
                "  例:talk#Hello World\n" +
                "encrypt 请求加密通信\n" +
                "reg 注册昵称" +
                "find 显示服务器接入的连接\n" +
                "file 发送文件给服务器 (文件名不要出现#)\n" +
                "  例:file#/var/www/html/index.html\n" +
                "kill 强制指定的连接下线\n" +
                "exit 退出\n");
    }

    private static void serverTip() {
        System.out.println("\nshow 显示当前接入的连接\n" +
                "send 发送消息给指定的连接\n" +
                "  例:send#id0:Hello World\n" +
                "encrypt 请求加密通信\n" +
                "  例:encrypt#id0" +
                "file 发送文件给指定的连接 (文件名不要出现#)\n" +
                "  例:file#id0:/var/www/html/index.html\n" +
                "kill 强制指定的连接下线\n" +
                "exit 退出\n");
    }
}
