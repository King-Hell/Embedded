package cn.kinghell.embedded.tools

import kotlin.experimental.or

class MatrixTools {

    private val TAG = "MatrixActvity"
    private var clientSocketThread: ClientSocketThread? = null
    private val buffer = byteArrayOf(0xFE.toByte(), 0xE0.toByte(), 0x08, 0x00, 0x00, 0x00, 0x01, 0x0A)
    init{
        Thread{
            clientSocketThread = ClientSocketThread.getClientSocket(ClientSocketTools.localIpAddress!!, Const.SERVER_PORT)
        }.start()
    }


    /**
     * 处理点阵控制按钮的点击事件
     */
    fun show(){
        buffer[3] = Const.CMD_RUN or Const.MATRIX
        buffer[4] = 0x72
        clientSocketThread!!.outputStream.write(buffer)
    }

    fun stop(){
        buffer[3] = Const.MATRIX or Const.CMD_CLOSE
        buffer[4] = 0x72
        clientSocketThread!!.outputStream.write(buffer)
    }

}