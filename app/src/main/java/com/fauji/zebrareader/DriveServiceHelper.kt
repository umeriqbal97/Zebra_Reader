package com.fauji.zebrareader

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
import java.lang.Exception
import java.util.concurrent.Executors

class DriveServiceHelper(drive: Drive) {

    private val executor = Executors.newSingleThreadExecutor()
    private var drive: Drive = drive

    fun uploadFile(file: java.io.File, fileCont: File, update: String): Task<String> {

        return Tasks.call(executor) {
            val fileContent = FileContent("application/*", file)
            var myfile: File? = null

            try {
                myfile = if (update.isNotEmpty()) {
                    drive.files().update(update, fileCont, fileContent).execute()
                } else {
                    drive.files().create(fileCont, fileContent).execute()
                }
            } catch (e: Exception) {

            }

            if (myfile == null) {
                throw IOException()
            }

            return@call myfile.id
        }
    }

}