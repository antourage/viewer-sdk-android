package com.antourage.weaverlib.other.ui

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.annotation.NonNull
import android.support.media.ExifInterface
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import com.antourage.weaverlib.BuildConfig
import com.antourage.weaverlib.R
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.File
import java.io.IOException

internal class AvatarChooser(val context: Context) {

    companion object {
        private const val REQUEST_IMAGE_GALLERY = 99
        private const val REQUEST_IMAGE_CAMERA = 98
        private const val REQUEST_PERMISSION_IMAGE_CAMERA = 97

        const val IMAGE_MAX_SIZE = 256
    }

    private var cameraUri: Uri? = null

    interface AvatarChooserListener {
        fun onImagePickedResult(bitmap: Bitmap?)
    }

    private var avatarChooserListener: AvatarChooserListener? = null

    fun showChoose(fragment: Fragment) {
        getDefaultDialog(fragment).show()
    }

    fun showChooseDelete(fragment: Fragment) {
        getDefaultDialog(fragment)
            .setNegativeButton(context.getString(R.string.ant_delete_image)) { dialog, _ ->
                avatarChooserListener?.onImagePickedResult(null)
                dialog.dismiss()
            }
            .show()
    }

    private fun getDefaultDialog(fragment: Fragment): AlertDialog.Builder {
        return AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setTitle(context.getString(R.string.ant_choose_image_source))
            .setPositiveButton(context.getString(R.string.ant_from_gallery)) { dialog, _ ->
                startActivityChoseImageFromGallery(fragment)
                dialog.dismiss()
            }
            .setNeutralButton(context.getString(R.string.ant_from_camera)) { _, _ ->
                checkCameraPermissionOrOpenCamera(fragment)
            }
    }

    fun setListener(listenerFunc: (Bitmap?) -> Unit) {
        avatarChooserListener = object : AvatarChooserListener {
            override fun onImagePickedResult(bitmap: Bitmap?) {
                listenerFunc.invoke(bitmap)
            }
        }
    }

    private fun startActivityChoseImageFromGallery(fragment: Fragment) {
        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        fragment.startActivityForResult(photoPickerIntent, REQUEST_IMAGE_GALLERY)
    }


    private fun checkCameraPermissionOrOpenCamera(fragment: Fragment) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            fragment.requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSION_IMAGE_CAMERA
            )
        } else {
            startActivityCaptureImageFromCamera(fragment)
        }
    }


    private fun startActivityCaptureImageFromCamera(fragment: Fragment) {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        doAsync {
            val cameraFile = File(context.externalCacheDir, "avatar.png")
            uiThread {
                cameraUri = FileProvider.getUriForFile(
                    context,
                    BuildConfig.LIBRARY_PACKAGE_NAME + ".provider",
                    cameraFile
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                fragment.startActivityForResult(cameraIntent, REQUEST_IMAGE_CAMERA)
            }
        }
    }


    fun onRequestPermissionsResult(
        fragment: Fragment,
        requestCode: Int, @NonNull permissions: Array<String>, @NonNull grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION_IMAGE_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startActivityCaptureImageFromCamera(fragment)
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_GALLERY -> getBitmapFromImage(data?.data)
                REQUEST_IMAGE_CAMERA -> getBitmapFromImage(cameraUri)
            }
        }
    }

    private fun getBitmapFromImage(picUri: Uri?) {
        picUri?.also {
            doAsync {
                //                var bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, picUri)
                var bitmap = handleSamplingAndRotationBitmap(context, picUri)
                bitmap?.also {
                    bitmap = cropAndScaleBitmap(it)
                }
                uiThread {
                    avatarChooserListener?.onImagePickedResult(bitmap)
                }
            }
        }
    }

    @Throws(IOException::class)
    fun handleSamplingAndRotationBitmap(context: Context, selectedImage: Uri): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        var imageStream = context.contentResolver.openInputStream(selectedImage)
        BitmapFactory.decodeStream(imageStream, null, options)
        imageStream!!.close()

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, IMAGE_MAX_SIZE, IMAGE_MAX_SIZE)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        imageStream = context.contentResolver.openInputStream(selectedImage)
        var img = BitmapFactory.decodeStream(imageStream, null, options)

        img = rotateImageIfRequired(context, img, selectedImage)
        return img
    }

    /**
     * Calculate an inSampleSize for use in a [BitmapFactory.Options] object when decoding
     * bitmaps using the decode* methods from [BitmapFactory]. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options   An options object with out* params already populated (run through a decode*
     * method with inJustDecodeBounds==true
     * @param reqWidth  The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int, reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            val heightRatio = Math.round(height.toFloat() / reqHeight.toFloat())
            val widthRatio = Math.round(width.toFloat() / reqWidth.toFloat())

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            val totalPixels = (width * height).toFloat()

            // Anything more than 2x the requested pixels we'll sample down further
            val totalReqPixelsCap = (reqWidth * reqHeight * 2).toFloat()

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++
            }
        }
        return inSampleSize
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    @Throws(IOException::class)
    private fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {

        val input = context.contentResolver.openInputStream(selectedImage)
        val ei: ExifInterface
        ei = if (Build.VERSION.SDK_INT > 23)
            ExifInterface(input)
        else
            ExifInterface(selectedImage.path!!)

        val orientation =
            ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    private fun cropAndScaleBitmap(
        bitmap: Bitmap,
        dstWidth: Int = IMAGE_MAX_SIZE,
        dstHeight: Int = IMAGE_MAX_SIZE
    ): Bitmap {
        var newBitmap: Bitmap
        if (bitmap.width >= bitmap.height) {
            newBitmap = Bitmap.createBitmap(
                bitmap,
                bitmap.width / 2 - bitmap.height / 2,
                0,
                bitmap.height,
                bitmap.height
            )
        } else {
            newBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                bitmap.height / 2 - bitmap.width / 2,
                bitmap.width,
                bitmap.width
            )
        }
        newBitmap = Bitmap.createScaledBitmap(newBitmap, dstWidth, dstHeight, false)
        return newBitmap
    }

}