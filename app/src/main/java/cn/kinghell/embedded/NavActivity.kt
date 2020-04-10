package cn.kinghell.embedded

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import cn.bertsir.zbar.QrConfig
import cn.bertsir.zbar.QrManager
import com.arcsoft.face.FaceEngine
import kotlinx.android.synthetic.main.activity_nav.*

class NavActivity : AppCompatActivity(),FaceFragment.OnFragmentInteractionListener {

    //Fragments
    private val passwordFragment = PasswordFragment()
    private val qrFragment = QRFragment()
    private val faceFragment = FaceFragment()
    private val nfcFragment = NfcFragment()
    private var currentFragment = Fragment()


    private fun setFragment(fragment: Fragment) {
        if (currentFragment != fragment) {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.hide(currentFragment)
            currentFragment = fragment
            if (!fragment.isAdded)
                transaction.add(R.id.content, fragment)
            transaction.show(fragment).commit()

        }
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_passwd -> {
                setFragment(passwordFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_qr -> {
                setFragment(qrFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_face -> {
                setFragment(faceFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_nfc -> {
                setFragment(nfcFragment)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        setFragment(passwordFragment)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }
}
