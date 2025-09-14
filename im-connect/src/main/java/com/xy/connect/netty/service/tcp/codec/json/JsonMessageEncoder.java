package com.xy.connect.netty.service.tcp.codec.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.core.model.IMConnectMessage;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * 自定义JSON编码器
 */
public class JsonMessageEncoder extends MessageToMessageEncoder<IMConnectMessage<?>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, IMConnectMessage<?> msg, List<Object> out) throws Exception {
        byte[] bytes = MAPPER.writeValueAsBytes(msg);
        // pipeline 已有 LengthFieldPrepender，会在最终写出前自动添加长度
        out.add(Unpooled.wrappedBuffer(bytes));
    }
}