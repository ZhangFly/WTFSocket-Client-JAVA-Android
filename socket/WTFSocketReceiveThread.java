package wtf.socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

class WTFSocketReceiveThread implements Runnable {

    private static final Logger logger = Logger.getLogger("socket");

    private WTFSocketClient wtfSocketClient;

    WTFSocketReceiveThread(WTFSocketClient wtfSocketClient) {
        this.wtfSocketClient = wtfSocketClient;
    }

    @Override
    public void run() {

        try {
            Socket socket = wtfSocketClient.getSocket();

            if (socket.isClosed() || !socket.isConnected()) {
                return;
            }

            int toReadLen = socket.getInputStream().available();

            if (toReadLen <= 0) {
                return;
            }

            byte bytes[] = new byte[toReadLen];
            int readLen = socket.getInputStream().read(bytes);
            String originalStr = new String(bytes, 0, readLen);

            for (String packet : wtfSocketClient.parseAndGetPackets(originalStr)) {

                WTFSocketMsgWrapper msgWrapper = JSON.parseObject(packet, WTFSocketMsgWrapper.class);

                if (msgWrapper.getFrom() == null || msgWrapper.getTo() == null || msgWrapper.getMsgId() == null || msgWrapper.getMsgType() == null) {
                    throw new IOException("protocol err!");
                }

                if (msgWrapper.getMsgType() == 1) {
                    logger.info(String.format("received msg from <%s> to <%s>:\nmsg => %s",
                            msgWrapper.getFrom(),
                            msgWrapper.getTo(),
                            msgWrapper));
                }
                WTFSocketSessionFactory.dispatchMsg(msgWrapper);
            }

        } catch (IOException | JSONException e) {
            wtfSocketClient.clearBuffer();

            WTFSocketSessionFactory.dispatchException(
                    new WTFSocketMsgWrapper(),
                    new WTFSocketException(e.getMessage())
                            .setLocation(this.getClass().getName() + "$run"));
        }
    }
}
