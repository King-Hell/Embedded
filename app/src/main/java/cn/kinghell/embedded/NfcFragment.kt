package cn.kinghell.embedded

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import cn.kinghell.embedded.tools.UserChecker
import kotlinx.android.synthetic.main.fragment_nfc.*
import java.lang.Thread.sleep


class NfcFragment : Fragment(), CheckListener {
    override var checkContext:Context? = null
    private var scanFlag = false

    override fun checkCallback(status: Boolean) {
        super.checkCallback(status)
        if(status)
            button_close.callOnClick()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_nfc, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        checkContext=requireContext()
        button_open.setOnClickListener {
            if (openNfc()) {
                Toast.makeText(context, "NFC开启成功", Toast.LENGTH_SHORT).show()
                button_open.isEnabled = false
                button_close.isEnabled = true
                scanFlag = true
                Thread {
                    while (scanFlag) {
                        val uid = scanNfc()
                        activity!!.runOnUiThread {
                            Toast.makeText(context, "UID:" + uid, Toast.LENGTH_SHORT).show()
                        }
                        UserChecker(this).checkUID(uid)
                        sleep(1000)
                    }
                }.start()
            } else
                Toast.makeText(context, "NFC开启失败", Toast.LENGTH_SHORT).show()
        }
        button_close.setOnClickListener {
            if (closeNfc()) {
                Toast.makeText(context, "NFC关闭成功", Toast.LENGTH_SHORT).show()
                scanFlag = false
                button_close.isEnabled = false
                button_open.isEnabled = true
            } else
                Toast.makeText(context, "NFC关闭失败", Toast.LENGTH_SHORT).show()
        }
    }

    external fun openNfc(): Boolean
    external fun closeNfc(): Boolean
    external fun scanNfc(): String

    companion object {
        init {
            System.loadLibrary("native-nfc")
        }
    }
}
