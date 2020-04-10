package cn.kinghell.embedded.tools

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import java.net.NetworkInterface
import java.net.SocketException
import kotlin.experimental.and

object ClientSocketTools {

    /**
     * get localhost IP address
     *
     * @return IP address
     */
    val localIpAddress: String?
        get() {
            try {
                val en = NetworkInterface.getNetworkInterfaces()
                while (en.hasMoreElements()) {
                    val intf = en.nextElement()
                    val enumIpAddr = intf.inetAddresses
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress && !inetAddress.isLinkLocalAddress) {
                            return inetAddress.hostAddress.toString()
                        }
                    }
                }
            } catch (ex: SocketException) {
                Log.e("WifiPreferenceIpAddress", ex.toString())
            }

            return null
        }


    /**
     * 获取屏幕的Density
     *
     * @param context
     * 上下文对象
     * @return density
     */
    fun getScreenDensity(context: Context): Float {
        try {
            val dm = DisplayMetrics()
            val manager = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
            manager.defaultDisplay.getMetrics(dm)
            return dm.density
        } catch (ex: Exception) {

        }

        return 1.0f
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * 获取屏幕的DisplayMetrics
     *
     * @param context
     * 上下文对象
     * @return DisplayMetrics
     */
    fun getDisplayMetrics(context: Context): DisplayMetrics? {
        try {
            val dm = DisplayMetrics()
            val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            manager.defaultDisplay.getMetrics(dm)
            return dm
        } catch (ex: Exception) {

        }

        return null
    }


    /**
     * java字节码转字符串
     * @param b
     * @return
     */
    fun byte2hex(b: ByteArray, len: Int): String { //一个字节的数，

        // 转成16进制字符串

        var hs = ""
        var tmp: String? = ""
        for (n in 0 until len) {
            //整数转成十六进制表示

            tmp = java.lang.Integer.toHexString((b[n] and 0XFF as Byte).toInt())
            if (tmp!!.length == 1) {
                hs = hs + "0" + tmp
            } else {
                hs = hs + tmp
            }
        }
        tmp = null
        return hs.toUpperCase() //转成大写

    }

    /**
     * java字节码转字符串
     * @param b
     * @return
     */
    fun byte2hex(b: ByteArray, start: Int, len: Int): String { //一个字节的数，

        // 转成16进制字符串

        var hs = ""
        var tmp: String? = ""
        for (n in start until len) {
            //整数转成十六进制表示

            tmp = java.lang.Integer.toHexString((b[n] and 0XFF as Byte).toInt())
            if (tmp!!.length == 1) {
                hs = hs + "0" + tmp
            } else {
                hs = hs + tmp
            }
        }
        tmp = null
        return hs.toUpperCase() //转成大写

    }

    /**
     * 字节转换为整型
     *
     * @param b 字节（至少4个字节）
     * @param index 开始位置
     * @return
     */
    fun byte2int(b: ByteArray, index: Int): Int {
        var temp: Int
        temp = b[index + 0].toInt()
        temp = temp and 0xff
        temp = temp or (b[index + 1].toInt() shl 8)
        temp = temp and 0xffff
        temp = temp or (b[index + 2].toInt() shl 16)
        temp = temp and 0xffffff
        temp = temp or (b[index + 3].toInt() shl 24)
        return temp
    }

    /**
     * 字节转换为浮点
     *
     * @param b 字节（至少4个字节）
     * @param index 开始位置
     * @return
     */
    fun byte2float(b: ByteArray, index: Int): Float {
        var temp: Int
        temp = b[index + 0].toInt()
        temp = temp and 0xff
        temp = temp or (b[index + 1].toInt() shl 8)
        temp = temp and 0xffff
        temp = temp or (b[index + 2].toInt() shl 16)
        temp = temp and 0xffffff
        temp = temp or (b[index + 3].toInt() shl 24)
        return java.lang.Float.intBitsToFloat(temp)
    }
}
