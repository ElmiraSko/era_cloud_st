package com.era.cloud.server;

import com.era.cloud.common.CloudPackage;

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

public class Server {
    public void run() throws Exception {
        EventLoopGroup mainGroup = new NioEventLoopGroup();
        // NioEventLoopGroup - многопоточный цикл событий (пул потоков), который обрабатывает операции ввода-вывода.
        EventLoopGroup workerGroup = new NioEventLoopGroup(); //
      // Запуск сервера начинается с создания объекта b класса ServerBootstrap.
        //Этот объект позволяет сконфигурировать сервер, наполнить его ключевыми
        // компонентами и наконец, запустить. В первую очередь необходимо указать фабрику каналов.
        try {
            ServerBootstrap b = new ServerBootstrap();
            //ServerBootstrap - это вспомогательный класс, который устанавливает сервер.

            b.group(mainGroup, workerGroup) //
                    .channel(NioServerSocketChannel.class) // Здесь мы указываем, чтобы использовать NioServerSocketChannel класс, который используется для создания экземпляра новогоChannel, чтобы принять входящие соединения.
                    .childHandler(new ChannelInitializer<SocketChannel>() { //
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new HandlerForInput(), new HandlerForOutput());
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true); //
            ChannelFuture future = b.bind(8078).sync();
            future.channel().closeFuture().sync();
        } finally {
            mainGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new Server().run();
    }
}
