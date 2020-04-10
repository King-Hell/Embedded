package cn.kinghell.embedded

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.kinghell.embedded.tools.MK60Tools
import cn.kinghell.embedded.tools.UserChecker
import kotlinx.android.synthetic.main.fragment_password.*


class PasswordFragment : Fragment(), CheckListener {
    override var checkContext: Context? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_password, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        checkContext = requireContext()
        //val mk60 = MK60Tools()
        button_check.setOnClickListener {
            UserChecker(this).checkUser(editText_password.text.toString())
/*            if (checkUser(editText.text.toString()))
                if (startLED()) {
                    Toast.makeText(context, "LED开启成功", Toast.LENGTH_SHORT).show()
                    mk60.start()
                    sleep(5000)
                    mk60.stop()
                } else {
                    Toast.makeText(context, "LED开启失败", Toast.LENGTH_SHORT).show()

 */
        }
    }

}
