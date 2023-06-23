package com.example.meco.Fragments;

import android.content.Intent;
import android.os.Bundle;


import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.meco.LoginActivity;
import com.example.meco.R;

import com.example.meco.models.CUSTOMER_REQUEST_ACCEPTED_DATA;
import com.google.firebase.auth.FirebaseAuth;



public class CustomerProfile extends Fragment {


    private Button logoutBtn;
    private TextView nameTxtview;
    private TextView emailTxtview, phoneTxtview, accountTypeTxtview;

    String userName, userEmail, userPhone, userType;

    FirebaseAuth auth;
    public CustomerProfile() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_customer_profile, container, false);

        // getting extra data
        Bundle bdl=getArguments();
        userName=bdl.getString("name");
        userEmail=bdl.getString("email");
        userPhone=bdl.getString("phone");
        userType=bdl.getString("accountType");


        // variables initialization
        logoutBtn=view.findViewById(R.id.customer_logout_btn);
        nameTxtview =view.findViewById(R.id.customer_name);
        emailTxtview =view.findViewById(R.id.customer_email);
        phoneTxtview =view.findViewById(R.id.customer_phone);
        accountTypeTxtview =view.findViewById(R.id.customer_account_type);
        auth=FirebaseAuth.getInstance();

        // setting texts
        nameTxtview.setText(userName);
        emailTxtview.setText(userEmail);
        phoneTxtview.setText(userPhone);
        accountTypeTxtview.setText(userType);


        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // resetting customer request accepted
                CUSTOMER_REQUEST_ACCEPTED_DATA.CUSTOMER_REQUEST_ACCEPTED=false;
                CUSTOMER_REQUEST_ACCEPTED_DATA.ongoingService=null;


                Intent i=new Intent(getActivity(), LoginActivity.class);
                startActivity(i);
                getActivity().finish();
                auth.signOut();
            }
        });

        return view;

    }
}