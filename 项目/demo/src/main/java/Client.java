import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.*;

/**
 * @program: demo
 * @description:
 * @author: wtq12138
 * @create: 2022-03-30 10:09
 */
public class Client {
    private int serverport;

    public final ExecutorService threadPool;

    public Client() {
        threadPool=Executors.newCachedThreadPool();
    }

    @SneakyThrows
    public void sendRequest() {
        Socket socket = new Socket("127.0.0.1", 10086);
        byte [] bytes=new byte[2];
        for(int i=0;i<bytes.length;i++) {
            bytes[i]= (byte) i;
        }
        socket.getOutputStream().write(bytes);
        socket.shutdownOutput();
        socket.close();
    }
    @SneakyThrows
    public static void main(String[] args) {
        String src="F:\\资料\\本科资料\\现在\\软件实训\\AntShare\\traacker.docx";
        String dest="F:\\资料\\本科资料\\现在\\软件实训\\AntShare\\tmp.docx";
        Client client=new Client();
        int size=32*(1<<10);
        File file=new File(src);
        int len= (int) file.length();
        int cnt=(len-1)/size+1;
        RandomAccessFile read = new RandomAccessFile(src,"rw");
        RandomAccessFile write = new RandomAccessFile(dest,"rw");
        write.setLength(len);
        for(int i=0;i<cnt;i++)
        {
            int finalI = i;
            client.threadPool.execute(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    int ci= finalI;
                    RandomAccessFile read1=new RandomAccessFile(src,"rw");
                    RandomAccessFile write1=new RandomAccessFile(dest,"rw");
                    byte []bytes=new byte[size];
                    read1.seek(ci*size);
                    read1.read(bytes,0,(ci==cnt-1)?len%size:size);
                    write1.seek(ci*size);
                    write1.write(bytes,0, (ci==cnt-1)?len%size:size);
                }
            });
        }
    }
}
