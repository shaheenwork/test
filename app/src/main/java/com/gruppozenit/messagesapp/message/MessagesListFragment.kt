package com.gruppozenit.messagesapp.message

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.google.android.material.navigation.NavigationView
import com.gruppozenit.messagesapp.R
import com.gruppozenit.messagesapp.adapter.MessagesAdapter
import com.gruppozenit.messagesapp.login.LoginActivity
import com.gruppozenit.messagesapp.login.LoginWaitActivity
import com.gruppozenit.messagesapp.model.messageListModels.GetMessageListResponse
import com.gruppozenit.messagesapp.model.messageListModels.MessageList
import com.gruppozenit.messagesapp.network.provider.RetrofitProviderJava
import com.gruppozenit.messagesapp.utils.ConnectivityReceiver
import com.gruppozenit.messagesapp.utils.Consts
import com.gruppozenit.messagesapp.utils.PrefManager
import com.gruppozenit.messagesapp.utils.Utils
import com.zenith.eteam.chronology.chronology1.progressdialog.WorkingProgressDialog
import kotlinx.android.synthetic.main.fragment_messages_list.*
import kotlinx.android.synthetic.main.fragment_messages_list.view.*
import kotlinx.android.synthetic.main.top_bar_settings.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.HashMap


class MessagesListFragment : Fragment(), MessagesAdapter.ItemClick, ConnectivityReceiver.ConnectivityReceiverListener, View.OnClickListener {


    private var adapterEntries: MessagesAdapter? = null

    private var prefManager: PrefManager? = null
    private var dialog: WorkingProgressDialog? = null

    private var receiver: NewMessageBroadcast? = null
    private val BUNDLE_RECYCLER_LAYOUT = "messageslist.recycler.layout"
    private var connectivityReceiver: ConnectivityReceiver? = null
    private var savedRecyclerLayoutState: Parcelable? = null
    private var nav: NavigationView? = null;

    companion object {

        fun newInstance(): MessagesListFragment {
            return MessagesListFragment()
        }
    }


    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val view: View = inflater.inflate(R.layout.fragment_messages_list, container,
                false)
        val activity = activity as Context

        val itemDecoration = DividerItemDecoration(activity, VERTICAL)
        itemDecoration.setDrawable(this!!.resources.getDrawable(R.drawable.divider))


        view.rv_messages!!.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        view.rv_messages!!.isNestedScrollingEnabled = true
        view.rv_messages!!.setHasFixedSize(true)
        view. rv_messages!!.addItemDecoration(itemDecoration)


