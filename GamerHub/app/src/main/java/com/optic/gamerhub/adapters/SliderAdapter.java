package com.optic.gamerhub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.optic.gamerhub.R;
import com.optic.gamerhub.models.SliderItem;
import com.smarteist.autoimageslider.SliderViewAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SliderAdapter extends SliderViewAdapter<SliderAdapter.SliderAdapterVH> {

    // Declaración de variables
    private Context context;
    private List<SliderItem> mSliderItems = new ArrayList<>();

    // Constructor del adaptador
    public SliderAdapter(Context context, List<SliderItem> sliderItems) {
        this.context = context;
        mSliderItems = sliderItems;
    }

    // Método para crear la vista del elemento de la lista
    @Override
    public SliderAdapterVH onCreateViewHolder(ViewGroup parent) {
        View inflate = LayoutInflater.from(parent.getContext()).inflate(R.layout.slider_layout_item, null);
        return new SliderAdapterVH(inflate);
    }

    // Método para enlazar los datos con la vista
    @Override
    public void onBindViewHolder(SliderAdapterVH viewHolder, final int position) {

        SliderItem sliderItem = mSliderItems.get(position);
        if (sliderItem.getImageUrl() != null) {
            if (!sliderItem.getImageUrl().isEmpty()) {
                Picasso.with(context).load(sliderItem.getImageUrl()).into(viewHolder.imageViewSlider);
            }
        }
    }

    // Método para obtener el número total de elementos en el control deslizante
    @Override
    public int getCount() {
        //slider view count could be dynamic size
        return mSliderItems.size();
    }

    // Clase ViewHolder para mantener las vistas de un elemento de la lista en memoria
    class SliderAdapterVH extends ViewHolder {

        View itemView;
        ImageView imageViewSlider;

        // Constructor del ViewHolder
        public SliderAdapterVH(View itemView) {
            super(itemView);
            imageViewSlider = itemView.findViewById(R.id.imageViewSlider);

            this.itemView = itemView;
        }
    }

}