package com.gruppozenit.messagesapp.message

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.DisplayMetrics
import android.util.Log
import android.view.TouchDelegate
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.gruppozenit.messagesapp.R
import com.gruppozenit.messagesapp.adapter.FilesAdapter
import com.gruppozenit.messagesapp.fileView.FileViewActivity
import com.gruppozenit.messagesapp.fileView.NoOrientationConfigChangeFileViewActivity
import com.gruppozenit.messagesapp.model.messageDetailsModels.GetMessageDetailsInput
import com.gruppozenit.messagesapp.model.messageDetailsModels.GetMessageDetailsResponse
import com.gruppozenit.messagesapp.model.messageDetailsModels.MessageAttachment
import com.gruppozenit.messagesapp.network.provider.RetrofitProviderJava
import com.gruppozenit.messagesapp.utils.*
import com.squareup.picasso.Picasso
import com.zenith.eteam.chronology.chronology1.progressdialog.WorkingProgressDialog
import kotlinx.android.synthetic.main.activity_message_details.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.math.roundToInt

class MessageDetailsActivity : AppCompatActivity(), FilesAdapter.ItemClick, View.OnClickListener {

    private var msg_id: Int? = null
    private var msg_date: String? = null
    private var dialog: WorkingProgressDialog? = null
    private var prefManager: PrefManager? = null
    private val requestCode = 501

    private var getMessageDetailsInput: GetMessageDetailsInput? = null