        return view
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        init()
    }

    override fun onResume() {

        registerReceiver()
        if (Utils.isInternetAvailable(activity!!)) {
            mGetMessages(true)
        }
      //  invalidateOptionsMenu()
        super.onResume()
    }

    override fun itemClickEvent(item: MessageList) {

        if (Utils.isInternetAvailable(activity!!)) {
            val intent = Intent(activity!!, MessageDetailsActivity::class.java)
            intent.putExtra(Consts.KEY_MSG_ID, item.messaggioId)
            intent.putExtra(Consts.KEY_MSG_DATE, item.messageDate)
            startActivity(intent)
        }

    }

    override fun onPause() {

        super.onPause()

        if (receiver != null) {
            Objects.requireNonNull(activity!!).unregisterReceiver(receiver)
        }
        if (connectivityReceiver != null) {
            Objects.requireNonNull(activity!!).unregisterReceiver(connectivityReceiver)
        }

        savedRecyclerLayoutState = rv_messages.layoutManager!!.onSaveInstanceState()


    }


    private fun init() {


        prefManager = PrefManager.getInstance(activity!!)

       // initDrawer()

        receiver = NewMessageBroadcast()
        connectivityReceiver = ConnectivityReceiver()
        registerReceiver()


        dialog = WorkingProgressDialog(activity!!)



    }

   /* private fun initDrawer() {
        drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        nav = findViewById<NavigationView>(R.id.nav_view)

        nav!!.menu.findItem(R.id.notification).actionView = Switch(this)
        nav!!.menu.findItem(R.id.dark).actionView = Switch(this)

// T
        (nav!!.menu.findItem(R.id.notification).actionView as Switch).isChecked = prefManager!!.notificationONorOFF
        (nav!!.menu.findItem(R.id.dark).actionView as Switch).isChecked = prefManager!!.darkModeONorOFF

        (nav!!.menu.findItem(R.id.dark).actionView as Switch).setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                prefManager!!.setDARKkMODE_ON_OFF(p1)
                if (p1) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }


        })

        (nav!!.menu.findItem(R.id.notification).actionView as Switch).setOnCheckedChangeListener(object : CompoundButton.OnCheckedChangeListener {
            override fun onCheckedChanged(p0: CompoundButton?, p1: Boolean) {
                //   prefManager!!.setNotificationOnOff(p1)


                //test screen lock

                if (p1) {
                    val km: KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    if (km.isKeyguardSecure) {
                        prefManager!!.setNotificationOnOff(p1)
                    } else {
                        Toast.makeText(this@MessagesListFragment, "No any security setup done by user(pattern or password or pin_orange or fingerprint", Toast.LENGTH_SHORT).show()
                         //switch back toggle
                    }

                }
                else{
                    prefManager!!.setNotificationOnOff(p1)
                }
            }


        })

        *//* nav!!.setNavigationItemSelectedListener( object : NavigationView.OnNavigationItemSelectedListener {

             override fun onNavigationItemSelected(p0: MenuItem): Boolean {
                 return when (p0.getItemId()) {
                     R.id.notification -> {
                         prefManager!!.setNotificationOnOff(p0.isChecked)
                         (p0.actionView as Switch).toggle()
                         return true
                     }
                     R.id.dark -> {
                         prefManager!!.setDARKkMODE_ON_OFF(p0.isChecked)
                         (p0.actionView as Switch).toggle()
                         return true
                     }
                     else -> false
                 }
             }
         })*//*
    }*/

    private fun mGetMessages(checkStatus: Boolean) {

        showProgressDialog()


        val api = RetrofitProviderJava.getInstance().retrofit!!.create(com.zenith.eteam.chronology.chronology1.network.api.Api::class.java)
        val hashMap = HashMap<String, Int>()

        hashMap[Consts.KEY_DEVICE_ID] = prefManager!!.deviceID

        val apiCall = api.mGetMessages(prefManager!!.fcmToken, hashMap)
        apiCall.enqueue(object : Callback<GetMessageListResponse> {
            override fun onResponse(call: Call<GetMessageListResponse>, response: Response<GetMessageListResponse>) {

                if (response.body() != null) {
                    if (response.body()!!.result.success == true) {


                        if (response.body()!!.result.data.messageList != null) {
                            setStatusInPref(response.body()!!.result.data.adminStatus.toInt())
                            showMessagesInRecyclerView(response.body()!!.result.data.messageList)
                        }

                    } else {
                        hideProgressDialog()

                        if (checkStatus) {
                            checkStatusAndMove(response)
                        }

                    }
                }

            }

            override fun onFailure(call: Call<GetMessageListResponse>, t: Throwable) {
                hideProgressDialog()
                Toast.makeText(activity!!, getString(R.string.failed_to_connect_server), Toast.LENGTH_SHORT).show()
            }
        })


    }

    private fun setStatusInPref(status: Int) {

        if (status == -1) {
            prefManager!!.setIsApproved(Consts.ACCESS_PENDING)
        } else if (status == 1) {
            prefManager!!.setIsApproved(Consts.ACCESS_APPROVED)
        } else {
            prefManager!!.setIsApproved(Consts.ACCESS_REJECTED)
        }


    }

    private fun checkStatusAndMove(response: Response<GetMessageListResponse>) {
        if (response.body()!!.result.data.adminStatus.toInt() == Consts.ACCESS_PENDING) {
            val intent = Intent(activity!!, LoginWaitActivity::class.java)
            startActivity(intent)
            activity!!.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            activity!!.finish()
        } else if (response.body()!!.result.data.adminStatus.toInt() == Consts.ACCESS_REJECTED || response.body()!!.result.data.adminStatus.toInt() == Consts.ACCESS_REMOVED) {

            // Toast.makeText(this@MessagesListActivity, response.body()!!.result.message, Toast.LENGTH_SHORT).show()

            prefManager!!.isLogin = false

            if (response.body()!!.result.data.adminStatus.toInt() == Consts.ACCESS_REMOVED) {
                prefManager!!.setDeviceId(0)
            }

            Utils.clearUserDetails(prefManager!!)
            val intent = Intent(activity!!, LoginActivity::class.java)
            startActivity(intent)
            activity!!.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            activity!!.finish()

        }
    }

    private fun showMessagesInRecyclerView(data: MutableList<MessageList>) {

        if (data.size != 0) {

            adapterEntries = MessagesAdapter(data, this, activity!!)
            adapterEntries!!.setHasStableIds(true)
            rv_messages!!.adapter = adapterEntries
            adapterEntries!!.notifyDataSetChanged()

            if (savedRecyclerLayoutState != null) {
                rv_messages.layoutManager!!.onRestoreInstanceState(savedRecyclerLayoutState)
            }


        }
        hideProgressDialog()

    }


    internal inner class NewMessageBroadcast : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            try {

                mGetMessages(true)

            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }

    private fun registerReceiver() {
        ConnectivityReceiver.connectivityReceiverListener = this
        try {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Consts.ACTION_NEW_MESSAGE)
            intentFilter.addAction(Consts.ACTION_ADMIN_STATUS)
            Objects.requireNonNull(activity!!).registerReceiver(receiver, intentFilter)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        try {
            Objects.requireNonNull(activity!!).registerReceiver(connectivityReceiver, null)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun showProgressDialog() {
        if (dialog != null) {
            dialog!!.setCancelable(false)
            dialog!!.show()
        }
    }

    fun hideProgressDialog() {
        try {
            if (dialog != null) {
                dialog!!.isShowing
                dialog!!.dismiss()
            }
        } catch (e: Exception) {
        }
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (isConnected) {
            mGetMessages(false)
        }
    }

    override fun onClick(p0: View?) {
        if (p0 == btn_settings) {
            //drawer!!.openDrawer(GravityCompat.START)
        }
    }

 /*   override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val notificationSwitch: MenuItem = menu!!.findItem(R.id.notification)
        notificationSwitch.setChecked(prefManager!!.notificationONorOFF)

        val darkModeSwitch: MenuItem = menu!!.findItem(R.id.dark)
        darkModeSwitch.setChecked(prefManager!!.darkModeONorOFF)

        return super.onPrepareOptionsMenu(menu);
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.notification -> {
                prefManager!!.setNotificationOnOff(item.isChecked)
                true
            }
            R.id.dark -> {
                prefManager!!.setDARKkMODE_ON_OFF(item.isChecked)
                true
            }

            else -> false
        }
    }*/

}
