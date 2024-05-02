package com.example.drawingapp

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.get
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.provider.MediaStore
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {
    private var drawingView : DrawingView? = null
 // variable for current color is picked from color pallet
    private var mImageButtonCurrentPaint : ImageButton? = null
    var customProgressDialog : Dialog? = null
    val openGalleryLauncher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackGround : ImageView = findViewById(R.id.iv_background)
                imageBackGround.setImageURI(result.data?.data)
            }
        }
 // create requestPermission for multiplepermissions since we are requesting both read and write
    val requestPermission : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName = it.key
                val isGranted = it.value
             // if permission is granted show a popup to perform operation
                if(isGranted){
                    Toast.makeText(
                        this@MainActivity,
                        "Permission granted now you can read the storage files.",
                        Toast.LENGTH_LONG
                    ).show()
             // this code is for selecting picture from device
                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }else{
             // displaying another popup if permission is not granted and
                    // this time focus on read external storage
                    if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(
                            this@MainActivity,
                            "Oops you just denied the permission.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(15.toFloat())
        var linearLayoutPaintColors = findViewById<LinearLayout>(R.id.all_paint_colors)
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )
        val ib_brush : ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener{
            showBrushSizeChooserDialog()
        }
        val ib_gallery : ImageButton = findViewById(R.id.ib_gallery)
        ib_gallery.setOnClickListener{
            requestStoragePermission()
        }
        val ibUndo : ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener{
           drawingView?.onClickUndo()
        }
        val ibRedo : ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener{
            drawingView?.onClickRedo()
        }
        // for save button from the layout
        val ibsave : ImageButton = findViewById(R.id.ib_save)
        ibsave.setOnClickListener{
        // chech if permission is allowed
           if(isReadStorageAllowed()){
               showProgressDialog()
               // launch a coroutine block
               lifecycleScope.launch {
                   // reference the frame layout
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                   // save the image to the device
                   saveBitmapFile(getBitmapFromView(flDrawingView))
               }
           }
        }
    }

    // func is uses to launch the dialog to select different brush sizes
    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush size:")
        val smallBtn: ImageButton = brushDialog.findViewById(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
           drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn: ImageButton = brushDialog.findViewById(R.id.ib_medium_brush)
        mediumBtn.setOnClickListener{
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
       brushDialog.show()
    }
    // func is called when color is clicked from the pallet_normal
    fun paintClicked(view: View){
       if(view !== mImageButtonCurrentPaint){
           // update the color
           val imageButton = view as ImageButton
           // tag is used for swaping the current color with previous color
           // tag stores the selected view
           val colorTage = imageButton.tag.toString()
           // color is set as per the selected tag here
           drawingView?.setColor(colorTage)
           // swap the background for last active and currently active image button
           imageButton.setImageDrawable(
               ContextCompat.getDrawable(
                   this,
                   R.drawable.pallet_pressed
               )
           )
           mImageButtonCurrentPaint?.setImageDrawable(
               ContextCompat.getDrawable(
                   this,
                   R.drawable.pallet_normal
               )
           )
           // current view is updated with selected view in the form of imagebutton
           mImageButtonCurrentPaint = view
       }
    }
    // func to check the permission status
    private fun isReadStorageAllowed(): Boolean {
        // getting the permission status and
        // checkselfPermission is particular permission is granted
        val result = ContextCompat.checkSelfPermission(this,
              Manifest.permission.READ_EXTERNAL_STORAGE
            )
        // if permission is granted returning true and if permission is not granted returning false
        return result == PackageManager.PERMISSION_GRANTED
    }

    // func to requeststorage permission
    private fun requestStoragePermission(){
        // check if the permission was denied and show rationale
       if(ActivityCompat.shouldShowRequestPermissionRationale(
           this,
           Manifest.permission.READ_EXTERNAL_STORAGE)){
           // call the rationale dialog to tell the user why they need to allow permission request
           showRationaleDialog(" Drawing_App_Kids "," Drawing_App_Kids"
           + " needs to Access your External Storage ")
       }else{
           // you can directly ask for the permission
           // if it has not been denied then request for permission
           // the registered ActivityResultCallback get the result of this request
           requestPermission.launch(arrayOf(
               Manifest.permission.READ_EXTERNAL_STORAGE,
               Manifest.permission.WRITE_EXTERNAL_STORAGE
           ))
       }
    }

    // create rationale dialog shows rationale dialog for display why the app needs permission
    // only shown if the user has denied the permission request previously
    private fun showRationaleDialog(
        title : String,
        message : String
    ){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
    private fun getBitmapFromView(view: View): Bitmap {
        // define a bitmap with the same size as the view
        // createBitmap: returns a mutable bitmap with the specified width and height
        val returnedBitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888)
        // bind a cnavas to it
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!= null){
            // has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas)
        }else{
            // does not have background drawbale, then draw white background on the canvas
            canvas.drawColor(Color.WHITE)
        }
        // draw the view on the canvas
        view.draw(canvas)
        // return the bitmap
        return returnedBitmap
    }
    // save the image on the device
    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                try{
                    // creates a new byte array output stream
                    val bytes = ByteArrayOutputStream()
                    // Write a compressed version of the bitmap to the specified outputstream.
                    // If this returns true, the bitmap can be reconstructed by passing a
                    // corresponding inputstream to BitmapFactory.decodeStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString() +
                        File.separator + "DrawingAppForKids_" +
                        System.currentTimeMillis() / 1000 + ".png"
                    )
                    // Creates a file output stream to write to the file represented by the specified object
                    val fo = FileOutputStream(f)
                    // Writes bytes from the specified byte array to this file output stream.
                    fo.write(bytes.toByteArray())
                    // Closes this file output stream and releases any system resources associated with this stream.
                    // This file output stream may no longer be used for writing bytes
                    fo.close()
                    // The file absolute path is return as a result
                    result = f.absolutePath
                    runOnUiThread {
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(
                                this@MainActivity,
                                "File saved successfully :$result",
                                Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(
                                this@MainActivity,
                                "Something went wrong while saving the file.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }catch (e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    // func is used to show the custom progress dialog
    private fun showProgressDialog(){
        customProgressDialog = Dialog(this@MainActivity)
        // Set the screen content from a layout resource
        // The resource will be inflated, adding all top-level views to the screen
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        // Start the dialog and display it on screen
        customProgressDialog?.show()
    }

    // func is used to dismiss the progress dialog if it is visible to user
    private fun cancelProgressDialog(){
        if(customProgressDialog!=null){
            customProgressDialog?.dismiss()
            customProgressDialog=null
        }
    }

    // func is for share image online
    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(
            this,
             arrayOf(result), null
            ){
            path, uri ->
                var shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(
                Intent.EXTRA_STREAM, uri
            )
            shareIntent.type ="image/png"
            startActivity(Intent.createChooser(shareIntent, "share"))
        }
    }
}