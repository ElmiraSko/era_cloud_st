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
        EventLoopGroup mainGroup = new NioEventLoopGroup(); //
        EventLoopGroup workerGroup = new NioEventLoopGroup(); //
        try {
            ServerBootstrap b = new ServerBootstrap(); //

            b.group(mainGroup, workerGroup) //
                    .channel(NioServerSocketChannel.class) //
                    .childHandler(new ChannelInitializer<SocketChannel>() { //
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new HandlerForInput());
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
