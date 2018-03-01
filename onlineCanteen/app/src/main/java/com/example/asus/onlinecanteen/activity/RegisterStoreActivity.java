package com.example.asus.onlinecanteen.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.asus.onlinecanteen.R;
import com.example.asus.onlinecanteen.model.Store;
import com.example.asus.onlinecanteen.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class RegisterStoreActivity extends AppCompatActivity {

    private static final String TAG = RegisterActivity.class.getSimpleName();
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 999;

    ImageView imageView;
    Button button, submitbtn;
    private static final int PICK_IMAGE = 100;
    Uri imageUri;
    String profPicUrl;
    FirebaseAuth mAuth;
    FirebaseUser user;
    DatabaseReference StoreReferences;
    private DatabaseReference databaseStore;

    //EditText
    EditText usernameET, passwordET, emailET, openhourET, closehourET, locationET;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(com.example.asus.onlinecanteen.R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_store);

        mAuth = FirebaseAuth.getInstance();

        imageView = findViewById(R.id.storeimageinput);
        button =  findViewById(R.id.storebrowse);
        submitbtn = findViewById(R.id.storeregisterbtn);
        usernameET = findViewById(R.id.storenamefill);
        passwordET = findViewById(R.id.storepasswordfill);
        emailET = findViewById(R.id.storeemailfill);
        openhourET = findViewById(R.id.storeopenhour);
        closehourET = findViewById(R.id.storeclosehour);
        locationET = findViewById(R.id.storelocationfill);


        //Browse Image in Gallery & set as Profile Picture
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        //Submit data for Sign Up & Upload to Storage
        submitbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitData();
            }
        });

        databaseStore = FirebaseDatabase.getInstance().getReference("store");
    }

    private void openGallery(){
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE){
            imageUri = data.getData();
            imageView.setImageURI(imageUri);
        }
    }

    //To submit data
    private void submitData() {

        if(!validateRegisterInfo()) {
            // Field is not filled
            return;
        }

        mAuth.createUserWithEmailAndPassword(emailET.getText().toString(),passwordET.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            if(imageUri != null) {
                                if(ContextCompat.checkSelfPermission(RegisterStoreActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    requestReadStoragePermission();
                                } else {
                                    addAdditionalUserInformation();
                                }
                            } else {
                                addAdditionalUserInformation();
                                backToLoginScreen();
                            }
                        } else {
                            emailET.setText("");
                            emailET.setError("Email is registered");
                            passwordET.setText("");
                        }
                    }
                });
    }

    private void addAdditionalUserInformation() {
        mAuth.signInWithEmailAndPassword(emailET.getText().toString(), passwordET.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()) {
                            user = mAuth.getCurrentUser();

                            UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();
                            profileBuilder.setDisplayName(usernameET.getText().toString());

                            UserProfileChangeRequest profileChangeRequest = profileBuilder.build();
                            user.updateProfile(profileChangeRequest);

                            String uid = user.getUid();
                            Store storeInfo = new Store(usernameET.getText().toString(),openhourET.getText().toString(),closehourET.getText().toString(),locationET.getText().toString());
                            StoreReferences = FirebaseDatabase.getInstance().getReference("store").child(uid);
                            StoreReferences.setValue(storeInfo);

                            if (user != null && imageUri != null &&
                                    ContextCompat.checkSelfPermission(RegisterStoreActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                uploadImage();
                            } else backToLoginScreen();

                        } else {
                            Toast.makeText(RegisterStoreActivity.this, "Failed to update profile.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    //To upload image
    private void uploadImage() {
        Log.d(TAG, "Uploading...");
        StorageReference profileImageRef = FirebaseStorage.getInstance().getReference("profilepics/"+System.currentTimeMillis()+".jpg");
        if (imageUri!=null){
            profileImageRef.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    profPicUrl = taskSnapshot.getDownloadUrl().toString();
                    Log.d(TAG, "Success in uploading");
                    UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();
                    if(profPicUrl != null) {
                        Log.d(TAG, "Photo is taken");
                        profileBuilder.setPhotoUri(Uri.parse(profPicUrl));
                    }
                    UserProfileChangeRequest profileChangeRequest = profileBuilder.build();
                    user.updateProfile(profileChangeRequest);

                    backToLoginScreen();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(),"Image failed to upload",Toast.LENGTH_LONG).show();
                    backToLoginScreen();
                }
            });
        }
    }

    private boolean validateRegisterInfo() {
        boolean valid = true;

        String username = usernameET.getText().toString();
        if(TextUtils.isEmpty(username)) {
            usernameET.setError("Username required");
            valid = false;
        } else {
            usernameET.setError(null);
        }

        String password = passwordET.getText().toString();
        if(TextUtils.isEmpty(password)) {
            passwordET.setError("Password required");
            valid = false;
        } else {
            passwordET.setError(null);
        }

        String email = emailET.getText().toString();
        if(TextUtils.isEmpty(email)) {
            emailET.setError("Email required");
            valid = false;
        } else {
            emailET.setError(null);
        }

        String nim = openhourET.getText().toString();
        if(TextUtils.isEmpty(nim)) {
            openhourET.setError("Open Hour required");
            valid = false;
        } else {
            openhourET.setError(null);
        }

        String phone = closehourET.getText().toString();
        if(TextUtils.isEmpty(phone)) {
            closehourET.setError("Close Hour required");
            valid = false;
        } else {
            closehourET.setError(null);
        }

        return valid;
    }

    //DRAFT
    //private void addUsers() {
    // User user = new User("Jessica","00000013452", "00000013452", "jessicaseaan@gmail.com", "A", null ,"081511030993" );
    // databaseUsers.push().setValue(user);
    //}

    private void requestReadStoragePermission() {
        ActivityCompat.requestPermissions(RegisterStoreActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addAdditionalUserInformation();
            }
        }
    }

    private void backToLoginScreen() {
        if(mAuth != null) {
            mAuth.signOut();
        }
        // GO TO LOGIN PAGE - after success
        Intent intent = new Intent(RegisterStoreActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}