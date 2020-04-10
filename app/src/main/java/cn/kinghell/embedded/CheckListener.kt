package cn.kinghell.embedded

import android.content.Context
import android.content.DialogInterface
import android.os.Looper
import androidx.appcompat.app.AlertDialog

interface CheckListener {
    var checkContext: Context?
    fun checkCallback(status: Boolean) {
        Looper.prepare()
        val dialog = AlertDialog.Builder(checkContext!!)
        dialog.setTitle("验证").setPositiveButton("确定", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, which: Int) {
            }
        })
        if (status) {
            dialog.setMessage("身份确认")
        }else{
            dialog.setMessage("身份拒绝")
        }
        dialog.show()
        Looper.loop()
    }
}