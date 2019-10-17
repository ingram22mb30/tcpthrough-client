package org.wls.tcpthrough.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.wls.tcpthrough.inner.InnerClient;
import org.wls.tcpthrough.model.ManagerProtocolBuf.RegisterProtocol;
import org.wls.tcpthrough.model.ManagerProtocolBuf.ManagerResponse;

public class DataHandler extends ChannelInboundHandlerAdapter {
    public Channel innerChannel;
    private RegisterProtocol registerProtocol;
    private InnerClient innerClient;
    private String channelId;

    public DataHandler(String channelId, RegisterProtocol registerProtocol){
        this.channelId = channelId;
        this.registerProtocol = registerProtocol;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        System.out.println("DATA CLIENT 有数据来了");
        if(innerChannel != null && innerChannel.isActive()){
            innerChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if(future.isSuccess()){
//                        System.out.println("DATA client 数据成功写到 innerchannel");
                        ctx.channel().read();
                    } else {
//                        System.out.println("DATA client 没有写到 innerchannel");
                    }
                }
            });
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Data channel start to register" + channelId);
        ByteBuf byteBuf = Unpooled.copiedBuffer(channelId.getBytes());
        DataHandler self = this;
        ctx.writeAndFlush(byteBuf).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if(future.isSuccess()){
//                    System.out.println("Data Client 成功发送channel id");
                    innerClient = new InnerClient(registerProtocol, ctx.channel(), self);
                    innerClient.run();
                } else {
//                    System.out.println("Data channel register error");
                }
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(innerClient != null){
            innerClient.channelFuture.channel().close();
        }
    }
}

