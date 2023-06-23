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

import com.example.meco.Adapters.MechanicsNearbyAdapter;
import com.example.meco.R;
import com.example.meco.models.CUSTOMER_LOCATION;
import com.example.meco.models.MechanicsNearby;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class CustomerHome extends Fragment {

    String customerId;
    RecyclerView recyclerMechanicsNearby;
    MechanicsNearbyAdapter mechanicsNearbyAdapter;
    FusedLocationProviderClient fusedLocationProviderClient;
    FirebaseFirestore db;

    ArrayList<MechanicsNearby> mechanicsNearbyData=new ArrayList<>();
    CustomerHomeListener interfaceListener;
    ListenerRegistration fetchNearbyMechanicsListener;

    double curLat=0.0, curLong=0.0;
    GeoPoint customerLocation;
    Timer timer = new Timer();
    TextView txtNoNearbyMechanics;
    public CustomerHome() {
        // Required empty public constructor

    }

    public interface CustomerHomeListener{
        void serveMechanicsNearbyData(ArrayList<MechanicsNearby> mechanicsNearbyData);
    }

    public void fetchMechanicsNearbyData()
    {
        interfaceListener.serveMechanicsNearbyData(mechanicsNearbyData);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(context instanceof CustomerHomeListener){
            interfaceListener=(CustomerHomeListener) context;
        }
        else {
            throw new RuntimeException(context.toString()
                    +" must implement CustomerHomeListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // dont make it null. cuz it will detach the activity from customerhome fragment when customerhome is not on screen.
        // but when we are on map screen and user again presses map button, then we still need data from customerhome. that is why
        // we need a constant connection of customermainactivity with customerhome fragment even though customerhome is not on the screen

//        interfaceListener=null;

        fetchNearbyMechanicsListener.remove();
        Log.d("MYTAGS", "stopped listening to changes in mechanics locations from customer side");



    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {



        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_customer_home, container, false);

        recyclerMechanicsNearby=view.findViewById(R.id.recyclerMechanicsNearby);
        recyclerMechanicsNearby.setLayoutManager(new LinearLayoutManager(getActivity()));

        txtNoNearbyMechanics=view.findViewById(R.id.no_mechanics_nearby);

        // getting arguments passed from customer main activity
        Bundle bdl=getArguments();
        if(bdl!=null)
        {
            customerId= bdl.getString("customerId");
        }

        // find current location of the user
        findCurrLocation();



        return view;
    }



    // finds the current location of the user
    private void findCurrLocation()
    {

        // first get the current location of the user
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(getActivity());
        try{

            Task<Location> locationResult=fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if(task.isSuccessful())
                    {
                        if(task!=null)
                        {
                            CUSTOMER_LOCATION.setLocation(task.getResult().getLatitude(),task.getResult().getLongitude());

                            customerLocation=new GeoPoint(CUSTOMER_LOCATION.curLat, CUSTOMER_LOCATION.curLong);
                            // now that we have the current location of the user, find nearby mechanics
                            fetchNearbyMechanicsFromFireStore(customerLocation);
                        }




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

    // keep fetching nearby mechanics data from firebase
    public void fetchNearbyMechanicsFromFireStore(GeoPoint customerLocation)
    {

        db=FirebaseFirestore.getInstance();
        CollectionReference mechanicsNearbyRef=db.collection("mechanic_locations");

        fetchNearbyMechanicsListener=mechanicsNearbyRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                if(error==null)
                {
                    mechanicsNearbyData.clear();

                    for(QueryDocumentSnapshot doc:value)
                    {
                        MechanicsNearby mechanicNearby=doc.toObject(MechanicsNearby.class); // converting data from firebase to object

                        // calculating distance and setting distance
                        double distance= calcDistance(CUSTOMER_LOCATION.curLat, mechanicNearby.getLocation().getLatitude(), CUSTOMER_LOCATION.curLong, mechanicNearby.getLocation().getLongitude());
                        mechanicNearby.setDistance(distance);

                        // settting id of mechanic
                        String id=doc.getId();
                        mechanicNearby.setMechanic_id(id);


                        // finally add a mechanic data into the array list if the distance is less than 1.5 Km
                        if(distance<1.5)
                            mechanicsNearbyData.add(mechanicNearby);
                    }

                    if(mechanicsNearbyData.size()==0)
                    {
                        txtNoNearbyMechanics.setVisibility(View.VISIBLE);
                    }
                    else{
                        txtNoNearbyMechanics.setVisibility(View.INVISIBLE);
                    }

                    // setting recycler view adapter
                    mechanicsNearbyAdapter=new MechanicsNearbyAdapter(getActivity(),  mechanicsNearbyData, customerId, customerLocation);
                    recyclerMechanicsNearby.setAdapter(mechanicsNearbyAdapter);

//                    mechanicsNearbyAdapter.notifyDataSetChanged();
                    Log.d("MYTAGS", "Fetched nearby mechanics");
                }
                else{
                    Log.d("MYTAGS", "listening to mechanic locations from customer side failed");
                }


            }
        });



//        mechanicsNearbyRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                if(task.isSuccessful())
//                {
////                    Log.d("MYTAGS", "previous data size is:"+mechanicsNearbyData.size());
//                    mechanicsNearbyData.clear();
//                    for(QueryDocumentSnapshot doc:task.getResult())
//                    {
//
//                        MechanicsNearby mechanicNearby=doc.toObject(MechanicsNearby.class); // converting data from firebase to object
//
//                        // calculating distance and setting distance
//                        double distance= calcDistance(curLat, mechanicNearby.getLocation().getLatitude(), curLong, mechanicNearby.getLocation().getLongitude());
//                        mechanicNearby.setDistance(distance);
//
//                        // settting id of mechanic
//                        String id=doc.getId();
//                        mechanicNearby.setMechanic_id(id);
//
//                        // finally add a mechanic data into the array list if the distance is less than 1.0Km
//                        if(distance<1.5)
//                            mechanicsNearbyData.add(mechanicNearby);
//                    }
//
//                    // setting recycler view adapter
//                    mechanicsNearbyAdapter=new MechanicsNearbyAdapter(getActivity(),  mechanicsNearbyData, customerId, customerLocation);
//                    recyclerMechanicsNearby.setAdapter(mechanicsNearbyAdapter);
//
//
//                }
//                else{
//                    Log.d("MYTAGS", "could not fetch nearby mechanics");
//                }
//            }
//        });

    }




    public double calcDistance(double lat1, double lat2, double lon1, double lon2)
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