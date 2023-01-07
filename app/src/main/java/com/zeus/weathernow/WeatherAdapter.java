package com.zeus.weathernow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.zeus.weathernow.databinding.WeatherRvItemBinding;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder> {

    private Context context;
    private ArrayList<WeatherModel> weatherModelArrayList;

    public WeatherAdapter(Context context, ArrayList<WeatherModel> weatherModelArrayList) {
        this.context = context;
        this.weatherModelArrayList = weatherModelArrayList;
    }

    @NotNull
    @Override
    public WeatherViewHolder onCreateViewHolder( @NotNull ViewGroup parent, int viewType) {
        return new WeatherViewHolder(LayoutInflater.from(context).inflate(R.layout.weather_rv_item, parent, false));
    }

    @Override
    public void onBindViewHolder( @NotNull WeatherViewHolder holder, int position) {
        WeatherModel weatherModel = weatherModelArrayList.get(position);

        holder.binding.temperatureTV.setText(weatherModel.getTemperature() + "°C");
        Picasso.get().load("http:".concat(weatherModel.getIcon())).into(holder.binding.conditionIV);
        holder.binding.windSpeedTV.setText(weatherModel.getWindSpeed() + "Km/h");

        SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd hh:mm");
        SimpleDateFormat output = new SimpleDateFormat("yyyy-MM-dd hh:mm");

        try{
            Date time= input.parse(weatherModel.getTime());
            holder.binding.timeTV.setText(output.format(time));
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return weatherModelArrayList.size();
    }

    public class WeatherViewHolder extends RecyclerView.ViewHolder {
        WeatherRvItemBinding binding;
        public WeatherViewHolder( @NotNull View itemView) {
            super(itemView);
            binding = WeatherRvItemBinding.bind(itemView);
        }
    }
}
