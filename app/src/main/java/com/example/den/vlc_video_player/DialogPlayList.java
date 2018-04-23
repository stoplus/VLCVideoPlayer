package com.example.den.vlc_video_player;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DialogPlayList extends DialogFragment {
    private RecyclerView recyclerView;
    private FragmentActivity current;
    private SelectedVideoInterface datable;
    private String restoreSearchText;     //переменная для сохранения поискового запроса при повороте экрана
    private List<String> filteredList;

    @Override // Метод onAttach() вызывается в начале жизненного цикла фрагмента
    public void onAttach(Context context) {
        super.onAttach(context);
        datable = (SelectedVideoInterface) context;
    } // onAttach

    @NonNull // создание диалога
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        current = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(current);

        //создаем вид
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_reference, null);
        if (savedInstanceState != null){
            restoreSearchText= savedInstanceState.getString("restoreSearchText");
        }
        final List<String> list = getListVodeo();
        EditText searchText = view.findViewById(R.id.searchText);
        recyclerView = view.findViewById(R.id.idRecyclerView);
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getContext(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                    // код для клика по элементу
                    @Override
                    public void onItemClick(View view, int position) {
                        int listPosition = 0;
                        if (filteredList !=null){
                            for (int i = 0; i < list.size(); i++) {
                                if (list.get(i).equals(filteredList.get(position))){
                                    listPosition = i;
                                    break;
                                }
                            }
                        }else listPosition = position;

                        datable.selectVideo(listPosition);
                        dismiss();
                    }//onItemClick

                    //длинное нажатие по элементу
                    @Override
                    public void onLongItemClick(View view, final int position) {

                    }//onLongItemClick
                })//RecyclerItemClickListener
        );


        final AdapterForList adapter = new AdapterForList(current, list);
        recyclerView.setAdapter(adapter);

        searchText.addTextChangedListener(new TextWatcher() {//слушатель изменения текста
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                filteredList = filter(list, editable.toString());//создаем отсортированный список
                adapter.animateTo(filteredList);//вызов анимации и удаление элементов в адаптере
                recyclerView.scrollToPosition(0);
            }
        });


        builder.setTitle("Плейлист")
                .setIcon(R.drawable.hplib_img_btn_playlist)
                .setView(view);//показываем созданный вид

        Dialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.color.colorDialog);

        return dialog;
    } // onCreateDialog

    public List<String> getListVodeo() {
        List<String> list = new ArrayList<>();
        ContentResolver contentResolver = current.getContentResolver();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        if (cursor == null) {
            Toast.makeText(current, "Ошибка", Toast.LENGTH_LONG).show();
        } else if (!cursor.moveToFirst()) {
            Toast.makeText(current, "На устройстве отсутствует видео файлы", Toast.LENGTH_LONG).show();
        } else {
            int dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
            do {
                String name = cursor.getString(dataColumn);
                if (name != null) list.add(name);
            } while (cursor.moveToNext());
        }
        if (cursor != null) cursor.close();
        return list;
    }

    //фильтруем по содержанию запроса в фамилии
    private List<String> filter(List<String> list, String searchText) {
        restoreSearchText = searchText;

        List<String> filteredList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            final String textOne = list.get(i);
            if (textOne.toLowerCase(Locale.getDefault()).contains(searchText.toLowerCase(Locale.getDefault()))) {
                filteredList.add(textOne);//добавляем пользователей в отфильтрованный список
            }//if
        }
        return filteredList;
    }//filter

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("saveSearchText", restoreSearchText);//запоминаем текст поиска
    }
} // class DialogPlayList
