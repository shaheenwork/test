package com.gruppozenit.messagesapp.fileView

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler
import com.bumptech.glide.Glide
import com.gruppozenit.messagesapp.R
import com.gruppozenit.messagesapp.model.messageDetailsModels.MessageAttachment
import com.gruppozenit.messagesapp.utils.Consts
import com.squareup.picasso.Picasso
import com.zenith.eteam.chronology.chronology1.progressdialog.WorkingProgressDialog
import kotlinx.android.synthetic.main.fragment_imageviewer.*


class ImageViewFragment : Fragment()
       {

    private var file: MessageAttachment? = null

    private var dialog: WorkingProgressDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_imageviewer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val bundle = arguments
        if (bundle != null) {
            file = bundle.getParcelable(Consts.FILE)
        }
        imageView.setOnTouchListener( ImageMatrixTouchHandler(view.context))



        Glide.with(this).load(file!!.filePath).into(imageView)

    }

           override fun onCreate(savedInstanceState: Bundle?) {
               super.onCreate(savedInstanceState)
               retainInstance = true
           }
       }
