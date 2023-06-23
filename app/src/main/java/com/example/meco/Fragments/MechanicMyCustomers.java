package com.example.meco.Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.meco.Adapters.MyCustomersAdapter;
import com.example.meco.CustomerMainActivity;
import com.example.meco.R;
import com.example.meco.models.AcceptedCustomers;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;


public class MechanicMyCustomers extends Fragment {


    int PERMISSION_REQUEST_CALL_PHONE=2;
    RecyclerView recyclerMyCustomers;
    TextView noActiveCustomer;
    ArrayList<AcceptedCustomers> acceptedCustomersData= new ArrayList<>();
    FirebaseFirestore db=FirebaseFirestore.getInstance();
    FusedLocationProviderClient fusedLocationProviderClient;
    FirebaseAuth currentMechanic=FirebaseAuth.getInstance();
    double curLat=0.0, curLong=0.0;

    public MechanicMyCustomers() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_mechanic_my_customers, container, false);

        // initializing variables
        recyclerMyCustomers=view.findViewById(R.id.recyclerMyCustomers);
        recyclerMyCustomers.setLayoutManager(new LinearLayoutManager(getActivity()));
        noActiveCustomer=view.findViewById(R.id.no_active_customer);

        // getting call permission
        if(checkCallPermission()==false)
        {
            getCallPermission();
        }

        findCurrLocation();



        return view;
    }

    private void findCurrLocation()
    {

        // first get the current location of the mechanic
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(getActivity());
        try{

            Task<Location> locationResult=fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.isSuccessful())
                    {
                        curLat=task.getResult().getLatitude();
                        curLong=task.getResult().getLongitude();

                        // now that we have the current location of the mechanic, find accepted customers
                        fetchAcceptedCustomers();

                    }
                    else{
                        Log.d("MYTAGS", "Could not fetch current location");
                    }
                }
            });

        }
        catch(SecurityException e){
            Log.d("MYTAGS", e.getMessage());
        }

    }

    private void fetchAcceptedCustomers()
    {
        // fetching accepted data from database and keep listening to it for changes

        db.collection("accepted_customers").document(currentMechanic.getCurrentUser().getUid()).collection("customers")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {

                        acceptedCustomersData.clear();
                        for(QueryDocumentSnapshot doc:queryDocumentSnapshots)
                        {
                            AcceptedCustomers acceptedCustomer= doc.toObject(AcceptedCustomers.class);
                            // setting distance
                            acceptedCustomer.setDistance(String.format("%.2f", calcDistance(curLat, acceptedCustomer.getCustomer_location().getLatitude(), curLong, acceptedCustomer.getCustomer_location().getLongitude())));
                            Log.d("LOCATIONCHANGES", acceptedCustomer.getCustomer_name()+" location changed to "+acceptedCustomer.getCustomer_location().getLongitude()+" "+acceptedCustomer.getCustomer_location().getLongitude());
                            acceptedCustomersData.add(acceptedCustomer);
                        }



                        // now that we have the data, put it into recycler view
                        MyCustomersAdapter myCustomersAdapter=new MyCustomersAdapter(getActivity(), acceptedCustomersData);
                        recyclerMyCustomers.setAdapter(myCustomersAdapter);

                        // if the mechanic has some active customers then dont show the "no customer" message
                        if(acceptedCustomersData.size()!=0)
                        {
                            noActiveCustomer.setVisibility(View.INVISIBLE);
                        }
                        else{
                            noActiveCustomer.setVisibility(View.VISIBLE);
                        }
                    }
                });

    }

    private double calcDistance(double lat1, double lat2, double lon1, double lon2)
    {

        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        // for miles
        double r = 6371;

        // calculate the result
        return(c * r);
    }

    // The code below deals with call permission at run time
    private boolean checkCallPermission()
    {
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE)== PackageManager.PERMISSION_GRANTED)
        {
            Log.d("MYTAGS", "Call permission is already granted");
            return true;
        }
        else return false;

    }
    private void getCallPermission()
    {
        Log.d("MYTAGS", "Requesting for call permission");
        requestPermissions( new String[]{Manifest.permission.CALL_PHONE}, PERMISSION_REQUEST_CALL_PHONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

Log.d("MYTAGS","Permission result");
        if(requestCode==PERMISSION_REQUEST_CALL_PHONE)
        {
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                    Log.d("MYTAGS", "Call permission granted");
            }
        }
        else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }


    }
}