    private var adapterEntries: FilesAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_details)

        init()
        if (Utils.isInternetAvailable(this)) {
            mGetMessageDetails()
        } else {
            finish()
        }
    }

    private fun mGetMessageDetails() {


        showProgressDialog()

        getMessageDetailsInput = GetMessageDetailsInput()
        getMessageDetailsInput!!.deviceId = prefManager!!.deviceID
        getMessageDetailsInput!!.messaggio_id = msg_id!!.toInt()


        val api = RetrofitProviderJava.getInstance().retrofit!!.create(com.zenith.eteam.chronology.chronology1.network.api.Api::class.java)
        val apiCall = api.mGetMessageDetails(prefManager!!.fcmToken, getMessageDetailsInput!!)
        apiCall.enqueue(object : Callback<GetMessageDetailsResponse> {
            override fun onResponse(call: Call<GetMessageDetailsResponse>, response: Response<GetMessageDetailsResponse>) {


                if (response.body() != null) {

                    mainscroll.visibility=View.VISIBLE
                    hideProgressDialog()

                    if (response.body()!!.result.success == true) {

                        if (response.body()!!.result.data != null && response.body()!!.result.data.messageDetailsList != null) {

                            txt_msgitem_date.text = msg_date
                            if (response.body()!!.result.data.messageDetailsList.user != null) {
                                txt_msgitem_name.text = response.body()!!.result.data.messageDetailsList.user
                            }
                            profile_image.visibility = View.VISIBLE

                            Picasso.get()
                                    .load(response.body()!!.result.data.messageDetailsList.userImage)
                                    //  .load("https://gpluseurope.com/wp-content/uploads/Mauro-profile-picture.jpg")
                                    .placeholder(R.drawable.user_default)
                                    .into(profile_image)


                            val bold = Typeface.createFromAsset(assets,"montserrat_semibold.ttf")

                            val b: TypefaceSpan = CustomTypefaceSpan("", bold)


                            if(response.body()!!.result.data.messageDetailsList.titolo == null){
                                response.body()!!.result.data.messageDetailsList.titolo=""
                            }

                            val msg = SpannableString(response.body()!!.result.data.messageDetailsList.titolo + "\n\n" + response.body()!!.result.data.messageDetailsList.testo)
                            msg.setSpan(b, 0, response.body()!!.result.data.messageDetailsList.titolo.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)


                            txt_testo.text = msg
                            Linkify.addLinks(txt_testo, Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS)
                            stripUnderlines(txt_testo)

                            showFilesInRecyclerView(response.body()!!.result.data.messageDetailsList.messageAttachments)

                        }
                    } else {
                        // Toast.makeText(this@MessageDetailsActivity, response.body()!!.result.message, Toast.LENGTH_SHORT).show()
                    }
                }

            }

            override fun onFailure(call: Call<GetMessageDetailsResponse>, t: Throwable) {
                hideProgressDialog()
                Toast.makeText(this@MessageDetailsActivity, getString(R.string.failed_to_connect_server), Toast.LENGTH_SHORT).show()
                finish()
            }
        })


    }

    private fun init() {
        msg_id = intent.getIntExtra(Consts.KEY_MSG_ID, -1)
        msg_date = intent.getStringExtra(Consts.KEY_MSG_DATE)
        dialog = WorkingProgressDialog(this)
        prefManager = PrefManager.getInstance(this)

        Objects.requireNonNull(AppClass.instance)!!.manger.cancelAll()

        btn_back_msg_details.setOnClickListener(this)

        rv_files!!.layoutManager = GridLayoutManager(this, 2)
        rv_files!!.isNestedScrollingEnabled = false
        rv_files!!.setHasFixedSize(true)
        rv_files!!.addItemDecoration(GridSpacingItemDecoration(2, dpToPx(10), false))


        //enlarge hit area
        val parent = btn_back_msg_details.getParent() as View

        parent.post {
            val rect = Rect()
            btn_back_msg_details.getHitRect(rect)
            rect.top -= 50 // increase top hit area
            rect.left -= 50 // increase left hit area
            rect.bottom += 50 // increase bottom hit area
            rect.right += 50 // increase right hit area
            parent.touchDelegate = TouchDelegate(rect, btn_back_msg_details)
        }

    }

    private fun showFilesInRecyclerView(data: MutableList<MessageAttachment>) {

     /*   data.add(MessageAttachment(1,"https://homepages.cae.wisc.edu/~ece533/images/airplane.png","image2",Consts.FILE_TYPE_IMAGE))
        data.add(MessageAttachment(1,"https://homepages.cae.wisc.edu/~ece533/images/arctichare.png","image3",Consts.FILE_TYPE_IMAGE))
        data.add(MessageAttachment(1,"http://www.orimi.com/pdf-test.pdf","pdf1",Consts.FILE_TYPE_PDF))
        data.add(MessageAttachment(1,"http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4","video1",Consts.FILE_TYPE_VIDEO))
*/

        if (data.size != 0) {

            rv_files.visibility = View.VISIBLE
            no_att_lyt.visibility = View.GONE
            adapterEntries = FilesAdapter(data, this, this@MessageDetailsActivity)
            adapterEntries!!.setHasStableIds(true)
            rv_files!!.adapter = adapterEntries
            adapterEntries!!.notifyDataSetChanged()

        } else {
            rv_files.visibility = View.GONE
            no_att_lyt.visibility = View.VISIBLE
        }

    }


    fun showProgressDialog() {
        if (dialog != null) {
            dialog!!.setCancelable(false)
            dialog!!.show()
        }
    }

    fun hideProgressDialog() {
        if (dialog != null) {
            dialog!!.isShowing
            dialog!!.dismiss()
        }
    }

    override fun itemClickEvent(item: MessageAttachment) {
        if (Utils.isInternetAvailable(this@MessageDetailsActivity)) {
            //checkPermission(item)

            if (item.type == Consts.FILE_TYPE_VIDEO || item.type == Consts.FILE_TYPE_AUDIO || item.type == Consts.FILE_TYPE_PDF) {
                val intent = Intent(this@MessageDetailsActivity, NoOrientationConfigChangeFileViewActivity::class.java)
                intent.putExtra(Consts.FILE, item)
                startActivity(intent)
            } else {
                val intent = Intent(this@MessageDetailsActivity, FileViewActivity::class.java)
                intent.putExtra(Consts.FILE, item)
                startActivity(intent)
            }
        }
    }


    private fun checkPermission(item: MessageAttachment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this@MessageDetailsActivity,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this@MessageDetailsActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this@MessageDetailsActivity,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE), requestCode)
            } else {
                val intent = Intent(this@MessageDetailsActivity, FileViewActivity::class.java)
                intent.putExtra(Consts.FILE, item)
                startActivity(intent)
            }

        } else {
            val intent = Intent(this@MessageDetailsActivity, FileViewActivity::class.java)
            intent.putExtra(Consts.FILE, item)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCodes: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCodes) {
            requestCode -> if (grantResults.isNotEmpty()) {
                val readPermissions = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val writeStatePermissions = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (readPermissions || writeStatePermissions) {
                    Log.e("shn", "now proceed")
                } else {
                    Log.e("shn", "not proceed")
                }
            }
        }
    }

    override fun onClick(view: View?) {
        if (view == btn_back_msg_details) {
            super.onBackPressed()
        }

    }

    private fun stripUnderlines(textView: TextView) {
        val s: Spannable = SpannableString(textView.getText())
        val spans: Array<URLSpan> = s.getSpans(0, s.length, URLSpan::class.java)

        for (span in spans) {
            val start: Int = s.getSpanStart(span)
            val end: Int = s.getSpanEnd(span)
            s.removeSpan(span)

            s.setSpan(URLSpanNoUnderline(span.url), start, end, 0)
        }
        textView.text = s
    }

    private class URLSpanNoUnderline(url: String?) : URLSpan(url) {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.setUnderlineText(false)
        }
    }

    fun dpToPx(dp: Int): Int {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        return (dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

}
