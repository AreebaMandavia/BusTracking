package com.example.bustracking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class carInfoAdapter extends RecyclerView.Adapter<carInfoAdapter.ViewHolder>{

    private ArrayList<Driver>driverList;
    private Context context;

    public carInfoAdapter(Context context, ArrayList<Driver> driverList) {
        this.context = context;
        this.driverList = driverList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.carlayout,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Driver model=driverList.get(position);
        holder.carId.setText(model.getBusId());
        holder.carRoute.setText(model.getRoute());
        holder.carLocation.setText(model.getAddress());
        holder.carLocation.setSelected(true);
    }

    @Override
    public int getItemCount() {
        return driverList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        private TextView carId,carRoute,carLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            carId=itemView.findViewById(R.id.txtcarid);
            carRoute=itemView.findViewById(R.id.txtcarRoute);
            carLocation=itemView.findViewById(R.id.txtcarLocation);

        }
    }
}
