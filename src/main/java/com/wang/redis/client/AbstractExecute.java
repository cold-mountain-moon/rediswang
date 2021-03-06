package com.wang.redis.client;

import com.wang.redis.Command.Command;
import com.wang.redis.connection.Connection;
import com.wang.redis.io.RedisInputStream;
import com.wang.redis.io.RedisOutputStream;
import com.wang.redis.transmission.TransmissionData;

import java.io.UnsupportedEncodingException;
import java.util.List;

public abstract class AbstractExecute<T> implements Execute<T> {


    @Override
    public T doExecute(Connection connection,Command command,Object ...params){
        T result;

        try {
            send(connection.getOutputStream(), command, params);
            connection.getOutputStream().flush();

            result = (T) receive(connection.getInputStream(), command, params);
            connection.getInputStream().clear();

        } catch (Exception e) {
            throw new RuntimeException("command execute failed!", e);
        }
        connection.close();
        return result;
    }


    protected static void send(RedisOutputStream outputStream, Command command, Object... arguments) throws Exception {
        String commandString = command.name();
        if (command.name().indexOf(TransmissionData.COMMAND_SEPARATOR) > 0) {
            commandString = command.name().replace(TransmissionData.COMMAND_SEPARATOR, TransmissionData.SPACE);
        }
        byte[][] argumentBytes = new byte[arguments.length][];
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof byte[]) {
                argumentBytes[i] = (byte[]) arguments[i];
            } else if (List.class.isAssignableFrom(arguments[i].getClass())) {
                List<?> list = (List<?>) arguments[i];
                byte[][] extendArgumentBytes = new byte[arguments.length + list.size() - 1][];
                System.arraycopy(argumentBytes, 0, extendArgumentBytes, 0, i);
                for (int j = 0; j < list.size(); j++) {
                    extendArgumentBytes[i++] = stringToBytes(list.get(j).toString());
                }
                argumentBytes = extendArgumentBytes;
            } else if (arguments[i].getClass().isArray()) {
                Object[] array = (Object[]) arguments[i];
                byte[][] extendArgumentBytes = new byte[arguments.length + array.length - 1][];
                System.arraycopy(argumentBytes, 0, extendArgumentBytes, 0, i);
                for (int j = 0; j < array.length; j++) {
                    extendArgumentBytes[i++] = stringToBytes(array[j].toString());
                }
                argumentBytes = extendArgumentBytes;
            } else {
                argumentBytes[i] = stringToBytes(arguments[i].toString());
            }
        }
        TransmissionData.sendCommand(outputStream, stringToBytes(commandString), argumentBytes);
    }

    //子类实现该方法，返回不同的结果
    protected abstract Object receive(RedisInputStream inputStream, Command command, Object... arguments) throws Exception;


    public static byte[] stringToBytes(String s){
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
