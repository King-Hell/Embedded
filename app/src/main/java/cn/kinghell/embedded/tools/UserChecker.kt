package cn.kinghell.embedded.tools

import android.util.Log
import cn.kinghell.embedded.CheckListener
import com.squareup.okhttp.*
import java.io.IOException
import java.lang.Thread.sleep

class UserChecker(listener: CheckListener) {
    companion object {
        private val client = OkHttpClient()

        init {
            System.loadLibrary("native-lib")
        }

    }

    external fun startLED(): Boolean
    private val listener = listener
    fun checkUser(username: String) {
        //检查用户名
        check("user", username)
    }

    fun checkUID(uid: String) {
        //检查UID
        check("uid", uid)
    }

    val mk60 = MK60Tools()
    private fun check(type: String, value: String) {
        val body = FormEncodingBuilder().add("type", type).add("value", value).build()
        val request = Request.Builder().url("https://kinghell.cn/embedded/check.php").post(body).build()
        client.newCall(request).enqueue(object : Callback {

            override fun onResponse(response: Response?) {
                val status = response!!.body().string()
                Log.d("用户检查", status)
                if (status == "YES \r\n") {
//                    Thread {
                    startLED()
                    mk60.start()
                    sleep(1000)
                    mk60.stop()
                    startLED()
                    //                   }.start()
                    listener.checkCallback(true)
                } else
                    listener.checkCallback(false)
            }

            override fun onFailure(request: Request?, e: IOException?) {
                Log.e("用户检查", "网络连接失败")
            }
        })
    }
}

