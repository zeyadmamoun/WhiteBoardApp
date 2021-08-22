package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialoge_brush_size.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private var mImageButtonCurrentPaint: ImageButton? = null
    private lateinit var mProgressDialog: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<DrawingView>(R.id.drawingView).setSizeForBrush(20f)

        mImageButtonCurrentPaint = llColorPalate[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this , R.drawable.palletpressed))

        brushSizeBtn.setOnClickListener{
            showBrushSizeDialog()
        }

        galleryBtn.setOnClickListener{
            if (isReadStorageAllowed()){
                val pickPhotoIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryActivityResultCallback.launch(pickPhotoIntent)
            }else{
                requestStoragePermission()
            }
        }

        undoBtn.setOnClickListener {
            drawingView.onUndoClicked()
        }

        saveBtn.setOnClickListener {
            if (isReadStorageAllowed()){
                showProgressDialog()
                GlobalScope.launch(Dispatchers.Main) {
                    val toastMessage = async{ bitmapAsyncTask(getBitmapFromView(flDrawingViewContainer)) }
                    dismissProgressDialog()
                    Toast.makeText(this@MainActivity,toastMessage.await(),Toast.LENGTH_SHORT).show()
                    shareArtwork(toastMessage.await())
                }
            }else{
                requestStoragePermission()
            }
        }

    }

    private fun showBrushSizeDialog (){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialoge_brush_size)
        brushDialog.setTitle("brush size:")

        val smallButton = brushDialog.smallBrushSizeButton
        smallButton.setOnClickListener{
            drawingView.setSizeForBrush(10f)
            brushDialog.dismiss()
        }

        val mediumButton = brushDialog.mediumBrushSizeButton
        mediumButton.setOnClickListener{
            drawingView.setSizeForBrush(20f)
            brushDialog.dismiss()
        }

        val largeButton = brushDialog.largeBrushSizeButton
        largeButton.setOnClickListener{
            drawingView.setSizeForBrush(30f)
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    fun paintClicked(view: View){
        if (view != mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()

            drawingView.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this , R.drawable.palletpressed))
            mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable(this , R.drawable.palletnormal))
            mImageButtonCurrentPaint = view
        }
    }

    //////////////////////when user press on gallery ImageButton///////////////////////////////////////////////////////////////////
    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this , arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this,"need to give the permission",Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"permission granted now you can read storage files",Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this,"u denied the permission",Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun isReadStorageAllowed():Boolean{
        val result = ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }


    private val galleryActivityResultCallback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == Activity.RESULT_OK){
            try {
                if (result.data!!.data != null){
                    imageViewBackground.visibility = View.VISIBLE
                    imageViewBackground.setImageURI(result.data!!.data)
                }else{
                    Toast.makeText(this,"Error in choosing such a photo",Toast.LENGTH_SHORT).show()
                }
            }catch (e: Exception){
                println(e)
                e.printStackTrace()
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////here are the functions that save the canvas ito pic//////////////////////////
    private fun getBitmapFromView(view: View) : Bitmap {

        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val backgroundDrawable = view.background

        if (backgroundDrawable != null){
            backgroundDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun bitmapAsyncTask(mBitmap: Bitmap?) : String{

        var result = ""

        if (mBitmap != null){
            try {
                val bytes = ByteArrayOutputStream()
                mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                val f = File(externalCacheDir!!.absoluteFile.toString()
                        + File.separator + "kids Drawing App_" + System.currentTimeMillis() / 1000 + ".png")
                withContext(Dispatchers.IO){
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                }

            }catch (e: Exception){
                result = ""
                e.printStackTrace()
            }
        }
        return result
    }
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private fun showProgressDialog () {
        mProgressDialog = Dialog(this)
        mProgressDialog.setContentView(R.layout.dialog_custom_progress)
        mProgressDialog.show()
    }

    private fun dismissProgressDialog(){
        mProgressDialog.dismiss()
    }

    private fun shareArtwork(path: String){
        MediaScannerConnection.scanFile(this, arrayOf(path), null){
                _, uri ->val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"

            startActivity(
                Intent.createChooser(shareIntent,"share")
            )
        }
    }

    companion object{
        private const val STORAGE_PERMISSION_CODE =  1
    }
}


