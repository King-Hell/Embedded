package cn.kinghell.embedded

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import androidx.core.content.FileProvider
import java.io.File
import android.graphics.Point
import android.hardware.Camera
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.arcsoft.arcfacedemo.faceserver.CompareResult
import com.arcsoft.arcfacedemo.faceserver.FaceServer
import com.arcsoft.arcfacedemo.model.DrawInfo
import com.arcsoft.arcfacedemo.model.FacePreviewInfo
import com.arcsoft.arcfacedemo.util.ConfigUtil
import com.arcsoft.arcfacedemo.util.DrawHelper
import com.arcsoft.arcfacedemo.util.camera.CameraHelper
import com.arcsoft.arcfacedemo.util.camera.CameraListener
import com.arcsoft.arcfacedemo.util.face.FaceHelper
import com.arcsoft.arcfacedemo.util.face.FaceListener
import com.arcsoft.arcfacedemo.util.face.RequestFeatureStatus
import com.arcsoft.arcfacedemo.widget.FaceRectView
import com.arcsoft.arcfacedemo.widget.ShowFaceInfoAdapter
import com.arcsoft.face.*
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_face.*
import kotlinx.android.synthetic.main.fragment_password.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [FaceFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [FaceFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class FaceFragment : Fragment(), ViewTreeObserver.OnGlobalLayoutListener {
    // TODO: Rename and change types of parameters
    private var listener: OnFragmentInteractionListener? = null
    private val TAKE_PHOTO = 1
    private var firstActive = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_face, container, false)
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        //本地人脸库初始化
        register_button.setOnClickListener{register()}
        clear_button.setOnClickListener { clearFaces() }
        FaceServer.getInstance().init(requireContext())

        //在布局结束后才做初始化操作
        texture_preview.viewTreeObserver.addOnGlobalLayoutListener(this)
        switchLivenessDetect = switch_liveness_detect
        switchLivenessDetect!!.isChecked = livenessDetect
        switchLivenessDetect!!.setOnCheckedChangeListener { buttonView, isChecked -> livenessDetect = isChecked }
        compareResultList = ArrayList<CompareResult>()
        adapter = ShowFaceInfoAdapter(compareResultList, requireContext())
        recycler_view_person.setAdapter(adapter)
        val dm = resources.displayMetrics
        val spanCount = (dm.widthPixels / (resources.displayMetrics.density * 100 + 0.5f)).toInt()
        recycler_view_person.setLayoutManager(GridLayoutManager(requireContext(), spanCount))
        recycler_view_person.setItemAnimator(DefaultItemAnimator())
    }

    fun initEngine() {
        faceEngine = FaceEngine()
        afCode = faceEngine!!.init(
            context, FaceEngine.ASF_DETECT_MODE_VIDEO, FaceEngine.ASF_OP_270_ONLY, 16, 1,
            FaceEngine.ASF_FACE_RECOGNITION or FaceEngine.ASF_FACE_DETECT or FaceEngine.ASF_LIVENESS)
        if (afCode == ErrorInfo.MERR_ASF_NOT_ACTIVATED && firstActive) {
            faceEngine!!.active(
                context,
                "J7Y2EFLJFSMg9ArWhsPhaNhWdpnw93WQ8L6LrdMq2dm1",
                "8JFkUNk99SHYnczaRevEL8JNmiXZF6zvvnUsk9HW5HYN"
            )
            firstActive = false
            initEngine()
        } else
            if (afCode == ErrorInfo.MOK)
                requireActivity().runOnUiThread { Toast.makeText(context, "引擎初始化成功", Toast.LENGTH_SHORT).show() }
            else
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        context,
                        getString(R.string.init_failed, afCode),
                        Toast.LENGTH_SHORT
                    ).show()
                }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }


    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    fun unInitEngine() {
        if (afCode == 0) {
            afCode = faceEngine!!.unInit()
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    private val TAG = "RegisterAndRecognize"
    private val MAX_DETECT_NUM = 10
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private val WAIT_LIVENESS_INTERVAL = 50L
    private var cameraHelper: CameraHelper? = null
    private var drawHelper: DrawHelper? = null
    private var previewSize: Camera.Size? = null
    /**
     * 优先打开的摄像头
     */
    private val cameraID = Camera.CameraInfo.CAMERA_FACING_FRONT
    private var faceEngine: FaceEngine? = null
    private var faceHelper: FaceHelper? = null
    private var compareResultList: MutableList<CompareResult>? = null
    private var adapter: ShowFaceInfoAdapter? = null
    /**
     * 活体检测的开关
     */
    private var livenessDetect = true

    /**
     * 注册人脸状态码，准备注册
     */
    private val REGISTER_STATUS_READY = 0
    /**
     * 注册人脸状态码，注册中
     */
    private val REGISTER_STATUS_PROCESSING = 1
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private val REGISTER_STATUS_DONE = 2

    private var registerStatus = REGISTER_STATUS_DONE

    private var afCode = -1
    private val requestFeatureStatusMap = ConcurrentHashMap<Int, Int>()
    private val livenessMap = ConcurrentHashMap<Int, Int>()
    private val getFeatureDelayedDisposables = CompositeDisposable()
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    /**
     * 绘制人脸框的控件
     */

    private var switchLivenessDetect: Switch? = null

    private val ACTION_REQUEST_PERMISSIONS = 0x001
    private val SIMILAR_THRESHOLD = 0.8f
    /**
     * 所需的所有权限信息
     */
    private val NEEDED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_PHONE_STATE)

    override fun onDestroy() {

        if (cameraHelper != null) {
            cameraHelper!!.release()
            cameraHelper = null
        }

        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            synchronized(faceHelper!!) {
                unInitEngine()
            }
            ConfigUtil.setTrackId(requireContext(), faceHelper!!.getCurrentTrackId())
            faceHelper!!.release()
        } else {
            unInitEngine()
        }
        getFeatureDelayedDisposables.dispose()
        getFeatureDelayedDisposables.clear()
        FaceServer.getInstance().unInit()
        super.onDestroy()
    }

    private fun checkPermissions(neededPermissions: Array<String>?): Boolean {
        if (neededPermissions == null || neededPermissions.size == 0) {
            return true
        }
        var allGranted = true
        for (neededPermission in neededPermissions) {
            allGranted = allGranted and (ContextCompat.checkSelfPermission(
                requireContext(),
                neededPermission
            ) == PackageManager.PERMISSION_GRANTED)
        }
        return allGranted
    }

    private fun initCamera() {
        val metrics = DisplayMetrics()
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics)

        val faceListener = object : FaceListener {
            override fun onFail(e: Exception) {
                Log.e(TAG, "onFail: " + e.message)
            }

            //请求FR的回调
            override fun onFaceFeatureInfoGet(faceFeature: FaceFeature?, requestId: Int) {
                //FR成功
                if (faceFeature != null) {
                    //                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);

                    //不做活体检测的情况，直接搜索
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId)
                    } else if (livenessMap[requestId] != null && livenessMap[requestId] == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId)
                    } else if (livenessMap[requestId] != null && livenessMap[requestId] == LivenessInfo.UNKNOWN) {
                        getFeatureDelayedDisposables.add(
                            Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                .subscribe(object : Consumer<Long> {
                                    override fun accept(aLong: Long?) {
                                        onFaceFeatureInfoGet(faceFeature, requestId)
                                    }
                                })
                        )
                    } else {
                        requestFeatureStatusMap[requestId] = RequestFeatureStatus.NOT_ALIVE
                    }//活体检测失败
                    //活体检测未出结果，延迟100ms再执行该函数
                    //活体检测通过，搜索特征

                } else {
                    requestFeatureStatusMap[requestId] = RequestFeatureStatus.FAILED
                }//FR 失败
            }

        }


        val cameraListener = object : CameraListener {
            override fun onCameraOpened(camera: Camera, cameraId: Int, displayOrientation: Int, isMirror: Boolean) {
                previewSize = camera.parameters.previewSize
                drawHelper = DrawHelper(
                    previewSize!!.width,
                    previewSize!!.height,
                    texture_preview.width,
                    texture_preview.height,
                    displayOrientation,
                    cameraId,
                    isMirror
                )

                faceHelper = FaceHelper.Builder()
                    .faceEngine(faceEngine)
                    .frThreadNum(MAX_DETECT_NUM)
                    .previewSize(previewSize)
                    .faceListener(faceListener)
                    .currentTrackId(ConfigUtil.getTrackId(requireContext().getApplicationContext()))
                    .build()
            }


            override fun onPreview(nv21: ByteArray, camera: Camera) {
                face_rect_view.clearFaceInfo()

                val facePreviewInfoList = faceHelper!!.onPreviewFrame(nv21)
                if (facePreviewInfoList != null && drawHelper != null) {
                    val drawInfoList = ArrayList<DrawInfo>()
                    for (i in facePreviewInfoList.indices) {
                        val name = faceHelper!!.getName(facePreviewInfoList.get(i).getTrackId())
                        drawInfoList.add(
                            DrawInfo(
                                facePreviewInfoList.get(i).getFaceInfo().getRect(),
                                GenderInfo.UNKNOWN,
                                AgeInfo.UNKNOWN_AGE,
                                LivenessInfo.UNKNOWN,
                                if (name == null) facePreviewInfoList.get(i).getTrackId().toString() else name
                            )
                        )
                    }
                    drawHelper!!.draw(face_rect_view, drawInfoList)
                }
                if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList!!.size > 0) {
                    registerStatus = REGISTER_STATUS_PROCESSING
                    Observable.create(object : ObservableOnSubscribe<Boolean> {
                        override fun subscribe(emitter: ObservableEmitter<Boolean>) {
                            val success = FaceServer.getInstance().register(
                                requireContext(),
                                nv21.clone(),
                                previewSize!!.width,
                                previewSize!!.height,
                                "registered " + faceHelper!!.getCurrentTrackId()
                            )
                            emitter.onNext(success)
                        }
                    })
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(object : Observer<Boolean> {
                            override fun onSubscribe(d: Disposable) {

                            }

                            override fun onNext(success: Boolean) {
                                val result = if (success) "register success!" else "register failed!"
                                Toast.makeText(requireContext(), result, Toast.LENGTH_SHORT).show()
                                registerStatus = REGISTER_STATUS_DONE
                            }

                            override fun onError(e: Throwable) {
                                Toast.makeText(
                                    requireContext(),
                                    "register failed!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                registerStatus = REGISTER_STATUS_DONE
                            }

                            override fun onComplete() {

                            }
                        })
                }
                clearLeftFace(facePreviewInfoList)

                if (facePreviewInfoList != null && facePreviewInfoList.size > 0 && previewSize != null) {

                    for (i in facePreviewInfoList.indices) {
                        if (livenessDetect) {
                            livenessMap[facePreviewInfoList.get(i).getTrackId()] =
                                facePreviewInfoList.get(i).getLivenessInfo().getLiveness()
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求FR（可根据需要添加其他判断以限制FR次数），
                         * FR回传的人脸特征结果在[FaceListener.onFaceFeatureInfoGet]中回传
                         */
                        if (requestFeatureStatusMap[facePreviewInfoList.get(i).getTrackId()] == null || requestFeatureStatusMap[facePreviewInfoList!!.get(
                                i
                            ).getTrackId()] === RequestFeatureStatus.FAILED
                        ) {
                            requestFeatureStatusMap[facePreviewInfoList.get(i).getTrackId()] =
                                RequestFeatureStatus.SEARCHING
                            faceHelper!!.requestFaceFeature(
                                nv21,
                                facePreviewInfoList.get(i).getFaceInfo(),
                                previewSize!!.width,
                                previewSize!!.height,
                                FaceEngine.CP_PAF_NV21,
                                facePreviewInfoList.get(i).getTrackId()
                            )
                            //                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackId());
                        }
                    }
                }
            }

            override fun onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ")
            }

            override fun onCameraError(e: Exception) {
                Log.i(TAG, "onCameraError: " + e.message)
            }

            override fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int) {
                if (drawHelper != null) {
                    drawHelper!!.setCameraDisplayOrientation(displayOrientation)
                }
                Log.i(TAG, "onCameraConfigurationChanged: $cameraID  $displayOrientation")
            }
        }

        cameraHelper = CameraHelper.Builder()
            .previewViewSize(Point(texture_preview.measuredWidth, texture_preview.measuredHeight))
            .rotation(requireActivity().windowManager.getDefaultDisplay().getRotation())
            .specificCameraId(cameraID ?: Camera.CameraInfo.CAMERA_FACING_FRONT)
            .isMirror(false)
            .previewOn(texture_preview)
            .cameraListener(cameraListener)
            .build()
        cameraHelper!!.init()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            var isAllGranted = true
            for (grantResult in grantResults) {
                isAllGranted = isAllGranted and (grantResult == PackageManager.PERMISSION_GRANTED)
            }
            if (isAllGranted) {
                initEngine()
                initCamera()
                if (cameraHelper != null) {
                    cameraHelper!!.start()
                }
            } else {
                Toast.makeText(requireContext(), R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private fun clearLeftFace(facePreviewInfoList: List<FacePreviewInfo>?) {
        val keySet = requestFeatureStatusMap.keys
        if (compareResultList != null) {
            for (i in compareResultList!!.indices.reversed()) {
                if (!keySet.contains(compareResultList!![i].getTrackId())) {
                    compareResultList!!.removeAt(i)
                    adapter!!.notifyItemRemoved(i)
                }
            }
        }
        if (facePreviewInfoList == null || facePreviewInfoList.size == 0) {
            requestFeatureStatusMap.clear()
            livenessMap.clear()
            return
        }

        for (integer in keySet) {
            var contained = false
            for (facePreviewInfo in facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() === integer) {
                    contained = true
                    break
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(integer)
                livenessMap.remove(integer)
            }
        }

    }

    private fun searchFace(frFace: FaceFeature, requestId: Int) {
        Observable
            .create(object : ObservableOnSubscribe<CompareResult> {
                override fun subscribe(emitter: ObservableEmitter<CompareResult>) {
                    //                        Log.i(TAG, "subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                    val compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace)
                    //                        Log.i(TAG, "subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    emitter.onNext(compareResult)
                }
            })
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<CompareResult> {
                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(compareResult: CompareResult) {
                    if (compareResult == null || compareResult!!.getUserName() == null) {
                        requestFeatureStatusMap[requestId!!] = RequestFeatureStatus.FAILED
                        faceHelper!!.addName(requestId, "VISITOR $requestId")
                        return
                    }

                    //                        Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                    if (compareResult!!.getSimilar() > SIMILAR_THRESHOLD) {
                        var isAdded = false
                        if (compareResultList == null) {
                            requestFeatureStatusMap[requestId!!] = RequestFeatureStatus.FAILED
                            faceHelper!!.addName(requestId, "VISITOR $requestId")
                            return
                        }
                        for (compareResult1 in compareResultList!!) {
                            if (compareResult1.getTrackId() === requestId) {
                                isAdded = true
                                break
                            }
                        }
                        if (!isAdded) {
                            //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                            if (compareResultList!!.size >= MAX_DETECT_NUM) {
                                compareResultList!!.removeAt(0)
                                adapter!!.notifyItemRemoved(0)
                            }
                            //添加显示人员时，保存其trackId
                            compareResult!!.setTrackId(requestId)
                            compareResultList!!.add(compareResult)
                            adapter!!.notifyItemInserted(compareResultList!!.size - 1)
                        }
                        requestFeatureStatusMap[requestId!!] = RequestFeatureStatus.SUCCEED
                        faceHelper!!.addName(requestId, compareResult!!.getUserName())

                    } else {
                        requestFeatureStatusMap[requestId!!] = RequestFeatureStatus.FAILED
                        faceHelper!!.addName(requestId, "VISITOR $requestId")
                    }
                }

                override fun onError(e: Throwable) {
                    requestFeatureStatusMap[requestId!!] = RequestFeatureStatus.FAILED
                }

                override fun onComplete() {

                }
            })
    }


    /**
     * 将准备注册的状态置为[.REGISTER_STATUS_READY]
     *
     * @param view 注册按钮
     */
    fun register() {
        if (registerStatus == REGISTER_STATUS_DONE) {
            registerStatus = REGISTER_STATUS_READY
        }
    }
    fun clearFaces() {
        val faceNum = FaceServer.getInstance().getFaceNumber(requireContext())
        if (faceNum == 0) {
            Toast.makeText(requireContext(), R.string.no_face_need_to_delete, Toast.LENGTH_SHORT).show()
        } else {
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.notification)
                .setMessage(getString(R.string.confirm_delete, faceNum))
                .setPositiveButton(R.string.ok) { dialog, which ->
                    val deleteCount = FaceServer.getInstance().clearAllFaces(requireContext())
                    Toast.makeText(requireContext(), "$deleteCount faces cleared!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.cancel, null)
                .create()
            dialog.show()
        }
    }

    /**
     * 在[.previewView]第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    override fun onGlobalLayout() {
        texture_preview.viewTreeObserver.removeOnGlobalLayoutListener(this)
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(requireActivity(), NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS)
        } else {
            initEngine()
            initCamera()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if(hidden)
            unInitEngine()
        else
            initEngine()
    }
}
