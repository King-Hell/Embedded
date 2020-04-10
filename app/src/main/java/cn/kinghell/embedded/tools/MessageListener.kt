package cn.kinghell.embedded.tools

interface MessageListener {
    fun Message(message: ByteArray, message_len: Int)
}
