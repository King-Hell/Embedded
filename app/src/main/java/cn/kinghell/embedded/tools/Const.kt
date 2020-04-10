package cn.kinghell.embedded.tools

object Const {

    val LED_FILENAME = "/dev/ledtest"
    val CMD_OPEN = 0x01
    val CMD_RUN:Byte = 0x02
    val CMD_CLOSE:Byte = 0x03
    val CMD_WRITE = 0x04
    val CMD_READ = 0x05

    val SERVER_PORT = 6109

    val K60:Byte = 0x70
    val MATRIX:Byte = 0x10
    val DIGTAL = 0x20
    val MOTOR = 0x30
    val BLIGHT = 0x40
    val NFC = 0x50
    val ZIGBEE = 0x60

    val CITYBUS_HEIGHT = 480
    val CITYBUS_WIDTH = 800
    val TRAFICLIGHT_H = 40
    val TRAFICLIGHT_W = 50
    val BUS_HEIGHT = 50
    val POINTS = intArrayOf(
        200,
        317,
        200,
        263,
        200,
        235,
        200,
        110,
        200,
        60,
        20,
        60,
        20,
        190,
        100,
        190,
        135,
        190,
        321,
        190,
        400,
        190,
        400,
        314
    )

}
