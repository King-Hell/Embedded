package cn.kinghell.embedded

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import cn.bertsir.zbar.QrConfig
import cn.bertsir.zbar.QrManager
import cn.kinghell.embedded.tools.UserChecker

class QRFragment : Fragment(),CheckListener {

    override var checkContext: Context? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        scanQR()
        return inflater.inflate(R.layout.fragment_qr, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        checkContext=requireContext()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            scanQR()
        }
    }

    fun scanQR(){
        val qrConfig = QrConfig.Builder()
            .setDesText("(识别二维码)")//扫描框下文字
            .setShowDes(false)//是否显示扫描框下面文字
            .setShowLight(true)//显示手电筒按钮
            //.setShowTitle(true)//显示Title
            .setShowAlbum(true)//显示从相册选择按钮
            //.setCornerColor(Color.WHITE)//设置扫描框颜色
            //.setLineColor(Color.WHITE)//设置扫描线颜色
            //.setLineSpeed(QrConfig.LINE_MEDIUM)//设置扫描线速度
            .setScanType(QrConfig.TYPE_QRCODE)//设置扫码类型（二维码，条形码，全部，自定义，默认为二维码）
            .setScanViewType(QrConfig.SCANVIEW_TYPE_QRCODE)//设置扫描框类型（二维码还是条形码，默认为二维码）
            //.setCustombarcodeformat(QrConfig.BARCODE_I25)//此项只有在扫码类型为TYPE_CUSTOM时才有效
            .setPlaySound(true)//是否扫描成功后bi~的声音
            .setNeedCrop(true)//从相册选择二维码之后再次截取二维码
            //.setDingPath(R.raw.test)//设置提示音(不设置为默认的Ding~)
            //.setIsOnlyCenter(true)//是否只识别框中内容(默认为全屏识别)
            .setTitleText("扫描二维码")//设置Tilte文字
            //.setTitleBackgroudColor(Color.BLUE)//设置状态栏颜色
            //.setTitleTextColor(Color.BLACK)//设置Title文字颜色
            .setShowZoom(false)//是否手动调整焦距
            .setAutoZoom(false)//是否自动调整焦距
            .setScreenOrientation(QrConfig.SCREEN_LANDSCAPE)//设置屏幕方向
            .create()
        QrManager.getInstance().init(qrConfig).startScan(activity) { result ->
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
            UserChecker(this).checkUser(result)
        }
    }
}
