package com.example.meco;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.meco.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText name;
    private EditText email;
    private EditText phone;
    private EditText password;
    private EditText confirmPassword;
    private CheckBox showPassword;
    private RadioGroup radioGroup;
    private RadioButton radioButton;
    private int checkedRadioId;
    private Button registerBtn;
    private TextView loginInstead;
    private FirebaseAuth auth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // connecting variables with elements
        name=findViewById(R.id.register_name);
        email=findViewById(R.id.register_email);
        phone=findViewById(R.id.register_phone);
        password=findViewById(R.id.register_password);
        confirmPassword=findViewById(R.id.register_confirm_password);
        showPassword=findViewById(R.id.show_password);
        radioGroup=findViewById(R.id.register_radio_buttons);
        registerBtn=findViewById(R.id.register_button);
        loginInstead=findViewById(R.id.login_instead);
        auth=FirebaseAuth.getInstance();
        db=FirebaseFirestore.getInstance();


        // when the user clicks on login instead button
        loginInstead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i=new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(i);
                finish();
            }
        });

        // when the user clicks on show password checkbox
        showPassword.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

                if(!isChecked)
                {
                    confirmPassword.setInputType(129);
                }
                else
                {
                    confirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                }
            }
        });



        // when the user clicks on register button
        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // GATHERING DATA FROM INPUT FIELDS
                String enteredName=name.getText().toString().trim();
                String enteredEmail=email.getText().toString().trim();
                String enteredPhone=phone.getText().toString().trim();
                String enteredPassword=password.getText().toString();
                String enteredConfirmPassword=confirmPassword.getText().toString();

                // extracting user type from radio buttons
                checkedRadioId= radioGroup.getCheckedRadioButtonId();
                radioButton=findViewById(checkedRadioId);
                String userType=radioButton.getText().toString();

                // FORM VALIDATION

                if(enteredName.isEmpty())
                {
                    Toast.makeText(RegisterActivity.this, "Enter a valid name", Toast.LENGTH_SHORT).show();
                }
                else if(enteredEmail.isEmpty() || !validateEmail(enteredEmail))
                {
                    Toast.makeText(RegisterActivity.this, "Enter a valid email", Toast.LENGTH_SHORT).show();
                }
                else if(enteredPhone.isEmpty())
                {
                    Toast.makeText(RegisterActivity.this, "Enter a valid phone", Toast.LENGTH_SHORT).show();
                }
                else if(enteredPassword.isEmpty() || enteredPassword.length()<6)
                {
                    Toast.makeText(RegisterActivity.this, "Password length should be atleast 6", Toast.LENGTH_SHORT).show();
                }
                else if(!enteredConfirmPassword.equals(enteredPassword))
                {
                    Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                }
                // PROCESS TO CREATE AN ACCOUNT AND ADD USER
                else{

                    Log.d("MYTAGS", enteredName+" "+enteredEmail+" "+enteredPassword+" "+enteredPhone+" "+userType);

                    // registers and creates a new user in database
                    registerUser(enteredName, enteredEmail, enteredPassword, enteredPhone, userType);




                }
            }
        });

    }

    // adds an account in firebase authentication
    private void registerUser(String name, String email, String password, String phone, String userType) {

        // creating an account with email and password
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful())
                {
                    // registration succesful

                    Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                    Toast.makeText(RegisterActivity.this, "", Toast.LENGTH_SHORT).show();

                    // when raw account is created update name
                    updateName(name, email, phone, userType);




                }
                else {
                    Toast.makeText(RegisterActivity.this, "Registration Unsuccessful", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }



    // updates users name
    public void updateName(String name, String email,String phone, String userType)
    {
        firebaseUser= auth.getCurrentUser();
        UserProfileChangeRequest profileUpdates=new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            Log.d("MYTAGS","User name succesfully updated to: "+name);

                            // when account creation and updating name is successful, then create a user in database

                            if(firebaseUser!=null)
                            {
                                Log.d("MYTAGS", "Registration Successful with id: "+firebaseUser.getUid());

                                // adds a user to firestore
                                User user=new User(email, phone, userType, name);
                                addUser(user, firebaseUser.getUid());


                                // when we are done entering user in database, go to login page
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();

                            }

                        }
                        else{
                            Log.d("MYTAGS","User name could not be updated to: "+name);
                        }
                    }
                });
    }

    // adds a new user in firestore
    private void addUser(User user, String userId)
    {

        db.collection("users").document(userId).set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d("MYTAGS", "user entered in database: "+user.getEmail()+" "+userId);

                // create entry in ongoing_services collection in firebase
                if(user.getUserType().equals("Customer"))
                {

                    entryInOngoingServices(firebaseUser.getUid());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("MYTAGS", "user could not be entered in database"+user.getEmail()+" "+userId);
            }
        });
    }

    public void entryInOngoingServices(String customerId)
    {
        Log.d("MYTAGS", "entry in ongoing_services");
        Map<String, Object> mechanic_data=new HashMap<>();
        mechanic_data.put("mechanic_id","");
        mechanic_data.put("mechanic_name", "");
        mechanic_data.put("mechanic_phone", "");

        db.collection("ongoing_services").document(customerId).set(mechanic_data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                {
                    Log.d("MYTAGS", "entryInOngoingServices is successful");
                }
                else{
                    Log.d("MYTAGS", "entryInOngoingServices is unsuccesful");
                }
            }
        });
    }

    // fired when user clicks on a radio button
    public void checkRadioBtn(View v)
    {
//        checkedRadioId= radioGroup.getCheckedRadioButtonId();
//        radioButton=findViewById(checkedRadioId);
//        Toast.makeText(this, ""+radioButton.getText(), Toast.LENGTH_SHORT).show();

    }

    public boolean validateEmail(String emailAddress)
    {
//        https://howtodoinjava.com/java/regex/java-regex-validate-email-address/

        String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
                + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

        Pattern pattern= Pattern.compile(regexPattern);
        Matcher matcher=pattern.matcher((emailAddress));
        return matcher.matches();
    }
}