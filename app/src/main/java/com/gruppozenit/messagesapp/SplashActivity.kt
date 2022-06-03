package com.gruppozenit.messagesapp

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.gruppozenit.messagesapp.login.LoginActivity
import com.gruppozenit.messagesapp.login.LoginWaitActivity
import com.gruppozenit.messagesapp.model.messageListModels.GetMessageListResponse
import com.gruppozenit.messagesapp.network.provider.RetrofitProviderJava
import com.gruppozenit.messagesapp.utils.Consts
import com.gruppozenit.messagesapp.utils.PrefManager
import com.gruppozenit.messagesapp.utils.Utils
import com.zenith.eteam.chronology.chronology1.progressdialog.WorkingProgressDialog
import kotlinx.android.synthetic.main.activity_splash.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SplashActivity : AppCompatActivity() {
    private var _splashTime = 2000
    private var prefManager: PrefManager? = null
    private var dialog: WorkingProgressDialog? = null
    private var CODE_AUTHENTICATION_VERIFICATION = 101;
    // private var connectivityReceiver: ConnectivityReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefManager = PrefManager.getInstance(this)
        if (prefManager!!.darkModeONorOFF && prefManager!!.isLogin) {

            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        setContentView(R.layout.activity_splash)

        dialog = WorkingProgressDialog(this)

        //   connectivityReceiver = ConnectivityReceiver()
        //   registerReceiver(connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))


        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardSecure && prefManager!!.lockONorOFF) {
            val i = km.createConfirmDeviceCredentialIntent("Autenticazione richiesta", "Conferma la sequenza di blocco, il PIN, la password o l'impronta digitale")
            startActivityForResult(i, CODE_AUTHENTICATION_VERIFICATION)
        } else {
            if (Utils.isInternetAvailable(this)) {
                countDownTimer.start()
            }
        }


    }

    override fun onPause() {
        super.onPause()
        /*  if (connectivityReceiver != null) {
              Objects.requireNonNull(baseContext).unregisterReceiver(connectivityReceiver)
          }*/
    }

    override fun onResume() {
        super.onResume()
        //test screen lock
        /* if (Utils.isInternetAvailable(this)) {
             countDownTimer.start()
         }*/

    }

    private fun checkUserAccessInPref() {

        if (prefManager!!.isLogin && (prefManager!!.isApproved == Consts.ACCESS_APPROVED)) {

            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else if (prefManager!!.isApproved == Consts.ACCESS_PENDING) {

            mGetUserStatus()

        } else {

            val intent: Intent?
            intent = if (prefManager!!.showInfoPage()) {
                Intent(this@SplashActivity, PrivacyActivity::class.java)
            } else {
                Intent(this@SplashActivity, LoginActivity::class.java)
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }


    private val countDownTimer = object : CountDownTimer(_splashTime.toLong(), 100) {
        override fun onTick(l: Long) {

        }

        override fun onFinish() {
            checkUserAccessInPref()
        }
    }


    private fun mGetUserStatus() {

        progress.visibility = View.VISIBLE


        val api = RetrofitProviderJava.getInstance().retrofit!!.create(com.zenith.eteam.chronology.chronology1.network.api.Api::class.java)
        val hashMap = HashMap<String, Int>()

        hashMap[Consts.KEY_DEVICE_ID] = prefManager!!.deviceID

        val apiCall = api.mGetMessages(prefManager!!.fcmToken, hashMap)
        apiCall.enqueue(object : Callback<GetMessageListResponse> {
            override fun onResponse(call: Call<GetMessageListResponse>, response: Response<GetMessageListResponse>) {

                if (response.body()!!.result.success == true) {

                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()


                } else {
                    progress.visibility = View.GONE

                    checkStatusAndMove(response)


                }

            }

            override fun onFailure(call: Call<GetMessageListResponse>, t: Throwable) {
                progress.visibility = View.GONE

                Toast.makeText(this@SplashActivity, getString(R.string.failed_to_connect_server), Toast.LENGTH_SHORT).show()
            }
        })


    }


    private fun checkStatusAndMove(response: Response<GetMessageListResponse>) {
        if (response.body()!!.result.data.adminStatus.toInt() == Consts.ACCESS_PENDING) {
            val intent = Intent(this@SplashActivity, LoginWaitActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()
        } else if (response.body()!!.result.data.adminStatus.toInt() == Consts.ACCESS_REJECTED || response.body()!!.result.data.adminStatus.toInt() == Consts.ACCESS_REMOVED) {

            //  Toast.makeText(this@SplashActivity, response.body()!!.result.message, Toast.LENGTH_SHORT).show()

            prefManager!!.isLogin = false

            if (response.body()!!.result.data.adminStatus.toInt() == Consts.ACCESS_REMOVED) {
                prefManager!!.setDeviceId(0)
            }

            Utils.clearUserDetails(prefManager!!)

            val intent: Intent?
            intent = if (prefManager!!.showInfoPage()) {
                Intent(this@SplashActivity, PrivacyActivity::class.java)
            } else {
                Intent(this@SplashActivity, LoginActivity::class.java)
            }

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            finish()

        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == CODE_AUTHENTICATION_VERIFICATION) {
            if (Utils.isInternetAvailable(this)) {
                countDownTimer.start()
            }
        } else {

            finish()
           // Toast.makeText(this, "Failure: Unable to verify user's identity", Toast.LENGTH_SHORT).show()
        }
    }


    /* override fun onNetworkConnectionChanged(isConnected: Boolean) {
         if (isConnected) {
             checkUserAccessInPref()
         }

     }*/


}