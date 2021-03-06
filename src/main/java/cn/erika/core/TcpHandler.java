package cn.erika.core;

import cn.erika.handler.DataHead;

import java.io.IOException;

/**
 * 规定一些处理器必须要做的动作
 */
public interface TcpHandler {
    /**
     * 初始化连接
     * 读取配置文件设置属性
     *
     * @param socket 要初始化的Socket对象
     */
    void init(TcpSocket socket) throws IOException;

    /**
     * 连接建立之后的一些必须要做的操作 比如握手
     *
     * @param socket 对应的Socket对象
     * @throws IOException 如果发生IO错误
     */
    void accept(TcpSocket socket) throws IOException;

    /**
     * 读取一些一些字符后将把这些字符交给处理器处理
     *
     * @param socket 对应的Socket对象
     * @param data   读取到的字节 注意 传输的数组的长度为缓冲区的大小 而不是读取到的字节数
     * @param len    读取到的字节数
     * @throws IOException 如果连接中断或者发生IO错误
     */
    void read(TcpSocket socket, byte[] data, int len) throws IOException;

    void deal(TcpSocket socket, DataHead head, byte[] data) throws IOException;

    /**
     * 当需要关闭指定Socket连接的时候 调用此方法
     *
     * @param socket 对应的Socket连接
     */
    void close(TcpSocket socket);
}
