package cn.kinghell.embedded.tools

import kotlin.experimental.or

class MK60Tools {
    private val buffer = byteArrayOf(0xFE.toByte(), 0xE0.toByte(), 0x08, 0x00, 0x00, 0x00, 0x02, 0x0A)
    private var clientSocketThread: ClientSocketThread? = null

    init{
        Thread{
            clientSocketThread = ClientSocketThread.getClientSocket(ClientSocketTools.localIpAddress!!, Const.SERVER_PORT)
        }.start()
    }


    fun start() {
        val data =
            byteArrayOf(0xFE.toByte(), 0xE0.toByte(), 0x0A, 0x74, 0x72, 0x00, 0xFF.toByte(), 0x01, 0xFE.toByte(), 0x0A)
        clientSocketThread!!.outputStream.write(data)
        clientSocketThread!!.outputStream.write(buffer)
    }

    fun stop() {
        val close =
            byteArrayOf(0xFE.toByte(), 0xE0.toByte(), 0x0A, 0x74, 0x72, 0x00, 0xFF.toByte(), 0x00, 0xFE.toByte(), 0x0A)
        clientSocketThread!!.outputStream.write(close)
        buffer[3] = Const.CMD_CLOSE or Const.K60
        buffer[4] = 0x72
        clientSocketThread!!.outputStream.write(buffer)
    }
}