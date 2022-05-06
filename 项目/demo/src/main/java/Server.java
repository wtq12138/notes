import lombok.SneakyThrows;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @program: demo
 * @description:
 * @author: wtq12138
 * @create: 2022-03-30 10:09
 */


public class Server {
    public Thread linstener;
    private int port;

    public Server(int port) {
        this.port = port;
        linstener=new Thread(new LinstenerThread());
        linstener.start();
    }

    private class LinstenerThread implements Runnable{
        @Override
        @SneakyThrows
        public void run() {
            ServerSocket serverSocket=new ServerSocket(port);
            for(;;)
            {
                System.out.println("服务器在监听端口");
                Socket socket = serverSocket.accept();
                handleSocket(socket);
                socket.close();
            }
        }
        @SneakyThrows
        void handleSocket(Socket socket) {
            InputStream is = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(is);
            BufferedInputStream bis = new BufferedInputStream(is);
            int len;
            byte[] bytes = new byte[1024];
            while((len=bis.read(bytes))!=-1)
            {
                System.out.println(bytes);
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server(10086);
    }
}
