package com.fauji.zebrareader

import android.Manifest
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.opengl.Visibility
import android.os.AsyncTask
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.fauji.zebrareader.databinding.ActivityBarCodeReaderBinding
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.skydoves.expandablelayout.ExpandableLayout
import com.symbol.emdk.EMDKManager
import com.symbol.emdk.EMDKResults
import com.symbol.emdk.barcode.*


class BarCodeReaderActivity : AppCompatActivity(), BarcodeItemReaderAdapter.AdapterNavigator, EMDKManager.EMDKListener, Scanner.StatusListener, Scanner.DataListener {
    private lateinit var binding: ActivityBarCodeReaderBinding
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var date: String
    private lateinit var databaseProductRef: DatabaseReference
    private val items: ArrayList<Items> = arrayListOf()
    private lateinit var progressDialog1: ProgressDialog
    private lateinit var barcodeItemReaderAdapter: BarcodeItemReaderAdapter
    private var flag: Boolean = false

    private var simpleItemTouchCallback: ItemTouchHelper.SimpleCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
            databaseProductRef.child("ZebraItems").child(date).child(items[viewHolder.adapterPosition].serialNumber).removeValue()
            items.removeAt(viewHolder.adapterPosition)
            barcodeItemReaderAdapter.notifyItemRemoved(viewHolder.adapterPosition)
            if(items.isEmpty()){
                binding.noRecordFound.visibility = View.VISIBLE
            }else{
                binding.noRecordFound.visibility = View.GONE
            }
            Toast.makeText(
                this@BarCodeReaderActivity,
                "Deleted Successfully!!!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Declare a variable to store EMDKManager object
    private var emdkManager: EMDKManager? = null

    // Declare a variable to store Barcode Manager object
    private var barcodeManager: BarcodeManager? = null

    // Declare a variable to hold scanner device to scan
    private var scanner: Scanner? = null

    private lateinit var fileId: String
    private val barcodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents != null) {
//            handlingBarcodeReaderData(date, result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarCodeReaderBinding.inflate(layoutInflater)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseProductRef = firebaseDatabase.getReference("InventoryDB")

        val view: View = binding.root
        setContentView(view)

        progressDialog1 = ProgressDialog(this@BarCodeReaderActivity)
        try {
            val intent = intent
            date = intent?.extras?.getString("date")!!
            flag = intent?.extras?.getBoolean("flag")!!
            fileId = intent?.extras?.getString("fileId")!!
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        barcodeItemReaderAdapter = BarcodeItemReaderAdapter(this@BarCodeReaderActivity, items)
        barcodeItemReaderAdapter.setClickListener(this@BarCodeReaderActivity)
        binding.recyclerView.adapter = barcodeItemReaderAdapter

        val itemTouchHelper = ItemTouchHelper(simpleItemTouchCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)



        getDataFromFirebase()

        binding.openCamera.setOnClickListener {
            Dexter.withContext(this).withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
                .withListener(object : MultiplePermissionsListener {

                    override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                        // check if all permissions are granted
                        if (p0?.areAllPermissionsGranted() == true) {
                            scanBarcode()
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
                    }
                })
                .check()
        }


// The EMDKManager object will be created and returned in the callback.

// The EMDKManager object will be created and returned in the callback.
        var results: EMDKResults? = null
        try {
            results = EMDKManager.getEMDKManager(
                applicationContext, this
            )
            // Check the return status of getEMDKManager and update the status Text
            // View accordingly
            // Check the return status of getEMDKManager and update the status Text
            // View accordingly
        } catch (e: java.lang.Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        if (results?.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            Toast.makeText(
                this@BarCodeReaderActivity,
                "EMDKManager Request Failed!!!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun scanBarcode() {
        val scanOptions = ScanOptions()
        scanOptions.setPrompt("Scan a barcode. For Turn ON Flash Click Volume Up and Turn OFF Flash Click Volume Down.")
        scanOptions.setBeepEnabled(true)
        scanOptions.setOrientationLocked(true)
        scanOptions.captureActivity = Capture::class.java
        barcodeLauncher.launch(scanOptions)
    }

    private fun getDataFromFirebase() {
        showProgressDialog()
        databaseProductRef.child("ZebraItems").child(date).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                items.clear()
//                for (parent in snapshot.children) {
                for (child in snapshot.children) {
                    child.getValue(Items::class.java)?.let { items.add(it) }
//                    }
                }
                emptyState()
                barcodeItemReaderAdapter.notifyDataSetChanged()
                progressDialog1.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog1.dismiss()
            }

        })
    }

    fun emptyState() {
        if (items.isEmpty()) {
            binding.noRecordFound.visibility = View.VISIBLE
        } else {
            binding.noRecordFound.visibility = View.GONE
        }
    }

    private fun handlingBarcodeReaderData(date: String, result: String) {

        if (flag) {
            handleListLogic(result)
        } else {
            val curDate = getCurrentDateTime()
            val dateInString = curDate.toString("E, dd MMM yyyy")
            if (date == dateInString) {
                handleListLogic(result)
            } else {
                val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
                builder1.setMessage("Date has been changed. Please save all the scanned data and create new sheet.")
                builder1.setCancelable(true)
                builder1.setPositiveButton("Ok") { dialog, id -> dialog.dismiss() }
                val alert11: AlertDialog = builder1.create()
                alert11.show()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.item_done, menu)
        return true
    }

    private fun handleListLogic(result: String) {
        if (items.isNotEmpty()) {
            var flag: Boolean = false
            items.forEachIndexed { index, item ->
                if (item.serialNumber == result) {
                    val pos = items.indexOf(item)
                    item.productCount = item.productCount + 1
                    flag = true
                }
            }
            if (!flag) {
                items.add(Items(1, result))
            }
        } else {
            items.add(Items(1, result))
        }
        emptyState()
        barcodeItemReaderAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_menu_done -> {
                if (flag) {
                    postDataIntoFirebase()
                } else {
                    val curDate = getCurrentDateTime()
                    val dateInString = curDate.toString("E, dd MMM yyyy")
                    if (date == dateInString) {
                        postDataIntoFirebase()
                    } else {
                        val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
                        builder1.setMessage("Date has been changed. Please save all the scanned data and  create new sheet.")
                        builder1.setCancelable(true)
                        builder1.setPositiveButton("Ok") { dialog, id -> dialog.dismiss() }
                        val alert11: AlertDialog = builder1.create()
                        alert11.show()
                    }
                }


                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun postDataIntoFirebase() {
        if(barcodeItemReaderAdapter.getList().isEmpty()) {
          finish()
        }else{
            barcodeItemReaderAdapter.getList().forEach {

                showProgressDialog()
                databaseProductRef.child("ZebraItems").child(date).child(it.serialNumber).setValue(it).addOnCompleteListener {
                    databaseProductRef.child("ZebraSheets").child(date).setValue(DailyData("Zebra Sheet", date, System.currentTimeMillis(), fileId)).addOnCompleteListener {
                        progressDialog1.dismiss()
                        finish()
                    }.addOnFailureListener {
                        progressDialog1.dismiss()
                    }.addOnCanceledListener {
                        progressDialog1.dismiss()
                    }
                }.addOnFailureListener {
                    progressDialog1.dismiss()
                }.addOnCanceledListener {
                    progressDialog1.dismiss()
                }

            }
        }
    }

    private fun showProgressDialog() {
        progressDialog1.setTitle("Loading...")
        progressDialog1.setMessage("Please wait")
        progressDialog1.setCancelable(false)
        progressDialog1.show()
    }

    override fun onItemClickListener(position: Int, view: View, item: Items) {
        if ((view as ConstraintLayout).findViewById<ExpandableLayout>(R.id.expandable).isExpanded) {
            view.findViewById<ExpandableLayout>(R.id.expandable).collapse()
        } else {
            view.findViewById<ExpandableLayout>(R.id.expandable).expand()
        }
    }

    override fun onBackPressed() {
        if (items.isNotEmpty()) {
            val builder1: AlertDialog.Builder = AlertDialog.Builder(this)
            builder1.setMessage("If you add or change the data please click on done to save them. Otherwise on clicking quit all changes will lost.")
            builder1.setCancelable(true)
            builder1.setPositiveButton("OK") { dialog, id ->
                dialog.dismiss()
            }
            builder1.setNegativeButton("Quit") { dialog, id ->
                dialog.dismiss()
                finish()
            }
            val alert11: AlertDialog = builder1.create()
            alert11.show()
        } else {
            finish()
        }
    }

    override fun onOpened(p0: EMDKManager?) {
        this.emdkManager = p0

        try {
            // Call this method to enable Scanner and its listeners
            initializeScanner()
        } catch (e: ScannerException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
        Toast.makeText(
            this@BarCodeReaderActivity,
            "Press Hard Scan Button to start scanning...",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onClosed() {
        if (this.emdkManager != null) {
            this.emdkManager?.release()
            this.emdkManager = null
        }
    }

    override fun onStatus(p0: StatusData?) {
        AsyncStatusUpdate().execute(p0)
    }

    override fun onData(p0: ScanDataCollection?) {
        AsyncDataUpdate().execute(p0)
    }

    override fun onStop() {
        super.onStop()
        try {
            if (scanner != null) {
                // releases the scanner hardware resources for other application
                // to use. You must call this as soon as you're done with the
                // scanning.
                scanner?.removeDataListener(this)
                scanner?.removeStatusListener(this)
                scanner?.disable()
                scanner = null
            }
        } catch (e: ScannerException) {
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    // Method to initialize and enable Scanner and its listeners
    @Throws(ScannerException::class)
    private fun initializeScanner() {
        if (scanner == null) {
            barcodeManager = emdkManager?.getInstance(EMDKManager.FEATURE_TYPE.BARCODE) as BarcodeManager
            scanner = barcodeManager?.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT)
            scanner?.addDataListener(this)
            scanner?.addStatusListener(this)
            scanner?.triggerType = Scanner.TriggerType.HARD
            scanner?.enable()
            scanner?.read()
        }
    }

    var dataLength = 0


    inner class AsyncDataUpdate : AsyncTask<ScanDataCollection, Void, String>() {
        override fun doInBackground(vararg params: ScanDataCollection?): String {
            var statusStr = ""

            try {
                scanner?.read()

                val scanDataCollection = params[0]

                if (scanDataCollection != null && scanDataCollection.result == ScannerResults.SUCCESS) {

                    val scanData: ArrayList<ScanDataCollection.ScanData> = scanDataCollection
                        .scanData
                    for (data in scanData) {
                        val barcodeData = data.data
                        statusStr = barcodeData
                    }
                }

            } catch (e: ScannerException) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            return statusStr;
        }

        override fun onPostExecute(result: String?) {
            if (!result.isNullOrEmpty()) {
                handlingBarcodeReaderData(date, result)
            }
        }

        override fun onPreExecute() {}

        override fun onProgressUpdate(vararg values: Void?) {}
    }

    inner class AsyncStatusUpdate : AsyncTask<StatusData, Void, String>() {

        override fun doInBackground(vararg params: StatusData?): String {
            var statusStr = ""
            val statusData = params[0]
            when (statusData?.state) {
                StatusData.ScannerStates.IDLE -> statusStr = "The scanner enabled and its idle"
                StatusData.ScannerStates.SCANNING -> statusStr = "Scanning.."
                StatusData.ScannerStates.WAITING -> statusStr = "Waiting for trigger press.."
                StatusData.ScannerStates.DISABLED -> statusStr = "Scanner is not enabled"
                else -> {
                    "Please contact to developer!!!"
                }
            }

            return statusStr
        }

        override fun onPostExecute(result: String?) {

        }

        override fun onPreExecute() {}

        override fun onProgressUpdate(vararg values: Void?) {}


    }

    override fun onDestroy() {
        super.onDestroy()
        if (emdkManager != null) {
            emdkManager?.release()
            emdkManager = null
        }
    }
}