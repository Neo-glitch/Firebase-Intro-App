package com.neo.firebaseapp;


import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * class handles firebase related operation
 */
public class FirebaseUtil {
    private static final String TAG = "FirebaseUtil";

    public static FirebaseDatabase sFirebaseDatabase;
    public static DatabaseReference sDatabaseReference;
    private static FirebaseUtil sFirebaseUtil;
    public static FirebaseStorage sFirebaseStorage;                 // fb storage obj
    public static StorageReference sStorageReference;               // fb storage ref obj

    // var
    public static ArrayList<TravelDeal> sDeals;
    private static ListActivity mCaller;                                                  // activity using this class, needed for firebase ui auth implementation
    private static final int RC_SIGN_IN = 123;
    public static boolean isAdmin;                                                    // to check if logged in user has admin access

    // handles auth in firebase
    public static FirebaseAuth sFirebaseAuth;
    public static FirebaseAuth.AuthStateListener sAuthStateListener;                    // listens for changes in authentication(sign in and sign out)


    // for singleton implementation
    private FirebaseUtil() {
    }

    /**
     * gets ref to child passed as an arg and instantiate the singleton class
     *
     * @param ref
     */
    public static void OpenFbReference(String ref, final ListActivity callerActivity) {
        if (sFirebaseUtil == null) {   // if method has not be called
            sFirebaseUtil = new FirebaseUtil();
            sFirebaseDatabase = FirebaseDatabase.getInstance();
            sFirebaseAuth = FirebaseAuth.getInstance();
            mCaller = callerActivity;

            sAuthStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if (firebaseAuth.getCurrentUser() == null) {          // true if no user is logged in
                        signIn();
                    } else {
                        String userId = firebaseAuth.getUid();              // gets current UserUid
                        checkAdmin(userId);
                    }
                    Toast.makeText(callerActivity.getBaseContext(), "Welcome back", Toast.LENGTH_LONG).show();
                }
            };
        }
        sDeals = new ArrayList<>();
        sDatabaseReference = sFirebaseDatabase.getReference().child(ref);   // gets db ref to arg passed
        connectStorage();
    }

    /**
     * func checks if user logged in has admin priviledges
     * @param userId
     */
    private static void checkAdmin(String userId) {
        FirebaseUtil.isAdmin = false;
        DatabaseReference ref = sFirebaseDatabase.getReference().child("administrators").child(userId);    // gets db ref to uid passed if it's in the administrators node
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {  // called if uid of current user is found in db ref
                FirebaseUtil.isAdmin = true;
                mCaller.showMenu();
                Toast.makeText(mCaller.getBaseContext(), "User is admin", Toast.LENGTH_LONG).show();
                Log.d(TAG, "onChildAdded: user is admin");
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        ref.addChildEventListener(listener);
    }


    /**
     * performs signing In operations
     */
    private static void signIn() {
        // Choose authentication providers i.e email passwd auth and google auth
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());
        // Create and launch sign-in intent
        mCaller.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
    }

    /**
     * attaches authListener
     */
    public static void attachListener() {
        sFirebaseAuth.addAuthStateListener(sAuthStateListener);
    }


    /**
     * detaches authListener
     */
    public static void detachListener() {
        sFirebaseAuth.removeAuthStateListener(sAuthStateListener);
    }

    /**
     * connects app to fb storage ref defined
     */
    public static void connectStorage(){
        sFirebaseStorage = FirebaseStorage.getInstance();
        sStorageReference = sFirebaseStorage.getReference().child("deals_pictures");
    }
}
