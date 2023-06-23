package com.example.meco;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.meco.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {

    private TextView forgotPassBtn;
    private EditText emailInput;
    private EditText passInput;
    private Button loginBtn;
    private TextView registerBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        forgotPassBtn=findViewById(R.id.forgot_password);
        emailInput=findViewById(R.id.login_email);
        passInput=findViewById(R.id.login_password);
        loginBtn=findViewById(R.id.login_button);
        registerBtn=findViewById(R.id.register_instead);

        auth= FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();


        // when user clicks on login button
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String enteredEmail=emailInput.getText().toString().trim();
                String enteredPass=passInput.getText().toString();

                // some basic validation
                if(enteredEmail.isEmpty())
                {
                    Toast.makeText(LoginActivity.this, "Enter a valid email", Toast.LENGTH_SHORT).show();
                }
                else if(enteredPass.isEmpty() || enteredPass.length()<6)
                {
                    Toast.makeText(LoginActivity.this, "Enter a valid password", Toast.LENGTH_SHORT).show();
                }
                else{

                    // now sign in with email and password
                    auth.signInWithEmailAndPassword(enteredEmail, enteredPass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            // if sign in is successful
                            if(task.isSuccessful())
                            {
                                // show a hi message
                                user=auth.getCurrentUser();
                                Toast.makeText(LoginActivity.this, "Hi "+user.getDisplayName(), Toast.LENGTH_SHORT).show();

                                // move to the respective activity
                                DocumentReference docRef=db.collection("users").document(user.getUid());
                                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if(task.isSuccessful())
                                        {
                                            DocumentSnapshot doc=task.getResult();
                                            if(doc.exists())
                                            {
                                                Log.d("MYTAGS", "CURRENTLY SIGNED IN USER DATA: "+doc.getData());

                                                User user=doc.toObject(User.class);

                                                // putting user data into a bundle
                                                Bundle bdl=new Bundle();
                                                ArrayList<String> userData=new ArrayList<>();
                                                userData.add(user.getName());
                                                userData.add(user.getEmail());
                                                userData.add(user.getPhone());
                                                userData.add(user.getUserType());
                                                userData.add(auth.getCurrentUser().getUid());
                                                bdl.putStringArrayList("userData",userData);

                                                String userType=user.getUserType();
                                                if(userType.equals("Mechanic"))
                                                {
                                                    Intent i=new Intent(LoginActivity.this, MechanicMainActivity.class);
                                                    i.putExtras(bdl);
                                                    startActivity(i);
                                                    finish();
                                                }
                                                else if(userType.equals("Customer")){
                                                    Intent i=new Intent(LoginActivity.this, CustomerMainActivity.class);
                                                    i.putExtras(bdl);
                                                    startActivity(i);
                                                    finish();
                                                }
                                                else{
                                                    Log.d("MYTAGS", "Invalid user type");
                                                }
                                            }
                                            else Log.d("MYTAGS", "no such document");
                                        }
                                    }
                                });
                            }
                            else{
                                Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }



            }
        });


        // Register instead button logic
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
                finish();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        user= auth.getCurrentUser();

        // if a user is already logged in then get the users data in database and find out the user type, and respectively openly their main acitivities
        if(user!=null)
        {
            Log.d("MYTAGS", "Logged in: "+user.getDisplayName()+" "+user.getEmail()+" "+user.getUid());
            Toast.makeText(this, "Hi "+user.getDisplayName(), Toast.LENGTH_SHORT).show();
            DocumentReference docRef=db.collection("users").document(user.getUid());
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful())
                    {
                        DocumentSnapshot doc=task.getResult();
                        if(doc.exists())
                        {
                            Log.d("MYTAGS", "CURRENTLY SIGNED IN USER DATA: "+doc.getData());

                            User user=doc.toObject(User.class);

                            // putting user data into a bundle
                            Bundle bdl=new Bundle();
                            ArrayList<String> userData=new ArrayList<>();
                            userData.add(user.getName());
                            userData.add(user.getEmail());
                            userData.add(user.getPhone());
                            userData.add(user.getUserType());
                            userData.add(auth.getCurrentUser().getUid());
                            bdl.putStringArrayList("userData",userData);

                            String userType=user.getUserType();
                            if(userType.equals("Mechanic"))
                            {
                                Intent i=new Intent(LoginActivity.this, MechanicMainActivity.class);
                                i.putExtras(bdl);
                                startActivity(i);
                                finish();
                            }
                            else if(userType.equals("Customer")){
                                Intent i=new Intent(LoginActivity.this, CustomerMainActivity.class);
                                i.putExtras(bdl);
                                startActivity(i);
                                finish();
                            }
                            else{
                                Log.d("MYTAGS", "Invalid user type");
                            }
                        }
                        else Log.d("MYTAGS", "no such document");
                    }
                }
            });
        }
    }
}
















