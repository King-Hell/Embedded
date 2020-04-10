package cn.kinghell.embedded.tools

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import kotlin.experimental.and

class ClientSocketThread
/** constructor  */
private constructor(ip: String, port: Int) : Thread() {
    private var socket: Socket? = null
    private var listener: MessageListener? = null
    private val buffer_size = 64

    /**
     * get inputstream from the ClientSocketThread
     *
     * @return InputStream
     * @throws Exception
     */
    private val inputStream: InputStream
        @Throws(Exception::class)
        get() = socket!!.getInputStream()

    /**
     * get outputstream from ClientSocketThread
     *
     * @return OutputStream
     * @throws IOException
     */
    val outputStream: OutputStream
        @Throws(Exception::class)
        get() = socket!!.getOutputStream()

    init {
        try {
            isConnected = true
            socket = Socket(ip, port)
        } catch (e: IOException) {
            isConnected = false
            e.printStackTrace()
        }

    }

    /** set a listener to report message received  */
    fun setListener(listener: MessageListener) {
        this.listener = listener
    }

    fun release() {
        this.interrupt()
        isConnected = false
        clientSocket = null
        socket = null
    }

    override fun run() {
        var len = buffer_size
        while (!Thread.interrupted() && isConnected!!) {
            val buffer = ByteArray(buffer_size)
            try {
                len = this.inputStream.read(buffer, 0, buffer_size)
                FrameFilter(buffer, len)
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                isConnected = false
                clientSocket = null
            } catch (e: Exception) {
                isConnected = false
                clientSocket = null
            }

        }
    }

    /**
     * Frame filter
     *
     * @param buffer
     * frame data
     * @param len
     * frame lenth
     * egg: FE E0 0B 55 72 00 6E 99 E6 AF 0A
     */
    fun FrameFilter(buffer: ByteArray, len: Int) {
        var len = len
        var index = 0
        var frmlen = 0
        var ch: Byte
        var status: Byte = 0
        var sensordata: ByteArray? = null
        while (len-- > 0) {
            ch = buffer[index++]
            when (status) {
                0.toByte()-> if (ch == 0xFE.toByte())
                    status = 1
                1.toByte()  -> if ((ch and 0xE0.toByte()) == 0xE0.toByte())
                    status = 2
                else
                    status = 0
                2.toByte() -> {
                    frmlen = ch.toInt()
                    if (frmlen < buffer_size) {
                        frmlen -= 6
                        index++
                        index++
                        sensordata = ByteArray(frmlen)
                        System.arraycopy(buffer, index, sensordata, 0, frmlen)
                        index = index + frmlen
                        status = 3
                    } else
                        status = 0
                }
                3.toByte() -> {
                    if (this.listener != null)
                        this.listener!!.Message(sensordata!!, frmlen)
                    status = 0
                }
            }
        }
    }

    companion object {
        private var clientSocket: ClientSocketThread? = null
        var isConnected: Boolean? = null

        /** get client socket thread instance  */
        fun getClientSocket(ip: String, port: Int): ClientSocketThread {
            if (clientSocket == null) {
                clientSocket = ClientSocketThread(ip, port)
                clientSocket!!.start()
            }
            return clientSocket!!
        }
    }
}