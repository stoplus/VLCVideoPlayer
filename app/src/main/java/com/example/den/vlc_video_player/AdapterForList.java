package com.example.den.vlc_video_player;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AdapterForList extends RecyclerView.Adapter<AdapterForList.ViewHolder> {
    private LayoutInflater inflater;    // для загрузки разметки элемента
    private List<String> list;    // коллекция выводимых данных

    public AdapterForList(Context context, List<String> list) {
        this.inflater = LayoutInflater.from(context);
        this.list = new ArrayList<>(list);
    }//AdapterForList

    //внутрений класс ViewHolder для хранения элементов разметки
    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;

        // в конструкторе получаем ссылки на элементы по id
        private ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.adapter_reference_name);

        }//ViewHolder
    }//class ViewHolder

    @Override
    public int getItemCount() {
        return list.size();
    }//getItemCount

    @Override
    public long getItemId(int position) {
        return position;
    }//getItemId

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.adapter_reference, parent, false);
        return new ViewHolder(view);
    } // onCreateViewHolder


    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        holder.name.setText(list.get(position));
    }//onBindViewHolder
    //=================================================================================================

    // Filter Metods
    public void animateTo(List<String> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }//animateTo

    //пишем букву - удаляем лишние Items
    private void applyAndAnimateRemovals(List<String> newModels) {
        for (int i = list.size() - 1; i >= 0; i--) {
            final String model = list.get(i);
            if (!newModels.contains(model)) {
                removeItem(i);
            }//if
        }//for
    }//applyAndAnimateRemovals

    //удаляем букву - добавляем Items
    private void applyAndAnimateAdditions(List<String> newModels) {
        for (int i = 0, count = newModels.size(); i < count; i++) {
            final String model = newModels.get(i);
            if (!list.contains(model)) {
                addItem(i, model);
            }//if
        }//for
    }//applyAndAnimateAdditions

    //присваиваем новые позиции
    private void applyAndAnimateMovedItems(List<String> newModels) {
        for (int toPosition = newModels.size() - 1; toPosition >= 0; toPosition--) {
            final String model = newModels.get(toPosition);
            final int fromPosition = list.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }//if
        }//for
    }//applyAndAnimateMovedItems

    private void removeItem(int position) {
        list.remove(position);
        notifyItemRemoved(position);//обновляем ресайклер при удалении Item
    }//removeItem

    private void addItem(int position, String model) {
        list.add(position, model);
        notifyItemInserted(position);//обновляем ресайклер при вставке Item
    }//addItem

    private void moveItem(int fromPosition, int toPosition) {
        final String model = list.remove(fromPosition);
        list.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);//обновляем ресайклер при перемещении Items
    }//moveItem
}//class AdapterForAdmin