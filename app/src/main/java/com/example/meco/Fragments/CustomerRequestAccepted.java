package com.example.meco.Fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.meco.R;
import com.example.meco.models.CUSTOMER_LOCATION;
import com.example.meco.models.CUSTOMER_REQUEST_ACCEPTED_DATA;
import com.example.meco.models.OngoingService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;


public class CustomerRequestAccepted extends Fragment {


    String mechanic_name, mechanic_id, mechanic_distance, mechanic_phone;
    Double latitude, longitude; // location of mechanic

    TextView txtMechanicName, txtMechanicDistance, btnLocate, btnCall;
    Button btnServiceOver;

    FusedLocationProviderClient fusedLocationProviderClient;

    double curLat=0.0, curLong=0.0;
    double latestMechanicLatitude, latestMechanicLongitude;
    FirebaseFirestore db;
    int PERMISSION_REQUEST_CALL_PHONE=2;
    ListenerRegistration listenerRegistration;


    public CustomerRequestAccepted() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_customer_request_accepted, container, false);

        // variables initialization
        txtMechanicName =view.findViewById(R.id.customer_request_accepted_mechanic_name);
        txtMechanicDistance =view.findViewById(R.id.customer_request_accepted_mechanic_distance);
        btnLocate=view.findViewById(R.id.customer_request_accepted_locate);
        btnCall=view.findViewById(R.id.customer_request_accepted_call);
        btnServiceOver=view.findViewById(R.id.customer_request_accepted_service_over);
        db=FirebaseFirestore.getInstance();

        // initialising data
        Bundle bdl=getArguments();
        mechanic_name= bdl.getString("mechanic_name");
        mechanic_id=bdl.getString("mechanic_id");
        mechanic_phone=bdl.getString("mechanic_phone");


        // showing data
        txtMechanicName.setText(mechanic_name);
//        btnCall.setText(mechanic_phone);

        // disabling locate button. we will enable it only when current location of the customer and mechanic location has been fetched
        btnLocate.setEnabled(false);

        if(checkCallPermission()==false)
        {
            getCallPermission();
        }

        // call to mechanic functionality
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkCallPermission()==true)
                {
                    Intent i=new Intent(Intent.ACTION_CALL);
                    i.setData(Uri.parse("tel:"+mechanic_phone));
                    startActivity(i);
                }
            }
        });



        // when customer clicks on locate button
        btnLocate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomerMaps customerMaps=new CustomerMaps();

                // making the maps button selected
                BottomNavigationView bottomNavigationView=getActivity().findViewById(R.id.customer_bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.customer_map);

                // sending mechanic location and name to maps
                Bundle bdl=new Bundle();
                bdl.putDouble("lat", latestMechanicLatitude);
                bdl.putDouble("lng", latestMechanicLongitude);
                bdl.putString("mechanic_name", mechanic_name);
                bdl.putBoolean("live_location", true);
                bdl.putString("mechanic_id", mechanic_id);
                customerMaps.setArguments(bdl);

                // loading maps
                loadFragment(customerMaps);
            }
        });


        // when customer clicks on service over button
        btnServiceOver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // update the global variables to indicate the ui of the app that there is no active mechanic, so go to home page instead
                CUSTOMER_REQUEST_ACCEPTED_DATA.CUSTOMER_REQUEST_ACCEPTED=false;
                CUSTOMER_REQUEST_ACCEPTED_DATA.ongoingService=null;
//                CUSTOMER_REQUEST_ACCEPTED_DATA.listenerRegistration=null;

                // update ongoing service
                OngoingService ongoingService =new OngoingService("", "", "");
               db.collection("ongoing_services").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).set(ongoingService)
                       .addOnSuccessListener(new OnSuccessListener<Void>() {
                           @Override
                           public void onSuccess(Void unused) {
                                Log.d("MYTAGS", "Mechanic de-assigned from ongoing_service");
                           }
                       })
                       .addOnFailureListener(new OnFailureListener() {
                           @Override
                           public void onFailure(@NonNull Exception e) {
                               Log.w("MYTAGS", "Could not de-assign mechanic from ongoing_service");
                           }
                       });

               // delete record of the customer from accepted_customers
                db.collection("accepted_customers").document(mechanic_id).collection("customers").document(FirebaseAuth.getInstance().getCurrentUser().getUid()).delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.d("MYTAGS", "Customer removed from accepted_customers");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("MYTAGS", "Could not remove customer from accepted_customers");
                            }
                        });

                // bring nearby mechanics page on the screen
//                CustomerHome customerHome=new CustomerHome();
//                loadFragment(customerHome);
                BottomNavigationView customerBottomNavigationView=getActivity().findViewById(R.id.customer_bottom_navigation);
                customerBottomNavigationView.setSelectedItemId(R.id.customer_home);

            }
        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // find current location and location of mechanic and set distance
        findCurrLocationAndDistanceOfMechanic();
    }

    @Override
    public void onPause() {
        super.onPause();

        listenerRegistration.remove(); // mechanic location listener from database
        CUSTOMER_LOCATION.setCustomerLocationInterfaceListener(null); // customer location listener from CUSTOMER_LOCATION
    }

    private void findCurrLocationAndDistanceOfMechanic()
    {

        Context context=getActivity();
        CUSTOMER_LOCATION.setCustomerLocationInterfaceListener(new CUSTOMER_LOCATION.customerLocationInterface() {
            @Override
            public void serveLocation(double curLat, double curLong) {

//                Toast.makeText(context, "Distance updated using customer location", Toast.LENGTH_SHORT).show();
                // finding distance
                if(latestMechanicLatitude!=0.0 || latestMechanicLongitude!=0.0)
                {
                    txtMechanicDistance.setText(String.format("%.2f M Away", calcDistance(curLat, latestMechanicLatitude, curLong, latestMechanicLongitude)*1000 ));

                }

            }
        });



        // now that we have the current location of the customer, keep fetching the location of the mechanic
        listenerRegistration=db.collection("mechanic_locations").document(mechanic_id).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {

                if(error==null)
                {
                    // we got the latest mechanic location
                    if(value!=null && value.exists())
                    {
                        btnLocate.setEnabled(true);

                        Map<String, Object> mechanicLocationData=value.getData();
                        GeoPoint location= (GeoPoint) mechanicLocationData.get("location");  // latest location of the mechanic
                        latestMechanicLatitude=location.getLatitude();
                        latestMechanicLongitude=location.getLongitude();

                        // finding distance
                        txtMechanicDistance.setText(String.format("%.2f M Away", calcDistance(CUSTOMER_LOCATION.curLat, location.getLatitude(), CUSTOMER_LOCATION.curLong, location.getLongitude())*1000 ));

//                        Toast.makeText(getActivity(), "Distance updated using mechanic location", Toast.LENGTH_SHORT).show();
                    }

                }
                else{
                    Log.d("MYTAGS", "Could not fetch mechanic location "+mechanic_id);
                }
            }
        });

        // first get the current location of the customer
//        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(getActivity());
//        try{
//
//            Task<Location> locationResult=fusedLocationProviderClient.getLastLocation();
//            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
//                @Override
//                public void onComplete(@NonNull Task<Location> task) {
//                    if(task.isSuccessful())
//                    {
//
//
//                    }
//                    else{
//                        Log.d("MYTAGS", "Could not fetch current location");
//                    }
//                }
//            });
//
//        }
//        catch(SecurityException e){
//            Log.d("MYTAGS", e.getMessage());
//        }

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

    public void loadFragment(Fragment fragment)
    {
        FragmentManager fragmentManager=getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.customer_screens_container, fragment);
//        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.commit();

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