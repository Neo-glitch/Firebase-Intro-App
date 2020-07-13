package com.neo.firebaseapp;


import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.data.model.Resource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private static final String TAG = "DealActivity";

    private FirebaseDatabase mFirebaseDatabase;             // firebase db obj
    private DatabaseReference mDatabaseReference;

    // vars
    TravelDeal mDeal;
    private static final int PICTURE_RESULT = 42;           // code for intent

    // Widgets
    EditText txtTitle;
    EditText txtDescription;
    EditText txtPrice;
    ImageView mImageView;
    private Button mBtnImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);

        mFirebaseDatabase = FirebaseUtil.sFirebaseDatabase;                  // gets the fbDb in the singleton class
        mDatabaseReference = FirebaseUtil.sDatabaseReference;

        txtTitle = findViewById(R.id.txtTitle);
        txtDescription = findViewById(R.id.txtDescription);
        txtPrice = findViewById(R.id.txtPrice);
        mImageView = findViewById(R.id.image);
        mBtnImage = findViewById(R.id.btnImage);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if (deal == null) {     // if got here by not clicking on item in rv
            deal = new TravelDeal();        // create inst of deal obj with no info passed
        }
        this.mDeal = deal;
        txtTitle.setText(mDeal.getTitle());
        txtDescription.setText(mDeal.getDescription());
        txtPrice.setText(mDeal.getPrice());
        showImage(mDeal.getImageUrl());

        mBtnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);          // allows user to selected a type of data and return it
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);         // specifies we receive only data on device
                startActivityForResult(Intent.createChooser(intent,
                        "Insert the picture of choice"), PICTURE_RESULT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICTURE_RESULT && resultCode == RESULT_OK){
            Uri imageUri = data.getData();          // gets uri of image
            final StorageReference ref = FirebaseUtil.sStorageReference.child(imageUri.getLastPathSegment());    // gets the file name from the image uri
            ref.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String imageUri = uri.toString();
                            Log.d(TAG, "onSuccess: uri "+ imageUri);
                            mDeal.setImageUrl(imageUri);                        // sets the imageUri for use
                            String pictureName = taskSnapshot.getStorage().getPath();
                            mDeal.setImageName(pictureName);
                            Log.d(TAG, "onSuccess: pictureName" + pictureName);
                            showImage(imageUri);
                        }
                    });
                }
            });

        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal saved", Toast.LENGTH_LONG).show();
                clean();                 // clears edit after data sent to firebase db
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal deleted", Toast.LENGTH_LONG).show();
                backToList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * saves deal obj to firebase db
     */
    private void saveDeal() {
        mDeal.setTitle(txtTitle.getText().toString());
        mDeal.setDescription(txtDescription.getText().toString());
        mDeal.setPrice(txtPrice.getText().toString());
        Log.d(TAG, "saveDeal: image Url: " + mDeal.getImageUrl());
        if (mDeal.getId() == null) {    // true if the deal not not an existing one and insert new deal to db
            mDatabaseReference.push().setValue(mDeal);    // inserts new obj to db
        } else {
            mDatabaseReference.child(mDeal.getId()).setValue(mDeal);     // updates existing db ref with value
        }
    }

    /**
     * del specified deal from the firebase Db
     */
    private void deleteDeal(){
        if(mDeal == null){    // true if deal not saved in db
            Toast.makeText(this, "Save deal before deleting since deal not present in db", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(mDeal.getId()).removeValue();    // del existing db ref

        if(mDeal.getImageName() != null && mDeal.getImageName().isEmpty() == false){
            // handles deleting of files
            StorageReference pictureRef = FirebaseUtil.sFirebaseStorage.getReference().child(mDeal.getImageName());    // gets ref to fileName
            pictureRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "onSuccess: DeleteD image successfully");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: Error" + e.getMessage());
                }
            });
        }
    }


    /**
     * takes user back to ListActivity after saving or deleting a deal
     */
    private void backToList(){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }


    /**
     * clears editText widget after saving deal to db
     */
    private void clean() {
        txtTitle.setText("");
        txtDescription.setText("");
        txtPrice.setText("");
        txtTitle.requestFocus();   // gives focus to txtTitle widget after clearing editTexts for more values
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);

        if(FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditText(true);
            mBtnImage.setEnabled(true);
        } else{
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditText(false);
            mBtnImage.setEnabled(false);
        }
        return true;
    }

    /**
     * enables or disable editText based on if user is admin or not
     * @param isEnabled
     */
    private void enableEditText(boolean isEnabled){
        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
    }

    /**
     * displays image, whose url is passed as an arg
     * @param url
     */
    private void showImage(String url){
        if(url != null && url.isEmpty() == false){
            // operations for resizing image and showing to user
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;                  // gets width of screen
            Picasso.get()
                    .load(url)
                    .resize(width, width * 2/3)
                    .centerCrop()
                    .into(mImageView);
        }
    }
}

