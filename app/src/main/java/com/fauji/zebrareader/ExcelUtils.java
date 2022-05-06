package com.fauji.zebrareader;

import static com.fauji.zebrareader.ExtensionFunctionUtilsKt.getCurrentDateTime;
import static com.fauji.zebrareader.MainActivity.driveServiceHelper;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ExcelUtils {
    public static final String TAG = "ExcelUtil";
    private static Cell cell;
    private static Sheet sheet;
    private static Workbook workbook;
    private static CellStyle headerCellStyle;
    private static CellStyle cellContent;
    private static FirebaseStorage storage;
    private static StorageReference storageReference;
    private static String fileName;
    private static FirebaseDatabase firebaseDatabase;
    private static DatabaseReference databaseProductRef;

    private static List<Items> importedExcelData;

//    /**
//     * Import data from Excel Workbook
//     *
//     * @param context - Application Context
//     * @param fileName - Name of the excel file
//     * @return importedExcelData
//     */
//    public static List<Product> readFromExcelWorkbook(Context context, String fileName) {
//        return retrieveExcelFromStorage(context, fileName);
//    }


    /**
     * Export Data into Excel Workbook
     *
     * @param context  - Pass the application context
     * @param fileName - Pass the desired fileName for the output excel Workbook
     * @param dataList - Contains the actual data to be displayed in excel
     */
    public static boolean exportDataIntoWorkbook(Context context, String fileName,
                                                 List<Items> dataList, Boolean upload, String fileID,String date) {
        boolean isWorkbookWrittenIntoStorage;

        // Check if available and not read only
        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            Log.e(TAG, "Storage not available or read only");
            return false;
        }

        // Creating a New HSSF Workbook (.xls format)
        workbook = new HSSFWorkbook();

        setHeaderCellStyle();
        setCellContent();

        // Creating a New Sheet and Setting width for each column
        sheet = workbook.createSheet("Sheet 1");
        sheet.setColumnWidth(0, (15 * 600));
        sheet.setColumnWidth(1, (15 * 600));
//        sheet.setColumnWidth(2, (15 * 600));

        setHeaderRow();
        fillDataIntoExcel(dataList);
        isWorkbookWrittenIntoStorage = storeExcelInStorage(context, fileName, upload, fileID,date);

        return isWorkbookWrittenIntoStorage;
    }

    /**
     * Checks if Storage is READ-ONLY
     *
     * @return boolean
     */
    private static boolean isExternalStorageReadOnly() {
        String externalStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState);
    }

    /**
     * Checks if Storage is Available
     *
     * @return boolean
     */
    private static boolean isExternalStorageAvailable() {
        String externalStorageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(externalStorageState);
    }

    /**
     * Setup header cell style
     */
    private static void setHeaderCellStyle() {
        headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFillForegroundColor(HSSFColor.AQUA.index);
        headerCellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        headerCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    }

    private static void setCellContent() {
        cellContent = workbook.createCellStyle();
        cellContent.setAlignment(CellStyle.ALIGN_CENTER);
    }

    /**
     * Setup Header Row
     */
    private static void setHeaderRow() {
        Row headerRow = sheet.createRow(0);

        cell = headerRow.createCell(0);
        cell.setCellValue("Serial Number");
        cell.setCellStyle(headerCellStyle);

//        cell = headerRow.createCell(1);
//        cell.setCellValue("No of items");
//        cell.setCellStyle(headerCellStyle);

        cell = headerRow.createCell(1);
        cell.setCellValue("Product Info");
        cell.setCellStyle(headerCellStyle);
    }

    /**
     * Fills Data into Excel Sheet
     * <p>
     * NOTE: Set row index as i+1 since 0th index belongs to header row
     *
     * @param dataList - List containing data to be filled into excel
     */
    private static void fillDataIntoExcel(List<Items> dataList) {
        for (int i = 0; i < dataList.size(); i++) {
            Row rowData = sheet.createRow(i + 1);

            // Create Cells for each row
            cell = rowData.createCell(0);
            cell.setCellValue(dataList.get(i).getSerialNumber());
            cell.setCellStyle(cellContent);

//            cell = rowData.createCell(1);
//            cell.setCellValue(dataList.get(i).getProductCount());
//            cell.setCellStyle(cellContent);

            cell = rowData.createCell(1);
            cell.setCellValue(dataList.get(i).getProductInfo());
            cell.setCellStyle(cellContent);
        }
    }

    /**
     * Store Excel Workbook in external storage
     *
     * @param context  - application context
     * @param fileName - name of workbook which will be stored in device
     * @return boolean - returns state whether workbook is written into storage or not
     */
    private static boolean storeExcelInStorage(Context context, String fileName, Boolean upload, String fileID, String date) {
        boolean isSuccess;
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString(), fileName);

        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(file);
            workbook.write(fileOutputStream);
            Log.e(TAG, "Writing file" + file);

            if (upload) {

                ProgressDialog progressDialog = new ProgressDialog(context);
                progressDialog.setTitle("Progressing...");
                progressDialog.setMessage("Please wait");
                progressDialog.setCancelable(false);
                progressDialog.show();

                com.google.api.services.drive.model.File contentFile = new com.google.api.services.drive.model.File();
                contentFile.setName(fileName);
                driveServiceHelper.uploadFile(file, contentFile, fileID).addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String s) {
                        firebaseDatabase = FirebaseDatabase.getInstance();
                        databaseProductRef = firebaseDatabase.getReference("InventoryDB");
                        databaseProductRef.child("ZebraSheets").child(date).child("fileId").setValue(s);
                        progressDialog.dismiss();
                        Toast.makeText(context, "Successfully Uploaded", Toast.LENGTH_LONG).show();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Error", Toast.LENGTH_LONG).show();
                    }
                });

