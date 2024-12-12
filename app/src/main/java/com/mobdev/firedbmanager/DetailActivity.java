package com.mobdev.firedbmanager;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.util.Calendar;

public class DetailActivity extends AppCompatActivity {

    TextView detailDesc, detailTitle, detailLang;
    ImageView detailImage;
    FloatingActionButton deleteButton, editButton;
    String key = "";
    String imageUrl = "";
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        detailDesc = findViewById(R.id.detailDesc);
        detailImage = findViewById(R.id.detailImage);
        detailTitle = findViewById(R.id.detailTitle);
        detailLang = findViewById(R.id.detailLang);
        deleteButton = findViewById(R.id.deleteButton);
        editButton = findViewById(R.id.editButton);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Redirect to LoginActivity if not logged in
            Intent loginIntent = new Intent(DetailActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
        } else {
            // Get the UID of the logged-in user
            userID = currentUser.getUid();
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            detailDesc.setText(bundle.getString("Description"));
            detailTitle.setText(bundle.getString("Title"));
            detailLang.setText(bundle.getString("Language"));
            key = bundle.getString("Key");
            imageUrl = bundle.getString("Image");

            // Log the key and image URL for debugging
            Log.d("DetailActivity", "Received Key: " + key);
            Log.d("DetailActivity", "Received Image URL: " + imageUrl);

            Glide.with(this).load(bundle.getString("Image")).into(detailImage);
        }

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Verify that key and imageUrl are not null
                if (TextUtils.isEmpty(key) || TextUtils.isEmpty(imageUrl)) {
                    Toast.makeText(DetailActivity.this, "Missing data for deletion", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get the current user's ID
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    Toast.makeText(DetailActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create a progress dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailActivity.this);
                builder.setCancelable(false);
                builder.setView(R.layout.progress_layout);
                AlertDialog dialog = builder.create();
                dialog.show();

                // Delete from database first
                DatabaseReference reference = FirebaseDatabase.getInstance()
                        .getReference("Android Tutorials")
                        .child(userID)
                        .child(key);

                reference.removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Attempt to delete storage image
                        try {
                            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                            storageReference.delete().addOnCompleteListener(task -> {
                                dialog.dismiss();
                                if (task.isSuccessful()) {
                                    Toast.makeText(DetailActivity.this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Log the error, but still consider it a success since database entry is deleted
                                    Log.e("DeleteImage", "Image deletion failed", task.getException());
                                    Toast.makeText(DetailActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                }
                                startActivity(new Intent(DetailActivity.this, MainActivity.class));
                                finish();
                            });
                        } catch (Exception e) {
                            // If storage reference creation fails, still proceed
                            dialog.dismiss();
                            Log.e("DeleteImage", "Storage reference error", e);
                            Toast.makeText(DetailActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(DetailActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();
                        Log.e("DeleteDatabase", "Database deletion failed", e);
                        Toast.makeText(DetailActivity.this, "Failed to delete database record: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DetailActivity.this, UpdateActivity.class)
                        .putExtra("Title", detailTitle.getText().toString())
                        .putExtra("Description", detailDesc.getText().toString())
                        .putExtra("Language", detailLang.getText().toString())
                        .putExtra("Image", imageUrl)
                        .putExtra("Key", key);
                Log.d("UpdateActivity", "Received Key: " + key);
                startActivity(intent);
            }
        });
    }
}