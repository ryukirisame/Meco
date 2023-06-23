package com.example.meco.Fragments;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.meco.Adapters.CustomerRequestsAdapter;
import com.example.meco.R;
import com.example.meco.models.CustomerRequests;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;


public class MechanicHome extends Fragment {

    RecyclerView recyclerCustomersRequest;
    TextView noCustomerRequests;
    String mechanic_id;
    String mechanic_phone;
    ArrayList<CustomerRequests> customerRequests=new ArrayList<>();

    double curLat=0.0, curLong=0.0;
    FusedLocationProviderClient fusedLocationProviderClient;
    CustomerRequestsAdapter customerRequestsAdapter;
    public static ListenerRegistration listenerRegistration; // since we are listening to requests timely

    Context context;

    MechanicHomeListener mechanicHomeIntefaceListener;
    public interface MechanicHomeListener{
        void serveListener(ListenerRegistration listenerRegistration);
    }
    public void getRealTimeDataListener()
    {
        mechanicHomeIntefaceListener.serveListener(listenerRegistration);
    }

    public MechanicHome() {
        // Required empty public constructor

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(getActivity() instanceof MechanicHome.MechanicHomeListener){
            mechanicHomeIntefaceListener=(MechanicHome.MechanicHomeListener) getActivity();
        }
        else {
            throw new RuntimeException(getActivity().toString()
                    +" must implement CustomerHomeListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_mechanic_home, container, false);

        // getting arguments passed from Mechanic main activity
        Bundle bdl=getArguments();
        if(bdl!=null)
            mechanic_phone=bdl.getString("phone");

        // variables initialization
        recyclerCustomersRequest=view.findViewById(R.id.recyclerCustomersRequest);
        recyclerCustomersRequest.setLayoutManager(new LinearLayoutManager(getActivity()));
        noCustomerRequests =view.findViewById(R.id.no_customer_requests);

        mechanic_id= FirebaseAuth.getInstance().getCurrentUser().getUid();

        findCurrLocation();
//        fetchCustomerRequests();


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

                        // now that we have the current location of the mechanic, find customer requests in real time
                        fetchCustomerRequests();

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
    // fetches customer requests in real time
    private void fetchCustomerRequests()
    {

        // keep fetching customer request in real time
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        listenerRegistration=db.collection("requests").document(mechanic_id).collection("customers").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                if(error!=null)
                {
                    Log.w("MYTAGS", "Error listening to requests "+error);
                    return ;
                }
                else{
                    // we could listen to requests collection

                    // first clear the current array then we will fill again
                    customerRequests.clear();

                    for(QueryDocumentSnapshot doc:value)
                    {

                        CustomerRequests customerRequest= doc.toObject(CustomerRequests.class);

                        // calculate distance
                        String distance=String.format("%.2f",calcDistance(curLat, customerRequest.getCustomer_location().getLatitude(), curLong, customerRequest.getCustomer_location().getLongitude()) );
                        customerRequest.setCustomer_distance(distance);

                        customerRequests.add(customerRequest);
                    }



                    Log.d("MYTAGS", "Customer requests data size: "+customerRequests.size());
                    // setting recycler view adapter
                    customerRequestsAdapter=new CustomerRequestsAdapter(getActivity(),customerRequests, mechanic_phone );
                    recyclerCustomersRequest.setAdapter(customerRequestsAdapter);

                    // if there are some requests then dont show the "no customer" message
                    if(customerRequests.size()!=0)
                    {
                        noCustomerRequests.setVisibility(View.INVISIBLE);
                    }
                    else{
                        noCustomerRequests.setVisibility(View.VISIBLE);
                    }

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

}