package com.fauji.zebrareader

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fauji.zebrareader.ExcelUtils.exportDataIntoWorkbook
import com.fauji.zebrareader.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.*


class MainActivity : AppCompatActivity(), MainScreenAdapter.MainScreenNavigator {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseProductRef: DatabaseReference
    private lateinit var progressDialog1: ProgressDialog
    private lateinit var mainScreenAdapter: MainScreenAdapter
    private val items: ArrayList<DailyData> = arrayListOf()
    private lateinit var context: Context
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        FirebaseApp.initializeApp(this)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseProductRef = firebaseDatabase.getReference("InventoryDB")

        val view: View = binding.root
        setContentView(view)


        requestForSignIn()

        context = this
        progressDialog1 = ProgressDialog(this@MainActivity)

        mainScreenAdapter = MainScreenAdapter(this@MainActivity, items)
        mainScreenAdapter.setClickListener(this@MainActivity)
        binding.recyclerView.adapter = mainScreenAdapter

        binding.openBarcodeActivity.setOnClickListener {
            var fileId: String = ""
            val date = getCurrentDateTime()
            val dateInString = date.toString("E, dd MMM yyyy")
            if(items.isNotEmpty()){
                for (item in items){
                    if(item.date == dateInString){
                        fileId = item.fileId
                    }
                }
            }

            val intent = Intent(this@MainActivity, BarCodeReaderActivity::class.java)
            intent.putExtra("date", dateInString)
            intent.putExtra("flag", false)
            intent.putExtra("fileId",fileId)
            startActivity(intent)
        }

        getDataFromFirebase()
    }

    override fun onResume() {
        super.onResume()
        databaseProductRef.child("ZebraSheets").addValueEventListener(valueEventListener)
    }

    override fun onPause() {
        super.onPause()
        databaseProductRef.child("ZebraSheets").removeEventListener(valueEventListener)
    }

    private fun getDataFromFirebase() {
        showProgressDialog()
        valueEventListener = object :ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                items.clear()
                for (parent in snapshot.children) {
                    parent.getValue(DailyData::class.java)?.let { items.add(it) }
                }
                emptyState()
                mainScreenAdapter.notifyDataSetChanged()
                progressDialog1.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog1.dismiss()
            }

        }

    }

    fun emptyState() {
        if (items.isEmpty()) {
            binding.noRecordFound.visibility = View.VISIBLE
        } else {
            binding.noRecordFound.visibility = View.GONE
        }
    }


    private fun showProgressDialog() {
        progressDialog1.setTitle("Loading...")
        progressDialog1.setMessage("Please wait")
        progressDialog1.setCancelable(false)
        progressDialog1.show()
    }

    override fun onClickListener(pos: Int, dailyData: DailyData) {
        val intent = Intent(this@MainActivity, BarCodeReaderActivity::class.java)
        intent.putExtra("date", dailyData.date)
        intent.putExtra("flag", true)
        intent.putExtra("fileId",dailyData.fileId)
        startActivity(intent)
    }

    override fun downloadListener(pos: Int, dailyData: DailyData) {
        val items: java.util.AbstractList<Items> = ArrayList()
        showProgressDialog()
        databaseProductRef.child("ZebraItems").child(dailyData.date).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Dexter.withContext(context).withPermissions(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                )
                    .withListener(object : MultiplePermissionsListener {

                        override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                            // check if all permissions are granted
                            if (p0?.areAllPermissionsGranted() == true) {
                                items.clear()
//                for (parent in snapshot.children) {
                                for (child in snapshot.children) {
                                    child.getValue(Items::class.java)?.let { items.add(it) }
//                    }
                                }
                                exportDataIntoWorkbook(this@MainActivity, "${dailyData.sheet} ${dailyData.date}.csv", items, true, dailyData.fileId, dailyData.date)
                                progressDialog1.dismiss()
                            }

                            // check for permanent denial of any permission
                            if (p0?.isAnyPermissionPermanentlyDenied == true) {
                                val intent = Intent()
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                val uri: Uri = Uri.fromParts("package", packageName, null)
                                intent.data = uri
                                startActivity(intent)
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(p0: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                            p1?.continuePermissionRequest()
                            progressDialog1.dismiss()
                        }
                    })
                    .check()
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog1.dismiss()
            }

        })

    }

    fun requestForSignIn() {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(Scope(DriveScopes.DRIVE_FILE)).build()
        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        startActivityForResult(googleSignInClient.signInIntent, 400)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            400 -> {
                if (resultCode == RESULT_OK) {
                    handleSignInIntent(data)
                }
            }
        }
    }

    private fun handleSignInIntent(data: Intent?) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener {
                val googleAccountCredential = GoogleAccountCredential.usingOAuth2(this, Collections.singleton(DriveScopes.DRIVE_FILE))
                googleAccountCredential.selectedAccount = it.account

                val googleDriveServices = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    googleAccountCredential
                )

                googleDriveServices.applicationName = "Zebra Reader"


                driveServiceHelper = DriveServiceHelper(googleDriveServices.build())
            }
            .addOnFailureListener {

            }
    }

    companion object {
        lateinit var driveServiceHelper: DriveServiceHelper
    }

}