package com.example.uploadpdf;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {
    Button selectFile, upload;
    TextView notification;

    FirebaseStorage storage;//used to uploading files...ex:pdf
    FirebaseDatabase database;//used to store URLs of uploaded files...
    
    Uri pdfUri;//uri are actually url
    
    ProgressDialog progressDialog;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = FirebaseStorage.getInstance();//return an object of firebase storage
        database = FirebaseDatabase.getInstance();//return an object of firebase database

        selectFile =(Button) findViewById(R.id.selectFile);
        upload =(Button) findViewById(R.id.upload);

        notification =  findViewById(R.id.notification);

        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                {
                    selectPDF();
                }
                else
                {
                    //if permission not granted we will ask user to grant permission
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 9);
                }
            }

        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (pdfUri!=null){
                uploadFile(pdfUri);
            }else {
                    Toast.makeText(MainActivity.this,"Select a file",Toast.LENGTH_SHORT).show();
                }
        }

            private void uploadFile(Uri pdfUri) {

                progressDialog =new ProgressDialog(MainActivity.this);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setTitle("Uploading file..");
                progressDialog.setProgress(0);
                progressDialog.show();
                
                final String filename=System.currentTimeMillis()+"";
                StorageReference storageReference=storage.getReference();//returns root path
                storageReference.child("Uploads").child(filename).putFile(pdfUri)//create upload folder in Firebase storage
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {//for acknowledgment for file uploaded or not
                            @Override
                            public void onSuccess(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                                String url = taskSnapshot.getStorage().getDownloadUrl().toString();//returns the url of the uploaded file
                                //store the url  in realtime database
                                DatabaseReference reference = database.getReference();
                                reference.child(filename).setValue(url).addOnCompleteListener(new OnCompleteListener<Void>() {// check url successfully uploaded or not
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if ((task.isSuccessful())) {
                                            Toast.makeText(MainActivity.this, "File successfully uploaded...", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(MainActivity.this, "File not successfully uploaded...", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,"File is not successfully uploaded...",Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        //track progress of = our upload
                        int currentProgress=(int) (100*taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                        progressDialog.setProgress(currentProgress);
                    }
                });
            }
            });
        
    }

    //permission encode by onRequestPermissionsResult
    //check request code is 9 or not
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode==9 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            selectPDF();
            
        }else {
            Toast.makeText(MainActivity.this,"Please provide permission..",Toast.LENGTH_SHORT).show();
        }
    }


    private void selectPDF() {
        
        //to offer user to select a file using file manager..
        Intent intent=new Intent();
        intent.setType("application/PDF");
        intent.setAction(Intent.ACTION_GET_CONTENT);//intent is wait to fetch files
        startActivityForResult(intent,86);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
       //check whether it has selected file or not
        if (requestCode==86 && resultCode==RESULT_OK && data!=null){
            pdfUri=data.getData();
            notification.setText("a  file is selected:"+data.getData().getLastPathSegment());
        }else {
            Toast.makeText(MainActivity.this,"Please select the file...",Toast.LENGTH_SHORT).show();
        }
    }
}