//                storage = FirebaseStorage.getInstance();
//                storageReference = storage.getReference(fileName);
//
//                UploadTask uploadTask = storageReference.putFile(Uri.fromFile(file));
//
//                uploadTask.addOnProgressListener(taskSnapshot -> {
//                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
//                    System.out.println("Upload is " + progress + "% done");
//                }).addOnPausedListener(taskSnapshot -> {
//                    progressDialog.setMessage("Upload is paused");
//                    System.out.println("Upload is paused");
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception exception) {
//                        progressDialog.dismiss();
//                    }
//                }).addOnSuccessListener(taskSnapshot -> {
//                    progressDialog.dismiss();
//                    Toast.makeText(context, "Successfully Download", Toast.LENGTH_LONG).show();
//                });
            }

            isSuccess = true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing Exception: ", e);
            isSuccess = false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save file due to Exception: ", e);
            isSuccess = false;
        } finally {
            try {
                if (null != fileOutputStream) {
                    fileOutputStream.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return isSuccess;
    }

//    /**
//     * Retrieve excel from External Storage
//     *
//     * @param context  - application context
//     * @param fileName - name of workbook to be read
//     * @return importedExcelData
//     */
//    private static List<Product> retrieveExcelFromStorage(Context context, String fileName) {
//        importedExcelData = new ArrayList<>();
//
//        File file = new File(context.getExternalFilesDir(null), fileName);
//        FileInputStream fileInputStream = null;
//
//        try {
//            fileInputStream = new FileInputStream(file);
//            Log.e(TAG, "Reading from Excel" + file);
//
//            // Create instance having reference to .xls file
//            workbook = new HSSFWorkbook(fileInputStream);
//
//            // Fetch sheet at position 'i' from the workbook
//            sheet = workbook.getSheetAt(0);
//
//            // Iterate through each row
//            for (Row row : sheet) {
//                int index = 0;
//                List<String> rowDataList = new ArrayList<>();
//                List<Product.PhoneNumber> phoneNumberList = new ArrayList<>();
//
//                if (row.getRowNum() > 0) {
//                    // Iterate through all the columns in a row (Excluding header row)
//                    Iterator<Cell> cellIterator = row.cellIterator();
//
//                    while (cellIterator.hasNext()) {
//                        Cell cell = cellIterator.next();
//                        // Check cell type and format accordingly
//                        switch (cell.getCellType()) {
//                            case Cell.CELL_TYPE_NUMERIC:
//
//                                break;
//                            case Cell.CELL_TYPE_STRING:
//                                rowDataList.add(index, cell.getStringCellValue());
//                                index++;
//                                break;
//                        }
//                    }
//
//                    // Adding cells with phone numbers to phoneNumberList
//                    for (int i = 1; i < rowDataList.size(); i++) {
//                        phoneNumberList.add(new Product.PhoneNumber(rowDataList.get(i)));
//                    }
//
//                    /**
//                     * Index 0 of rowDataList will Always have name.
//                     * So, passing it as 'name' in ContactResponse
//                     *
//                     * Index 1 onwards of rowDataList will have phone numbers (if >1 numbers)
//                     * So, adding them to phoneNumberList
//                     *
//                     * Thus, importedExcelData list has appropriately mapped data
//                     */
//
//                    importedExcelData.add(new Product(rowDataList.get(0), phoneNumberList));
//                }
//
//            }
//
//        } catch (IOException e) {
//            Log.e(TAG, "Error Reading Exception: ", e);
//
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to read file due to Exception: ", e);
//
//        } finally {
//            try {
//                if (null != fileInputStream) {
//                    fileInputStream.close();
//                }
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }
//
//        return importedExcelData;
//    }

}