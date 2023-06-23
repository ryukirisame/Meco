package com.example.meco.Adapters;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.example.meco.Fragments.MechanicMaps;
import com.example.meco.R;
import com.example.meco.models.AcceptedCustomers;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MyCustomersAdapter extends RecyclerView.Adapter<MyCustomersAdapter.ViewHolder> {

    Context context;
    ArrayList<AcceptedCustomers> acceptedCustomersData;
    public MyCustomersAdapter(Context context, ArrayList<AcceptedCustomers> acceptedCustomersData) {
        this.acceptedCustomersData=acceptedCustomersData;
        this.context=context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.accepted_customers_card, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyCustomersAdapter.ViewHolder holder, int position) {

        holder.customerName.setText(acceptedCustomersData.get(position).getCustomer_name());
        holder.customerDistance.setText(acceptedCustomersData.get(position).getDistance()+" KM");
        holder.customerMessage.setText(acceptedCustomersData.get(position).getCustomer_message());

        // when mechanic presses call button
        holder.phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if we have call permission
                if(checkCallPermission()==true)
                {
                    Intent i=new Intent(Intent.ACTION_CALL);
                    i.setData(Uri.parse("tel:"+acceptedCustomersData.get(position).getCustomer_phone()));
                    context.startActivity(i);
                }
                // if we dont have call permission
                else{

                }

            }
        });


        // when mechanic presses locate button then he should be redirected to the map fragment with the location of the current customer
        holder.locateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MechanicMaps mechanicMaps=new MechanicMaps();

                // making the maps button selected
                BottomNavigationView bottomNavigationView=((AppCompatActivity)context).findViewById(R.id.mechanic_bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.mechanic_map);

                // sending mechanic location and name to maps
                Bundle bdl=new Bundle();
                bdl.putDouble("lat", acceptedCustomersData.get(position).getCustomer_location().getLatitude());
                bdl.putDouble("lng", acceptedCustomersData.get(position).getCustomer_location().getLongitude());
                bdl.putString("customer_name", acceptedCustomersData.get(position).getCustomer_name());
                bdl.putString("customer_id", acceptedCustomersData.get(position).getCustomer_id());
                bdl.putBoolean("live_location", true);
                mechanicMaps.setArguments(bdl);

                // loading maps
                loadFragment(mechanicMaps);

            }
        });

    }

    public void loadFragment(Fragment fragment)
    {
        FragmentManager fragmentManager=((AppCompatActivity)context).getSupportFragmentManager();
        FragmentTransaction fragmentTransaction=fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mechanic_screens_container, fragment);

        fragmentTransaction.commit();

    }

    @Override
    public int getItemCount() {
        return acceptedCustomersData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView customerName;
        TextView customerDistance;
        TextView phone;

        TextView locateBtn;
        TextView customerMessage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            customerName=itemView.findViewById(R.id.card_accepted_customer_name);
            customerDistance=itemView.findViewById(R.id.card_accepted_customer_distance);

            locateBtn=itemView.findViewById(R.id.card_accepted_btn_locate);
            customerMessage=itemView.findViewById(R.id.card_accepted_customer_message);
            phone=itemView.findViewById(R.id.card_accepted_btn_phone);

        }
    }

    private boolean checkCallPermission()
    {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)== PackageManager.PERMISSION_GRANTED)
        {
            Log.d("MYTAGS", "Call permission is already granted");
            return true;
        }
        else return false;

    }

}
